package com.ksen0byte.jobsboard.config

import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class AppConfig(postgresConfig: PostgresConfig, emberConfig: EmberConfig) derives ConfigReader
