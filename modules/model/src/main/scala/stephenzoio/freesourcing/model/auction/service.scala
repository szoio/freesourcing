package stephenzoio.freesourcing.model.auction

import java.util.UUID

object service {
  object request {
    final case class CreateAccount(accountId: UUID, accountDto: CreateAccount.Account)
    object CreateAccount {
      final case class Account(userName: String, funds: BigDecimal)
    }

    final case class UpdateAccount(userName: String)

    final case class AddFunds(funds: BigDecimal)

    final case class ReserveFunds(reservationId: UUID, description: String, amount: BigDecimal)

    final case class ConfirmReservation(amount: BigDecimal)
  }

  object response {
    final case class Result[A](result: A)
  }
}
