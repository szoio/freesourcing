package stephenzoio.freesourcing.shared.eventsrc

import java.util.UUID

import cats._
import cats.data.EitherK
import cats.free.Free
import stephenzoio.freesourcing.shared.free.FreeOp

trait EventSourcingSimplest {

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

  // the logging algebra
  sealed trait L[A]                                   extends FreeOp[L, A]
  final case class Append(eventSpec: List[EventSpec]) extends L[Unit]
  final case class Exists(commandId: UUID)            extends L[Boolean]

  // free of the Coproduct of Q and E (the language commands are interpreted into)
  type EQ[A] = EitherK[E, Q, A]
  type FEQ[A] = Free[EQ, A]

  def c2MLogged[M[_]](c2EQ: C ~> FEQ, e2M: E ~> M, q2M: Q ~> M, l2M: L ~> M)(implicit M: Monad[M]): C ~> M = {

    type EL[A] = EitherK[E, L, A]
    type FEL[A] = Free[EL, A]
    def el2M: EL ~> M = e2M or l2M

    val e2MLogged = new (E ~> M) {
      override def apply[A](ea: E[A]): M[A] = {
        val fEL: Free[EL, A] = for {
          _ <- Append(List(asEventSpec(ea))).inject[EL]
          ea <- Free.liftF[E, A](ea).inject[EL]
        } yield ea

        fEL.foldMap(el2M)
      }
    }

    val fEQ2M: FEQ ~> M = Free.foldMap(e2MLogged or q2M: EQ ~> M)
    def c2M: C ~> M = c2EQ.andThen(fEQ2M)
    c2M
  }
}
