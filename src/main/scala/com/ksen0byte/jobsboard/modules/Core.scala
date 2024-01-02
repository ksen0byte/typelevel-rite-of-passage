package com.ksen0byte.jobsboard.modules

import cats.effect.*
import cats.implicits.*
import com.ksen0byte.jobsboard.core.{Jobs, LiveJobs}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

final class Core[F[_]: MonadCancelThrow] private (val jobs: Jobs[F])

// postgres > jobs > core > httpApi > app
object Core {
  def postgresResource[F[_]: Async]: Resource[F, HikariTransactor[F]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[F](
      driverClassName = "org.postgresql.Driver",
      url = "jdbc:postgresql:board", // TODO move to the config
      user = "docker",
      pass = "docker",
      connectEC = ec
    )
  } yield xa

  def apply[F[_]: Async]: Resource[F, Core[F]] = {
    postgresResource[F]
      .evalMap(postgres => LiveJobs[F](postgres))
      .map(jobs => new Core(jobs))
  }
}
