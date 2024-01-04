package com.ksen0byte.jobsboard.domain

import java.util.UUID
import cats.syntax.*

object job {
  case class Job(id: UUID, date: Long, ownerEmail: String, jobInfo: JobInfo, active: Boolean = false)

  case class JobInfo(
      company: String,
      title: String,
      description: String,
      externalUrl: String,
      salaryLo: Option[Int],
      salaryHi: Option[Int],
      currency: Option[String],
      remote: Boolean,
      location: String,
      country: Option[String],
      tags: Option[List[String]],
      image: Option[String],
      seniority: Option[String],
      other: Option[String]
  )

  object JobInfo {
    val empty: JobInfo = JobInfo("", "", "", "", None, None, None, false, "", None, None, None, None, None)
    def minimal(
        company: String,
        title: String,
        description: String,
        externalUrl: String,
        remote: Boolean,
        location: String
    ): JobInfo = new JobInfo(
      company = company,
      title = title,
      description = description,
      externalUrl = externalUrl,
      salaryLo = None,
      salaryHi = None,
      currency = None,
      remote = remote,
      location = location,
      country = None,
      tags = None,
      image = None,
      seniority = None,
      other = None
    )
  }

  final case class JobFilter(
      companies: List[String] = List(),
      locations: List[String] = List(),
      countries: List[String] = List(),
      seniorities: List[String] = List(),
      tags: List[String] = List(),
      maxSalary: Option[Int] = None,
      remote: Boolean = false
  )

}
