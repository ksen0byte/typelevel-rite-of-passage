package com.ksen0byte.jobsboard.http.validation

import cats.MonadThrow
import cats.data.*
import cats.data.Validated.*
import cats.implicits.*
import com.ksen0byte.jobsboard.http.responses.FailureResponse
import com.ksen0byte.jobsboard.http.validation.validators.{ValidationResult, Validator}
import com.ksen0byte.jobsboard.logging.syntax.*
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.typelevel.log4cats.Logger

object syntax {
  private def validateEntity[A](entity: A)(using validator: Validator[A]): ValidationResult[A] =
    validator.validate(entity)

  trait HttpValidationDsl[F[_]: MonadThrow: Logger] extends Http4sDsl[F] {
    extension (req: Request[F])
      def validate[A: Validator](serverLogicIfValid: A => F[Response[F]])(using EntityDecoder[F, A]): F[Response[F]] =
        req
          .as[A]
          .logError(e => s"Parsing payload failed $e")
          .map(validateEntity)
          .flatMap {
            case Valid(entity)   => serverLogicIfValid(entity)
            case Invalid(errors) => BadRequest(FailureResponse(errors.toList.map(_.errorMessage).mkString(", ")))
          }

  }
}
