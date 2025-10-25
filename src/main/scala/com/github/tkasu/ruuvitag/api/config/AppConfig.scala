package com.github.tkasu.ruuvitag.api.config

import com.typesafe.config.ConfigFactory
import zio.*

case class ServerConfig(host: String, port: Int)

case class AuthConfig(mode: String)

case class StorageConfig(mode: String)

case class AppConfig(
    server: ServerConfig,
    auth: AuthConfig,
    storage: StorageConfig
)

object AppConfig:
  // Load configuration directly from Typesafe Config
  // This bypasses ZIO Config's complex provider chain
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

        AppConfig(
          ServerConfig(host, port),
          AuthConfig(authMode),
          StorageConfig(storageMode)
        )
      }
    }
