/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import static com.digitalasset.testing.utils.SandboxUtils.getSandboxPort;
import static com.digitalasset.testing.utils.SandboxUtils.waitForSandbox;

import com.daml.ledger.rxjava.DamlLedgerClient;
import com.daml.ledger.api.v1.LedgerIdentityServiceGrpc;
import com.daml.ledger.api.v1.LedgerIdentityServiceOuterClass;
import com.daml.ledger.api.v1.testing.ResetServiceGrpc;
import com.daml.ledger.api.v1.testing.ResetServiceOuterClass;
import com.daml.ledger.api.v1.testing.TimeServiceGrpc;
import com.digitalasset.testing.junit4.LogLevel;
import com.digitalasset.testing.ledger.clock.SandboxTimeProvider;
import com.digitalasset.testing.ledger.clock.SystemTimeProvider;
import com.digitalasset.testing.ledger.clock.TimeProvider;
import com.digitalasset.testing.store.DefaultValueStore;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SandboxManager {
  private static final Logger logger = LoggerFactory.getLogger(SandboxManager.class);
  private int sandboxPort;

  private final Optional<String> testModule;
  private final Optional<String> testStartScript;
  private final Duration waitTimeout;
  private final boolean useWallclockTime;
  private final Optional<String> ledgerId;
  private final String[] parties;
  private final Path darPath;
  private final Optional<LogLevel> logLevel;
  private final BiConsumer<DamlLedgerClient, ManagedChannel> setupApplication;

  private SandboxRunner sandboxRunner;
  private DamlLedgerClient ledgerClient;
  private DefaultLedgerAdapter ledgerAdapter;
  private ManagedChannel channel;

  public SandboxManager(
      Optional<String> testModule,
      Optional<String> testStartScript,
      Duration waitTimeout,
      String[] parties,
      Path darPath,
      BiConsumer<DamlLedgerClient, ManagedChannel> setupApplication,
      boolean useWallclockTime) {
    this(
        testModule,
        testStartScript,
        waitTimeout,
        parties,
        darPath,
        setupApplication,
        useWallclockTime,
        Optional.empty(),
        Optional.empty());
  }

  public SandboxManager(
      Optional<String> testModule,
      Optional<String> testStartScript,
      Duration waitTimeout,
      String[] parties,
      Path darPath,
      BiConsumer<DamlLedgerClient, ManagedChannel> setupApplication,
      boolean useWallclockTime,
      Optional<String> ledgerId,
      Optional<LogLevel> logLevel) {
    this.testModule = testModule;
    this.testStartScript = testStartScript;
    this.waitTimeout = waitTimeout;
    this.parties = parties;
    this.darPath = darPath;
    this.setupApplication = setupApplication;
    this.useWallclockTime = useWallclockTime;
    this.ledgerId = ledgerId;
    this.logLevel = logLevel;
  }

  public int getPort() {
    return sandboxPort;
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

  public void start() throws TimeoutException, IOException {
    start(getSandboxPort());
  }

  public void start(int port) throws TimeoutException, IOException {
    startSandbox(port);
    startCommChannels();
  }

  public void stop() {
    stopCommChannels();
    stopSandbox();
  }

  public void restart() throws TimeoutException, IOException {
    stop();
    start();
  }

  public void reset() throws TimeoutException {
    ResetServiceGrpc.newBlockingStub(channel)
        .reset(
            ResetServiceOuterClass.ResetRequest.newBuilder()
                .setLedgerId(ledgerClient.getLedgerId())
                .build());
    stopCommChannels();
    startCommChannels();
  }

  private void startSandbox(int port) throws IOException {
    sandboxPort = port;
    sandboxRunner =
        SandboxRunnerFactory.getSandboxRunner(
            darPath, testModule, testStartScript, sandboxPort, useWallclockTime, ledgerId, logLevel);
    sandboxRunner.startSandbox();
  }

  private void startCommChannels() throws TimeoutException {
    channel =
        ManagedChannelBuilder.forAddress("localhost", sandboxPort)
            .usePlaintext()
            .maxInboundMessageSize(Integer.MAX_VALUE)
            .build();
    DamlLedgerClient.Builder builder = DamlLedgerClient.newBuilder("localhost", sandboxPort);
    ledgerClient = builder.build();
    waitForSandbox(ledgerClient, waitTimeout, logger);
    String ledgerId =
        LedgerIdentityServiceGrpc.newBlockingStub(channel)
            .getLedgerIdentity(
                LedgerIdentityServiceOuterClass.GetLedgerIdentityRequest.newBuilder().build())
            .getLedgerId();

    Supplier<TimeProvider> timeProviderFactory;
    if (useWallclockTime) {
      timeProviderFactory = SystemTimeProvider.factory();
    } else {
      timeProviderFactory = SandboxTimeProvider.factory(TimeServiceGrpc.newStub(channel), ledgerId);
    }
    ledgerAdapter =
        new DefaultLedgerAdapter(new DefaultValueStore(), ledgerId, channel, timeProviderFactory);
    ledgerAdapter.start(parties);
    setupApplication.accept(ledgerClient, channel);
  }

  private void stopCommChannels() {
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

  private void stopSandbox() {
    try {
      if (sandboxRunner != null) {
        sandboxRunner.stopSandbox();
      }
    } catch (Exception e) {
      logger.warn("Failed to stop sandbox", e);
    }
    sandboxRunner = null;
  }

  public String getLedgerId() {
    return ledgerId.orElse(getClient().getLedgerId());
  }

  public Optional<LogLevel> getLogLevel() {
    return logLevel;
  }
}
