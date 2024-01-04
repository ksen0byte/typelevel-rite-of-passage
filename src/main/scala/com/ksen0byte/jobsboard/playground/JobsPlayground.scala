package com.ksen0byte.jobsboard.playground

import cats.effect.{IO, IOApp, Resource}
import com.ksen0byte.jobsboard.core.LiveJobs
import com.ksen0byte.jobsboard.domain.job.JobInfo
import doobie.*
import doobie.implicits.*
import doobie.util.*
import doobie.hikari.HikariTransactor
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.io.StdIn

object JobsPlayground extends IOApp.Simple {
  val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      driverClassName = "org.postgresql.Driver",
      url = "jdbc:postgresql:board",
      user = "docker",
      pass = "docker",
      connectEC = ec
    )
  } yield xa

  val jobInfo = JobInfo.minimal(
    company = "Rock the JVM",
    title = "Software Engineer",
    description = "Best job Ever",
    externalUrl = "rockthejvm.com",
    remote = false,
    location = "Ukraine"
  )

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] = postgresResource.use { xa =>
    for {
      jobs         <- LiveJobs[IO](xa)
      _            <- IO.println("Ready. Next...") *> IO(StdIn.readLine)
      id           <- jobs.create("vlad@rockthejvm.com", jobInfo)
      _            <- IO.println("Created. Next...") *> IO(StdIn.readLine)
      jobList      <- jobs.all()
      _            <- IO.println(s"All jobs: $jobList. Next...") *> IO(StdIn.readLine)
      _            <- jobs.update(id, jobInfo.copy(title = "SWE"))
      job          <- jobs.find(id)
      _            <- IO.println(s"Updated job $job. Next...") *> IO(StdIn.readLine)
      _            <- jobs.delete(id)
      jobListAfter <- jobs.all()
      _            <- IO.println(s"Deleted job $id. Remaining jobs: $jobListAfter. Next...") *> IO(StdIn.readLine)
    } yield ()
  }
}
