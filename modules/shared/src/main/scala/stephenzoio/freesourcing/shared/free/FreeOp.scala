package stephenzoio.freesourcing.shared.free

import cats.InjectK
import cats.free.Free

trait FreeOp[F[_], A] { this: F[A] =>
  def liftF: Free[F, A]                                   = Free.liftF(this)
  def inject[G[_]](implicit I: InjectK[F, G]): Free[G, A] = Free.inject(this)
}
