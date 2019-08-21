/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.junit4;

import com.daml.ledger.javaapi.data.Party;
import com.digitalasset.daml_lf.DamlLf1;
import com.digitalasset.ledger.api.v1.testing.ResetServiceGrpc;
import com.digitalasset.ledger.api.v1.testing.ResetServiceOuterClass;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import com.digitalasset.testing.ledger.SandboxCommunicator;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.rules.ExternalResource;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.digitalasset.testing.utils.PackageUtils.findPackage;

public class Sandbox extends ExternalResource {
  private static Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(30);
  private static String[] DEFAULT_PARTIES = new String[] {};
  private final SandboxCommunicator sandboxCommunicator;

  public static SandboxBuilder builder() {
    return new SandboxBuilder();
  }

  public static class SandboxBuilder {
    private Optional<String> testModule = Optional.empty();
    private Optional<String> testScenario = Optional.empty();
    private Duration waitTimeout = DEFAULT_WAIT_TIMEOUT;
    private String[] parties = DEFAULT_PARTIES;
    private Path darPath;
    private Consumer<DamlLedgerClient> setupApplication;
    private boolean useReset = false;

    public SandboxBuilder dar(Path darPath) {
      this.darPath = darPath;
      return this;
    }

    public SandboxBuilder module(String testModule) {
      this.testModule = Optional.of(testModule);
      return this;
    }

    public SandboxBuilder scenario(String testScenario) {
      this.testScenario = Optional.of(testScenario);
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
      this.setupApplication = setupApplication;
      return this;
    }

    public SandboxBuilder useReset() {
      this.useReset = true;
      return this;
    }

    public Sandbox build() {
      Objects.requireNonNull(darPath);

      if (useReset && (testModule.isPresent() || testScenario.isPresent())) {
        throw new IllegalStateException(
            "Reset mode cannot be used together with market setup module/scenario.");
      }

      if (testModule.isPresent() ^ testScenario.isPresent()) {
        throw new IllegalStateException(
            "Market setup module and scenario need to be defined together.");
      }

      if (setupApplication == null) {
        setupApplication = (t) -> {};
      }

      return new Sandbox(
              testModule, testScenario, waitTimeout, parties, darPath, setupApplication, useReset);
    }

    private SandboxBuilder() {}
  }

  private final boolean useReset;

  private Sandbox(
      Optional<String> testModule,
      Optional<String> testScenario,
      Duration waitTimeout,
      String[] parties,
      Path darPath,
      Consumer<DamlLedgerClient> setupApplication,
      boolean useReset) {
    this.sandboxCommunicator =
        new SandboxCommunicator(
            testModule, testScenario, waitTimeout, parties, darPath, setupApplication);
    this.useReset = useReset;
  }

  public DamlLedgerClient getClient() {
    return sandboxCommunicator.getClient();
  }

  public DefaultLedgerAdapter getLedgerAdapter() {
    return sandboxCommunicator.getLedgerAdapter();
  }

  public Identifier templateIdentifier(
      DamlLf1.DottedName packageName, String moduleName, String entityName)
      throws InvalidProtocolBufferException {
    String pkg = findPackage(sandboxCommunicator.getClient(), packageName);
    return new Identifier(pkg, moduleName, entityName);
  }

  public ExternalResource getClassRule() {
    return new ExternalResource() {
      @Override
      protected void before() throws Throwable {
        if (useReset) {
          sandboxCommunicator.startSandbox();
        }
      }

      @Override
      protected void after() {
        if (useReset) {
          sandboxCommunicator.stopSandbox();
        }
      }
    };
  }

  public ExternalResource getRule() {
    return new ExternalResource() {
      @Override
      protected void before() throws Throwable {
        if (useReset) {
          sandboxCommunicator.startCommChannels();
        } else {
          sandboxCommunicator.startSandbox();
          sandboxCommunicator.startCommChannels();
        }
      }

      @Override
      protected void after() {
        if (useReset) {
          ResetServiceGrpc.newBlockingStub(sandboxCommunicator.getChannel())
              .reset(
                  ResetServiceOuterClass.ResetRequest.newBuilder()
                      .setLedgerId(sandboxCommunicator.getClient().getLedgerId())
                      .build());
          sandboxCommunicator.stopCommChannels();
        } else {
          sandboxCommunicator.stopAll();
        }
      }
    };
  }
}
