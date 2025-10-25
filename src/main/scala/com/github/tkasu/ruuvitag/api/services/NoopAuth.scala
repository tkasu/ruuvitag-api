package com.github.tkasu.ruuvitag.api.services

import zio.Task
import zio.ZIO
import com.github.tkasu.ruuvitag.api.domain.user.*

/** A no-operation authentication service that always returns the same user.
  *
  * This implementation is intended for testing and development scenarios where
  * authentication logic needs to be bypassed. The token parameter is
  * intentionally ignored.
  *
  * @param user
  *   The user to return for all authentication requests
  */
final case class NoopAuth(user: User) extends Auth:
  def findUser(token: String): Task[Option[User]] =
    ZIO.succeed(Some(user))
