package stephenzoio.freesourcing.shared.syntax

import cats.effect.IO
import stephenzoio.freesourcing.shared.util.Attempt

trait IOSyntax {
  implicit class IOAttemptOps[A](taskAttempt: IO[Attempt[A]]) {
    def unattempt: IO[A] = taskAttempt.flatMap(_.fold(IO.raiseError, IO.pure))
  }

  implicit class IOAttemptOptionOps[A](taskAttempt: IO[Attempt[Option[A]]]) {
    def unoption(errorIfNone: => Throwable): IO[Attempt[A]] = taskAttempt.map {
      case Right(None)    => Left(errorIfNone)
      case Right(Some(a)) => Right(a)
      case Left(l)        => Left(l)
    }
  }

  implicit class IOOptionOps[A](taskOption: IO[Option[A]]) {
    def unoption(errorIfNone: => Throwable): IO[A] = taskOption.flatMap {
      case Some(a) => IO.pure(a)
      case None    => IO.raiseError(errorIfNone)
    }
  }
}
