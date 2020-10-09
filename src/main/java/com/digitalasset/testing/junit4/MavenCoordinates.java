package com.digitalasset.testing.junit4;

import com.digitalasset.testing.utils.Preconditions;
import com.google.common.base.Strings;

import java.util.Optional;

public class MavenCoordinates {
  private final String repoUrl;
  private final String group;
  private final String darArtifact;
  private final Optional<String> yamlArtifact;
  private final String version;
  private Optional<MavenCredentials> mavenCredentials;

  private MavenCoordinates(
      String repoUrl,
      String group,
      String darArtifact,
      Optional<String> yamlArtifact,
      String version,
      Optional<MavenCredentials> mavenCredentials) {
    this.repoUrl = repoUrl;
    this.group = group;
    this.darArtifact = darArtifact;
    this.yamlArtifact = yamlArtifact;
    this.version = version;
    this.mavenCredentials = mavenCredentials;
  }

  public String getRepoUrl() {
    return repoUrl;
  }

  public String getGroup() {
    return group;
  }

  public String getDarArtifact() {
    return darArtifact;
  }

  public Optional<String> getYamlArtifact() {
    return yamlArtifact;
  }

  public String getVersion() {
    return version;
  }

  public Optional<MavenCredentials> getMavenCredentials() {
    return mavenCredentials;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String repoUrl;
    private String group;
    private String darArtifact;
    private Optional<String> yamlArtifact;
    private String version;
    private Optional<MavenCredentials> mavenCredentials;

    private Builder() {}

    public Builder repoUrl(String repoUrl) {
      this.repoUrl = repoUrl;
      return this;
    }

    public Builder group(String group) {
      this.group = group;
      return this;
    }

    public Builder darArtifact(String darArtifact) {
      this.darArtifact = darArtifact;
      return this;
    }

    public Builder yamlArtifact(String yamlArtifact) {
      this.yamlArtifact = Optional.ofNullable(yamlArtifact);
      return this;
    }

    public Builder mavenCredentials(MavenCredentials mavenCredentials) {
      this.mavenCredentials = Optional.ofNullable(mavenCredentials);
      return this;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public MavenCoordinates build() {
      Preconditions.require(
          !Strings.isNullOrEmpty(repoUrl),
          "a repository URL is required when building DAR maven coordinates");
      Preconditions.require(
          !Strings.isNullOrEmpty(group),
          "a maven group hosting the DAR file is required when building DAR maven coordinates");
      Preconditions.require(
          !Strings.isNullOrEmpty(darArtifact),
          "a maven artifact pointing to the DAR file is required when building DAR maven coordinates");
      Preconditions.require(
          !Strings.isNullOrEmpty(version),
          "a version is required when building DAR maven coordinates");
      return new MavenCoordinates(
          repoUrl, group, darArtifact, yamlArtifact, version, mavenCredentials);
    }
  }
}
