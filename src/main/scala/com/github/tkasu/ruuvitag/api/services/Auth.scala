package com.github.tkasu.ruuvitag.api.services

import dev.profunktor.auth.jwt.JwtToken

import com.github.tkasu.ruuvitag.api.domain.user._

trait Auth[F[_]] {
  def findUser(token: JwtToken): F[Option[User]]
}
