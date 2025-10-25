package com.github.tkasu.ruuvitag.api.config

import zio.*
import zio.config.*

case class ServerConfig(host: String, port: Int)

case class AuthConfig(mode: String)

case class StorageConfig(mode: String)

case class AppConfig(
    server: ServerConfig,
    auth: AuthConfig,
    storage: StorageConfig
)

object AppConfig:
  val layer: ZLayer[Any, Config.Error, AppConfig] =
    ZLayer {
      for
        host <- ZIO.config[String](
          Config.string("ruuvitag-api.server.host")
        )
        port <- ZIO.config[Int](Config.int("ruuvitag-api.server.port"))
        authMode <- ZIO.config[String](
          Config.string("ruuvitag-api.auth.mode")
        )
        storageMode <- ZIO.config[String](
          Config.string("ruuvitag-api.storage.mode")
        )
      yield AppConfig(
        ServerConfig(host, port),
        AuthConfig(authMode),
        StorageConfig(storageMode)
      )
    }
