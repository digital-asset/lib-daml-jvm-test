/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.junit4;

import com.digitalasset.daml_lf.DamlLf;
import com.digitalasset.daml_lf.DamlLf1;
import com.digitalasset.ledger.api.v1.LedgerIdentityServiceGrpc;
import com.digitalasset.ledger.api.v1.LedgerIdentityServiceOuterClass;
import com.digitalasset.ledger.api.v1.testing.ResetServiceGrpc;
import com.digitalasset.ledger.api.v1.testing.ResetServiceOuterClass;
import com.digitalasset.ledger.api.v1.testing.TimeServiceGrpc;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import com.digitalasset.testing.ledger.SandboxRunner;
import com.digitalasset.testing.ledger.clock.SandboxTimeProvider;
import com.digitalasset.testing.store.DefaultValueStore;

import com.daml.ledger.javaapi.data.GetPackageResponse;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.daml.ledger.rxjava.PackageClient;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Range;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Sandbox extends ExternalResource {
  private static final Logger logger = LoggerFactory.getLogger(Sandbox.class);

  private static Range<Integer> SANDBOX_PORT_RANGE = Range.closed(6860, 6890);
  private static Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(30);
  private static String[] DEFAULT_PARTIES = new String[] {};

  private static final String COMPILATION_LOG = "integration-test-compilation.log";
  private static final String DAML_EXE = "daml";
  private int sandboxPort;

  public static SandboxBuilder builder() {
    return new SandboxBuilder();
  }

  private static final AtomicInteger SANDBOX_PORT_COUNTER =
      new AtomicInteger(SANDBOX_PORT_RANGE.lowerEndpoint());

  private static int getSandboxPort() {
    return SANDBOX_PORT_COUNTER.updateAndGet(
        p -> {
          if (SANDBOX_PORT_RANGE.contains(p)) {
            return p + 1;
          } else {
            return SANDBOX_PORT_RANGE.lowerEndpoint();
          }
        });
  }

  public static class SandboxBuilder {
    private Path projectDir;
    private Optional<String> testModule = Optional.empty();
    private Optional<String> testScenario = Optional.empty();
    private Duration waitTimeout = DEFAULT_WAIT_TIMEOUT;
    private String[] parties = DEFAULT_PARTIES;
    private Path darPath;
    private Consumer<DamlLedgerClient> setupApplication;
    private boolean useReset = false;

    public SandboxBuilder projectDir(Path projectDir) {
      this.projectDir = projectDir;
      return this;
    }

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
      Objects.requireNonNull(projectDir);

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
          projectDir,
          testModule,
          testScenario,
          waitTimeout,
          parties,
          darPath,
          setupApplication,
          useReset);
    }

    private SandboxBuilder() {}
  }

  private final Path projectDir;
  private final Optional<String> testModule;
  private final Optional<String> testScenario;
  private final Duration waitTimeout;
  private final String[] parties;
  private final Path darPath;
  private final Consumer<DamlLedgerClient> setupApplication;
  private final boolean useReset;
  private SandboxRunner sandboxRunner;
  private DamlLedgerClient ledgerClient;
  private DefaultLedgerAdapter ledgerAdapter;
  private ManagedChannel channel;
  private ConcurrentHashMap<DamlLf1.DottedName, String> packageNames = new ConcurrentHashMap<>();

  private Sandbox(
      Path projectDir,
      Optional<String> testModule,
      Optional<String> testScenario,
      Duration waitTimeout,
      String[] parties,
      Path darPath,
      Consumer<DamlLedgerClient> setupApplication,
      boolean useReset) {
    this.projectDir = projectDir;
    this.testModule = testModule;
    this.testScenario = testScenario;
    this.waitTimeout = waitTimeout;
    this.parties = parties;
    this.darPath = darPath;
    this.setupApplication = setupApplication;
    this.useReset = useReset;
  }

  public DamlLedgerClient getClient() {
    return ledgerClient;
  }

  public DefaultLedgerAdapter getLedgerAdapter() {
    return ledgerAdapter;
  }

  public Identifier templateIdentifier(
      DamlLf1.DottedName packageName, String moduleName, String entityName)
      throws InvalidProtocolBufferException {
    String pkg = findPackage(packageName);
    return new Identifier(pkg, moduleName, entityName);
  }

  public ExternalResource getClassRule() {
    return new ExternalResource() {
      @Override
      protected void before() throws Throwable {
        if (useReset) {
          startSandbox();
        }
      }

      @Override
      protected void after() {
        if (useReset) {
          stopSandbox();
        }
      }
    };
  }

  public ExternalResource getRule() {
    return new ExternalResource() {
      @Override
      protected void before() throws Throwable {
        if (useReset) {
          startCommChannels(sandboxPort);
        } else {
          startSandbox();
          startCommChannels(sandboxPort);
        }
      }

      @Override
      protected void after() {
        if (useReset) {
          ResetServiceGrpc.newBlockingStub(channel)
              .reset(
                  ResetServiceOuterClass.ResetRequest.newBuilder()
                      .setLedgerId(ledgerClient.getLedgerId())
                      .build());
          stopCommChannels();
        } else {
          stopAll();
        }
      }
    };
  }

  private void startSandbox() throws IOException, TimeoutException {
    sandboxPort = getSandboxPort();
    sandboxRunner =
        new SandboxRunner(darPath.toString(), testModule, testScenario, sandboxPort, waitTimeout);
    sandboxRunner.startSandbox();
  }

  private void startCommChannels(int sandboxPort) throws TimeoutException {
    channel =
        ManagedChannelBuilder.forAddress("localhost", sandboxPort)
            .usePlaintext()
            .maxInboundMessageSize(Integer.MAX_VALUE)
            .build();
    ledgerClient = new DamlLedgerClient(Optional.empty(), channel);
    waitForSandbox(ledgerClient);
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

  private void stopAll() {
    stopCommChannels();
    stopSandbox();
  }

  private void stopCommChannels() {
    try {
      ledgerAdapter.stop();
    } catch (InterruptedException e) {
      logger.warn("Failed to stop ledger adapter", e);
    }
    try {
      ledgerClient.close();
    } catch (Exception e) {
      logger.warn("Failed to close ledger client", e);
    }

    try {
      channel.shutdown().awaitTermination(5L, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      logger.warn("Failed to stop the managed channel", e);
    }

    channel = null;
    ledgerAdapter = null;
    ledgerClient = null;
  }

  private void stopSandbox() {
    try {
      sandboxRunner.stopSandbox();
    } catch (Exception e) {
      logger.warn("Failed to stop sandbox", e);
    }
    sandboxRunner = null;
  }

  private void waitForSandbox(DamlLedgerClient client) throws TimeoutException {
    boolean connected = false;
    Stopwatch time = Stopwatch.createStarted();
    int attempts = 0;
    while (!connected && time.elapsed().compareTo(waitTimeout) <= 0) {
      try {
        client.connect();
        connected = true;
      } catch (Exception ignored) {
        try {
          logger.info("Waiting for sandbox...");
          TimeUnit.SECONDS.sleep(2 * attempts);
          attempts += 1;
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    if (connected) logger.info("Connected to sandbox.");
    else throw new TimeoutException("Can't connect to sandbox");
  }

  private String findPackage(DamlLf1.DottedName packageName) throws InvalidProtocolBufferException {
    String strName = packageNames.get(packageName);
    if (strName != null) {
      return strName;
    } else {
      PackageClient pkgClient = ledgerClient.getPackageClient();
      Iterable<String> pkgs = pkgClient.listPackages().blockingIterable();
      for (String pkgId : pkgs) {
        GetPackageResponse pkgResp = pkgClient.getPackage(pkgId).blockingGet();
        DamlLf.ArchivePayload archivePl =
            DamlLf.ArchivePayload.parseFrom(pkgResp.getArchivePayload());
        List<DamlLf1.Module> mods = archivePl.getDamlLf1().getModulesList();
        for (DamlLf1.Module mod : mods) {
          if (mod.getName().equals(packageName)) {
            packageNames.put(packageName, pkgId);
            return pkgId;
          }
        }
      }
    }

    throw new IllegalArgumentException("No package found " + packageName);
  }
}
