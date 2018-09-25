package stephenzoio.freesourcing.shared.util

trait Types {
  type Attempt[A] = Either[Throwable, A]
}
