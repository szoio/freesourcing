package stephenzoio.freesourcing.core.data.event

import cats.~>
import doobie.free.connection.ConnectionIO
import stephenzoio.freesourcing.model.auction.event.{Event, EventOp, EventSpec}
import stephenzoio.freesourcing.model.auction.{event => algebra}
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import stephenzoio.freesourcing.shared.db.implicits._
import doobie.implicits._
import stephenzoio.freesourcing.core.data
import stephenzoio.freesourcing.shared.util.Attempt

object EventOp2ConnectionIO extends (EventOp ~> ConnectionIO) {
  override def apply[A](fa: EventOp[A]): ConnectionIO[A] = fa match {
    case EventSpec(_, event, _) => applyE[A](event)
  }

  def applyE[A](event: Event): ConnectionIO[Attempt[Unit]] = event match {
    case algebra.account.AccountCreated(accountId, userName, funds) =>
      account
        .accountCreated(accountId, userName, funds)
        .run
        .attemptSql
        .mapDbErrors {
          case UNIQUE_VIOLATION => new Exception(s"Name $userName is not unique")
        }
        .mapToUnit

    case algebra.account.AccountUpdated(accountId, userName) =>
      data.event.account
        .accountUpdated(accountId, userName)
        .run
        .attemptSql
        .mapDbErrors {
          case UNIQUE_VIOLATION => new Exception(s"Name $userName is not unique")
        }
        .mapToUnit

    case algebra.account.FundsAdded(accountId, amount) =>
      data.event.account
        .fundsAdded(accountId, amount)
        .run
        .attemptSql
        .mapToUnit

    case algebra.account.FundsReserved(accountId, reservationId, description, amount) =>
      data.event.account
        .fundsReserved(accountId, reservationId, description, amount)
        .run
        .attemptSql
        .mapDbErrors {
          case UNIQUE_VIOLATION => new Exception(s"Reservation with ID $reservationId already exists")
        }
        .mapToUnit

    case algebra.account.ReservationCancelled(reservationId) =>
      data.event.account
        .reservationCancelled(reservationId)
        .run
        .attemptSql
        .mapToUnit

    case algebra.account.ReservationConfirmed(reservationId, amount) =>
      data.event.account
        .reservationConfirmed(reservationId, amount)
        .run
        .attemptSql
        .mapToUnit
  }
}
