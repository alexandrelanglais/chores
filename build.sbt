import Dependencies._
import sbt.Keys.libraryDependencies

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

lazy val root = (project in file("."))
  .aggregate(backend, frontend)

lazy val commonSettings = Seq(
  organization := "fr.demandeatonton",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.4"
)

//val scalaJsDom = "org.scala-js" %%% "scalajs-dom" % "0.9.3"


//lazy val frontendDeps =
//  Seq("be.doeraene" %%% "scalajs-jquery" % "0.9.1")

lazy val frontendSettings = Seq(
  scalaJSUseMainModuleInitializer := true,
  skip in packageJSDependencies := false,
  jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()
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
    libraryDependencies ++= Seq("be.doeraene" %%% "scalajs-jquery" % "0.9.1", "com.lihaoyi" %%% "utest" % "0.4.4" % "test"),
    jsDependencies += "org.webjars" % "jquery" % "2.1.4" / "2.1.4/jquery.js",
    testFrameworks += new TestFramework("utest.runner.Framework")
  )
  .enablePlugins(ScalaJSPlugin)
