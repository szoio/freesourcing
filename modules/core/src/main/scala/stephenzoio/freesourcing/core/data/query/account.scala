package stephenzoio.freesourcing.core.data.query
import java.util.UUID

import stephenzoio.freesourcing.core.model.domain.{Account, Reservation}
import doobie.implicits._
import doobie.postgres.implicits._
import stephenzoio.freesourcing.core.implicits._

object account {
  def getAccount(accountId: UUID) =
    sql"select id, user_name, funds from account where id = $accountId".query[Account]

  private def selectReservation() = fr"select id, account_id, description, amount, state from reservation"

  def getAccountReservations(accountId: UUID) =
    (selectReservation ++ sql"where account_id = $accountId").query[Reservation]

  def getReservation(reservationId: UUID) =
    (selectReservation ++ sql"where id = $reservationId").query[Reservation]

}
