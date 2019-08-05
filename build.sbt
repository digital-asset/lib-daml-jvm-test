import Dependencies._

addCommandAlias("packageAll", ";package")
addCommandAlias("verify", ";test;scalafmtCheck;headerCheck")

resolvers ++= Seq(
  Resolver.bintrayRepo("digitalassetsdk", "DigitalAssetSDK"),
  Resolver.url("hseeberger", url("http://dl.bintray.com/hseeberger/sbt-plugins"))(Resolver.ivyStylePatterns)
)

credentials += Credentials("Sonatype Nexus Repository Manager",
        "oss.sonatype.org",
        sys.env.get("MAVEN_LOGIN").getOrElse("NO_MAVEN_LOGIN_SPECIFIED"),
        sys.env.get("MAVEN_PASSWORD").getOrElse("NO_MAVEN_PASSWORD_SPECIFIED"))

name := "functest-java"
organization := "com.digitalasset"
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
  damlLedgerClient,
  grizzledLogger,
  snakeYaml,
  logbackClassic,
  commonsIO,
  guava,
  junit4,
  junitInterface,
  hamcrestOptional
)

scalaVersion := "2.12.8"
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
scalacOptions ++= Seq("-target:jvm-1.8")

// Publishing to Maven Central
// POM settings for Sonatype
homepage := Some(url("https://github.com/digital-asset/lib-daml-jvm-test"))
scmInfo := Some(ScmInfo(url("https://github.com/digital-asset/lib-daml-jvm-test"),
                            "git@github.com:digital-asset/lib-daml-jvm-test.git"))
developers := List(Developer("digital-asset",
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

usePgpKeyHex(sys.env.get("GPG_SIGNING_KEY_ID").getOrElse("0"))
pgpPassphrase := Some(sys.env.get("GPG_PASSPHRASE").getOrElse("").toArray)
publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
