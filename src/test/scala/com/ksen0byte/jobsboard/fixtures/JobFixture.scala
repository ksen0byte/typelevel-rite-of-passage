package com.ksen0byte.jobsboard.fixtures

import cats.syntax.all.*
import com.ksen0byte.jobsboard.domain.job.{Job, JobInfo}

import java.util.UUID

trait JobFixture {
  val NotFoundJobUuid: UUID = UUID.fromString("6ea79557-3112-4c84-a8f5-1d1e2c300948")
  val AwesomeJobUuid: UUID  = UUID.fromString("843df718-ec6e-4d49-9289-f799c0f40064")

  val AwesomeJob: Job = Job(
    AwesomeJobUuid,
    1659186086L,
    "daniel@rockthejvm.com",
    JobInfo(
      company = "Awesome Company",
      title = "Tech Lead",
      description = "An awesome job in Berlin",
      externalUrl = "https://rockthejvm.com/awesomejob",
      salaryLo = 2000.some,
      salaryHi = 3000.some,
      currency = "EUR".some,
      remote = false,
      location = "Berlin",
      country = "Germany".some,
      tags = Some(List("scala", "scala-3", "cats")),
      image = None,
      seniority = "Senior".some,
      other = None
    )
  )

  val InvalidJob: Job = Job(null, 42L, "nothing@gmail.com", JobInfo.empty)
  val UpdatedAwesomeJob: Job = Job(
    AwesomeJobUuid,
    1659186086L,
    "daniel@rockthejvm.com",
    JobInfo(
      company = "Awesome Company (Spain Branch)",
      title = "Engineering Manager",
      description = "An awesome job in Barcelona",
      externalUrl = "http://www.awesome.com",
      salaryLo = 2200.some,
      salaryHi = 3200.some,
      currency = "USD".some,
      remote = false,
      location = "Barcelona",
      country = "Spain".some,
      tags = Some(List("scala", "scala-3", "zio")),
      image = "http://www.awesome.com/logo.png".some,
      seniority = "Highest".some,
      other = "Some additional info".some
    )
  )

  val RockTheJvmNewJob: JobInfo = JobInfo(
    company = "RockTheJvm",
    title = "Technical Author",
    description = "For the glory of the RockTheJvm!",
    externalUrl = "https://rockthejvm.com/",
    salaryLo = 2000.some,
    salaryHi = 3500.some,
    currency = "EUR".some,
    remote = true,
    location = "From remote",
    country = "Romania".some,
    tags = Some(List("scala", "scala-3", "cats", "akka", "spark", "flink", "zio")),
    image = None,
    seniority = "High".some,
    other = None
  )

  val RockTheJvmJobWithNotFoundId: Job = AwesomeJob.copy(id = NotFoundJobUuid)

  val AnotherAwesomeJobUuid: UUID = UUID.fromString("19a941d0-aa19-477b-9ab0-a7033ae65c2b")
  val AnotherAwesomeJob: Job      = AwesomeJob.copy(id = AnotherAwesomeJobUuid)

  val RockTheJvmAwesomeJob: Job = AwesomeJob.copy(jobInfo = AwesomeJob.jobInfo.copy(company = "RockTheJvm"))

  val NewJobUuid: UUID = UUID.fromString("efcd2a64-4463-453a-ada8-b1bae1db4377")
  val AwesomeNewJob: JobInfo = JobInfo(
    company = "Awesome Company",
    title = "Tech Lead",
    description = "An awesome job in Berlin",
    externalUrl = "example.com",
    salaryLo = 2000.some,
    salaryHi = 3000.some,
    currency = "EUR".some,
    remote = false,
    location = "Berlin",
    country = "Germany".some,
    tags = Some(List("scala", "scala-3", "cats")),
    image = None,
    seniority = "High".some,
    other = None
  )
}
