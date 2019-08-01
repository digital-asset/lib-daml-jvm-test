/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.junit4;

import com.digitalasset.daml_lf.DamlLf;
import com.digitalasset.daml_lf.DamlLf1;
import com.digitalasset.testing.comparator.ledger.ContractCreated;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import com.digitalasset.testing.ledger.SandboxRunner;
import com.digitalasset.testing.utils.ContractWithId;

import com.daml.ledger.javaapi.data.GetPackageResponse;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Record;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.daml.ledger.rxjava.PackageClient;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Sandbox extends ExternalResource {

  private static String DEFAULT_TEST_MODULE = "Main";
  private static String DEFAULT_TEST_SCENARIO = "test";
  private static int DEFAULT_SANDBOX_PORT = 6865;
  private static int DEFAULT_WAIT_TIMEOUT = 30;
  private static String[] DEFAULT_PARTIES = new String[] {};

  private static final String COMPILATION_LOG = "integration-test-compilation.log";
  private static final String DAML_EXE = "daml";

  public static SandboxBuilder builder() {
    return new SandboxBuilder();
  }

  public static class SandboxBuilder {
    private Path projectDir;
    private String testModule = DEFAULT_TEST_MODULE;
    private String testScenario = DEFAULT_TEST_SCENARIO;
    private int sandboxPort = DEFAULT_SANDBOX_PORT;
    private int waitTimeout = DEFAULT_WAIT_TIMEOUT;
    private String[] parties = DEFAULT_PARTIES;
    private Path darPath;
    private Consumer<DamlLedgerClient> setupApplication;

    public SandboxBuilder projectDir(Path projectDir) {
      this.projectDir = projectDir;
      return this;
    }

    public SandboxBuilder dar(Path darPath) {
      this.darPath = darPath;
      return this;
    }

    public SandboxBuilder module(String testModule) {
      this.testModule = testModule;
      return this;
    }

    public SandboxBuilder scenario(String testScenario) {
      this.testScenario = testScenario;
      return this;
    }

    public SandboxBuilder port(int sandboxPort) {
      this.sandboxPort = sandboxPort;
      return this;
    }

    public SandboxBuilder timeout(int waitTimeout) {
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

    public Sandbox build() {
      Objects.requireNonNull(darPath);
      Objects.requireNonNull(projectDir);

      if (setupApplication == null) {
        setupApplication = (t) -> {};
      }

      return new Sandbox(
          projectDir,
          testModule,
          testScenario,
          sandboxPort,
          waitTimeout,
          parties,
          darPath,
          setupApplication);
    }

    private SandboxBuilder() {}
  }

  private final Path projectDir;
  private final String testModule;
  private final String testScenario;
  private final int sandboxPort;
  private final int waitTimeout;
  private final String[] parties;
  private final Path darPath;
  private final Consumer<DamlLedgerClient> setupApplication;

  private Sandbox(
      Path projectDir,
      String testModule,
      String testScenario,
      int sandboxPort,
      int waitTimeout,
      String[] parties,
      Path darPath,
      Consumer<DamlLedgerClient> setupApplication) {
    this.projectDir = projectDir;
    this.testModule = testModule;
    this.testScenario = testScenario;
    this.sandboxPort = sandboxPort;
    this.waitTimeout = waitTimeout;
    this.parties = parties;
    this.darPath = darPath;
    this.setupApplication = setupApplication;
  }

  public ExternalResource compilation() {
    return new ExternalResource() {
      public void before() throws IOException, InterruptedException {
        new ProcessBuilder(
                DAML_EXE,
                "build",
                "--project-root",
                projectDir.toString(),
                "--output",
                darPath.toString())
            .redirectError(new File(COMPILATION_LOG))
            .redirectOutput(new File(COMPILATION_LOG))
            .directory(projectDir.toFile())
            .start()
            .waitFor();
      }
    };
  }

  public Process process() {
    return new Process();
  }

  public class Process extends ExternalResource {
    private SandboxRunner sandboxRunner;
    private ConcurrentHashMap<DamlLf1.DottedName, String> packageNames = new ConcurrentHashMap<>();

    private static final String key = "internal-cid-query";
    private static final String recordKey = "internal-recordKey";

    @Override
    protected void before() throws Throwable {
      sandboxRunner =
          new SandboxRunner(
              darPath.toString(),
              testModule,
              testScenario,
              sandboxPort,
              waitTimeout,
              parties,
              setupApplication);
      sandboxRunner.startSandbox();
    };

    @Override
    protected void after() {
      sandboxRunner.stopSandbox();
      sandboxRunner = null;
    }

    public DamlLedgerClient getClient() {
      return sandboxRunner.damlLedgerClient();
    }

    public DefaultLedgerAdapter getLedgerAdapter() {
      return sandboxRunner.ledgerAdapter();
    }

    public <Cid> Cid getCreatedContractId(
        Party party, Identifier identifier, Record arguments, Function<String, Cid> ctor) {
      getLedgerAdapter()
          .observeEvent(
              party.getValue(),
              ContractCreated.apply(identifier, "{CAPTURE:" + key + "}", arguments));
      String val = getLedgerAdapter().valueStore().get(key).getContractId();
      Cid cid = ctor.apply(val);
      getLedgerAdapter().valueStore().remove(key);
      return cid;
    }

    public <Cid> Cid getCreatedContractId(
        Party party, Identifier identifier, Function<String, Cid> ctor) {
      getLedgerAdapter()
          .observeEvent(
              party.getValue(), ContractCreated.apply(identifier, "{CAPTURE:" + key + "}"));
      String val = getLedgerAdapter().valueStore().get(key).getContractId();
      Cid cid = ctor.apply(val);
      getLedgerAdapter().valueStore().remove(key);
      return cid;
    }

    public <Cid> ContractWithId<Cid> getMatchedContract(
        Party party, Identifier identifier, Function<String, Cid> ctor) {
      try {
        getLedgerAdapter()
            .observeEvent(
                party.getValue(),
                ContractCreated.apply(identifier, "{CAPTURE:" + key + "}", recordKey));

        String contractId = getLedgerAdapter().valueStore().get(key).getContractId();
        Cid cid = ctor.apply(contractId);
        Value record =
            Value.fromProto(
                com.digitalasset.ledger.api.v1.ValueOuterClass.Value.parseFrom(
                    getLedgerAdapter().valueStore().get(recordKey).toByteArray()));
        getLedgerAdapter().valueStore().remove(key);
        getLedgerAdapter().valueStore().remove(recordKey);
        return new ContractWithId<>(cid, record);
      } catch (InvalidProtocolBufferException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Makes sure that a set of contracts are present on the ledger. The contracts with the given
     * templateIds are fetched one-by-one and matched against the given set of predicates. If all
     * predicates are satisfied the method returns with true.
     *
     * <p>If `exact` is true, each incoming contract must match a predicate, otherwise the method
     * will return false.
     *
     * <p>Note: the ledger cursor will be moved during the execution of this method.
     *
     * @return true if all predicates were satisfied, false if a non-matching contract was observed
     *     in exact mode.
     */
    public <Contract> boolean observeMatchingContracts(
        Party party,
        Identifier templateId,
        Function<Value, Contract> ctor,
        boolean exact,
        Predicate<Contract>... predicates) {
      Set<Predicate<Contract>> predicateSet = new HashSet<>(Arrays.asList(predicates));
      while (!predicateSet.isEmpty()) {
        ContractWithId<String> contractWithId = getMatchedContract(party, templateId, i -> i);
        Contract contract = ctor.apply(contractWithId.record);
        Optional<Predicate<Contract>> predicateMatch =
            predicateSet.stream().filter(p -> p.test(contract)).findFirst();
        if (predicateMatch.isPresent()) {
          predicateSet.remove(predicateMatch.get());
        } else {
          if (exact) return false;
        }
      }
      return true;
    }

    public Identifier templateIdentifier(
        DamlLf1.DottedName packageName, String moduleName, String entityName)
        throws InvalidProtocolBufferException {
      String pkg = findPackage(packageName);
      return new Identifier(pkg, moduleName, entityName);
    }

    private String findPackage(DamlLf1.DottedName packageName)
        throws InvalidProtocolBufferException {
      String strName = packageNames.get(packageName);
      if (strName != null) {
        return strName;
      } else {
        PackageClient pkgClient = getClient().getPackageClient();
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
}
