package stephenzoio.freesourcing.backend.service

import cats.effect.IO
import org.http4s.HttpService
import org.http4s.dsl.io._
import org.http4s.circe._
import io.circe.generic.auto._
import stephenzoio.freesourcing.backend.service.Extractors._
import stephenzoio.freesourcing.model.auction.service._

object AccountService {

  case class User(name: String)
  case class Hello(greeting: String)

  implicit val createAccountDecoder      = jsonOf[IO, request.CreateAccount]
  implicit val updateAccountDecoder      = jsonOf[IO, request.UpdateAccount]
  implicit val addFundsDecoder           = jsonOf[IO, request.AddFunds]
  implicit val reserveFundsDecoder       = jsonOf[IO, request.ReserveFunds]
  implicit val confirmReservationDecoder = jsonOf[IO, request.ConfirmReservation]

  def apply(sc: IO[SC]) = HttpService[IO] {
    case GET -> Root / "hello" / name => Ok(s"Hello, $name.")

    case req @ POST -> Root / "accounts" =>
      req.as[request.CreateAccount].flatMap(AccountImpl.createAccount(sc)).toHttpOk

    case req @ PUT -> Root / "accounts" / uuid(accountId) =>
      req.as[request.UpdateAccount].flatMap(r => AccountImpl.updateAccount(sc)(accountId, r)).toHttpOk

    case req @ POST -> Root / "accounts" / uuid(accountId) / "funds" =>
      req.as[request.AddFunds].flatMap(r => AccountImpl.addFunds(sc)(accountId, r)).toHttpOk

    case req @ POST -> Root / "accounts" / uuid(accountId) / "funds" / "reservations" =>
      req.as[request.ReserveFunds].flatMap(r => AccountImpl.reserveFunds(sc)(accountId, r)).toHttpOk

    case req @ POST -> Root / "accounts" / uuid(_) / "funds" / "reservations" / uuid(reservationId) =>
      req
        .as[request.ConfirmReservation]
        .flatMap(r => AccountImpl.confirmReservation(sc)(reservationId, r))
        .toHttpOk

    case DELETE -> Root / "accounts" / uuid(_) / "funds" / "reservations" / uuid(reservationId) =>
      AccountImpl.cancelReservation(sc)(reservationId).toHttpOk
  }
}
