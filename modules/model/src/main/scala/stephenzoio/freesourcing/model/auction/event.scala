package stephenzoio.freesourcing.model.auction

import java.util.UUID

import stephenzoio.freesourcing.shared.free.FreeOp

object event {
  sealed trait EventOp[A] extends FreeOp[EventOp, A]
  type EventOpE[A] = EventOp[Either[Throwable, A]]

  sealed trait Event {
    def withKey(key: UUID, commandId: UUID) = EventSpec(key, this, commandId)
  }

  final case class EventSpec(key: UUID, event: Event, commandId: UUID) extends EventOpE[Unit]

  object account {
    final case class AccountCreated(accountId: UUID, userName: String, initialFunds: BigDecimal) extends Event
    final case class AccountUpdated(accountId: UUID, userName: String)                           extends Event
    final case class FundsAdded(accountId: UUID, amount: BigDecimal)                             extends Event
    final case class FundsReserved(accountId: UUID,
                                   reservationId: UUID,
                                   description: String,
                                   amount: BigDecimal)
        extends Event
    final case class ReservationCancelled(reservationId: UUID)                     extends Event
    final case class ReservationConfirmed(reservationId: UUID, amount: BigDecimal) extends Event
  }
}
