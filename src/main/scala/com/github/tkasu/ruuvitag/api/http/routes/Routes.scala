package com.github.tkasu.ruuvitag.api.http.routes

import zio.http.{Routes as ZioRoutes, Response}
import com.github.tkasu.ruuvitag.api.programs.MeasurementsProgram
import com.github.tkasu.ruuvitag.api.services.HealthCheck
import com.github.tkasu.ruuvitag.api.http.middleware.RequestLoggingMiddleware

object Routes:
  def make(
      healthCheck: HealthCheck,
      measurementsProgram: MeasurementsProgram
  ): ZioRoutes[Any, Response] =
    val allRoutes =
      HealthRoutes.routes(healthCheck) ++ MeasurementRoutes.routes(
        measurementsProgram
      )
    // Apply request logging middleware for debug logging
    allRoutes @@ RequestLoggingMiddleware()
