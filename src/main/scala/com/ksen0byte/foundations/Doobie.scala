package com.ksen0byte.jobsboard.foundations

import cats.effect.kernel.{MonadCancelThrow, Resource}
import cats.effect.{IO, IOApp}
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import doobie.implicits.*
import doobie.util.ExecutionContexts

object Doobie extends IOApp.Simple {

  case class Student(id: Int, name: String)

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver", // JDBC connector
    url = "jdbc:postgresql:docker",
    user = "docker",
    pass = "docker"
  )

  def findAllStudentNames: IO[List[String]] =
    val query  = sql"select name from students".query[String]
    val action = query.to[List]
    action.transact(xa)

  def saveStudent(student: Student): IO[Int] =
    val query  = sql"insert into students(id,name) values (${student.id}, ${student.name})"
    val action = query.update.run
    action.transact(xa)

  def findStudentsByInitial(letter: String): IO[List[Student]] =
    val selectPart = fr"select id, name"
    val fromPart   = fr"from students"
    val wherePart  = fr"where left(name, 1) = $letter"

    val statement = selectPart ++ fromPart ++ wherePart
    val action    = statement.query[Student].to[List]
    action.transact(xa)

  // organize code "repository"
  trait Students[F[_]]:
    def findById(id: Int): F[Option[Student]]
    def findAll: F[List[Student]]
    def create(name: String): F[Int]

  object Students:
    def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): Students[F] = new Students[F] {
      override def findById(id: Int): F[Option[Student]] =
        sql"select id, name from students where id=$id".query[Student].option.transact(xa)
      override def findAll: F[List[Student]] =
        sql"select id, name from students".query[Student].to[List].transact(xa)
      override def create(name: String): F[Int] =
        sql"insert into students(name) values ($name)".update.withUniqueGeneratedKeys[Int]("id").transact(xa)
    }
  end Students

  val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool[IO](16)
    xa <- HikariTransactor.newHikariTransactor[IO](
      driverClassName = "org.postgresql.Driver",
      url = "jdbc:postgresql:docker",
      user = "docker",
      pass = "docker",
      connectEC = ec
    )
  } yield xa

  val smallProgram = postgresResource.use { xa =>
    val studentsRepo = Students[IO](xa)
    for {
      id   <- studentsRepo.create("Mike")
      mike <- studentsRepo.findById(id)
      _    <- IO.println(s"The first student is $mike")
    } yield ()
  }

  override def run: IO[Unit] = smallProgram
}
