package com.ksen0byte.jobsboard.modules

import cats.effect.*
import cats.implicits.*
import com.ksen0byte.jobsboard.core.{Jobs, LiveJobs}
import doobie.util.transactor.Transactor

final class Core[F[_]: MonadCancelThrow] private (val jobs: Jobs[F])

object Core:
  def apply[F[_]: Async](xa: Transactor[F]): Resource[F, Core[F]] =
    Resource.eval(LiveJobs[F](xa)).map(jobs => new Core(jobs))
