package stephenzoio.freesourcing.core.data.query

import cats.~>
import doobie.free.connection.ConnectionIO
import stephenzoio.freesourcing.core.model.query.QueryOp
import stephenzoio.freesourcing.core.model.{query => op}

object QueryOp2ConnectionIO extends (QueryOp ~> ConnectionIO) {
  override def apply[A](fa: QueryOp[A]): ConnectionIO[A] = fa match {
    case op.account.GetAccount(accountId)             => account.getAccount(accountId).option
    case op.account.GetAccountReservations(accountId) => account.getAccountReservations(accountId).to[List]
    case op.account.GetReservation(accountId)         => account.getReservation(accountId).option
  }
}
