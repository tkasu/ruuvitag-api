package com.github.tkasu.ruuvitag.api.services

import zio.Task
import zio.ZIO
import com.github.tkasu.ruuvitag.api.domain.user.*

final case class NoopAuth(user: User) extends Auth:
  def findUser(token: String): Task[Option[User]] =
    ZIO.succeed(Some(user))
