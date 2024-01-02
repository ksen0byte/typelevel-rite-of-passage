package com.ksen0byte.jobsboard.http.validation

import cats.*
import cats.data.*
import cats.data.Validated.*
import cats.implicits.*
import com.ksen0byte.jobsboard.domain.job.JobInfo

import java.net.URL
import scala.util.{Failure, Success, Try}

object validators {
  sealed trait ValidationFailure(val errorMessage: String)
  case class EmptyField(fieldName: String) extends ValidationFailure(s"'$fieldName' is empty")
  case class InvalidUrl(fieldName: String) extends ValidationFailure(s"'$fieldName' is not a valid URL")
  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

  trait Validator[A]:
    def validate(value: A): ValidationResult[A]

  private def validateRequired[A](field: A, fieldName: String)(required: A => Boolean): ValidationResult[A] =
    if required(field) then field.validNel
    else EmptyField(fieldName).invalidNel

  private def validateUrl(field: String, fieldName: String): ValidationResult[String] =
    Try(URL(field).toURI) match
      case Success(_) => field.validNel
      case Failure(_) => InvalidUrl(fieldName).invalidNel

  given Validator[JobInfo] = (jobInfo: JobInfo) => {
    val JobInfo(
      company,
      title,
      description,
      externalUrl,
      salaryLo,
      salaryHi,
      currency,
      remote,
      location,
      country,
      tags,
      image,
      seniority,
      other
    ) = jobInfo

    val validCompany     = validateRequired(company, "company")(_.nonEmpty)
    val validTitle       = validateRequired(title, "title")(_.nonEmpty)
    val validDescription = validateRequired(description, "description")(_.nonEmpty)
    val validLocation    = validateRequired(location, "location")(_.nonEmpty)
    val validExternalUrl = validateUrl(externalUrl, "externalUrl")

    (
      validCompany,       // company
      validTitle,         // title
      validDescription,   // description
      validExternalUrl,   // externalUrl
      salaryLo.validNel,  // salaryLo
      salaryHi.validNel,  // salaryHi
      currency.validNel,  // currency
      remote.validNel,    // remote
      validLocation,      // location
      country.validNel,   // country
      tags.validNel,      // tags
      image.validNel,     // image
      seniority.validNel, // seniority
      other.validNel      // other
    ).mapN(JobInfo.apply)
  }

}
