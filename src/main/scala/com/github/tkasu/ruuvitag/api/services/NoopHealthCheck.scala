package com.github.tkasu.ruuvitag.api.services

import zio.*
import com.github.tkasu.ruuvitag.api.domain.healthcheck.*

/** A no-operation health check implementation that always reports healthy
  * status. Useful for testing or when persistence layer monitoring is not
  * required.
  */
object NoopHealthCheck extends HealthCheck:
  def status(): Task[AppStatus] =
    ZIO.succeed(AppStatus(PersistenceLayerStatus(Status.Ok)))
