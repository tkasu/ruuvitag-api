inThisBuild(
  List(
    scalaVersion := "2.13.10",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value),
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
  )
)

lazy val root = (project in file(".")).settings(
  resolvers += Resolver.url("typesafe", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(
    Resolver.ivyStylePatterns
  ),
  scalacOptions ++= List("-Wunused", "-Ymacro-annotations", "-Yrangepos", "-Wconf:cat=unused:info"),
)

libraryDependencies ++= Seq(
  "io.estatico" %% "newtype" % "0.4.4",
  "com.beachape" %% "enumeratum" % "1.7.2",
  "org.typelevel" %% "cats-core" % "2.9.0",
)
