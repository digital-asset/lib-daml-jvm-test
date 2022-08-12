/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.junit5;

import com.daml.daml_lf_dev.DamlLf1;
import com.daml.extensions.testing.ledger.DefaultLedgerAdapter;
import com.daml.extensions.testing.ledger.SandboxManager;
import com.daml.extensions.testing.utils.SandboxUtils;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.daml.extensions.testing.JvmTestLibCommon.*;
import static com.daml.extensions.testing.utils.PackageUtils.findPackage;
import static com.daml.extensions.testing.utils.Preconditions.require;

public class Sandbox {
  private static final Logger logger = LoggerFactory.getLogger(SandboxManager.class);

  private final SandboxManager sandboxManager;

  public static SandboxBuilder builder() {
    return new SandboxBuilder();
  }

  private Sandbox(
      Path damlRoot,
      Optional<String> testModule,
      Optional<String> testStartScript,
      int port,
      Duration sandboxWaitTimeout,
      Duration observationTimeout,
      String[] parties,
      Path darPath,
      BiConsumer<DamlLedgerClient, ManagedChannel> setupApplication,
      boolean useWallclockTime,
      boolean useContainers,
      Optional<String> damlImage,
      Optional<String> ledgerId,
      Optional<LogLevel> logLevel) {
    this.sandboxManager =
        new SandboxManager(
            damlRoot,
            testModule,
            testStartScript,
            Optional.of(port),
            sandboxWaitTimeout,
            observationTimeout,
            parties,
            darPath,
            setupApplication,
            useWallclockTime,
            useContainers,
            damlImage,
            ledgerId,
            logLevel);
  }

  public boolean isRunnning() {
    return sandboxManager.isRunning();
  }

  public static class SandboxBuilder {
    private Optional<String> testModule = Optional.empty();
    private Optional<DamlLf1.DottedName> moduleDottedName = Optional.empty();
    private Optional<String> testStartScript = Optional.empty();
    private int port = 0;
    private Duration sandboxWaitTimeout = DEFAULT_WAIT_TIMEOUT;
    private Duration observationTimeout = DEFAULT_OBSERVATION_TIMEOUT;
    private String[] parties = DEFAULT_PARTIES;
    private Path damlRoot;
    private Path darPath;
    private boolean useWallclockTime = false;
    private boolean useContainers = false;
    private Optional<String> damlImage = Optional.empty();
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

    public SandboxBuilder port(int port) {
      this.port = port;
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

    public SandboxBuilder setupAppCallback(Consumer<DamlLedgerClient> setupApplication) {
      // todo what this feature does?
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

    public SandboxBuilder useContainers() {
      this.useContainers = true;
      return this;
    }

    public SandboxBuilder damlImage(String image) {
      // todo test it
      this.damlImage = Optional.ofNullable(image);
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

      if (port != 0 && useContainers) {
        logger.info("Custom port setup ignored if containers are used");
      }

      if (port == 0) {
        port = SandboxUtils.getSandboxPort();
      }

      validate();

      return new Sandbox(
          damlRoot,
          testModule,
          testStartScript,
          port,
          sandboxWaitTimeout,
          observationTimeout,
          parties,
          darPath,
          setupApplication,
          useWallclockTime,
          useContainers,
          damlImage,
          ledgerId,
          logLevel);
    }

    private void validate() {
      require(darPath != null, "DAR path cannot be null.");
      require(setupApplication != null, "Application setup function cannot be null.");
      require(port != 0, "Port should be defined before start");
      if (!useContainers) {
        require(damlRoot != null, "Daml root cannot be null if run sandbox on host");
      }
    }
  }

  public DamlLedgerClient getClient() {
    return sandboxManager.getClient();
  }

  public DefaultLedgerAdapter getLedgerAdapter() {
    return sandboxManager.getLedgerAdapter();
  }

  public Party getPartyId(String partyName) {
    return sandboxManager.getPartyId(partyName);
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

  public SandboxManager getSandboxManager() {
    return sandboxManager;
  }

  public Identifier templateIdentifier(
      DamlLf1.DottedName packageName, String moduleName, String entityName)
      throws InvalidProtocolBufferException {

    String pkg = findPackage(sandboxManager.getClient(), packageName);
    return new Identifier(pkg, moduleName, entityName);
  }

  public void stop() {
    sandboxManager.stop();
  }

  public void restart() throws IOException, InterruptedException, TimeoutException {
    sandboxManager.restart();
  }
}
