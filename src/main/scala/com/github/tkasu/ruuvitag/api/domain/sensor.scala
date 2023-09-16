package com.github.tkasu.ruuvitag.api.domain

object sensor:
  case class SensorName(value: String)
  case class Sensor(name: SensorName)
