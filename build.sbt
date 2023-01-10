import Dependencies._

addCommandAlias("packageAll", ";package")
addCommandAlias("verify",
                ";test;cucumber;scalafmtCheck;headerCheck;Test/headerCheck")
addCommandAlias("cucumberTest", ";compileDaml;cucumber")

// do not try to run tests in parallel (each test has one sandbox)
ThisBuild / parallelExecution := false

credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "s01.oss.sonatype.org",
  sys.env.getOrElse("MAVEN_LOGIN", "NO_MAVEN_LOGIN_SPECIFIED"),
  sys.env.getOrElse("MAVEN_PASSWORD", "NO_MAVEN_PASSWORD_SPECIFIED")
)

name := "junit5-support"
sonatypeProfileName := "com.daml"
organization := "com.daml.extensions"
organizationName := "Digital Asset"
startYear := Some(2020)
headerLicense := Some(
  HeaderLicense.ALv2(
    "2020",
    "Digital Asset (Switzerland) GmbH and/or its affiliates",
    HeaderLicenseStyle.SpdxSyntax
  ))
licenses += ("Apache-2.0", new URL(
  "https://www.apache.org/licenses/LICENSE-2.0.txt"))

libraryDependencies ++= Seq(
  damlJavaBinding % "provided",
  damlLFArchive % "provided",
  damlLedgerClient % "provided",
  grizzledLogger,
  logbackClassic,
  commonsIO,
  guava,
  scalaz,
  junit5,
  junitInterface,
  hamcrestOptional,
  cucumberJ8,
  cucumberJunit,
  cucumberPicoContainer
)

scalaVersion := "2.13.10"
javacOptions ++= Seq("-source", "11", "-target", "11")
scalacOptions ++= Seq("-release:11")
scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint:deprecation")

// Publishing to Maven Central
// POM settings for Sonatype
ThisBuild / description := "This library provides functions to test that a DAML application and its bots are working together correctly."
ThisBuild / licenses := List(
  "Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")
)
homepage := Some(url("https://github.com/digital-asset/lib-daml-jvm-test"))
scmInfo := Some(
  ScmInfo(url("https://github.com/digital-asset/lib-daml-jvm-test"),
          "git@github.com:digital-asset/lib-daml-jvm-test.git"))
developers := List(
  Developer("digital-asset",
            "Digital Asset SDK Feedback",
            "sdk-feedback@digitalasset.com",
            url("https://github.com/digital-asset")))
publishMavenStyle := true

// Add sonatype repository settings
// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
publishTo := sonatypePublishToBundle.value

// For all Sonatype accounts created on or after February 2021
sonatypeCredentialHost := "s01.oss.sonatype.org"
// Set this to the repository to publish to using `s01.oss.sonatype.org`
// for accounts created after Feb. 2021.
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

usePgpKeyHex(sys.env.getOrElse("GPG_SIGNING_KEY_ID", "0"))
pgpPassphrase := Some(sys.env.getOrElse("GPG_PASSPHRASE", "").toArray)
publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)

// Daml compilation

import scala.sys.process._

lazy val compileDaml = taskKey[Unit]("Compile DAR with daml")

compileDaml := {
  val pwd = new java.io.File(".").getCanonicalPath
  val isWindows = System.getProperty("os.name").contains("indows")
  val command = if (isWindows) "daml.cmd" else "daml"
  Seq(command,
      "build",
      "--project-root",
      s"$pwd/src/test/resources/ping-pong",
      "--output",
      s"$pwd/src/test/resources/ping-pong.dar").!
}

(Test / test) := (Test / test).dependsOn(compileDaml).value

enablePlugins(CucumberPlugin)
CucumberPlugin.glues := List("com/daml/extensions/testing/steps")
