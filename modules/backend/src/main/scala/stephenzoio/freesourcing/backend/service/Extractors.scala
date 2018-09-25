package stephenzoio.freesourcing.backend.service
import java.util.UUID

import scala.util.Try

object Extractors {
  object uuid {
    def unapply(str: String): Option[UUID] = {
      if (!str.isEmpty)
        Try(java.util.UUID.fromString(str)).toOption
      else
        None
    }
  }
}
