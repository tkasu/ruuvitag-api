package com.github.tkasu.ruuvitag.api.http.middleware

import zio.*
import zio.http.*
import com.fasterxml.uuid.Generators

object RequestLoggingMiddleware:

  // Maximum payload size to log (in characters)
  private val MaxLogSize = 1000

  // Header name for request ID
  private val RequestIdHeaderName = "X-Request-Id"

  // UUID v7 generator for time-ordered request IDs
  private val uuidGenerator = Generators.timeBasedEpochGenerator()

  /** Generate a UUID v7 for request tracking.
    *
    * Uses time-based UUID v7 which includes a timestamp, making request IDs
    * naturally ordered by time. This is useful for request correlation and
    * debugging.
    */
  private def generateRequestId(): String =
    uuidGenerator.generate().toString

  /** Extract request ID from headers or generate a new one. */
  private def getOrGenerateRequestId(request: Request): String =
    request.headers.toList
      .find(h => h.headerName.equalsIgnoreCase(RequestIdHeaderName))
      .map(_.renderedValue)
      .getOrElse(generateRequestId())

  /** Middleware that logs HTTP requests and responses at DEBUG level.
    *
    * Logs:
    *   - Request ID (from X-Request-Id header or generated)
    *   - Request method and path
    *   - Sanitized request headers (excluding sensitive headers like
    *     Authorization and Cookie)
    *   - Request payload (for POST/PUT requests, truncated if too large)
    *   - Response status code
    *   - Request execution time
    *
    * Request ID Handling: If the X-Request-Id header is not present in the
    * incoming request, a new UUID will be generated and added to the request.
    * This allows correlation of log lines for the same request, especially
    * useful in concurrent scenarios.
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
            // Get or generate request ID
            val requestId = getOrGenerateRequestId(request)

            // Add request ID to request headers if not present
            val hasRequestId = request.headers.toList.exists(h =>
              h.headerName.equalsIgnoreCase(RequestIdHeaderName)
            )

            val requestWithId =
              if hasRequestId then request
              else
                request.copy(
                  headers =
                    request.headers ++ Headers(RequestIdHeaderName, requestId)
                )

            (for
              startTime <- Clock.nanoTime

              // Log request with structured annotations including request ID
              _ <- ZIO.logAnnotate("request_id", requestId) {
                ZIO.logAnnotate("http.method", request.method.toString) {
                  ZIO.logAnnotate("http.path", request.url.path.encode) {
                    ZIO.logDebug(
                      s"request_id=$requestId ${request.method} ${request.url.path.encode}"
                    )
                  }
                }
              }

              // Log sanitized headers (exclude sensitive ones)
              safeHeaders = requestWithId.headers.toList
                .filterNot { h =>
                  val name = h.headerName.toLowerCase
                  name.contains("authorization") ||
                  name.contains("cookie") ||
                  name.contains("token")
                }
                .map(h => s"${h.headerName}: ${h.renderedValue}")

              _ <- ZIO.logAnnotate("request_id", requestId) {
                if safeHeaders.nonEmpty then
                  ZIO.logDebug(
                    s"request_id=$requestId Headers: ${safeHeaders.mkString(", ")}"
                  )
                else ZIO.unit
              }

              // Read and log request payload for POST and PUT requests
              // Important: We must preserve the body for the handler by reconstructing the request
              requestWithBody <- requestWithId.method match
                case Method.POST | Method.PUT =>
                  for
                    chunk <- requestWithId.body.asChunk.catchAll(_ =>
                      ZIO.succeed(Chunk.empty)
                    )
                    bodyStr = new String(chunk.toArray, "UTF-8")

                    // Truncate large payloads
                    logBody =
                      if bodyStr.length > MaxLogSize then
                        s"${bodyStr.take(MaxLogSize)}... (truncated, total: ${bodyStr.length} chars)"
                      else bodyStr

                    _ <- ZIO.logAnnotate("request_id", requestId) {
                      if bodyStr.nonEmpty then
                        ZIO.logDebug(
                          s"request_id=$requestId Request payload: $logBody"
                        )
                      else ZIO.unit
                    }

                    // Reconstruct request with fresh body for handler
                    // Remove Content-Length header as it may not match the reconstructed body
                    newRequest = requestWithId.copy(
                      body = Body.fromChunk(chunk),
                      headers = Headers(
                        requestWithId.headers.toList.filterNot(h =>
                          h.headerName.equalsIgnoreCase("content-length")
                        )
                      )
                    )
                  yield newRequest
                case _ =>
                  ZIO.succeed(requestWithId)

              // Process the request through the original handler
              response <- handler.runZIO(requestWithBody)

              // Log response status and execution time
              endTime <- Clock.nanoTime
              duration = (endTime - startTime) / 1_000_000 // Convert to ms

              _ <- ZIO.logAnnotate("request_id", requestId) {
                ZIO.logAnnotate(
                  "http.status",
                  response.status.code.toString
                ) {
                  ZIO.logAnnotate("http.duration_ms", duration.toString) {
                    ZIO.logDebug(
                      s"request_id=$requestId Response status: ${response.status.code} (${duration}ms)"
                    )
                  }
                }
              }
            yield response).catchAll(err => ZIO.fail(err))
          }
        }
