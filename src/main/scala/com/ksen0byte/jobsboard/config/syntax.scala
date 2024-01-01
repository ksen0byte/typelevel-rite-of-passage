package com.ksen0byte.jobsboard.config

import cats.MonadThrow
import cats.implicits.*
import pureconfig.error.ConfigReaderException
import pureconfig.{ConfigReader, ConfigSource}

import scala.reflect.ClassTag

object syntax {

  extension (source: ConfigSource)
    def loadF[F[_], A](using A: ConfigReader[A], F: MonadThrow[F], tag: ClassTag[A]): F[A] =
      source.load[A].pure.flatMap {
        case Left(errors) => ConfigReaderException(errors).raiseError[F, A]
        case Right(conf)  => conf.pure
      }

}
