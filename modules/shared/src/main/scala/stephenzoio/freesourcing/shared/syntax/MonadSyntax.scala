package stephenzoio.freesourcing.shared.syntax

import cats.MonadError
import stephenzoio.freesourcing.shared.util.Attempt

trait MonadSyntax {
  implicit class AttemptOps[F[_], A, B](f: F[Attempt[B]])(implicit m: MonadError[F, Throwable]) {
    def unattempt: F[B] =
      m.flatMap(f)(attempt => attempt.fold[F[B]](e => m.raiseError[B](e), b => m.pure[B](b)))
  }
}
