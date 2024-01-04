package com.ksen0byte.jobsboard.fixtures

import com.ksen0byte.jobsboard.domain.user.{Role, User}

trait UserFixture {

  val Daniel: User = User(
    email = "daniel@rockthejvm.com",
    hashedPassword = "rockthejvm",
    firstName = Some("Daniel"),
    lastName = Some("Ciocirlan"),
    company = Some("Rock The JVM"),
    role = Role.ADMIN
  )
  val Riccardo: User = User(
    email = "riccardo@rockthejvm.com",
    hashedPassword = "riccardorulez",
    firstName = Some("Riccardo"),
    lastName = Some("Cardin"),
    company = Some("Rock The JVM"),
    role = Role.RECRUITER
  )

  val UpdatedRiccardo: User = User(
    email = "riccardo@rockthejvm.com",
    hashedPassword = "riccardorulez",
    firstName = Some("RICCARDO"),
    lastName = Some("CARDIN"),
    company = Some("Adobe"),
    role = Role.RECRUITER
  )

  val NewUser: User = User(
    email = "newuser@gmail.com",
    hashedPassword = "newuserpass",
    firstName = Some("John"),
    lastName = Some("Doe"),
    company = Some("Some company"),
    role = Role.RECRUITER
  )
}
