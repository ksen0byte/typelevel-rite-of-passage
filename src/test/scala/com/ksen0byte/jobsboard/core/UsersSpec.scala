package com.ksen0byte.jobsboard.core

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.ksen0byte.jobsboard.domain.user.User
import com.ksen0byte.jobsboard.fixtures.UserFixture
import org.postgresql.util.PSQLException
import org.scalatest.Inside.inside
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.implicits.*

class UsersSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with DoobieSpec with UserFixture {
  override val initScript: String = "sql/users.sql"

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Users 'algebra" - {
    "should return no user if the given email does not exist" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          user  <- users.find("notfound@example.com")
        } yield user

        program.asserting(_ shouldBe None)
      }
    }

    "should return a user if the given email exists" in {
      transactor.use { xa =>
        val existingUser = Riccardo
        val program = for {
          users <- LiveUsers[IO](xa)
          user  <- users.find(existingUser.email)
        } yield user

        program.asserting(_ shouldBe existingUser.some)
      }
    }

    "should create a new user" in {
      transactor.use { xa =>
        val newUser = NewUser
        val program = for {
          users     <- LiveUsers[IO](xa)
          id        <- users.create(newUser)
          maybeUser <- users.find(id)
        } yield maybeUser

        program.asserting(_ shouldBe NewUser.some)
      }
    }

    "should fail creating a new user if the email exists" in {
      transactor.use { xa =>
        val existingUser = Riccardo
        val newUser      = NewUser.copy(email = existingUser.email)
        val program = for {
          users  <- LiveUsers[IO](xa)
          userId <- users.create(newUser).attempt
        } yield userId

        program.asserting { outcome =>
          inside(outcome) {
            case Left(e) => e shouldBe a[PSQLException]
            case _       => fail()
          }
        }
      }
    }

    "should return None if the user does not exist" in {
      transactor.use { xa =>
        val nonExistingUser = NewUser
        val program = for {
          users <- LiveUsers[IO](xa)
          user  <- users.update(nonExistingUser)
        } yield user

        program.asserting(_ shouldBe None)
      }
    }

    "should update a user if the given user exists" in {
      transactor.use { xa =>
        val existingUser = Riccardo
        val updatedUser  = UpdatedRiccardo
        val program = for {
          users <- LiveUsers[IO](xa)
          _     <- users.update(UpdatedRiccardo)
          user  <- users.find(existingUser.email)
        } yield user

        program.asserting(_ shouldBe updatedUser.some)
      }
    }

    "should not delete a user if the given email does not exist" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          isDeleted <- users.delete(NewUser.email)
          maybeUser <- users.find(Daniel.email)
        } yield (isDeleted, maybeUser)

        program.asserting(_ shouldBe (false, Daniel.some))
      }
    }

    "should delete a user if the given email exists" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          isDeleted <- users.delete(Daniel.email)
          maybeUser <- users.find(Daniel.email)
        } yield (isDeleted, maybeUser)

        program.asserting(_ shouldBe (true, None))
      }
    }

  }
}
