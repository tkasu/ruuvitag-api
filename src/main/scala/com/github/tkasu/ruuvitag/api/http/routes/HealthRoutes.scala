package com.github.tkasu.ruuvitag.api.http.routes

import zio.*
import zio.http.*
import zio.json.*
import com.github.tkasu.ruuvitag.api.services.HealthCheck
import com.github.tkasu.ruuvitag.api.http.dto.ErrorResponseDto

object HealthRoutes:
  def routes(healthCheck: HealthCheck): Routes[Any, Response] =
    Routes(
      Method.GET / "health" -> handler {
        (for
          _ <-
            com.github.tkasu.ruuvitag.api.http.routes.Routes.httpRequestsTotal.increment
          _ <-
            com.github.tkasu.ruuvitag.api.http.routes.Routes.httpHealthCheckRequests.increment
          status <- healthCheck.status()
          response <- ZIO.succeed(
            Response.json(status.toJson)
          )
        yield response).catchAll { error =>
          ZIO.succeed(
            Response
              .json(
                ErrorResponseDto("InternalError", error.getMessage).toJson
              )
              .status(Status.InternalServerError)
          )
        }
      }
    )
