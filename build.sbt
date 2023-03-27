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
  "com.beachape" %% "enumeratum" % "1.7.2",
  "dev.profunktor" %% "http4s-jwt-auth" % "1.2.0",
  "dev.optics" %% "monocle-law" % "3.2.0",
  "io.circe" %% "circe-core" % "0.14.2",
  "io.estatico" %% "newtype" % "0.4.4",
  "org.typelevel" %% "cats-core" % "2.9.0",
  "tf.tofu"    %% "derevo-core" % "0.13.0",
  "tf.tofu"    %% "derevo-cats" % "0.13.0",
  "tf.tofu"    %% "derevo-circe-magnolia" % "0.13.0",
)
