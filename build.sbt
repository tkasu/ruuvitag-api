val scala3Version = "3.7.3"

lazy val zioVersion = "2.1.14"
lazy val zioJsonVersion = "0.7.3"
lazy val zioPreludeVersion = "1.0.0-RC35"
lazy val zioHttpVersion = "3.0.1"
lazy val zioConfigVersion = "4.0.2"
lazy val zioLoggingVersion = "2.3.2"
lazy val logbackVersion = "1.5.6"
lazy val zioMetricsConnectorsVersion = "2.3.1"
lazy val uuidGeneratorVersion = "5.1.0"
lazy val quillVersion = "4.8.6"
lazy val sqliteJdbcVersion = "3.47.1.0"
lazy val hikariCpVersion = "6.2.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ruuvitag-api",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= List(
      // Core ZIO
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-json" % zioJsonVersion,
      "dev.zio" %% "zio-prelude" % zioPreludeVersion,

      // HTTP Server
      "dev.zio" %% "zio-http" % zioHttpVersion,

      // Configuration
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,

      // Logging
      "dev.zio" %% "zio-logging" % zioLoggingVersion,
      "dev.zio" %% "zio-logging-slf4j2" % zioLoggingVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,

      // Metrics
      "dev.zio" %% "zio-metrics-connectors-prometheus" % zioMetricsConnectorsVersion,

      // UUID Generation
      "com.fasterxml.uuid" % "java-uuid-generator" % uuidGeneratorVersion,

      // Database - Quill with SQLite
      "io.getquill" %% "quill-jdbc-zio" % quillVersion,
      "org.xerial" % "sqlite-jdbc" % sqliteJdbcVersion,
      "com.zaxxer" % "HikariCP" % hikariCpVersion,

      // Testing
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-http-testkit" % zioHttpVersion % Test,
    )
  )
