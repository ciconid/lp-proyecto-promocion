ThisBuild / scalaVersion := "3.3.4"
ThisBuild / version := "0.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "resumen-extractivo",
    Compile / mainClass := Some("resumen.Main"),
    libraryDependencies += "com.lihaoyi" %% "ujson" % "3.3.1",
    run / fork := true,
    run / connectInput := true
  )
