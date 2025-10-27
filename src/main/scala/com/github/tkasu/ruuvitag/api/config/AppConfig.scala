package com.github.tkasu.ruuvitag.api.config

import com.typesafe.config.ConfigFactory
import zio.*

case class ServerConfig(host: String, port: Int)

case class AuthConfig(mode: String)

case class DatabaseConfig(path: String)

case class StorageConfig(mode: String, database: DatabaseConfig)

case class AppConfig(
    server: ServerConfig,
    auth: AuthConfig,
    storage: StorageConfig
)

object AppConfig:
  // Load configuration using Typesafe Config directly instead of zio-config's descriptor-based approach,
  // but still provides the configuration as a ZLayer for use in ZIO applications.
  private val typesafeConfig = ConfigFactory.load()

  val layer: ZLayer[Any, Throwable, AppConfig] =
    ZLayer.fromZIO {
      ZIO.attempt {
        // Read from Typesafe Config with environment variable fallbacks
        val host = sys.env.getOrElse(
          "SERVER_HOST",
          typesafeConfig.getString("ruuvitag-api.server.host")
        )
        val port = sys.env
          .get("SERVER_PORT")
          .map(_.toInt)
          .getOrElse(typesafeConfig.getInt("ruuvitag-api.server.port"))
        val authMode = sys.env.getOrElse(
          "AUTH_MODE",
          typesafeConfig.getString("ruuvitag-api.auth.mode")
        )
        val storageMode = sys.env.getOrElse(
          "STORAGE_MODE",
          typesafeConfig.getString("ruuvitag-api.storage.mode")
        )
        val dbPath = sys.env.getOrElse(
          "DATABASE_PATH",
          typesafeConfig.getString("ruuvitag-api.storage.database.path")
        )

        AppConfig(
          ServerConfig(host, port),
          AuthConfig(authMode),
          StorageConfig(storageMode, DatabaseConfig(dbPath))
        )
      }
    }
