package com.ksen0byte.jobsboard.domain

import doobie.util.meta.Meta

object user {
  final case class User(
      email: String,
      hashedPassword: String,
      firstName: Option[String],
      lastName: Option[String],
      company: Option[String],
      role: Role
  )

  final case class NewUserInfo(
      email: String,
      password: String,
      firstName: Option[String],
      lastName: Option[String],
      company: Option[String]
  )

  enum Role {
    case ADMIN, RECRUITER
  }
  object Role {
    given Meta[Role] = Meta[String].timap(Role.valueOf)(_.toString)
  }

}
