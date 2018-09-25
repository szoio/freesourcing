package stephenzoio.freesourcing.shared.syntax

import cats.InjectK
import cats.data.{EitherT, OptionT}
import cats.free.Free
import stephenzoio.freesourcing.shared.free.FreeOp

/************************************
  *
  * A utility belt of Free monad extension methods
  *
  * To use:
  * As a trait:
  * object impl extends FreeSyntax[MyFreeOp] { ... }
  * or as an object:
  * val v = FreeSyntax[FreeSyntax]; import v._
  *
  ************************************/
private[syntax] trait FreeSyntax0 extends FunctorSyntax {
  type Op[_]
  type OpE[A]      = Op[Either[Throwable, A]]
  type OpFree[A]   = Free[Op, A]
  type OpFreeE[A]  = OpFree[Either[Throwable, A]]
  type OpFreeET[A] = EitherT[OpFree, Throwable, A]

  // lifts Free[Op, Either[Throwable, A]] to EitherT[Free[Op, ?], Throwable, A]
  implicit class FreeEOps[A](fa: OpFreeE[A]) {
    val eitherT: EitherT[OpFree, Throwable, A] = EitherT(fa)
  }

  // lifts Free[Op, A] to the right part of EitherT[Free[Op, ?], Throwable, A]
  // lifts Free[Op, A] to the Some part of OptionT[Free[Op, ?], A]
  implicit class FreeOps[A](free: OpFree[A]) {
    val rightT: OpFreeET[A]       = EitherT.liftF[OpFree, Throwable, A](free)
    val someT: OptionT[OpFree, A] = OptionT.liftF[OpFree, A](free)
  }

  // lifts Free[Op, Throwable] to the left part of EitherT[Free[Op, ?], Throwable, A]
  implicit class FreeThrowOps(free: OpFree[Throwable]) {
    def leftT[A]: OpFreeET[A] = EitherT.liftF[OpFree, A, Throwable](free).swap
  }

  // lifts Free[Op, Either[A, B]] to EitherT[Free[Op, ?], A, B]
  implicit class FreeEitherOps[A, B](free: OpFree[Either[A, B]]) {
    val eitherT: EitherT[OpFree, A, B] = EitherT.apply[OpFree, A, B](free)
  }

  // lift/inject Free[F, A] to Free[Op, B] where typically Op[_] is the coproduct of F[_] and something else
  implicit class FreeInjectOps[F[_], A](freeOp: FreeOp[F, A]) {
    def injectD(implicit I: InjectK[F, Op]): OpFree[A] = freeOp.inject[Op]
  }

  // lifts an instance of Free[Op, Option[A]] into either a Free[Op, Either[B, A]] or a EitherT[Free[Op, ?], B, A]
  // where the None case is mapped to a specific instance of B
  implicit class FreeOptionOps[A](free: OpFree[Option[A]]) {
    val optionT: OptionT[OpFree, A]                 = OptionT.apply[OpFree, A](free)
    def toRight[B](b: => B): OpFree[Either[B, A]]   = free.map(_.fold[Either[B, A]](Left(b))(a => Right(a)))
    def toRightT[B](b: => B): EitherT[OpFree, B, A] = toRight(b).eitherT
  }

  // lifts an instance of Free[Op, Either[Throwable, Option[A]]] into either a Free[Op, Either[B, A]] or a EitherT[Free[Op, ?], B, A]
  // where the None case is mapped to a specific instance of Throwable (i.e. missing is treated as an error)
  // and prior errors are propagated as is
  implicit class FreeEOptionOps[A](free: OpFreeE[Option[A]]) {
    def toRight(b: => Throwable): OpFreeE[A] = free.map[Either[Throwable, A]] {
      case Right(aOpt) => aOpt.fold[Either[Throwable, A]](Left(b))(a => Right(a))
      case Left(t)     => Left(t)
    }
    def toRightT(b: Throwable): OpFreeET[A] = toRight(b).eitherT
  }
}

trait FreeSyntax[F[_]] extends FreeSyntax0 {
  override type Op[A] = F[A]
}

object FreeSyntax {
  def apply[F[_]] = new FreeSyntax[F] {}
}
