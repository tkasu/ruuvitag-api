package com.github.tkasu.ruuvitag.api.http.middleware

import zio.*
import zio.http.*

object RequestLoggingMiddleware:

  // Maximum payload size to log (in characters)
  private val MaxLogSize = 1000

  /** Middleware that logs HTTP requests and responses at DEBUG level.
    *
    * Logs:
    *   - Request method and path
    *   - Sanitized request headers (excluding sensitive headers like
    *     Authorization and Cookie)
    *   - Request payload (for POST/PUT requests, truncated if too large)
    *   - Response status code
    *   - Request execution time
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
              startTime <- Clock.nanoTime

              // Log request with structured annotations
              _ <- ZIO.logAnnotate("http.method", request.method.toString) {
                ZIO.logAnnotate("http.path", request.url.path.encode) {
                  ZIO.logDebug(
                    s"${request.method} ${request.url.path.encode}"
                  )
                }
              }

              // Log sanitized headers (exclude sensitive ones)
              safeHeaders = request.headers.toList
                .filterNot { h =>
                  val name = h.headerName.toLowerCase
                  name.contains("authorization") ||
                  name.contains("cookie") ||
                  name.contains("token")
                }
                .map(h => s"${h.headerName}: ${h.renderedValue}")

              _ <-
                if safeHeaders.nonEmpty then
                  ZIO.logDebug(s"Headers: ${safeHeaders.mkString(", ")}")
                else ZIO.unit

              // Read and log request payload for POST and PUT requests
              // Important: We must preserve the body for the handler by reconstructing the request
              requestWithBody <- request.method match
                case Method.POST | Method.PUT =>
                  for
                    chunk <- request.body.asChunk.catchAll(_ =>
                      ZIO.succeed(Chunk.empty)
                    )
                    bodyStr = new String(chunk.toArray, "UTF-8")

                    // Truncate large payloads
                    logBody =
                      if bodyStr.length > MaxLogSize then
                        s"${bodyStr.take(MaxLogSize)}... (truncated, total: ${bodyStr.length} chars)"
                      else bodyStr

                    _ <-
                      if bodyStr.nonEmpty then
                        ZIO.logDebug(s"Request payload: $logBody")
                      else ZIO.unit

                    // Reconstruct request with fresh body for handler
                    newRequest = request.copy(body = Body.fromChunk(chunk))
                  yield newRequest
                case _ =>
                  ZIO.succeed(request)

              // Process the request through the original handler
              response <- handler.runZIO(requestWithBody)

              // Log response status and execution time
              endTime <- Clock.nanoTime
              duration = (endTime - startTime) / 1_000_000 // Convert to ms

              _ <- ZIO.logAnnotate(
                "http.status",
                response.status.code.toString
              ) {
                ZIO.logAnnotate("http.duration_ms", duration.toString) {
                  ZIO.logDebug(
                    s"Response status: ${response.status.code} (${duration}ms)"
                  )
                }
              }
            yield response).catchAll(err => ZIO.succeed(err))
          }
        }
