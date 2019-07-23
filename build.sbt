import Dependencies._

addCommandAlias("packageAll", ";package")
addCommandAlias("verify", ";test;scalafmtCheck;headerCheck")

resolvers ++= Seq(
  Resolver.bintrayRepo("digitalassetsdk", "DigitalAssetSDK"),
  Resolver.url("hseeberger", url("http://dl.bintray.com/hseeberger/sbt-plugins"))(Resolver.ivyStylePatterns)
)

name := "functest-java"
version := "0.1.0-SNAPSHOT"
organization := "com.digitalasset.daml"
organizationName := "Digital Asset"
startYear := Some(2019)
headerLicense := Some(
  HeaderLicense.ALv2(
    "2019",
    "Digital Asset (Switzerland) GmbH and/or its affiliates",
    HeaderLicenseStyle.SpdxSyntax
  ))
licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))

libraryDependencies ++= Seq(
  damlJavaBinding,
  damlLFArchive,
  damlScalaBinding,
  damlLedgerClient,
  grizzledLogger,
  scalapbJson4s,
  snakeYaml,
  logbackClassic,
  commonsIO,
  junit4,
  junitInterface,
  hamcrestOptional
)

scalaVersion := "2.12.8"
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
scalacOptions ++= Seq("-target:jvm-1.8")
