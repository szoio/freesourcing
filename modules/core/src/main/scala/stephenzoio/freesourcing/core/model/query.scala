package stephenzoio.freesourcing.core.model

import java.util.UUID

import domain.{Account, Reservation}
import stephenzoio.freesourcing.shared.free.FreeOp

object query {
  sealed trait QueryOp[A] extends FreeOp[QueryOp, A]
  sealed trait QueryEvent extends Product with Serializable { self: QueryOp[_] =>
  }

  object account {
    final case class GetAccount(accountId: UUID)             extends QueryOp[Option[Account]]
    final case class GetAccountReservations(accountId: UUID) extends QueryOp[List[Reservation]]
    final case class GetReservation(reservationId: UUID)     extends QueryOp[Option[Reservation]]
  }
}
