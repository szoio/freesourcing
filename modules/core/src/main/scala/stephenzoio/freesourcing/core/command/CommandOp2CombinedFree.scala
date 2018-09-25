package stephenzoio.freesourcing.core.command

import java.util.UUID

import cats.~>
import stephenzoio.freesourcing.core.model.combined._
import stephenzoio.freesourcing.model.auction.command.CommandOp
import stephenzoio.freesourcing.model.auction.command.Command
import stephenzoio.freesourcing.model.auction.command.CommandSpec
import stephenzoio.freesourcing.model.auction.{command => op}

object CommandOp2CombinedFree extends (CommandOp ~> CombinedFree) {
  override def apply[A](fa: CommandOp[A]): CombinedFree[A] = fa match {
    case CommandSpec(id, command) => applyC[A](id, command)
  }

  def applyC[A](cid: UUID, command: Command): CombinedFreeE[Unit] = command match {
    case op.account.CreateAccount(accountId, userName, funds) =>
      account.createAccount(cid)(accountId, userName, funds)

    case op.account.UpdateAccount(accountId, userName) => account.updateAccount(accountId, userName)

    case op.account.AddFunds(accountId, funds) => account.addFunds(accountId, funds)

    case op.account.ReserveFunds(accountId, reservationId, description, amount) =>
      account.reserveFunds(accountId, reservationId, description, amount)

    case op.account.CancelReservation(reservationId) => account.cancelReservation(reservationId)

    case op.account.ConfirmReservation(reservationId, finalAmount) =>
      account.confirmReservation(reservationId, finalAmount)
  }
}
