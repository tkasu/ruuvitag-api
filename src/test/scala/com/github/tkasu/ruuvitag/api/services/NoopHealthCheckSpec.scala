package com.github.tkasu.ruuvitag.api.services

import zio.*
import zio.test.*
import com.github.tkasu.ruuvitag.api.domain.healthcheck.*

object NoopHealthCheckSpec extends ZIOSpecDefault:

  def spec = suite("NoopHealthCheckSpec")(
    test("should always return Ok status") {
      NoopHealthCheck
        .status()
        .map(status =>
          assertTrue(
            status.persistenceLayerStatus.value == Status.Ok
          )
        )
    },
    test("should always succeed with idempotent results") {
      for
        status1 <- NoopHealthCheck.status()
        status2 <- NoopHealthCheck.status()
      yield assertTrue(status1 == status2)
    }
  )
