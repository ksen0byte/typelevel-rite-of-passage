package com.ksen0byte.jobsboard.core

import cats.effect.MonadCancelThrow
import cats.implicits.*
import com.ksen0byte.jobsboard.logging.syntax.*
import com.ksen0byte.jobsboard.domain.job.{Job, JobFilter, JobInfo}
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*
import LiveJobs.given
import com.ksen0byte.jobsboard.domain.pagination.Pagination
import org.typelevel.log4cats.Logger

import java.util.UUID

trait Jobs[F[_]] {
  // algebra
  // CRUD

  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
  def all(): F[List[Job]]
  def all(filter: JobFilter, pagination: Pagination): F[List[Job]]
  def find(id: UUID): F[Option[Job]]
  def update(id: UUID, jobInfo: JobInfo): F[Option[Job]]
  def delete(id: UUID): F[Int]
}

class LiveJobs[F[_]: MonadCancelThrow: Logger] private (xa: Transactor[F]) extends Jobs[F] {
  override def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] =
    sql"""
         INSERT INTO jobs (
           date,
           ownerEmail,
           company,
           title,
           description,
           externalUrl,
           remote,
           location,
           salaryLo,
           salaryHi,
           currency,
           country,
           tags,
           image,
           seniority,
           other,
           active
         ) VALUES (
           ${System.currentTimeMillis()},
           $ownerEmail,
           ${jobInfo.company},
           ${jobInfo.title},
           ${jobInfo.description},
           ${jobInfo.externalUrl},
           ${jobInfo.remote},
           ${jobInfo.location},
           ${jobInfo.salaryLo},
           ${jobInfo.salaryHi},
           ${jobInfo.currency},
           ${jobInfo.country},
           ${jobInfo.tags},
           ${jobInfo.image},
           ${jobInfo.seniority},
           ${jobInfo.other},
           false
         )
         """.update.withUniqueGeneratedKeys[UUID]("id").transact(xa)

  override def all(): F[List[Job]] =
    sql"""
      SELECT
        id,
        date,
        ownerEmail,
        company,
        title,
        description,
        externalUrl,
        remote,
        location,
        salaryLo,
        salaryHi,
        currency,
        country,
        tags,
        image,
        seniority,
        other,
        active
      FROM jobs
         """.query[Job].to[List].transact(xa)

  override def all(filter: JobFilter, pagination: Pagination): F[List[Job]] =
    val selectFragment =
      fr"""
        SELECT
          id,
          date,
          ownerEmail,
          company,
          title,
          description,
          externalUrl,
          remote,
          location,
          salaryLo,
          salaryHi,
          currency,
          country,
          tags,
          image,
          seniority,
          other,
          active
        """
    val fromFragment =
      fr"FROM jobs"

    val whereFragment = Fragments.whereAndOpt(
      filter.companies.toNel.map(companies => Fragments.in(fr"company", companies)),
      filter.countries.toNel.map(countries => Fragments.in(fr"country", countries)),
      filter.locations.toNel.map(locations => Fragments.in(fr"location", locations)),
      filter.seniorities.toNel.map(seniorities => Fragments.in(fr"seniority", seniorities)),
      filter.tags.toNel.map(tags => Fragments.or(tags.toList.map(tag => fr"$tag=any(tags)"): _*)),
      filter.maxSalary.map(salary => fr"salaryHi > $salary"),
      filter.remote.some.map(remote => fr"remote = $remote")
    )

    val paginationFragment =
      fr"ORDER BY id LIMIT ${pagination.limit} OFFSET ${pagination.offset}"

    val statement = selectFragment |+| fromFragment |+| whereFragment |+| paginationFragment

    Logger[F].info(statement.toString) *>
      statement.query[Job].to[List].transact(xa).logError(e => s"Failed query ${e.getMessage}")

  override def find(id: UUID): F[Option[Job]] =
    sql"""
    SELECT
      id,
      date,
      ownerEmail,
      company,
      title,
      description,
      externalUrl,
      remote,
      location,
      salaryLo,
      salaryHi,
      currency,
      country,
      tags,
      image,
      seniority,
      other,
      active
    FROM jobs WHERE id = $id
       """.query[Job].option.transact(xa)

  override def update(id: UUID, jobInfo: JobInfo): F[Option[Job]] =
    sql"""
         UPDATE jobs
         SET
           company = ${jobInfo.company},
           title = ${jobInfo.title},
           description = ${jobInfo.description},
           externalUrl = ${jobInfo.externalUrl},
           remote = ${jobInfo.remote},
           location = ${jobInfo.location},
           salaryLo = ${jobInfo.salaryLo},
           salaryHi = ${jobInfo.salaryHi},
           currency = ${jobInfo.currency},
           country = ${jobInfo.country},
           tags = ${jobInfo.tags},
           image = ${jobInfo.image},
           seniority = ${jobInfo.seniority},
           other = ${jobInfo.other}
         WHERE id = $id
         """.update.run.transact(xa).flatMap(_ => find(id))

  override def delete(id: UUID): F[Int] =
    sql"""
         DELETE FROM jobs
         WHERE id = $id
         """.update.run.transact(xa)
}

object LiveJobs {
  given jobRead: Read[Job] = Read[
    (
        UUID,                 // id
        Long,                 // date
        String,               // ownerEmail
        String,               // company
        String,               // title
        String,               // description
        String,               // externalUrl
        Boolean,              // remote
        String,               // location
        Option[Int],          // salaryLo
        Option[Int],          // salaryHi
        Option[String],       // currency
        Option[String],       // country
        Option[List[String]], // tags
        Option[String],       // image
        Option[String],       // seniority
        Option[String],       // other
        Boolean               // active
    )
  ].map {
    case (
          id: UUID,
          date: Long,
          ownerEmail: String,
          company: String,
          title: String,
          description: String,
          externalUrl: String,
          remote: Boolean,
          location: String,
          salaryLo: Option[Int] @unchecked,
          salaryHi: Option[Int] @unchecked,
          currency: Option[String] @unchecked,
          country: Option[String] @unchecked,
          tags: Option[List[String]] @unchecked,
          image: Option[String] @unchecked,
          seniority: Option[String] @unchecked,
          other: Option[String] @unchecked,
          active: Boolean
        ) =>
      Job(
        id = id,
        date = date,
        ownerEmail = ownerEmail,
        jobInfo = JobInfo(
          company = company,
          title = title,
          description = description,
          externalUrl = externalUrl,
          remote = remote,
          location = location,
          salaryLo = salaryLo,
          salaryHi = salaryHi,
          currency = currency,
          country = country,
          tags = tags,
          image = image,
          seniority = seniority,
          other = other
        ),
        active = active
      )
  }

  def apply[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveJobs[F]] = new LiveJobs[F](xa).pure[F]

}
