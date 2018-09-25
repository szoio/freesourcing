package stephenzoio.freesourcing.shared.json

import io.circe.Decoder.Result
import io.circe._

final case class EnumEncoder[A](fromEnum: A => String)(toEnum: String => Option[A])(
    implicit stringDecoder: Decoder[String],
    stringEncoder: Encoder[String])
    extends Encoder[A]
    with Decoder[A] {

  override def apply(c: HCursor): Result[A] =
    stringDecoder
      .apply(c)
      .flatMap(s =>
        toEnum(s) match {
          case Some(a) => Right(a)
          case None    => Left(DecodingFailure(s"Unable to decode string value $s.", List.empty))
      })

  override def apply(x: A): Json = stringEncoder(fromEnum(x))
}
