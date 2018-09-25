package stephenzoio.freesourcing.core.model

import cats.data.{EitherK, EitherT}
import cats.free.Free
import stephenzoio.freesourcing.core.model.query.QueryOp
import stephenzoio.freesourcing.model.auction.event.EventOp

object combined {
  type CombinedOp[A]     = EitherK[QueryOp, EventOp, A]
  type CombinedFree[A]   = Free[CombinedOp, A]
  type CombinedFreeE[A]  = CombinedFree[Either[Throwable, A]]
  type CombinedFreeET[A] = EitherT[CombinedFree, Throwable, A]
}
