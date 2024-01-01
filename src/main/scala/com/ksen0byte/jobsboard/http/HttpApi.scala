package com.ksen0byte.jobsboard.http

import cats.effect.Concurrent
import cats.implicits.*
import com.ksen0byte.jobsboard.http.routes.{HealthRoutes, JobRoutes}
import org.http4s.*
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

class HttpApi[F[_]: Concurrent: Logger] private {
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes    = JobRoutes[F].routes

  val endpoints: HttpRoutes[F] = Router(
    "/api" -> (healthRoutes <+> jobRoutes)
  )
}

object HttpApi:
  def apply[F[_]: Concurrent: Logger]: HttpApi[F] = new HttpApi[F]
