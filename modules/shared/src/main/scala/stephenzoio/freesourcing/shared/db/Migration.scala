package stephenzoio.freesourcing.shared.db

import cats.effect.Sync
import org.flywaydb.core.Flyway
import stephenzoio.freesourcing.shared.config.CommonConfig

object Migration {
  def flywayMigrate[M[_]](flywayCfg: CommonConfig.Db)(implicit suspend: Sync[M]) = suspend.delay {
    val flyway = new Flyway()
    flyway.setDataSource(flywayCfg.url, flywayCfg.user, flywayCfg.password)
    flyway.migrate()
  }

  def flywayClean[M[_]](flywayCfg: CommonConfig.Db)(implicit suspend: Sync[M]) = suspend.delay {
    val flyway = new Flyway()
    flyway.setDataSource(flywayCfg.url, flywayCfg.user, flywayCfg.password)
    flyway.clean()
  }
}
