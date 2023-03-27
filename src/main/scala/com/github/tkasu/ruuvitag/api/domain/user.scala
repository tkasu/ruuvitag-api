package com.github.tkasu.ruuvitag.api.domain

import java.util.UUID

import io.estatico.newtype.macros.newtype

object user {
  @newtype case class UserId(value: UUID)
  @newtype case class UserName(value: String)
  case class User(id: UserId, name: UserName)
}
