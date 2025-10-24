package com.github.tkasu.ruuvitag.api.services

import zio.*
import zio.test.*
import com.github.tkasu.ruuvitag.api.domain.healthcheck.*

object NoopHealthCheckSpec extends ZIOSpecDefault:

  def spec = suite("NoopHealthCheckSpec")(
    test("NoopHealthCheck.status() should return Ok status") {
      for
        status <- NoopHealthCheck.status()
      yield assertTrue(
        status.persistenceLayerStatus.value == Status.Ok
      )
    }
  )
