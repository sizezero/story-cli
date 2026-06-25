val scala3Version = "3.8.4"

lazy val root = project
  .in(file("."))
  .settings(
    name := "story-cli",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalameta" %% "munit" % "1.3.2" % Test,

    // June 2026: I tried to use newer versions but they failed
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.11.8"

  )
