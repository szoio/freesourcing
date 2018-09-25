package stephenzoio.freesourcing.core.data.event
import java.util.UUID

import doobie.util.update.Update0
import stephenzoio.freesourcing.core.model.domain.ReservationState
import doobie.implicits._
import doobie.postgres.implicits._
import stephenzoio.freesourcing.core.implicits._

object account {
  def accountCreated(accountId: UUID, userName: String, funds: BigDecimal): Update0 =
    sql"""
      insert into account(id, user_name, funds)
      values ($accountId, $userName, $funds)
      """.update

  def accountUpdated(accountId: UUID, userName: String): Update0 =
    sql"""
      update account
      set user_name = $userName
      where id = $accountId
      """.update

  def fundsAdded(accountId: UUID, amount: BigDecimal): Update0 =
    sql"""
      update account
      set funds = funds + $amount
      where id = $accountId
      """.update

  def fundsReserved(accountId: UUID, reservationId: UUID, description: String, amount: BigDecimal): Update0 =
    sql"""
      insert into reservation(id, account_id, description, amount, state)
      values ($reservationId, $accountId, $description, $amount, ${ReservationState.of(
      ReservationState.Pending)})
      """.update

  def reservationCancelled(reservationId: UUID): Update0 =
    sql"""
      update reservation
      set state = ${ReservationState.of(ReservationState.Cancelled)}
      where id = $reservationId
      """.update

  def reservationConfirmed(reservationId: UUID, amount: BigDecimal): Update0 =
    sql"""
      update reservation
      set state = ${ReservationState.of(ReservationState.Confirmed)}, amount = $amount
      where id = $reservationId
      """.update
}
