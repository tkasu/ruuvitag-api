package com.github.tkasu.ruuvitag.api.domain

import io.estatico.newtype.macros.newtype

object user {
  @newtype case class UserName(value: String)
  case class User(name: UserName)
}
