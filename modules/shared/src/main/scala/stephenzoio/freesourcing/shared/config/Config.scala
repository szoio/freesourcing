package stephenzoio.freesourcing.shared.config
import io.circe.{Decoder, Encoder}
import stephenzoio.freesourcing.shared.json.FiniteDurationDecoder

import scala.concurrent.duration.FiniteDuration

object Config {
  implicit val durationDecoder: Decoder[FiniteDuration] with Encoder[FiniteDuration] =
    FiniteDurationDecoder()
  final case class AppConfig()
  implicit val configDecoder = io.circe.generic.semiauto.deriveDecoder[AppConfig]
}
