package stephenzoio.freesourcing.core.command

import java.util.UUID

import stephenzoio.freesourcing.core.model.combined.{CombinedFree, CombinedFreeE, CombinedOp}
import stephenzoio.freesourcing.model.auction.event
import stephenzoio.freesourcing.core.model.query
import stephenzoio.freesourcing.core.model.domain.{Account, Reservation, ReservationState}
import stephenzoio.freesourcing.shared.syntax.{FreeSyntax, PureSyntax}

object account extends FreeSyntax[CombinedOp] with PureSyntax {
  def createAccount(
      commandId: UUID)(accountId: UUID, userName: String, funds: BigDecimal): CombinedFreeE[Unit] =
    event.account
      .AccountCreated(accountId, userName, funds)
      .withKey(key = accountId, commandId = UUID.randomUUID())
      .inject[CombinedOp]

  def updateAccount(accountId: UUID, userName: String): CombinedFreeE[Unit] =
    event.account
      .AccountUpdated(accountId, userName)
      .withKey(key = accountId, commandId = UUID.randomUUID())
      .inject[CombinedOp]

  def addFunds(accountId: UUID, amount: BigDecimal): CombinedFreeE[Unit] =
    event.account
      .FundsAdded(accountId, amount)
      .withKey(key = accountId, commandId = UUID.randomUUID())
      .inject[CombinedOp]

  def reserveFunds(accountId: UUID,
                   reservationId: UUID,
                   amount: BigDecimal,
                   description: String): CombinedFreeE[Unit] = {
    val reserveT = for {
      account <- query.account
        .GetAccount(accountId)
        .inject[CombinedOp]
        .toRightT(error("Account could not be found"))
      reservations <- query.account.GetAccountReservations(accountId).inject[CombinedOp].rightT
      available = getAvailableFunds(account, reservations)
      // make sure there's enough funds available
      _ <- assert(available >= amount)(error("Insufficient funds")).eitherT
      result <- event.account
        .FundsReserved(accountId, reservationId, description, amount)
        .withKey(key = accountId, commandId = UUID.randomUUID())
        .inject[CombinedOp]
        .eitherT
    } yield result
    reserveT.value
  }

  def cancelReservation(reservationId: UUID): CombinedFreeE[Unit] = {
    val cancelT = for {
      reservation <- query.account
        .GetReservation(reservationId)
        .inject[CombinedOp]
        .toRightT(error("Reservation could not be found"))
      _ <- assert(reservation.state == ReservationState.Pending)(error("Reservation state must be pending")).eitherT
      result <- event.account
        .ReservationCancelled(reservationId)
        .withKey(key = reservation.accountId, commandId = UUID.randomUUID())
        .inject[CombinedOp]
        .eitherT
    } yield result
    cancelT.value
  }

  def confirmReservation(reservationId: UUID, finalAmount: BigDecimal): CombinedFreeE[Unit] = {
    val confirmT = for {
      reservation <- query.account
        .GetReservation(reservationId)
        .inject[CombinedOp]
        .toRightT(error("Reservation could not be found"))
      _            <- assert(reservation.state == ReservationState.Pending)(error("Reservation state must be pending")).eitherT
      reservations <- query.account.GetAccountReservations(reservation.accountId).inject[CombinedOp].rightT
      account <- query.account
        .GetAccount(reservation.accountId)
        .inject[CombinedOp]
        .toRightT(error("Account could not be found"))
      available = getAvailableFunds(account, reservations) + reservation.amount
      _ <- assert(available >= finalAmount)(error("Insufficient funds")).eitherT
      commandId = UUID.randomUUID()
      result <- event.account
        .ReservationConfirmed(reservationId, finalAmount)
        .withKey(key = account.accountId, commandId = commandId)
        .inject[CombinedOp]
        .eitherT
      _ <- event.account
        .FundsAdded(account.accountId, -finalAmount)
        .withKey(key = account.accountId, commandId = commandId)
        .inject[CombinedOp]
        .eitherT
    } yield result
    confirmT.value
  }

  private def getAvailableFunds(account: Account, reservations: List[Reservation]): BigDecimal =
    account.balance - reservations.collect {
      case Reservation(_, _, _, amount, state) if state == ReservationState.Pending => amount
    }.sum

  private def assert[A](condition: => Boolean)(errorIfFalse: => Throwable): CombinedFreeE[Unit] = {
    if (!condition) errorIfFalse.pureLeft[CombinedFree, Unit] else ().pureRight[CombinedFree, Throwable]
  }

  private def error(reason: String): Throwable = new Exception(reason)
}
