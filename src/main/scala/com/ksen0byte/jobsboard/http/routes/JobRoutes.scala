package com.ksen0byte.jobsboard.http.routes

import cats.effect.Concurrent
import cats.implicits.*
import com.ksen0byte.jobsboard.domain.job.{Job, JobInfo}
import com.ksen0byte.jobsboard.http.responses.FailureResponse
import com.ksen0byte.jobsboard.logging.syntax.logError
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

import java.util.UUID
import scala.collection.mutable

class JobRoutes[F[_]: Concurrent: Logger] private extends Http4sDsl[F]:
  // DATABASE
  val database = mutable.Map[UUID, Job]()

  // POST /jobs?offset=x&limit=y { filters } // TODO add query params
  private val allJobsRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    Ok(database.values)
  }

  // GET /jobs/UUID
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(jobId) =>
    database.get(jobId) match
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Cannot find job with id $jobId"))
  }

  // POST /jobs/create { jobInfo }
  private def createJob(jobInfo: JobInfo): F[Job] =
    Job(
      id = UUID.randomUUID(),
      date = System.currentTimeMillis(),
      ownerEmail = "TODO@rockthejvm.com",
      jobInfo = jobInfo,
      active = true
    ).pure[F]

  import com.ksen0byte.jobsboard.logging.syntax.*
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "create" =>
    for {
      jobInfo <- req.as[JobInfo].logError(e => s"Parsing payload failed with $e")
      job     <- createJob(jobInfo)
      _       <- database.put(job.id, job).pure[F]
      resp    <- Created(job.id)
    } yield resp
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / UUIDVar(jobId) =>
    database.get(jobId) match
      case Some(job) =>
        for {
          jobInfo <- req.as[JobInfo]
          _       <- database.put(jobId, job.copy(jobInfo = jobInfo)).pure[F]
          resp    <- Ok()
        } yield resp
      case None => NotFound(FailureResponse(s"Cannot update job with id $jobId"))
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ DELETE -> Root / UUIDVar(jobId) =>
    database.get(jobId) match
      case Some(job) =>
        for {
          _    <- database.remove(jobId).pure[F]
          resp <- Ok()
        } yield resp
      case None => NotFound(FailureResponse(s"Cannot delete job with id $jobId"))
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobsRoutes <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )

object JobRoutes:
  def apply[F[_]: Concurrent: Logger]: JobRoutes[F] = new JobRoutes[F]
