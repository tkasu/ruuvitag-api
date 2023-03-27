package com.github.tkasu.ruuvitag.api.services

import com.github.tkasu.ruuvitag.api.domain.healthcheck._

trait HealthCheck[F[_]] {
  def status(): F[AppStatus]
}
