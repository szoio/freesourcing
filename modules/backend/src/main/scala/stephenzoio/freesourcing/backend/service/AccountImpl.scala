package stephenzoio.freesourcing.backend.service

import java.util.UUID

import cats.effect.IO
import stephenzoio.freesourcing.model.auction.command
import stephenzoio.freesourcing.model.auction.command.CommandFreeE
import stephenzoio.freesourcing.model.auction.service._

object AccountImpl {

  implicit class CommandOps(c: CommandFreeE[Unit]) {
    def toResult(sc: IO[SC]): IOE[response.Result[String]] =
      c.toIO(sc)
        .map(_.map(_ => response.Result("done.")))
  }

  def createAccount(sc: IO[SC])(req: request.CreateAccount): IOE[response.Result[String]] = {
    command.account
      .CreateAccount(req.accountId, req.accountDto.userName, req.accountDto.funds)
      .liftWithId(UUID.randomUUID())
      .toResult(sc)
  }

  def updateAccount(sc: IO[SC])(accountId: UUID, req: request.UpdateAccount): IOE[response.Result[String]] = {
    command.account
      .UpdateAccount(accountId, req.userName)
      .liftWithId(UUID.randomUUID())
      .toResult(sc)
  }

  def addFunds(sc: IO[SC])(accountId: UUID, req: request.AddFunds): IOE[response.Result[String]] = {
    command.account
      .AddFunds(accountId, req.funds)
      .liftWithId(UUID.randomUUID())
      .toResult(sc)
  }

  def reserveFunds(sc: IO[SC])(accountId: UUID, req: request.ReserveFunds): IOE[response.Result[String]] = {
    command.account
      .ReserveFunds(accountId, req.reservationId, req.amount, req.description)
      .liftWithId(UUID.randomUUID())
      .toResult(sc)
  }

  def cancelReservation(sc: IO[SC])(reservationId: UUID): IOE[response.Result[String]] = {
    command.account
      .CancelReservation(reservationId)
      .liftWithId(UUID.randomUUID())
      .toResult(sc)
  }

  def confirmReservation(sc: IO[SC])(reservationId: UUID,
                                     req: request.ConfirmReservation): IOE[response.Result[String]] = {
    command.account
      .ConfirmReservation(reservationId, req.amount)
      .liftWithId(UUID.randomUUID())
      .toResult(sc)
  }
}
