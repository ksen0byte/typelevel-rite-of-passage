package com.ksen0byte.jobsboard.domain

object pagination {
  final case class Pagination(limit: Int, offset: Int)

  object Pagination {
    private val defaultPageSize = 20
    def apply(maybeLimit: Option[Int], maybeOffset: Option[Int]): Pagination =
      new Pagination(maybeLimit.getOrElse(20), maybeOffset.getOrElse(0))

    def default = new Pagination(limit = defaultPageSize, offset = 0)
  }
}
