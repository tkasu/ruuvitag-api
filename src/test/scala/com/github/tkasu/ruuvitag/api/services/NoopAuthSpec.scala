package com.github.tkasu.ruuvitag.api.services

import zio.*
import zio.test.*
import java.util.UUID
import com.github.tkasu.ruuvitag.api.domain.user.*

object NoopAuthSpec extends ZIOSpecDefault:

  private def createTestUser(id: UUID, name: String): User =
    User(UserId(id), UserName(name))

  def spec = suite("NoopAuthSpec")(
    test("findUser should always return the same user") {
      val testUser = createTestUser(UUID.randomUUID(), "test-user")
      val noopAuth = NoopAuth(testUser)

      for result <- noopAuth.findUser("any-token")
      yield assertTrue(result == Some(testUser))
    },
    test("findUser should return the same user regardless of token") {
      val testUser = createTestUser(UUID.randomUUID(), "test-user")
      val noopAuth = NoopAuth(testUser)

      for
        result1 <- noopAuth.findUser("token1")
        result2 <- noopAuth.findUser("token2")
        result3 <- noopAuth.findUser("")
      yield assertTrue(
        result1 == Some(testUser) &&
          result2 == Some(testUser) &&
          result3 == Some(testUser)
      )
    },
    test("findUser should return user with correct id and name") {
      val expectedId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
      val expectedUser = createTestUser(expectedId, "specific-user")
      val noopAuth = NoopAuth(expectedUser)

      for result <- noopAuth.findUser("test-token")
      yield assertTrue(result.contains(expectedUser))
    }
  )
