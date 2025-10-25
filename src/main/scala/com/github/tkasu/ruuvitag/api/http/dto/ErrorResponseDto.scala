package com.github.tkasu.ruuvitag.api.http.dto

import zio.json.*

case class ErrorResponseDto(
    error: String,
    message: String
)

object ErrorResponseDto:
  given JsonEncoder[ErrorResponseDto] = DeriveJsonEncoder.gen[ErrorResponseDto]
  given JsonDecoder[ErrorResponseDto] = DeriveJsonDecoder.gen[ErrorResponseDto]
