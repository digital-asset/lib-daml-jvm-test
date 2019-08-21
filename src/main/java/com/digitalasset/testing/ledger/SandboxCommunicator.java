/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import com.daml.ledger.rxjava.DamlLedgerClient;
import com.digitalasset.ledger.api.v1.LedgerIdentityServiceGrpc;
import com.digitalasset.ledger.api.v1.LedgerIdentityServiceOuterClass;
import com.digitalasset.ledger.api.v1.testing.TimeServiceGrpc;
import com.digitalasset.testing.ledger.clock.SandboxTimeProvider;
import com.digitalasset.testing.store.DefaultValueStore;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static com.digitalasset.testing.utils.SandboxUtils.getSandboxPort;
import static com.digitalasset.testing.utils.SandboxUtils.waitForSandbox;

public class SandboxCommunicator {
  private static final Logger logger = LoggerFactory.getLogger(SandboxCommunicator.class);
  private static final String COMPILATION_LOG = "integration-test-compilation.log";
  private static final String DAML_EXE = "daml";
  private int sandboxPort;

  private final Optional<String> testModule;
  private final Optional<String> testScenario;
  private final Duration waitTimeout;
  private final String[] parties;
  private final Path darPath;
  private final Consumer<DamlLedgerClient> setupApplication;

  private SandboxRunner sandboxRunner;
  private DamlLedgerClient ledgerClient;
  private DefaultLedgerAdapter ledgerAdapter;
  private ManagedChannel channel;

  public SandboxCommunicator(
      Optional<String> testModule,
      Optional<String> testScenario,
      Duration waitTimeout,
      String[] parties,
      Path darPath,
      Consumer<DamlLedgerClient> setupApplication) {
    this.testModule = testModule;
    this.testScenario = testScenario;
    this.waitTimeout = waitTimeout;
    this.parties = parties;
    this.darPath = darPath;
    this.setupApplication = setupApplication;
  }

  public DamlLedgerClient getClient() {
    return ledgerClient;
  }

  public DefaultLedgerAdapter getLedgerAdapter() {
    return ledgerAdapter;
  }

  public ManagedChannel getChannel() {
    return channel;
  }

  public void startSandbox() throws IOException, TimeoutException {
    sandboxPort = getSandboxPort();
    sandboxRunner =
        new SandboxRunner(darPath.toString(), testModule, testScenario, sandboxPort, waitTimeout);
    sandboxRunner.startSandbox();
  }

  public void startCommChannels() throws TimeoutException {
    channel =
        ManagedChannelBuilder.forAddress("localhost", sandboxPort)
            .usePlaintext()
            .maxInboundMessageSize(Integer.MAX_VALUE)
            .build();
    ledgerClient = new DamlLedgerClient(Optional.empty(), channel);
    waitForSandbox(ledgerClient, waitTimeout, logger);
    String ledgerId =
        LedgerIdentityServiceGrpc.newBlockingStub(channel)
            .getLedgerIdentity(
                LedgerIdentityServiceOuterClass.GetLedgerIdentityRequest.newBuilder().build())
            .getLedgerId();
    ledgerAdapter =
        new DefaultLedgerAdapter(
            new DefaultValueStore(),
            ledgerId,
            channel,
            SandboxTimeProvider.factory(TimeServiceGrpc.newStub(channel), ledgerId));
    ledgerAdapter.start(parties);
    setupApplication.accept(ledgerClient);
  }

  public void startAll() throws TimeoutException, IOException {
    startSandbox();
    startCommChannels();
  }

  public void stopAll() {
    stopCommChannels();
    stopSandbox();
  }

  public void stopCommChannels() {
    try {
      if (ledgerAdapter != null) {
        ledgerAdapter.stop();
      }
    } catch (InterruptedException e) {
      logger.warn("Failed to stop ledger adapter", e);
    }
    try {
      if (ledgerClient != null) {
        ledgerClient.close();
      }
    } catch (Exception e) {
      logger.warn("Failed to close ledger client", e);
    }

    try {
      if (channel != null) {
        channel.shutdown().awaitTermination(5L, TimeUnit.SECONDS);
      }
    } catch (InterruptedException e) {
      logger.warn("Failed to stop the managed channel", e);
    }

    channel = null;
    ledgerAdapter = null;
    ledgerClient = null;
  }

  public void stopSandbox() {
    try {
      if (sandboxRunner != null) {
        sandboxRunner.stopSandbox();
      }
    } catch (Exception e) {
      logger.warn("Failed to stop sandbox", e);
    }
    sandboxRunner = null;
  }
}
