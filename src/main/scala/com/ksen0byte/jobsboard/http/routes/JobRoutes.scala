package com.ksen0byte.jobsboard.http.routes

import cats.effect.Concurrent
import cats.implicits.*
import com.ksen0byte.jobsboard.core.Jobs
import com.ksen0byte.jobsboard.domain.job.{Job, JobInfo}
import com.ksen0byte.jobsboard.http.responses.FailureResponse
import com.ksen0byte.jobsboard.http.validation.syntax.*
import com.ksen0byte.jobsboard.logging.syntax.logError
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

class JobRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F]) extends HttpValidationDsl[F]:
  // POST /jobs?offset=x&limit=y { filters } // TODO add query params
  private val allJobsRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    for {
      jobList <- jobs.all()
      resp    <- Ok(jobList)
    } yield resp
  }

  // GET /jobs/UUID
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(jobId) =>
    jobs.find(jobId).flatMap {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Cannot find job with id $jobId"))
    }
  }

  // POST /jobs/create { jobInfo }
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "create" =>
    req
      .validate[JobInfo] { jobInfo =>
        for {
          jobId <- jobs.create("TODO@rockthejvm.com", jobInfo)
          resp  <- Created(jobId)
        } yield resp
      }
      .logError(e => s"Parsing payload failed with $e")
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / UUIDVar(jobId) =>
    req
      .validate[JobInfo] { jobInfo =>
        for {
          maybeJob <- jobs.update(jobId, jobInfo)
          resp <- maybeJob match
            case Some(job) => Ok()
            case None      => NotFound(FailureResponse(s"Cannot update job with id $jobId"))
        } yield resp
      }
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ DELETE -> Root / UUIDVar(jobId) =>
    jobs.find(jobId).flatMap {
      case Some(job) =>
        for {
          _    <- jobs.delete(jobId)
          resp <- Ok()
        } yield resp
      case None => NotFound(FailureResponse(s"Cannot delete job with id $jobId"))
    }
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobsRoutes <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )

object JobRoutes:
  def apply[F[_]: Concurrent: Logger](jobs: Jobs[F]): JobRoutes[F] = new JobRoutes[F](jobs)
