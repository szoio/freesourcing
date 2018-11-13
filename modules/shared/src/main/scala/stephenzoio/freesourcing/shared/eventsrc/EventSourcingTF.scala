//package stephenzoio.freesourcing.shared.eventsrc
//
//import java.util.UUID
//
//import cats._
//import cats.data.{EitherK, WriterT}
//import cats.free.Free
//import cats.implicits._
//import stephenzoio.freesourcing.shared.free.FreeOp
//
//trait EventSourcingTF {
//  type M[_]
//
//
//  // abstract definitions
//  type C[_, _] // command
//  type E[_, _] // update
//  type Q[_, _] // query
//  type EventKey   // event key
//  type EventValue // event value
//
//  final case class EventSpec(eventKey: EventKey, eventValue: EventValue, commandId: UUID)
//  final case class CommandSpec(commandId: UUID)
//
//  def asEventSpec[M[_], A]: E[M, A] => EventSpec
//  def asCommandSpec[M[_], A]: C[M[?], A] => CommandSpec
//  def c2Id: C ~> Id
//
//  // the logging algebra
//  sealed trait L[M[_], A]
//  final case class Append[M](eventSpec: List[EventSpec]) extends L[M, Unit]
//
//  type D[M, A] = E[M, ?] with Q[M, A]
//
//  // free of the Coproduct of Q and E (the language commands are interpreted into)
//
//  type EQ[M[_], A] = E[M, A] with Q[M, A]
//
//  def c2MLogged[M[_]](c2U: C[EQ[M[_], ?], ?], e2M: E[M, ?], q2M: Q[M, ?], l2M: L[M, ?])(implicit M: Monad[M]): C[M, ?] = {
//
//    val e2MLogged = new E[M, ?] {
//    }
//    ???
//  }
//}
