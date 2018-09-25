package stephenzoio.freesourcing.backend

import cats.effect.IO
import cats.~>
import stephenzoio.freesourcing.model.auction.command.{CommandFree, CommandOp}

trait ServiceContext {
  type IOE[A] = IO[Either[Throwable, A]]

  final case class SC(c2IO: CommandOp ~> IO)

  implicit class CommandFreeOps[A](cf: CommandFree[A]) {
    def toIO(scIo: IO[SC]): IO[A] =
      for {
        sc <- scIo
        x  <- cf.foldMap(sc.c2IO)
      } yield x
  }
}

package object service extends ServiceContext with HttpExtensions {}
