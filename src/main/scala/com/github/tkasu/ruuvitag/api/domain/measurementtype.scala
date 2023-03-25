package com.github.tkasu.ruuvitag.api.domain

import enumeratum._

object measurementtype {
  sealed trait MeasurementType extends EnumEntry

  object MeasurementType extends Enum[MeasurementType] {
    def values = findValues

    case object Temperature extends MeasurementType
    case object Pressure    extends MeasurementType
    case object Humidity    extends MeasurementType
  }
}
