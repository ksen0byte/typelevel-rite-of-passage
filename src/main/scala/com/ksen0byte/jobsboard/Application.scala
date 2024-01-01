package com.ksen0byte.jobsboard

import cats.effect.*
import com.ksen0byte.jobsboard.config.EmberConfig
import com.ksen0byte.jobsboard.config.syntax.*
import com.ksen0byte.jobsboard.http.HttpApi
import org.http4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource

object Application extends IOApp.Simple:

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
      EmberServerBuilder
        .default[IO]
        .withHost(config.host)
        .withPort(config.port)
        .withHttpApp(HttpApi[IO].endpoints.orNotFound)
        .build
        .use(_ => IO.println("Rock the JVM!") *> IO.never)
    }
