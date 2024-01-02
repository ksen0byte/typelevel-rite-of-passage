package com.ksen0byte.jobsboard.config

import pureconfig.ConfigReader
import pureconfig._
import pureconfig.generic.derivation.default._

final case class PostgresConfig(nThreads: Int, url: String, user: String, pass: String) derives ConfigReader
