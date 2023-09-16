package com.github.tkasu.ruuvitag.api.services

import zio.*
import com.github.tkasu.ruuvitag.api.domain.healthcheck.*

trait HealthCheck:
  def status(): Task[AppStatus]
