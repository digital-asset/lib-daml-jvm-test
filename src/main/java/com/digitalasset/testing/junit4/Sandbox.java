/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.junit4;

import static com.digitalasset.testing.utils.PackageUtils.findPackage;
import static com.digitalasset.testing.utils.Preconditions.require;
import static com.digitalasset.testing.utils.SandboxUtils.isDamlRoot;
import static com.google.common.io.Files.createTempDir;

import com.daml.daml_lf_dev.DamlLf1;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import com.digitalasset.testing.ledger.SandboxManager;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sandbox {
  private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration DEFAULT_OBSERVATION_TIMEOUT = Duration.ofSeconds(10);
  private static final String[] DEFAULT_PARTIES = new String[] {};
  private final SandboxManager sandboxManager;

  private final Logger logger = LoggerFactory.getLogger(getClass().getCanonicalName());

  public static SandboxBuilder builder() {
    return new SandboxBuilder();
  }

  private Sandbox(
      Path damlRoot,
      Optional<String> testModule,
      Optional<String> testStartScript,
      Duration sandboxWaitTimeout,
      Duration observationTimeout,
      String[] parties,
      Path darPath,
      BiConsumer<DamlLedgerClient, ManagedChannel> setupApplication,
      boolean useWallclockTime,
      boolean useReset,
      Optional<String> ledgerId,
      Optional<LogLevel> logLevel) {
    this.sandboxManager =
        new SandboxManager(
            damlRoot,
            testModule,
            testStartScript,
            sandboxWaitTimeout,
            observationTimeout,
            parties,
            darPath,
            setupApplication,
            useWallclockTime,
            ledgerId,
            logLevel);
    this.useReset = useReset;
  }

  private final boolean useReset;

  public static class SandboxBuilder {
    private final Logger logger = LoggerFactory.getLogger(getClass().getCanonicalName());

    private static final Path WORKING_DIRECTORY = Paths.get("").toAbsolutePath();
    private Optional<String> testModule = Optional.empty();
    private Optional<String> testStartScript = Optional.empty();
    private Duration sandboxWaitTimeout = DEFAULT_WAIT_TIMEOUT;
    private Duration observationTimeout = DEFAULT_OBSERVATION_TIMEOUT;
    private String[] parties = DEFAULT_PARTIES;
    private Path localDamlRoot = WORKING_DIRECTORY;
    private Optional<Path> darPath = Optional.empty();
    private Optional<MavenCoordinates> darMavenCoordinates = Optional.empty();
    boolean downloadDamlYamlFromMaven;
    private boolean useWallclockTime = false;
    private boolean useReset = false;
    private BiConsumer<DamlLedgerClient, ManagedChannel> setupApplication = (t, u) -> {};
    private Optional<String> ledgerId = Optional.empty();
    private Optional<LogLevel> logLevel = Optional.empty();

    public SandboxBuilder dar(Path darPath) {
      this.darPath = Optional.ofNullable(darPath);
      return this;
    }

    public SandboxBuilder darMavenCoordinates(MavenCoordinates darMavenCoordinates) {
      this.darMavenCoordinates = Optional.ofNullable(darMavenCoordinates);
      return this;
    }

    public SandboxBuilder moduleAndScript(String testModule, String testStartScript) {
      this.testModule = Optional.of(testModule);
      this.testStartScript = Optional.of(testStartScript);
      return this;
    }

    public SandboxBuilder sandboxWaitTimeout(Duration waitTimeout) {
      this.sandboxWaitTimeout = waitTimeout;
      return this;
    }

    public SandboxBuilder observationTimeout(Duration timeout) {
      this.observationTimeout = timeout;
      return this;
    }

    public SandboxBuilder parties(String... parties) {
      this.parties = parties;
      return this;
    }

    public SandboxBuilder parties(Party... parties) {
      this.parties = new String[parties.length];
      for (int i = 0; i < parties.length; i++) {
        this.parties[i] = parties[i].getValue();
      }
      return this;
    }

    public SandboxBuilder setupAppCallback(Consumer<DamlLedgerClient> setupApplication) {
      this.setupApplication = (client, channel) -> setupApplication.accept(client);
      return this;
    }

    public SandboxBuilder setupAppCallback(
        BiConsumer<DamlLedgerClient, ManagedChannel> setupApplication) {
      this.setupApplication = setupApplication;
      return this;
    }

    public SandboxBuilder useReset() {
      this.useReset = true;
      return this;
    }

    public SandboxBuilder useWallclockTime() {
      this.useWallclockTime = true;
      return this;
    }

    public SandboxBuilder ledgerId(String ledgerId) {
      this.ledgerId = Optional.of(ledgerId);
      return this;
    }

    public SandboxBuilder logLevel(LogLevel logLevel) {
      this.logLevel = Optional.of(logLevel);
      return this;
    }

    public SandboxBuilder damlRoot(Path damlRoot) {
      this.localDamlRoot = damlRoot;
      return this;
    }

    public Sandbox build() {
      validate();

      final Path resolvedDarPath =
          darPath.orElseGet(
              () ->
                  darMavenCoordinates
                      .map(this::downloadDarFile)
                      .orElseThrow(
                          () ->
                              new IllegalArgumentException(
                                  "you must specify either a local path to a dar file or some maven coordinates")));

      final Path damlRootPath =
          darMavenCoordinates
              .flatMap(MavenCoordinates::getYamlArtifact)
              .map(s -> this.setupDamlRootWithYamlFromMaven(darMavenCoordinates.get()))
              .orElse(localDamlRoot);

      return new Sandbox(
          damlRootPath,
          testModule,
          testStartScript,
          sandboxWaitTimeout,
          observationTimeout,
          parties,
          resolvedDarPath,
          setupApplication,
          useWallclockTime,
          useReset,
          ledgerId,
          logLevel);
    }

    private Path downloadDarFile(MavenCoordinates coordinates) {
      logger.info("downloading dar file from maven repo: " + coordinates.getRepoUrl());
      final Path path =
          MavenDownloader.downloadDarFileFromMaven(coordinates, createTempDir())
              .map(File::toPath)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Unable to download dar file from specified maven repository"));
      logger.info("dar file downloaded at path: " + path.toAbsolutePath().toString());
      return path;
    }

    private Path setupDamlRootWithYamlFromMaven(MavenCoordinates coordinates) {
      return MavenDownloader.downloadDamlYamlFileFromMaven(coordinates, createTempDir())
          .map(File::getParentFile)
          .map(File::toPath)
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      "Unable to download dar file from specified maven repository"));
    }

    private void validate() {
      require(
          darPath != null && darPath.isPresent() || darMavenCoordinates.isPresent(),
          "you must specify either a local path to a DAR file or coordinates that point to a DAR file in a maven repository");
      require(
          (darPath == null || !darPath.isPresent())
              || (darMavenCoordinates == null || !darMavenCoordinates.isPresent()),
          "you cannot specify both a local path to a DAR file and coordinates that point to a DAR file in a maven repository");
      require(setupApplication != null, "Application setup function cannot be null.");
      require(
          isDamlRoot(localDamlRoot),
          String.format("DAML root '%s' must contain a daml.yaml.", localDamlRoot));
    }
  }

  public DamlLedgerClient getClient() {
    return sandboxManager.getClient();
  }

  public DefaultLedgerAdapter getLedgerAdapter() {
    return sandboxManager.getLedgerAdapter();
  }

  public String getLedgerId() {
    return sandboxManager.getLedgerId();
  }

  public Optional<LogLevel> getLogLevel() {
    return sandboxManager.getLogLevel();
  }

  public int getSandboxPort() {
    return sandboxManager.getPort();
  }

  public Identifier templateIdentifier(
      DamlLf1.DottedName packageName, String moduleName, String entityName)
      throws InvalidProtocolBufferException {
    String pkg = findPackage(sandboxManager.getClient(), packageName);
    return new Identifier(pkg, moduleName, entityName);
  }

  public ExternalResource getClassRule() {
    return new ExternalResource() {
      @Override
      protected void before() throws Throwable {
        if (useReset) {
          sandboxManager.start();
        }
      }

      @Override
      protected void after() {
        sandboxManager.stop();
      }
    };
  }

  public ExternalResource getRule() {
    return new ExternalResource() {
      @Override
      protected void before() throws Throwable {
        if (useReset) {
          sandboxManager.reset();
        } else {
          sandboxManager.restart();
        }
      }

      @Override
      protected void after() {}
    };
  }
}
