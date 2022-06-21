/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.ledger;

import com.daml.extensions.testing.junit4.LogLevel;
import com.daml.extensions.testing.ledger.clock.SandboxTimeProvider;
import com.daml.extensions.testing.ledger.clock.SystemTimeProvider;
import com.daml.extensions.testing.ledger.clock.TimeProvider;
import com.daml.extensions.testing.store.DefaultValueStore;
import com.daml.ledger.api.v1.testing.TimeServiceGrpc;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.rxjava.DamlLedgerClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Hashtable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static com.daml.extensions.testing.utils.SandboxUtils.getSandboxPort;
import static com.daml.extensions.testing.utils.SandboxUtils.waitForSandbox;

public class SandboxManager {
  private static final Logger logger = LoggerFactory.getLogger(SandboxManager.class);
  private final Path damlRoot;
  private int sandboxPort;

  private final Optional<String> testModule;
  private final Optional<String> testStartScript;
  private final Duration sandboxWaitTimeout;
  private final Duration observationTimeout;
  private final boolean useWallclockTime;
  private final Optional<String> ledgerId;
  private final String[] parties;
  private Hashtable<Party, Party> partyIdHashTable;
  private final Path darPath;
  private final Optional<LogLevel> logLevel;
  private final BiConsumer<DamlLedgerClient, ManagedChannel> setupApplication;

  private SandboxRunner sandboxRunner;
  private DamlLedgerClient ledgerClient;
  private DefaultLedgerAdapter ledgerAdapter;
  private ManagedChannel channel;

  public SandboxManager(
      Path damlRoot,
      Optional<String> testModule,
      Optional<String> testStartScript,
      Duration sandboxWaitTimeout,
      Duration observationTimeout,
      String[] parties,
      Path darPath,
      BiConsumer<DamlLedgerClient, ManagedChannel> setupApplication,
      boolean useWallclockTime) {
    this(
        damlRoot,
        testModule,
        testStartScript,
        sandboxWaitTimeout,
        observationTimeout,
        parties,
        darPath,
        setupApplication,
        useWallclockTime,
        Optional.empty(),
        Optional.empty());
  }

  public SandboxManager(
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
    this.damlRoot = damlRoot;
    this.testModule = testModule;
    this.testStartScript = testStartScript;
    this.sandboxWaitTimeout = sandboxWaitTimeout;
    this.observationTimeout = observationTimeout;
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

  public void start() throws TimeoutException, IOException, InterruptedException {
    start(getSandboxPort());
  }

  public void start(int port) throws TimeoutException, IOException, InterruptedException {
    startSandbox(port);
    startCommChannels();
    mapParties();
  }

  public void allocateParty(String partyName) {
    ledgerAdapter.allocatePartyOnLedger(partyName);
  }

  public void mapParties() {
    this.partyIdHashTable = ledgerAdapter.getMapKnownParties();
  }

  public Party getPartyIdForce(Party partyName) {
    // <DisplayName:LPartyId>
    if (!partyIdHashTable.containsKey(partyName)) {
      allocateParty(partyName.getValue());
      mapParties();
    }
    return partyIdHashTable.get(partyName);
  }

  public Party getPartyId(Party partyName) {
    if (!partyIdHashTable.containsKey(partyName)) {
      throw new NullPointerException(
          String.format("Party %s is not allocated", partyName.getValue()));
    }
    return partyIdHashTable.get(partyName);
  }

  public void stop() {
    stopCommChannels();
    stopSandbox();
  }

  public void restart() throws TimeoutException, IOException, InterruptedException {
    stop();
    start();
  }

  public String getLedgerId() {
    return ledgerId.orElse(getClient().getLedgerId());
  }

  public Optional<LogLevel> getLogLevel() {
    return logLevel;
  }

  private void startSandbox(int port) throws IOException {
    sandboxPort = port;
    sandboxRunner =
        SandboxRunnerFactory.getSandboxRunner(
            damlRoot, darPath, sandboxPort, useWallclockTime, ledgerId, logLevel);
    sandboxRunner.startSandbox();
  }

  private void startCommChannels() throws TimeoutException, IOException, InterruptedException {
    channel =
        ManagedChannelBuilder.forAddress("localhost", sandboxPort)
            .usePlaintext()
            .maxInboundMessageSize(Integer.MAX_VALUE)
            .build();
    DamlLedgerClient.Builder builder = DamlLedgerClient.newBuilder("localhost", sandboxPort);
    ledgerClient = builder.build();
    try {
      waitForSandbox(ledgerClient, sandboxWaitTimeout, logger);
    } catch (TimeoutException e) {
      try {
        sandboxRunner.stopSandbox();
      } catch (Exception ee) {
        throw new IOException("Unable to connect to sandbox, and failed to kill it.", ee);
      }
      throw e;
    }

    runScriptIfConfigured();

    String ledgerId = "sandbox";

    Supplier<TimeProvider> timeProviderFactory;
    if (useWallclockTime) {
      timeProviderFactory = SystemTimeProvider.factory();
    } else {
      timeProviderFactory = SandboxTimeProvider.factory(TimeServiceGrpc.newStub(channel), ledgerId);
    }
    ledgerAdapter =
        new DefaultLedgerAdapter(
            new DefaultValueStore(), ledgerId, channel, observationTimeout, timeProviderFactory);
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

  private void runScriptIfConfigured() throws IOException, InterruptedException {
    if (testModule.isPresent() && testStartScript.isPresent()) {
      DamlScriptRunner scriptRunner =
          new DamlScriptRunner.Builder()
              .damlRoot(damlRoot)
              .dar(darPath)
              .sandboxPort(sandboxPort)
              .scriptName(String.format("%s:%s", testModule.get(), testStartScript.get()))
              .useWallclockTime(useWallclockTime)
              .build();
      scriptRunner.run();
    }
  }
}
