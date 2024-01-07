package com.ksen0byte.jobsboard.core

import cats.data.OptionT
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.ksen0byte.jobsboard.domain.user
import com.ksen0byte.jobsboard.domain.user.{NewUserInfo, Role, User}
import com.ksen0byte.jobsboard.fixtures.UserFixture
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.implicits.*
import com.ksen0byte.jobsboard.domain.auth.NewPasswordInfo
import com.ksen0byte.jobsboard.domain.security.Authenticator
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import concurrent.duration.DurationInt

class AuthSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with UserFixture {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val mockedUsers: Users[IO] = new Users[IO]:
    override def find(email: String): IO[Option[User]] = IO.pure { if email == Daniel.email then Daniel.some else None }
    override def create(user: User): IO[String]        = IO.pure(user.email)
    override def update(user: User): IO[Option[User]]  = IO.pure(user.some)
    override def delete(email: String): IO[Boolean]    = IO.pure(true)

  private val mockedAuthenticator: Authenticator[IO] = {
    val key = HMACSHA256.unsafeGenerateKey
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if email == Daniel.email then OptionT.pure(Daniel)
      else if email == Riccardo.email then OptionT.pure(Riccardo)
      else OptionT.none[IO, User]

    JWTAuthenticator.unbacked.inBearerToken(
      expiryDuration = 1.day,
      maxIdle = None,
      identityStore = idStore,
      signingKey = key
    )
  }

  // ----------- TESTS -----------
  "Auth 'algebra" - {
    "login should return None if the user does not exist" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        maybeToken <- auth.login("does not exist", "does not exist")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return None if the user exists but the password is incorrect" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        maybeToken <- auth.login(Daniel.email, "wrong pass")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return a token if the user exists and the password is correct" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        maybeToken <- auth.login(Daniel.email, Daniel.hashedPassword)
      } yield maybeToken

      program.asserting(_ shouldBe defined)
    }
  }

  "signUp should create a new user successfully" in {
    val newUserInfo =
      NewUserInfo("newUserEmail@test.com", "newUserPassword", Some("firstName"), Some("lastName"), Some("company"))

    val program = for {
      auth      <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
      maybeUser <- auth.signUp(newUserInfo)
    } yield maybeUser

    // Check if created user is wrapped in 'Some'
    program.asserting {
      case Some(user) =>
        user.email shouldBe "ewUserEmail@test.com"
        user.firstName shouldBe Some("firstName")
        user.lastName shouldBe Some("lastName")
        user.company shouldBe Some("company")
        user.role shouldBe Role.RECRUITER
      case _ => fail()
    }
  }

  "signUp should return None if the user already exists" in {
    val newUserInfo =
      NewUserInfo(email = Daniel.email, password = "userPassword", firstName = None, lastName = None, company = None)

    val program = for {
      auth      <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
      maybeUser <- auth.signUp(newUserInfo)
    } yield maybeUser

    program.asserting(_ shouldBe None)
  }

  "changePassword should update the password if old password is correct" in {
    val newPasswordInfo = NewPasswordInfo(Daniel.hashedPassword, "DanielNewPassword")

    val program = for {
      auth   <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
      result <- auth.changePassword(Daniel.email, newPasswordInfo)
      isNicePassword <- result match
        case Right(Some(user)) => BCrypt.checkpwBool[IO]("DanielNewPassword", PasswordHash[BCrypt](user.hashedPassword))
        case _                 => IO.pure(false)
    } yield isNicePassword

    program.asserting(_ shouldBe true)
  }

  "changePassword should return Right(None) if the user does not exist" in {
    val newPasswordInfo = NewPasswordInfo(oldPassword = "old", newPassword = "new")

    val program = for {
      auth   <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
      result <- auth.changePassword(email = "doesn't exist", newPasswordInfo = newPasswordInfo)
    } yield result

    program.asserting(_ shouldBe Right(None))
  }

  "changePassword should return Left with an error message if old password is incorrect" in {
    val newPasswordInfo = NewPasswordInfo("wrongOldPassword", "newPassword")

    val program = for {
      auth      <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
      maybeUser <- auth.changePassword(Daniel.email, newPasswordInfo)
    } yield maybeUser

    program.asserting(_ shouldBe Left("Invalid password"))
  }
}
