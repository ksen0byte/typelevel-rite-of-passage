package com.ksen0byte.jobsboard.core

import cats.effect.MonadCancelThrow
import cats.implicits.*
import com.ksen0byte.jobsboard.domain.auth
import com.ksen0byte.jobsboard.domain.auth.NewPasswordInfo
import com.ksen0byte.jobsboard.domain.security.{Authenticator, JwtToken}
import com.ksen0byte.jobsboard.domain.user.{NewUserInfo, User}
import org.typelevel.log4cats.Logger

trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[JwtToken]]
  def signUp(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(email: String, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]]
}

class LiveAuth[F[_]: MonadCancelThrow: Logger] private (users: Users[F], JWTAuthenticator: Authenticator[F])
    extends Auth[F] {
  override def login(email: String, password: String): F[Option[JwtToken]] = ???

  override def signUp(newUserInfo: NewUserInfo): F[Option[User]] = ???

  override def changePassword(email: String, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]] = ???
}
object LiveAuth {
  def apply[F[_]: MonadCancelThrow: Logger](users: Users[F], authenticator: Authenticator[F]): F[LiveAuth[F]] =
    new LiveAuth(users, authenticator).pure[F]
}
