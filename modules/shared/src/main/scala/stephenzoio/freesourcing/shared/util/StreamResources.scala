package stephenzoio.freesourcing.shared.util

import java.util.concurrent.Executors

import cats.effect._
import fs2.Scheduler

import scala.concurrent.ExecutionContext

object StreamResources {
  implicit val scheduler: Scheduler =
    Scheduler.fromScheduledExecutorService(Executors.newScheduledThreadPool(8))
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val timer: Timer[IO]     = IO.timer(scala.concurrent.ExecutionContext.Implicits.global)
}
