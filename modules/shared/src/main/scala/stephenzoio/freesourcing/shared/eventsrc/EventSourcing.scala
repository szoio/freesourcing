package stephenzoio.freesourcing.shared.eventsrc

import java.util.UUID

import cats._
import cats.implicits._
import cats.data.{EitherK, WriterT}
import cats.free.Free
import stephenzoio.freesourcing.shared.free.FreeOp

trait EventSourcing {

  // abstract definitions
  type C[_] // command
  type E[_] // update
  type Q[_] // query
  type EventKey   // event key
  type EventValue // event value

  final case class EventSpec(eventKey: EventKey, eventValue: EventValue, commandId: UUID)
  final case class CommandSpec(commandId: UUID)

  def asEventSpec[A]: E[A] => EventSpec
  def asCommandSpec[A]: C[A] => CommandSpec
  def c2Id: C ~> Id

  // the logging algebra
  sealed trait L[A]                                   extends FreeOp[L, A]
  final case class Append(eventSpec: List[EventSpec]) extends L[Unit]
  final case class Exists(commandId: UUID)            extends L[Boolean]

  // free of the Coproduct of Q and E (the language commands are interpreted into)
  type FEQ[A] = Free[EitherK[Q, E, ?], A]

  def c2MLogged[M[_]](c2U: C ~> FEQ, e2M: E ~> M, q2M: Q ~> M, l2M: L ~> M)(implicit M: Monad[M]): C ~> M = {
    type WM[A] = WriterT[M, List[EventSpec], A]

    val u2WM: E ~> WM =
      new (E ~> WM) {
        override def apply[A](fa: E[A]): WM[A] =
          WriterT[M, List[EventSpec], A](e2M(fa).map(x => (List(asEventSpec(fa)), x)))
      }

    val q2WM: Q ~> WM = new (Q ~> WM) {
      override def apply[A](fa: Q[A]): WM[A] =
        WriterT[M, List[EventSpec], A](q2M(fa).map(x => (List.empty, x)))
    }

    val c2WM: C ~> WM = new (C ~> WM) {
      override def apply[A](fa: C[A]) = c2U(fa).foldMap(q2WM or u2WM)
    }

    new (C ~> M) {
      private def doApply[A](fa: C[A]) = c2WM.apply(fa).run.flatMap {
        case result @ (updateList, Right(_)) => Append(updateList).liftF.foldMap(l2M) >> result._2.pure[M]
        case (_, a)                          => a.pure[M]
      }

      override def apply[A](fa: C[A]): M[A] =
        for {
          exists <- Exists(asCommandSpec(fa).commandId).liftF.foldMap(l2M)
          a      <- if (exists) c2Id(fa).pure[M] else doApply(fa)
        } yield a
    }
  }
}
