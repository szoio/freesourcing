package stephenzoio.freesourcing.backend.service

import cats.{Monad, MonadError}
import cats.effect.Effect
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{Response, Status}
import org.http4s.circe._
import org.http4s.dsl.io._
import cats.implicits._
import stephenzoio.freesourcing.shared.util.Attempt

trait HttpExtensions {
  final case class Error(error: String)

  private[service] def okResponse[F[_], A](a: A, status: Status = Status.Ok)(implicit encoder: Encoder[A],
                                                                             M: Monad[F]) =
    Response[F](status = Status.Ok).withBody(a.asJson)

  private[service] def errorResponse[F[_]](e: Throwable)(implicit M: Monad[F]) =
    Response[F](status = Status.InternalServerError).withBody(Error(e.getMessage).asJson)

  private[service] def notFoundResponse[F[_], A](e: Throwable)(implicit M: Monad[F]) =
    Response[F](status = Status.NotFound).withBody(Error(e.getMessage).asJson)

  implicit class FAttemptHttpOps[F[_], A](fa: F[Attempt[A]])(implicit M: MonadError[F, Throwable],
                                                             F: Effect[F]) {
    def toHttpOk(implicit encoder: Encoder[A]): F[Response[F]]      = toHttpWithStatus(Status.Ok)
    def toHttpCreated(implicit encoder: Encoder[A]): F[Response[F]] = toHttpWithStatus(Status.Created)
    private def toHttpWithStatus(status: Status)(implicit encoder: Encoder[A]): F[Response[F]] = fa.flatMap {
      case Left(l)  => errorResponse(l)
      case Right(r) => okResponse(r)
    }
  }

  implicit class FHttpOps[F[_], A](fa: F[A])(implicit M: MonadError[F, Throwable], F: Effect[F]) {
    def attemptToHttpOk(implicit encoder: Encoder[A]): F[Response[F]]      = fa.attempt.toHttpOk
    def attemptToHttpCreated(implicit encoder: Encoder[A]): F[Response[F]] = fa.attempt.toHttpCreated
  }

  implicit class FHttpOptionOps[F[_], A](fa: F[Option[A]]) {
    def toHttpResultIfFound(errIfMissing: => Throwable)(implicit encoder: Encoder[A],
                                                        M: MonadError[F, Throwable]): F[Response[F]] =
      fa.attempt.flatMap {
        case Left(l)        => errorResponse(l)
        case Right(Some(r)) => okResponse(r)
        case Right(None)    => notFoundResponse[F, A](errIfMissing)
      }
  }
}
