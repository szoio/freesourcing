package stephenzoio.freesourcing.shared.json

import io.circe.Decoder.Result
import io.circe._

final case class ValueClassEncoder[A, B](ap: B => A)(unap: A => Option[B])(implicit decodeB: Decoder[B],
                                                                           encodeB: Encoder[B])
    extends Encoder[A]
    with Decoder[A] {

  override def apply(c: HCursor): Result[A] = decodeB.apply(c).map(ap)

  override def apply(x: A): Json = unap(x).map(encodeB(_)).getOrElse(Json.Null)
}
