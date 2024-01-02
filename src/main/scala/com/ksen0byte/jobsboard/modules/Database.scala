package com.ksen0byte.jobsboard.modules

import cats.effect.{Async, Resource}
import com.ksen0byte.jobsboard.config.PostgresConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

object Database:
  def makePostgresResource[F[_]: Async](config: PostgresConfig): Resource[F, HikariTransactor[F]] = for {
    ec <- ExecutionContexts.fixedThreadPool(config.nThreads)
    xa <- HikariTransactor.newHikariTransactor[F](
      driverClassName = "org.postgresql.Driver",
      url = config.url,
      user = config.user,
      pass = config.pass,
      connectEC = ec
    )
  } yield xa
