package stephenzoio.freesourcing.core.eventsrc

import java.util.UUID

import cats.{Id, ~>}
import doobie.util.meta.Meta
import io.circe.{Decoder, Encoder, Json}
import stephenzoio.freesourcing.model.auction.command.{CommandOp, CommandSpec => AuctionCommandSpec}
import stephenzoio.freesourcing.core.model.query.QueryOp
import stephenzoio.freesourcing.model.auction.event.{
  EventOp,
  Event => AuctionEvent,
  EventSpec => AuctionEventSpec
}

import scala.reflect.runtime.universe.TypeTag
import io.circe.syntax._
import io.circe.generic.auto._
import stephenzoio.freesourcing.shared.db.implicits._
import doobie.postgres.implicits._

object AuctionEventSourcing extends EventSourcingToDb {

  override type EventValue = AuctionEvent
  override type EventKey   = UUID
  override type E[A]       = EventOp[A]
  override type C[A]       = CommandOp[A]
  override type Q[A]       = QueryOp[A]

  override def asEventSpec[A]: EventOp[A] => EventSpec = {
    case AuctionEventSpec(key, event, commandId) => EventSpec(key, event, commandId)
  }

  override def asCommandSpec[A]: CommandOp[A] => CommandSpec = {
    case AuctionCommandSpec(commandId, _) => CommandSpec(commandId)
  }

  def getEventMeta(implicit d: Decoder[AuctionEvent],
                   e: Encoder[AuctionEvent],
                   typeTag: TypeTag[AuctionEvent]): Meta[EventValue] =
    Meta[Json].xmap[EventValue](
      j =>
        j.as[EventValue]
          .fold[EventValue](_ => throw new Exception("Decoding Error"), identity), // failure raises an exception
      a => a.asJson
    )

  override val valueMeta = getEventMeta
  override val keyMeta   = implicitly[Meta[UUID]]

  override lazy val c2Id = new (CommandOp ~> Id) {
    override def apply[A](fa: CommandOp[A]) = fa match {
      case AuctionCommandSpec(_, _) => Right[Throwable, Unit](())
    }
  }
}
