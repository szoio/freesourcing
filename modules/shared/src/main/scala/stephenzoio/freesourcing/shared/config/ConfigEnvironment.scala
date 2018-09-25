package stephenzoio.freesourcing.shared.config

import cats.effect.Async
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions, Config => TSConfig}
import io.circe._
import io.circe.parser._

import scala.util.Try

object ConfigEnvironment {

  private[config] def unsafeConfig(profile: Option[String] = None): TSConfig = {
    val rootConfig                        = ConfigFactory.load()
    val sharedPrefix                      = "base"
    val environmentPrefix: Option[String] = Try { rootConfig.getString("config-environment") }.toOption
    val overridePrefix                    = "vars"

    println(s"Using shared config prefix [$sharedPrefix]")

    environmentPrefix.foreach { prefix =>
      println(s"Using environment config prefix [$prefix]")
    }

    println(s"Using override config prefix [$overridePrefix] (for environment variables)")

    def getConfig(configString: String) = Try { rootConfig.getConfig(configString) }.toOption
    def getConfigOption(configString: Option[String]) =
      Try { configString.map(rootConfig.getConfig) }.toOption.flatten

    val optionalSharedConfig      = getConfig(sharedPrefix)
    val optionalEnvironmentConfig = getConfigOption(environmentPrefix)
    val overrideEnvironmentConfig = getConfig(overridePrefix)
    val profileConfig             = getConfigOption(profile.map(p => s"profiles.$p"))

    val configOptionList: Seq[Option[TSConfig]] =
      List(overrideEnvironmentConfig, profileConfig, optionalEnvironmentConfig, optionalSharedConfig)
    configOptionList
      .foldRight(optionalSharedConfig) {
        case (Some(c1), Some(c2)) => Some(c1.withFallback(c2))
        case (cO1, cO2)           => cO1.orElse(cO2)
      }
      .getOrElse(rootConfig)
  }

  private[config] def as[A](profile: Option[String])(implicit decoder: Decoder[A]): Either[Error, A] = {
    val jsonString: String = unsafeConfig(profile).root().render(ConfigRenderOptions.concise())
    println(s"Config: ${jsonString.substring(0, 50)}...")
    val jsonA: Either[Error, Json] = parse(jsonString)
    jsonA.flatMap(decoder.decodeJson)
  }

  def asM[M[_], A: Decoder](profile: Option[String] = None)(implicit async: Async[M]): M[A] = {
    async.rethrow(async.delay(as[A](profile)))
  }
}
