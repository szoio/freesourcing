package stephenzoio.freesourcing.data

import java.time.Instant
import java.util.UUID

import cats.effect.IO
import doobie.hikari.implicits._
import doobie.scalatest.IOChecker
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import stephenzoio.freesourcing.shared.db.{DbTransactor, Migration}
import stephenzoio.freesourcing.core.eventsrc.sql
import stephenzoio.freesourcing.core.eventsrc.sql.EventSpec
import stephenzoio.freesourcing.shared.config.{CommonConfig, ConfigEnvironment}
import stephenzoio.freesourcing.shared.db.implicits._
import doobie.postgres.implicits._

class DbTests extends WordSpec with Matchers with BeforeAndAfterAll with IOChecker {

  final case class PaymentConfig(db: CommonConfig.Db)

  val transactor = (for {
    cfg <- ConfigEnvironment.asM[IO, PaymentConfig](None)
    t   <- DbTransactor.prepareHikariTransactor[IO](cfg.db)
    _   <- Migration.flywayMigrate[IO](cfg.db)
  } yield t).unsafeRunSync()

  override protected def afterAll(): Unit = {
    super.afterAll()
    transactor.shutdown.unsafeRunSync()
  }

  val now = Instant.now

  "event log" must {
    "fetchByUser" in check(sql.appendQ("event", EventSpec("{}".asJson, UUID.randomUUID(), UUID.randomUUID())))
  }

  // TODO: db tests for everything
}
