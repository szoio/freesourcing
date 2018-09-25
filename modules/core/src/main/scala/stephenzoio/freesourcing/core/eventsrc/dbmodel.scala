package stephenzoio.freesourcing.core.eventsrc

import java.time.Instant
import java.util.UUID

import io.circe.Json

object dbmodel {
  final case class Event(id: Int, created: Instant, payload: Json, partitionKey: UUID, commandId: UUID)
}