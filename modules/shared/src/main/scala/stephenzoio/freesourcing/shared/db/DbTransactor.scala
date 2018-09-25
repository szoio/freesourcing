package stephenzoio.freesourcing.shared.db

import cats.implicits._
import cats.effect.Async
import doobie.hikari.HikariTransactor
import doobie.hikari.HikariTransactor.newHikariTransactor
import stephenzoio.freesourcing.shared.config.CommonConfig

object DbTransactor {
  def prepareHikariTransactor[M[_]: Async](transactorCfg: CommonConfig.Db) =
    newHikariTransactor[M](driverClassName = transactorCfg.driver,
                           url = transactorCfg.url,
                           user = transactorCfg.user,
                           pass = transactorCfg.password)

  def transactorWithMigrate[M[_]: Async](dbConfig: M[CommonConfig.Db]): M[HikariTransactor[M]] =
    for {
      cfg <- dbConfig
      t   <- DbTransactor.prepareHikariTransactor[M](cfg)
      _   <- Migration.flywayMigrate[M](cfg)
    } yield t
}
