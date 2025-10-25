package com.github.tkasu.ruuvitag.api.http.middleware

import zio.*
import zio.http.*

object RequestLoggingMiddleware:

  /** Middleware that logs HTTP requests and responses at DEBUG level.
    *
    * Logs:
    *   - Request method and path
    *   - Request payload (for POST/PUT requests)
    *   - Response status code
    *
    * This is useful for local development and debugging. The logging can be
    * controlled via the LOG_LEVEL environment variable (default: DEBUG). Set
    * LOG_LEVEL=INFO to disable these debug messages.
    */
  def apply(): Middleware[Any] =
    new Middleware[Any]:
      override def apply[Env1 <: Any, Err](
          routes: Routes[Env1, Err]
      ): Routes[Env1, Err] =
        routes.transform[Env1] { handler =>
          Handler.fromFunctionZIO { (request: Request) =>
            (for
              // Log request information at DEBUG level
              _ <- ZIO.logDebug(
                s"${request.method} ${request.url.path.encode}"
              )

              // Log request payload for POST and PUT requests
              bodyStr <- request.method match
                case Method.POST | Method.PUT =>
                  request.body.asString.catchAll(_ => ZIO.succeed(""))
                case _ =>
                  ZIO.succeed("")

              _ <-
                if bodyStr.nonEmpty then
                  ZIO.logDebug(s"Request payload: $bodyStr")
                else ZIO.unit

              // Process the request through the original handler
              response <- handler.runZIO(request)

              // Log response status at DEBUG level
              _ <- ZIO.logDebug(s"Response status: ${response.status.code}")
            yield response).catchAll(err => ZIO.succeed(err))
          }
        }
