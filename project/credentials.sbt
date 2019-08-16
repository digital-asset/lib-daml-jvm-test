credentials += Credentials("Sonatype Nexus Repository Manager",
        "oss.sonatype.org",
        sys.env.get("MAVEN_LOGIN").getOrElse("NO_MAVEN_LOGIN_SPECIFIED"),
        sys.env.get("MAVEN_PASSWORD").getOrElse("NO_MAVEN_PASSWORD_SPECIFIED"))