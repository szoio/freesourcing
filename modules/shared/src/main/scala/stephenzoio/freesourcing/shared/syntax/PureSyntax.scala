package stephenzoio.freesourcing.shared.syntax

import cats.Applicative
import cats.data.EitherT
import cats.free.Free
import cats.implicits._

trait PureSyntax {
  implicit class PureOps[A](a: A) {
    def pureF[F[_]]: Free[F, A] = Free.pure[F, A](a)
    def left[B]: Either[A, B]   = Left[A, B](a)
    def right[B]: Either[B, A]  = Right[B, A](a)
  }

  implicit class EitherOps[A, B](inst: Either[A, B]) {
    def toRight(f: A => B): Either[A, B] = inst.fold(l => Right[A, B](f(l)), r => Right[A, B](r))
    def toLeft(f: B => A): Either[A, B]  = inst.fold(l => Left[A, B](l), r => Left[A, B](f(r)))
  }

  implicit class PureEitherOps[A](inst: A) {
    def pureLeft[M[_]: Applicative, B]: M[Either[A, B]]    = (Left[A, B](inst): Either[A, B]).pure[M]
    def pureLeftT[M[_]: Applicative, B]: EitherT[M, A, B]  = EitherT[M, A, B](inst.pureLeft[M, B])
    def pureRight[M[_]: Applicative, B]: M[Either[B, A]]   = (Right[B, A](inst): Either[B, A]).pure[M]
    def pureRightT[M[_]: Applicative, B]: EitherT[M, B, A] = EitherT[M, B, A](inst.pureRight[M, B])
  }
}
