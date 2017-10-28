import Dependencies._
name := "chores"

version := "0.1"

scalaVersion := "2.12.4"

wartremoverErrors ++= Warts.allBut(
  Wart.DefaultArguments,
  Wart.Nothing,
  Wart.ImplicitParameter,
  Wart.ImplicitConversion,
  Wart.PublicInference,
  Wart.Overloading,
  Wart.ExplicitImplicitTypes,
  Wart.ToString,
  Wart.Any,
  Wart.AsInstanceOf,
  Wart.IsInstanceOf,
  Wart.Recursion,
  Wart.LeakingSealed,
  Wart.MutableDataStructures,
  Wart.NonUnitStatements,
  Wart.Product,
  Wart.Serializable
)

val playJsonV              = "2.6.7" // For Mongo.
val pureConfigV            = "0.8.0"
val reactiveMongoV         = "0.12.6"
val reactiveMongoPlayJsonV = "0.12.7-play26" // For Mongo.

lazy val root = (project in file("."))
  .aggregate(backend, frontend)

lazy val commonSettings = Seq(
  organization := "fr.demandeatonton",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.4"
)
lazy val frontendSettings = Seq(
  scalaJSUseMainModuleInitializer := true
)

lazy val backend = (project in file("backend"))
  .settings(
    commonSettings,
    libraryDependencies ++= backendDeps
  )
lazy val frontend = (project in file("frontend"))
  .settings(
    commonSettings,
    frontendSettings,
    libraryDependencies ++= frontendDeps,
    libraryDependencies ++= "org.scala-js" %%% "scalajs-dom" % "0.9.2"
  ).enablePlugins(ScalaJSPlugin)

