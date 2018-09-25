package stephenzoio.freesourcing.core

import cats.effect.IO
import doobie.hikari.HikariTransactor
import fs2.{Stream, StreamApp}
import fs2.StreamApp.ExitCode
import org.http4s.server.blaze.BlazeBuilder
import stephenzoio.freesourcing.backend.service._
import stephenzoio.freesourcing.shared.config.{CommonConfig, ConfigEnvironment}
import stephenzoio.freesourcing.shared.util.StreamResources
import io.circe.generic.auto._
import org.http4s.server.middleware.CORS
import stephenzoio.freesourcing.core.command.CommandOp2CombinedFree
import stephenzoio.freesourcing.core.data.event.EventOp2ConnectionIO
import stephenzoio.freesourcing.core.data.query.QueryOp2ConnectionIO
import stephenzoio.freesourcing.shared.db.DbTransactor
import stephenzoio.freesourcing.core.eventsrc.AuctionEventSourcing
import doobie.hikari.implicits._

object App extends StreamApp[IO] {
  import StreamResources._
  final case class Config(db: CommonConfig.Db)

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    val transactor: HikariTransactor[IO] = {
      val cfg = ConfigEnvironment.asM[IO, Config](None)
      DbTransactor.transactorWithMigrate[IO](cfg.map(_.db)).unsafeRunSync()
    }

    val serviceContext: IO[SC] = for {
      trans <- IO(transactor)
      interpreter = AuctionEventSourcing.commandOp2IO(CommandOp2CombinedFree,
                                                      QueryOp2ConnectionIO,
                                                      EventOp2ConnectionIO,
                                                      trans)
    } yield SC(interpreter)

    sys.addShutdownHook {
      transactor.shutdown.unsafeRunSync()
    }

    val service = CORS(AccountService(serviceContext))
    BlazeBuilder[IO]
      .bindHttp(8079, "localhost")
      .mountService(service, "/auction-example")
      .serve
  }
}
