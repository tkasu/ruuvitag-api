package com.github.tkasu.ruuvitag.api.domain

import java.util.UUID

object user:
  case class UserId(value: UUID)
  case class UserName(value: String)
  case class User(id: UserId, name: UserName)
