package stephenzoio.freesourcing.shared.util

import java.util.Calendar

import cats._
import cats.effect._
import cats.implicits._
import fs2._
import fs2.async.mutable.Signal
import cats.implicits._

import scala.concurrent.duration._

object StreamUtils {
  import stephenzoio.freesourcing.shared.util.StreamResources._

  def printLine(prefix: String, message: String): Unit =
    println(s"$prefix: (${Calendar.getInstance.getTime.toInstant}): ${message.toString}")

  def log[A](prefix: String): Pipe[IO, A, A] = _.map { a =>
    printLine(prefix, "")
    a
  }

  def logValue[F[_], A: Show](prefix: String, maxLen: Int = 120): Pipe[F, A, A] = _.map { a =>
    val aString     = implicitly[Show[A]].show(a)
    val printString = if (aString.length > maxLen) aString.substring(0, maxLen) + "..." else aString
    printLine(prefix, printString)
    a
  }

  private val baseMillis      = System.currentTimeMillis()
  private def currentMillis() = System.currentTimeMillis() - baseMillis

  def defaultBackoffStrategy(timeList: List[Long], lastDelay: Long): Long = timeList match {
    case tminus1 :: tminus2 :: _ if (tminus1 - tminus2) < 30000 =>
      val delay = Math.min(Math.max(lastDelay, 500L) * 2L, 5L * 50L * 1000L)
      Console.println(s"Time gap ${tminus1 - tminus2} - Delay: $delay ms")
      delay
    case _ => 0L
  }

  def repeatedIO[B](taskB: => IO[B], delay: FiniteDuration): Stream[IO, B] = {
    Stream.repeatEval[IO, B](IO.sleep(delay).flatMap(_ => taskB))
  }

  def eitherHaltBoth[F[_], I, I2](implicit e: Effect[F]): Pipe2[F, I, I2, Either[I, I2]] =
    (s1, s2) => s1.map(Left(_)) mergeHaltBoth s2.map(Right(_))

  def terminateIfStale[A](eventStream: Stream[IO, A], timeout: FiniteDuration) = {
    val timerStream: Stream[IO, Unit] = repeatedIO(IO(()), timeout)
    timerStream
      .eitherHaltBoth(eventStream)
      .zipWithPrevious
      .map[Either[Boolean, A]] {
        case (Some(Left(_)), Left(_)) => Left(false)
        case (_, Left(_))             => Left(true)
        case (_, Right(r))            => Right(r)
      }
      .takeWhile {
        case (Left(false)) => false
        case (_)           => true
      }
      .collect {
        case Right(r) => r
      }
  }

  def repeatedStream[A](streamCreator: => Stream[IO, A],
                        delayStrategy: (List[Long], Long) => Long,
                        paused: Signal[IO, Boolean],
                        streamName: String): Stream[IO, A] = {
    val streamOfStreams = Stream
      .eval[IO, Long](IO(currentMillis()))
      .repeat
      .zip(paused.continuous)
      .evalMap[(Long, Boolean)] {
        case (timeVal, true) => timer.sleep(10.seconds) >> IO { (timeVal, true) }
        case x               => IO(x)
      }
      .filter(_._2 == false)
      .map(_._1)
      .scan((List.empty[Long], 0L)) {
        case ((previousTimeList, previousDelay), newTime) =>
          val delay        = delayStrategy(previousTimeList, previousDelay)
          val shiftedTimes = (newTime :: previousTimeList).map(_ + delay)
          (shiftedTimes, delay)
      }
      .drop(1)
      .through(logValue("TimesAndDelays"))
      .evalMap[Stream[IO, A]] {
        case (_, delay) =>
          IO { Thread.sleep(delay / 1000) } >> IO { streamCreator.interruptWhen(paused) }
      }
      .through(log("Creating new stream..."))

    Stream.bracket[IO, Unit, A](IO.pure(()))(_ => streamOfStreams.join(1),
                                             _ => IO(println(s"Stream $streamName terminated.")))
  }

  def sequenceStream[F[_], A](ordering: A => Long, maxBufferLen: Int): Pipe[F, A, A] =
    stream =>
      stream
        .scan((-1L, List.empty[(Long, A)], true)) {
          case ((lastEmitIndex, lastList, previousEmitted), a) =>
            val currentList = if (!previousEmitted) lastList else List.empty
            val index       = ordering(a)
            if (index <= lastEmitIndex) (lastEmitIndex, currentList, previousEmitted)
            else {
              val sorted = ((index, a) :: currentList).sortBy(_._1)
              val emit = (sorted.length == maxBufferLen) ||
                (lastEmitIndex == -1L || sorted.head._1 == lastEmitIndex + 1) &&
                  (sorted.last._1 - sorted.head._1 == sorted.length - 1)
              val newEmitIndex = if (emit) sorted.last._1 else lastEmitIndex
              if (emit && sorted.length > 1)
                println("emitting buffered sequence: " + sorted)
              (newEmitIndex, sorted, emit)
            }
        }
        .collect { case (_, sorted, emit) if emit => sorted.map(_._2) }
        .flatMap(list => Stream.emits(list))

  def throttleStream[A](inputStream: fs2.Stream[IO, A], waitTime: FiniteDuration) = {
    final case class ST(now: Boolean = true, next: Boolean = true)

    Stream.bracket[IO, Signal[IO, ST], A](fs2.async.signalOf[IO, ST](ST()))(
      signal => {
        val streamWithSignals: Stream[IO, A] = inputStream.evalMap[A](a => {
          signal
            .modify {
              case ST(true, true) =>
                ST(true, false)
              case _ =>
                ST(false, false)
            }
            .flatMap { _ =>
              signal.get.map {
                case ST(true, false) =>
                  (timer.sleep(waitTime) >> signal.set(ST(true, true))).unsafeRunSync()
                  a
                case _ => a
              }
            }
        })

        (streamWithSignals zip signal.continuous).filter(_._2.now).map(_._1)

      },
      _ => IO { () }
    )
  }

  implicit class StreamOps[A](inputStream: Stream[IO, A]) {
    def throttle(waitTime: FiniteDuration): Stream[IO, A] = {
      StreamUtils.throttleStream(inputStream, waitTime)
    }

    def terminateIfStale(timeout: FiniteDuration) = {
      StreamUtils.terminateIfStale(inputStream, timeout)
    }

    def eitherHaltBoth[B](s2: Stream[IO, B]): Stream[IO, Either[A, B]] =
      inputStream.through2(s2)(StreamUtils.eitherHaltBoth)
  }
}
