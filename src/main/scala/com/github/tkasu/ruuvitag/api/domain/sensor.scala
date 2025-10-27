package com.github.tkasu.ruuvitag.api.domain

import zio.json.*

object sensor:
  /** MAC address in standard colon-separated format (e.g., FE:26:88:7A:66:66).
    *
    * Validation: - Must match pattern XX:XX:XX:XX:XX:XX where X is a hex digit
    * \- Case insensitive (stored in uppercase)
    *
    * Note: Constructor is private to enforce normalization through the
    * companion object's apply method.
    */
  case class MacAddress private (value: String):
    require(
      MacAddress.isValid(value),
      s"Invalid MAC address format: $value. Expected format: XX:XX:XX:XX:XX:XX"
    )

  object MacAddress:
    private val macAddressPattern =
      "^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$".r

    /** Validates MAC address format */
    def isValid(address: String): Boolean =
      macAddressPattern.matches(address)

    /** Creates a MacAddress from string, normalizing to uppercase */
    def apply(value: String): MacAddress =
      new MacAddress(value.toUpperCase)

    given JsonEncoder[MacAddress] = JsonEncoder[String].contramap(_.value)
    given JsonDecoder[MacAddress] = JsonDecoder[String].map(MacAddress(_))

  /** Sensor name for future use with SensorNameRegistry. Currently not used in
    * the API, but kept for future extensibility.
    */
  case class SensorName(value: String)

  object SensorName:
    given JsonEncoder[SensorName] = JsonEncoder[String].contramap(_.value)
    given JsonDecoder[SensorName] = JsonDecoder[String].map(SensorName(_))

  /** Sensor identified by MAC address. In the future, this may also include an
    * optional sensor name from the SensorNameRegistry.
    */
  case class Sensor(macAddress: MacAddress)

  object Sensor:
    given JsonEncoder[Sensor] = DeriveJsonEncoder.gen[Sensor]
    given JsonDecoder[Sensor] = DeriveJsonDecoder.gen[Sensor]
