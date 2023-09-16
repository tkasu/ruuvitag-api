package com.github.tkasu.ruuvitag.api.domain

import zio.json.*

object healthcheck:
  enum Status:
    case Ok, Unreachable

  object Status:
    given encoder: JsonEncoder[Status] = DeriveJsonEncoder.gen[Status]

  case class PersistenceLayerStatus(value: Status)

  object PersistenceLayerStatus:
    given encoder: JsonEncoder[PersistenceLayerStatus] =
      DeriveJsonEncoder.gen[PersistenceLayerStatus]

  case class AppStatus(persistenceLayerStatus: PersistenceLayerStatus)

  object AppStatus:
    given encoder: JsonEncoder[AppStatus] =
      DeriveJsonEncoder.gen[AppStatus]
