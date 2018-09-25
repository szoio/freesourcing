package stephenzoio.freesourcing.shared.json
import io.circe.Decoder.Result
import io.circe._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

final case class FiniteDurationDecoder()(implicit stringDecoder: Decoder[String],
                                         stringEncoder: Encoder[String])
    extends Decoder[FiniteDuration]
    with Encoder[FiniteDuration] {
  override def apply(c: HCursor): Result[FiniteDuration] =
    stringDecoder
      .apply(c)
      .flatMap(durationString =>
        Try {
          val duration: Duration = Duration(durationString)
          new FiniteDuration(duration.length, duration.unit)
        }.toOption match {
          case Some(finiteDuration) =>
            Right(finiteDuration)
          case None => Left(DecodingFailure(s"Unable to parse duration $durationString.", List.empty))
      })

  override def apply(x: FiniteDuration): Json = stringEncoder.apply(x.toString)
}
