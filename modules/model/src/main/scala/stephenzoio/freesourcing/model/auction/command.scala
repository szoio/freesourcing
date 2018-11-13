package stephenzoio.freesourcing.model.auction

import java.util.UUID

import cats.Id
import cats.data.{Const, EitherT}
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

  trait CommandTF[F[_]] {
    def commandSpec(commandId: UUID, command: Command): F[Either[Throwable, Unit]]
  }

  trait QueryTF[F[_]] {
    def opInt(): F[Int]
    def opString(): F[String]
  }

  trait Domain[E[_]] {
    def getUser(key: String): E[Option[String]]
    def createUser(key: String, a: String): E[Unit]
  }


  trait User[F[_]] {
    def get(key: String): F[Option[String]]
    def create(key: String, a: String): F[Unit]
  }

  trait KVStoreGet[G[_]] {
    def get(key: String): G[Option[String]]
  }

  trait KVStorePut[G[_]] {
    def put(key: String, a: String): G[Unit]
  }

  def userKVStore[G[_]](K: KVStoreGet[G] with KVStorePut[G]): User[G] = new User[G] {
    override def get(key: String): G[Option[String]] = K.get(key)
    override def create(key: String, name: String): G[Unit] = K.put(key, name)
  }

  val analysisInterpreter: KVStoreGet[Const[(Set[String], Map[String, String]), ?]] =
    new KVStoreGet[Const[(Set[String], Map[String, String]), ?]] with KVStorePut[Const[(Set[String], Map[String, String]), ?]] {
      override def get(key: String) = Const((Set(key), Map.empty))
      override def put(key: String, a: String) = Const((Set.empty, Map(key -> a)))
    }

//
//  sealed trait Event
//  trait EventTF[F[_]] {
//    def eventSpec(eventId: UUID, event: Event): F[Either[Throwable, Unit]]
//  }
//
//  def eventId: EventTF[Id] = new EventTF[Id] {
//    override def eventSpec(eventId: UUID,
//                           event: Event)
//      : Id[Either[Throwable, Unit]] = ???
//  }
//
//  val a: Either[Throwable, Unit] = eventId.eventSpec(???, ???)
//
//  type X[G] = CommandTF[QueryTF[G] with EventTF[G]]
//
//  def command2Event[G[_]] = new CommandTF[QueryTF[G] with EventTF[G]] {
//    override def commandSpec(commandId: UUID,
//                             command: Command)
//      : QueryTF[G]
//        with EventTF[G][Either[Throwable,
//                                                                                           Unit]] = ???
}
