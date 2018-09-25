package stephenzoio.freesourcing.core.eventsrc

import doobie.ConnectionIO
import stephenzoio.freesourcing.core.eventsrc.sql.{EventSpec => DbEventSpec}

import cats.effect.{Async, IO}
import cats.~>
import doobie.util.meta.Meta
import doobie.util.transactor.Transactor
import stephenzoio.freesourcing.shared.eventsrc.EventSourcing

trait EventSourcingToDb extends EventSourcing {

  def keyMeta: Meta[EventKey]
  def valueMeta: Meta[EventValue]

  def commandOp2IO(c2CF: C ~> FEQ,
                   q2CIO: Q ~> ConnectionIO,
                   e2CIO: E ~> ConnectionIO,
                   transactor: Transactor[IO]): C ~> IO = {
    implicit val asyncConIO: Async[ConnectionIO] = doobie.free.connection.AsyncConnectionIO
    c2MLogged[ConnectionIO](c2CF, e2CIO, q2CIO, logOp2ConIO("event")).andThen(transactor.trans)
  }

  private def logOp2ConIO(tableName: String) = new (L ~> ConnectionIO) {
    implicit val km: Meta[EventKey]   = keyMeta
    implicit val vm: Meta[EventValue] = valueMeta
    override def apply[A](fa: L[A]): ConnectionIO[A] = fa match {
      case Append(events) =>
        sql
          .append(tableName)(events.map(e => DbEventSpec(e.eventValue, e.eventKey, e.commandId)))
          .map(_ => ())
      case Exists(commandId) => sql.exists(tableName)(commandId)
    }
  }
}
