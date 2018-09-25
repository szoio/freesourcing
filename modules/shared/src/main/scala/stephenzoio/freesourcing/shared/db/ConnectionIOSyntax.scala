package stephenzoio.freesourcing.shared.db
import cats.Functor
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import cats.implicits._
import doobie.enum.SqlState
import org.postgresql.util.PSQLException
import stephenzoio.freesourcing.shared.util.Attempt

trait ConnectionIOSyntax {
  implicit class ConIOOps[A](cio: ConnectionIO[A]) {
    def toAttemptUnit: ConnectionIO[Attempt[Unit]] = cio.attempt.map(x => x.map(_ => ()))
  }

  implicit class ConIOIntOps(cio: ConnectionIO[Int]) {
    def attemptNonEmptyUnit(errorIfEmpty: Throwable): ConnectionIO[Attempt[Unit]] =
      cio.attempt.map { x =>
        x.flatMap {
          case 0 => Left(errorIfEmpty)
          case _ => Right(())
        }
      }
  }

  implicit class DbFreeErrors[F[_], E <: Throwable, A](fea: F[Either[E, A]])(implicit F: Functor[F]) {
    def mapDbErrors(errorMap: PartialFunction[SqlState, Throwable]): F[Attempt[A]] =
      fea.map {
        case Left(e: PSQLException) => Left(errorMap.lift(SqlState(e.getSQLState)).getOrElse(e))
        case value                  => value
      }

    def mapToUnit: F[Attempt[Unit]] = fea.map(x => x.map(_ => ()))
  }
}
