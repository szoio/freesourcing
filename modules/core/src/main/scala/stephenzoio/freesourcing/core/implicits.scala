package stephenzoio.freesourcing.core

import doobie.util.meta.Meta
import doobie.postgres.implicits._
import stephenzoio.freesourcing.core.model.domain.ReservationState

object implicits {
  implicit val reservationMeta: Meta[ReservationState] =
    pgEnumStringOpt("reservation_state", ReservationState.fromString, ReservationState.toString)
}
