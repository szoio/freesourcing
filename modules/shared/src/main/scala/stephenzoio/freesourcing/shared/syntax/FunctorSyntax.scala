package stephenzoio.freesourcing.shared.syntax

import cats.Functor
import cats.data.{EitherT, OptionT}

trait FunctorSyntax {
  implicit class FunctorOps[F[_]: Functor, A](f: F[A]) {
    val rightT: EitherT[F, Throwable, A] = EitherT.liftF[F, Throwable, A](f)
    val someT: OptionT[F, A]             = OptionT.liftF[F, A](f)
  }

  implicit class FunctorThrowOps[F[_]: Functor](f: F[Throwable]) {
    def leftT[A]: EitherT[F, Throwable, A] = EitherT.liftF[F, A, Throwable](f).swap
  }

  implicit class FunctorEitherOps[F[_], A, B](f: F[Either[A, B]])(implicit func: Functor[F]) {
    val eitherT: EitherT[F, A, B] = EitherT.apply(f)
  }

  implicit class FunctorOptionOps[F[_], A](fa: F[Option[A]])(implicit func: Functor[F]) {
    val optionT: OptionT[F, A]              = OptionT.apply(fa)
    def toRight[B](b: B): F[Either[B, A]]   = func.map(fa)(_.fold[Either[B, A]](Left(b))(a => Right(a)))
    def toRightT[B](b: B): EitherT[F, B, A] = toRight(b).eitherT
  }

  implicit class FunctorEitherOptionOps[F[_], A, B](fa: F[Either[A, Option[B]]])(implicit func: Functor[F]) {
    def toRight(a: A): F[Either[A, B]]   = func.map(fa)(_.flatMap(_.fold[Either[A, B]](Left(a))(b => Right(b))))
    def toRightT(a: A): EitherT[F, A, B] = toRight(a).eitherT
  }
}
