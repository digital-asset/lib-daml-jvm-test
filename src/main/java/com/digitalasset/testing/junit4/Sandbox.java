/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.junit4;

import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.daml.daml_lf_dev.DamlLf1;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import com.digitalasset.testing.ledger.SandboxManager;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import org.junit.rules.ExternalResource;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.digitalasset.testing.utils.PackageUtils.findPackage;

public class Sandbox {
  private static Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(30);
  private static String[] DEFAULT_PARTIES = new String[] {};
  private final SandboxManager sandboxManager;

  public static SandboxBuilder builder() {
    return new SandboxBuilder();
  }

  public static class SandboxBuilder {
    private Optional<String> testModule = Optional.empty();
    private Optional<String> testStartScript = Optional.empty();
    private Duration waitTimeout = DEFAULT_WAIT_TIMEOUT;
    private String[] parties = DEFAULT_PARTIES;
    private Path darPath;
    private boolean useWallclockTime = false;
    private boolean useReset = false;
    private BiConsumer<DamlLedgerClient, ManagedChannel> setupApplication;
    private Optional<String> ledgerId = Optional.empty();
    private Optional<LogLevel> logLevel = Optional.empty();

    public SandboxBuilder dar(Path darPath) {
      this.darPath = darPath;
      return this;
    }

    public SandboxBuilder module(String testModule) {
      this.testModule = Optional.of(testModule);
      return this;
    }

    public SandboxBuilder startScript(String testStartScript) {
      this.testStartScript = Optional.of(testStartScript);
      return this;
    }

    public SandboxBuilder timeout(Duration waitTimeout) {
      this.waitTimeout = waitTimeout;
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

    public Sandbox build() {
      Objects.requireNonNull(darPath);

      if (useReset && (testModule.isPresent() || testStartScript.isPresent())) {
        throw new IllegalStateException(
            "Reset mode cannot be used together with market setup module/scenario.");
      }

      if (testModule.isPresent() ^ testStartScript.isPresent()) {
        throw new IllegalStateException(
            "Market setup module and scenario need to be defined together.");
      }

      if (setupApplication == null) {
        setupApplication = (t, u) -> {};
      }

      return new Sandbox(
          testModule,
          testStartScript,
          waitTimeout,
          parties,
          darPath,
          setupApplication,
          useWallclockTime,
          useReset,
          ledgerId,
          logLevel);
    }

    private SandboxBuilder() {}
  }

  private final boolean useReset;

  private Sandbox(
      Optional<String> testModule,
      Optional<String> testStartScript,
      Duration waitTimeout,
      String[] parties,
      Path darPath,
      BiConsumer<DamlLedgerClient, ManagedChannel> setupApplication,
      boolean useWallclockTime,
      boolean useReset,
      Optional<String> ledgerId,
      Optional<LogLevel> logLevel) {
    this.sandboxManager =
        new SandboxManager(
            testModule,
            testStartScript,
            waitTimeout,
            parties,
            darPath,
            setupApplication,
            useWallclockTime,
            ledgerId,
            logLevel);
    this.useReset = useReset;
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
