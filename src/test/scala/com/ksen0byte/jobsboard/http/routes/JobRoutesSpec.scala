package com.ksen0byte.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.implicits.*
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.ksen0byte.jobsboard.core.Jobs
import com.ksen0byte.jobsboard.domain.{job, pagination}
import com.ksen0byte.jobsboard.domain.job.{Job, JobFilter, JobInfo}
import com.ksen0byte.jobsboard.domain.pagination.Pagination
import com.ksen0byte.jobsboard.fixtures.JobFixture
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.dsl.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

class JobRoutesSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with Http4sDsl[IO] with JobFixture {
  val jobs: Jobs[IO] = new Jobs[IO]:
    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] = IO.pure(NewJobUuid)

    override def all(): IO[List[Job]] = IO.pure(List(AwesomeJob))

    override def all(filter: JobFilter, pagination: Pagination): IO[List[Job]] =
      if filter.remote then IO.pure(List.empty)
      else IO.pure(List(AwesomeJob))

    override def find(id: UUID): IO[Option[Job]] =
      if id == AwesomeJobUuid then IO.pure(Some(AwesomeJob)) else IO.pure(None)

    override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]] =
      if id == AwesomeJobUuid then IO.pure(Some(UpdatedAwesomeJob)) else IO.pure(None)

    override def delete(id: UUID): IO[Int] =
      if id == AwesomeJobUuid then IO.pure(1) else IO.pure(0)

  given logger: Logger[IO]      = Slf4jLogger.getLogger[IO]
  val jobRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs).routes

  "JobRoutes" - {
    "should return a job with a given id" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
        )
        job <- response.as[Job]
      } yield {
        response.status shouldBe Status.Ok
        job shouldBe AwesomeJob
      }
    }

    "should return all jobs" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs").withEntity(JobFilter())
        )
        jobs <- response.as[List[Job]]
      } yield {
        response.status shouldBe Status.Ok
        jobs shouldBe List(AwesomeJob)
      }
    }

    "should return all jobs that satisfy a filter" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs").withEntity(JobFilter(remote = true))
        )
        jobs <- response.as[List[Job]]
      } yield {
        response.status shouldBe Status.Ok
        jobs shouldBe List()
      }
    }

    "should create a new job" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/create").withEntity(AwesomeJob.jobInfo)
        )
        createdJobId <- response.as[UUID]
      } yield {
        response.status shouldBe Status.Created
        createdJobId shouldBe NewJobUuid
      }
    }

    "should only update just a job that exists" in {
      for {
        responseOk <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(AwesomeJob.jobInfo)
        )
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/6ea79557-3112-4c84-a8f5-000000000000")
            .withEntity(AwesomeJob.jobInfo)
        )
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }

    "should only delete a job that exists" in {
      for {
        responseOk <- jobRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
        )
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/6ea79557-3112-4c84-a8f5-000000000000")
        )
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }
  }

}
