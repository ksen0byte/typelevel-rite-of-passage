package com.ksen0byte.jobsboard

import cats.effect.*
import com.ksen0byte.jobsboard.config.EmberConfig
import com.ksen0byte.jobsboard.config.syntax.*
import com.ksen0byte.jobsboard.modules.{Core, HttpApi}
import org.http4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource

object Application extends IOApp.Simple:

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
      val appResource = for {
        core    <- Core[IO]
        httpApi <- HttpApi[IO](core)
        server <- EmberServerBuilder
          .default[IO]
          .withHost(config.host)
          .withPort(config.port)
          .withHttpApp(httpApi.endpoints.orNotFound)
          .build
      } yield server

      appResource.use(_ => IO.println("Rock the JVM!") *> IO.never)
    }
