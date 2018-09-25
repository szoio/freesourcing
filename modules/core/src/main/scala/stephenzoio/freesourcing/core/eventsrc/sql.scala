package stephenzoio.freesourcing.core.eventsrc

import java.util.UUID

import doobie._
import doobie.implicits._
import stephenzoio.freesourcing.shared.db.implicits._
import doobie.postgres.implicits._
import cats.implicits._
import doobie.util.meta.Meta
import fs2._

object sql {
  final case class EventSpec[A, K](event: A, partitionKey: K, commandId: UUID)

  def appendQ[A: Meta, K: Meta](tableName: String, e: EventSpec[A, K]): Update0 =
    Update[(A, K, UUID)](s"insert into $tableName(payload, partition_key, command_id) values(?, ?, ?)")
      .toUpdate0((e.event, e.partitionKey, e.commandId))

  def append[A: Meta, K: Meta](tableName: String)(events: List[EventSpec[A, K]]): ConnectionIO[Int] =
    events
      .traverse[ConnectionIO, Int](e =>
        appendQ(tableName, EventSpec(e.event, e.partitionKey, e.commandId)).run)
      .map(_.count(_ > 0))

  def existsQ(tableName: String, commandId: UUID): Query0[Int] = Query[UUID, Int](s"""
      select count(command_id) from $tableName
      where command_id = ?
      limit 1
      """).toQuery0(commandId)

  def exists(tableName: String)(commandId: UUID): ConnectionIO[Boolean] =
    existsQ(tableName, commandId).unique.map(_ > 0)

  private[eventsrc] def streamAllQ(tableName: String): Query0[dbmodel.Event] =
    sql"select id, created, payload, partition_key, command_id from $tableName".query[dbmodel.Event]

  def streamAll(tableName: String): Stream[ConnectionIO, dbmodel.Event] =
    streamAllQ(tableName).stream
}
