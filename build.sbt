import Dependencies._

addCommandAlias("packageAll", ";package")
addCommandAlias("verify",
                ";test;cucumber;scalafmtCheck;headerCheck;test:headerCheck")
addCommandAlias("cucumberTest", ";compileDaml;cucumber")

resolvers ++= Seq(
  Resolver.bintrayRepo("digitalassetsdk", "DigitalAssetSDK"),
  Resolver.url("hseeberger",
               url("http://dl.bintray.com/hseeberger/sbt-plugins"))(
    Resolver.ivyStylePatterns)
)

// do not try to run tests in parallel (each test has one sandbox)
parallelExecution in ThisBuild := false

credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  sys.env.getOrElse("MAVEN_LOGIN", "NO_MAVEN_LOGIN_SPECIFIED"),
  sys.env.getOrElse("MAVEN_PASSWORD", "NO_MAVEN_PASSWORD_SPECIFIED")
)

name := "functest-java"
organization := "com.digitalasset"
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
  snakeYaml,
  logbackClassic,
  commonsIO,
  guava,
  scalaz,
  junit4,
  junitInterface,
  hamcrestOptional,
  cucumberJ8,
  cucumberJunit,
  cucumberPicoContainer,
  aether1,
  aether2,
  aether3,
  aether4,
  aether5,
  aether6,
  aether7
)

scalaVersion := "2.12.8"
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
scalacOptions ++= Seq("-target:jvm-1.8")

// Publishing to Maven Central
// POM settings for Sonatype
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
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

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

(test in Test) := (test in Test).dependsOn(compileDaml).value

enablePlugins(CucumberPlugin)
CucumberPlugin.glues := List("com/digitalasset/testing/steps")
