package com.github.tkasu.ruuvitag.api.domain

import derevo.cats.eqv
import derevo.circe.magnolia.encoder
import derevo.derive
import io.circe.Encoder
import io.estatico.newtype.macros.newtype
import monocle.Iso

object healthcheck {

  @derive(eqv)
  sealed trait Status
  object Status {
    case object Ok          extends Status
    case object Unreachable extends Status

    val _Bool: Iso[Status, Boolean] = Iso[Status, Boolean] {
      case Ok          => true
      case Unreachable => false
    }(if (_) Ok else Unreachable)

    implicit val jsonEncoder: Encoder[Status] =
      Encoder.forProduct1("status")(_.toString)
  }

  @derive(encoder)
  @newtype
  case class PersistenceLayerStatus(value: Status)

  @derive(encoder)
  case class AppStatus(persistenceLayerStatus: PersistenceLayerStatus)
}
