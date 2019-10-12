package zio.config

import zio.system.System
import zio.{ IO, UIO, ZIO }

trait Sources {
  val envSource: ZIO[System, Nothing, ConfigSource] =
    ZIO.access { env =>
      new ConfigSource {
        val configService: ConfigSource.Service =
          new ConfigSource.Service {
            val sourceDescription: String = "Environment"

            def getConfigValue(path: String): IO[ReadError, ConfigSource.Value] =
              env.system
                .env(path)
                .mapError { err =>
                  ReadError.FatalError(path, err)
                }
                .flatMap {
                  IO.fromOption(_)
                    .foldM(
                      _ => IO.fail(ReadError.MissingValue(path)),
                      value => UIO(ConfigSource.Value(value, sourceDescription))
                    )
                }
          }
      }
    }

  val propSource: ZIO[System, Nothing, ConfigSource] =
    ZIO.access { env =>
      new ConfigSource {
        val configService: ConfigSource.Service =
          new ConfigSource.Service {
            val sourceDescription: String = "System properties"

            def getConfigValue(path: String): IO[ReadError, ConfigSource.Value] =
              env.system
                .property(path)
                .mapError { err =>
                  ReadError.FatalError(path, err)
                }
                .flatMap {
                  IO.fromOption(_)
                    .foldM(
                      _ => IO.fail(ReadError.MissingValue(path)),
                      value => UIO(ConfigSource.Value(value, sourceDescription))
                    )
                }
          }
      }
    }

  def mapSource(map: Map[String, String]): ConfigSource =
    new ConfigSource {
      val configService: ConfigSource.Service =
        new ConfigSource.Service {
          val sourceDescription: String = "Scala Map"

          def getConfigValue(path: String): IO[ReadError, ConfigSource.Value] =
            ZIO
              .fromOption(map.get(path))
              .foldM(
                _ => IO.fail(ReadError.MissingValue(path)),
                value => UIO(ConfigSource.Value(value, sourceDescription))
              )
        }
    }

  // TODO HOCON etc as separate modules
}
