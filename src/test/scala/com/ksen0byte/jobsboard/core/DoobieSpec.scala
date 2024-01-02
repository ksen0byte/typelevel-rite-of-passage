package com.ksen0byte.jobsboard.core

import cats.effect.{IO, Resource}
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.util.*
import org.testcontainers.containers.PostgreSQLContainer

trait DoobieSpec {
  val initScript: String

  val postgres: Resource[IO, PostgreSQLContainer[Nothing]] = {
    val acquire = IO {
      val container: PostgreSQLContainer[Nothing] = new PostgreSQLContainer("postgres").withInitScript(initScript)
      container.start()
      container
    }
    val release = (container: PostgreSQLContainer[Nothing]) => IO(container.stop())
    Resource.make(acquire)(release)
  }

  val transactor: Resource[IO, Transactor[IO]] = for {
    db <- postgres
    ce <- ExecutionContexts.fixedThreadPool[IO](1)
    xa <- HikariTransactor.newHikariTransactor[IO](
      driverClassName = "org.postgresql.Driver",
      url = db.getJdbcUrl,
      user = db.getUsername,
      pass = db.getPassword,
      connectEC = ce
    )
  } yield xa

}
