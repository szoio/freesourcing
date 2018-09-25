package stephenzoio.freesourcing.shared.config

object CommonConfig {
  final case class Db(driver: String, url: String, user: String, password: String)
}
