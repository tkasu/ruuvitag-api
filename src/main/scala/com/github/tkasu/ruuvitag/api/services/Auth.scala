package com.github.tkasu.ruuvitag.api.services

import zio.Task
import com.github.tkasu.ruuvitag.api.domain.user.*

trait Auth:
  def findUser(token: String): Task[Option[User]]
