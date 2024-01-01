package com.ksen0byte.jobsboard.playground

import cats.effect.{IO, IOApp, Resource}
import com.ksen0byte.jobsboard.core.LiveJobs
import com.ksen0byte.jobsboard.domain.job.JobInfo
import doobie.*
import doobie.implicits.*
import doobie.util.*
import doobie.hikari.HikariTransactor

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

  override def run: IO[Unit] = postgresResource.use { xa =>
    for {
      jobs  <- LiveJobs[IO](xa)
      test1 <- jobs.find(java.util.UUID.fromString("af67b99c-06ea-4747-bdd3-0dac10d77a0d"))
      _     <- IO.println(s"Found Job ${test1}. Next...") *> IO(StdIn.readLine)
      //      _            <- IO.println("Ready. Next...")
      //      id           <- jobs.create("vlad@rockthejvm.com", jobInfo)
      //      _            <- IO.println("Created. Next...")
//      jobList      <- jobs.all()
//      _            <- IO.println(s"All jobs: $jobList. Next...") *> IO(StdIn.readLine)
//      _            <- jobs.update(id, jobInfo.copy(title = "SWE"))
//      job          <- jobs.find(id)
//      _            <- IO.println(s"Updated job $job. Next...") *> IO(StdIn.readLine)
//      _            <- jobs.delete(id)
//      jobListAfter <- jobs.all()
//      _            <- IO.println(s"Deleted job $id. Remaining jobs: $jobListAfter. Next...") *> IO(StdIn.readLine)
    } yield ()
  }
}
