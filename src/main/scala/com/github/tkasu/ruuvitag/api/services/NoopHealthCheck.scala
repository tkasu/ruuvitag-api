package com.github.tkasu.ruuvitag.api.services

import zio.*
import com.github.tkasu.ruuvitag.api.domain.healthcheck.*

object NoopHealthCheck extends HealthCheck:
  def status(): Task[AppStatus] =
    ZIO.succeed(AppStatus(PersistenceLayerStatus(Status.Ok)))
