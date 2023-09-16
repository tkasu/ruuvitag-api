val scala3Version = "3.3.1"

lazy val zioVersion = "2.0.13"
lazy val zioJsonVersion = "0.6.1"
lazy val zioPreludeVersion = "1.0.0-RC20"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ruuvitag-api",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= List(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-json" % zioJsonVersion,
      "dev.zio" %% "zio-prelude" % zioPreludeVersion,
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
    )
  )
