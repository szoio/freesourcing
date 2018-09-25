package stephenzoio.freesourcing.shared.db
import doobie.util.meta.Meta
import io.circe.Json
import org.postgresql.util.PGobject

trait Metas {

  /**
    * Adds support for serializing json from/to postgres jsonb type
    **/
  implicit val jsonMeta: Meta[Json] =
    Meta
      .other[PGobject]("jsonb")
      .xmap[Json](
        a =>
          io.circe.parser.parse(a.getValue).getOrElse {
            throw new Exception(s"invalid data from db: ${a.getValue} is not a valid json")
        },
        a => {
          val v = new PGobject()
          v.setType("jsonb")
          v.setValue(a.noSpaces)
          v
        }
      )
}
