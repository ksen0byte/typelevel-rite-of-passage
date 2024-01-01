package com.ksen0byte.jobsboard.foundations

import cats.*
import cats.data.Validated
import cats.effect.*
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.*
import org.http4s.server.*
import org.typelevel.ci.CIString
import java.util.UUID

object Http4s extends IOApp.Simple {

  // simulate http server with "students" and "courses"
  type Student = String
  case class Instructor(firstName: String, lastName: String)
  case class Course(id: String, title: String, year: Int, students: List[Student], instructorName: String)

  object CourseRepository {
    private val catsEffectCourse = Course(
      "ab48f4ae-9a58-490f-bfab-c4308b0a2c97",
      "Rock the JVM Ultimate Scala course",
      2022,
      List("Vlad", "Tania"),
      "Martin Odersky"
    )
    private val courses: Map[String, Course] = Map(catsEffectCourse.id -> catsEffectCourse)

    // API
    def findCoursesById(courseId: UUID): Option[Course]     = courses.get(courseId.toString)
    def findCoursesByInstructor(name: String): List[Course] = courses.values.filter(_.instructorName == name).toList
  }

  // essential REST endpoints
  // EGT localhost:8080/courses?instructor=Martin%20Odersky&year=2022
  // EGT localhost:8080/courses/ab48f4ae-9a58-490f-bfab-c4308b0a2c97/students

  object InstructorQueryParamMatcher extends QueryParamDecoderMatcher[String]("instructor")
  object YearQueryParamMatcher       extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")

  def courseRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(instructor) +& YearQueryParamMatcher(maybeYear) =>
        val courses = CourseRepository.findCoursesByInstructor(instructor)
        maybeYear match
          case Some(yearValidated) =>
            yearValidated match
              case Validated.Valid(year)        => Ok(courses.filter(_.year == year).asJson)
              case Validated.Invalid(errorList) => BadRequest(errorList.map(_.message).toList.asJson)
          case None => Ok(courses.asJson)

      case GET -> Root / "courses" / UUIDVar(courseId) / "students" =>
        CourseRepository.findCoursesById(courseId).map(_.students) match
          case Some(students) => Ok(students.asJson, Header.Raw(CIString("My-Custom-Header"), "Hello, Header"))
          case None           => NotFound(s"No course with $courseId was found")
    }
  }

  def healthEndpoint[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*
    HttpRoutes.of[F] { case GET -> Root / "health" =>
      Ok("All good!")
    }
  }

  def allRoutes[F[_]: Monad]: HttpRoutes[F] = courseRoutes[F] <+> healthEndpoint[F]

  def routerWithPathPrefixes = Router(
    "/api" -> courseRoutes[IO],
    "/private" -> healthEndpoint[IO],
  ).orNotFound
  
  override def run: IO[Unit] = EmberServerBuilder
    .default[IO]
    .withHttpApp(routerWithPathPrefixes)
    .build
    .use(_ => IO.println("Server ready!") *> IO.never)
}
