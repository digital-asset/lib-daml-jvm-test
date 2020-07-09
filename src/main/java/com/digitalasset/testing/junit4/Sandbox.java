/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.junit4;

import static com.digitalasset.testing.utils.PackageUtils.findPackage;
import static com.digitalasset.testing.utils.Preconditions.require;
import static com.digitalasset.testing.utils.SandboxUtils.isDamlRoot;

import com.daml.daml_lf_dev.DamlLf1;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.digitalasset.testing.ledger.DamlScriptRunner;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import com.digitalasset.testing.ledger.SandboxManager;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.junit.rules.ExternalResource;

public class Sandbox {
  private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration DEFAULT_OBSERVATION_TIMEOUT = Duration.ofSeconds(10);
  private static final String[] DEFAULT_PARTIES = new String[] {};
  private final SandboxManager sandboxManager;

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
  }

  public void runScript(String testModule, String testStartScript, Party... parties)
      throws IOException, InterruptedException {
    this.sandboxManager.runScript(testModule, testStartScript, parties);
  }

  public static class SandboxBuilder {
    private static final Path WORKING_DIRECTORY = Paths.get("").toAbsolutePath();
    private Optional<String> testModule = Optional.empty();
    private Optional<String> testStartScript = Optional.empty();
    private Duration sandboxWaitTimeout = DEFAULT_WAIT_TIMEOUT;
    private Duration observationTimeout = DEFAULT_OBSERVATION_TIMEOUT;
    private String[] parties = DEFAULT_PARTIES;
    private Path damlRoot = WORKING_DIRECTORY;
    private Path darPath;
    private boolean useWallclockTime = false;
    private BiConsumer<DamlLedgerClient, ManagedChannel> setupApplication = (t, u) -> {};
    private Optional<String> ledgerId = Optional.empty();
    private Optional<LogLevel> logLevel = Optional.empty();

    public SandboxBuilder dar(Path darPath) {
      this.darPath = darPath;
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
      this.damlRoot = damlRoot;
      return this;
    }

    public Sandbox build() {
      validate();

      return new Sandbox(
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
    }

    private void validate() {
      require(darPath != null, "DAR path cannot be null.");
      require(setupApplication != null, "Application setup function cannot be null.");
      require(
          isDamlRoot(damlRoot),
          String.format("DAML root '%s' must contain a daml.yaml.", damlRoot));
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

  public ExternalResource getRule() {
    return new ExternalResource() {
      @Override
      protected void before() throws Throwable {
        sandboxManager.start();
      }

      @Override
      protected void after() {
        sandboxManager.stop();
      }
    };
  }

  public static Party getUniqueParty(String party) {
    return new Party(String.format("%s_%s", party, UUID.randomUUID()));
  }
}
