package stephenzoio.freesourcing.model.auction

import java.util.UUID

import cats.data.EitherT
import cats.free.Free
import stephenzoio.freesourcing.shared.free.FreeOp

object command {
  sealed trait Command {
    def withId(id: UUID)                         = CommandSpec(id, this)
    def liftWithId(id: UUID): CommandFreeE[Unit] = CommandSpec(id, this).liftF
  }

  sealed trait CommandOp[A]                                       extends FreeOp[CommandOp, A]
  final case class CommandSpec(commandId: UUID, command: Command) extends CommandOpE[Unit]

  type CommandOpE[A]    = CommandOp[Either[Throwable, A]]
  type CommandFree[A]   = Free[CommandOp, A]
  type CommandFreeE[A]  = CommandFree[Either[Throwable, A]]
  type CommandFreeET[A] = EitherT[CommandFree, Throwable, A]

  object account {
    final case class CreateAccount(accountId: UUID, userName: String, funds: BigDecimal) extends Command
    final case class UpdateAccount(accountId: UUID, userName: String)                    extends Command
    final case class AddFunds(accountId: UUID, funds: BigDecimal)                        extends Command
    final case class ReserveFunds(accountId: UUID,
                                  reservationId: UUID,
                                  funds: BigDecimal,
                                  description: String)
        extends Command
    final case class CancelReservation(reservationId: UUID)                           extends Command
    final case class ConfirmReservation(reservationId: UUID, finalAmount: BigDecimal) extends Command
  }
}
