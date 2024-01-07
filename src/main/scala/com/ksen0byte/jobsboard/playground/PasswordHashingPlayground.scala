package com.ksen0byte.jobsboard.playground

import cats.effect.{IO, IOApp}
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

object PasswordHashingPlayground extends IOApp.Simple {
  override def run: IO[Unit] =
    BCrypt.hashpw[IO]("DanielNewPassword").flatMap(IO.println) *>
      BCrypt
        .checkpwBool[IO](
          "DanielNewPassword",
          PasswordHash[BCrypt]("$2a$10$tL9Vu0wNa1M7LQYyLvK0n.EUf1fylgAukiE08esRYhP/SUqgfc6.y")
        )
        .flatMap(IO.println)
}
