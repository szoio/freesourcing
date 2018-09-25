package stephenzoio.freesourcing.core.model
import java.util.UUID

object domain {
//  case class User(userId: UUID, userName: String)

  sealed trait ReservationState extends Product with Serializable
  object ReservationState {
    case object Pending   extends ReservationState
    case object Confirmed extends ReservationState
    case object Cancelled extends ReservationState

    def of(reservationState: ReservationState): ReservationState = reservationState

    def toString(r: ReservationState): String = r.toString.toLowerCase
    def fromString(s: String): Option[ReservationState] = s.toLowerCase match {
      case "pending"   => Some(Pending)
      case "confirmed" => Some(Confirmed)
      case "cancelled" => Some(Cancelled)
      case _           => None
    }
  }

  final case class Reservation(reservationId: UUID,
                         accountId: UUID,
                         description: String,
                         amount: BigDecimal,
                         state: ReservationState)
  final case class Account(accountId: UUID, userName: String, balance: BigDecimal)
}
