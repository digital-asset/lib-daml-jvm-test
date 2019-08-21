// Copyright (c) 2019, Digital Asset (Switzerland) GmbH and/or its affiliates.
// All rights reserved.

package com.digitalasset.testing.cucumber.steps;

import static com.digitalasset.testing.Dsl.party;
import static com.digitalasset.testing.Dsl.record;
import static com.digitalasset.testing.cucumber.utils.TableUtils.fieldsToArgs;
import static com.digitalasset.testing.utils.PackageUtils.findTemplate;
import static com.digitalasset.testing.utils.SandboxUtils.getSandboxPort;
import static com.digitalasset.testing.utils.SandboxUtils.waitForSandbox;
import static org.junit.Assert.assertTrue;

import com.daml.ledger.javaapi.data.ContractId;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Record;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.digitalasset.ledger.api.v1.LedgerIdentityServiceGrpc;
import com.digitalasset.ledger.api.v1.LedgerIdentityServiceOuterClass;
import com.digitalasset.ledger.api.v1.testing.TimeServiceGrpc;
import com.digitalasset.testing.comparator.ledger.ContractArchived;
import com.digitalasset.testing.comparator.ledger.ContractCreated;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import com.digitalasset.testing.ledger.SandboxRunner;
import com.digitalasset.testing.ledger.clock.SandboxTimeProvider;
import com.digitalasset.testing.store.DefaultValueStore;
import com.digitalasset.testing.utils.PackageUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import cucumber.api.java8.En;
import io.cucumber.datatable.DataTable;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

// Notes:
// - for optional parts, one needs to use the form (:? expecting (failure))? because otherwise
//   Cucumber will complain about having an extra argument in the lambda expression
@SuppressWarnings("unused")
public class LedgerInteractions implements En {
  private static final String WITH = "WITH";
  private final AtomicReference<Throwable> resultHolder = new AtomicReference<>();
  // TODO: Have a common class with all this Sandbox runner stuff
  private SandboxRunner sandboxRunner = null;
  private DamlLedgerClient ledgerClient;
  private DefaultLedgerAdapter ledgerAdapter;
  private ManagedChannel channel;
  private static final Logger logger = LoggerFactory.getLogger(LedgerInteractions.class);

  public LedgerInteractions() {
    Given(
        "^Sandbox is started with DAR \"([^\"]+)\"$",
        (String darPath, DataTable dataTable) -> {
          logger.info("Starting Sandbox...");
          Integer sandboxPort = getSandboxPort();
          sandboxRunner =
              new SandboxRunner(
                  darPath.toString(),
                  Optional.empty(),
                  Optional.empty(),
                  sandboxPort,
                  Duration.ofSeconds(30));
          sandboxRunner.startSandbox();
          channel =
              ManagedChannelBuilder.forAddress("localhost", sandboxPort)
                  .usePlaintext()
                  .maxInboundMessageSize(Integer.MAX_VALUE)
                  .build();
          ledgerClient = new DamlLedgerClient(Optional.empty(), channel);
          waitForSandbox(ledgerClient, Duration.ofSeconds(30), logger);
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
          ledgerAdapter.start(dataTable.asList().toArray(new String[] {}));
          // setupApplication.accept(ledgerClient);
        });
    After(
        () -> {
          logger.info("Stopping Sandbox...");
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

          try {
            sandboxRunner.stopSandbox();
          } catch (Exception e) {
            logger.warn("Failed to stop sandbox", e);
          }

          channel = null;
          sandboxRunner = null;

          ledgerAdapter = null;
          ledgerClient = null;
        });

    // Given - When - Then clauses :
    // ----------------------------
    When(
        "^.*\"([^\"]+)\" creates contract \"([^\"]+)\" using values(?: expecting (failure))?$",
        (String party, String moduleAndEntityName, String expectedFailure, DataTable dataTable) ->
            new LedgerExecutor(expectedFailure != null) {
              void run() throws InvalidProtocolBufferException {
                PackageUtils.TemplateType idWithArgs =
                    findTemplate(ledgerClient, moduleAndEntityName);
                // TODO: check datatable's dimension
                Record args = fieldsToArgs(dataTable.asList(), idWithArgs.createFields);
                ledgerAdapter.createContract(new Party(party), idWithArgs.identifier, args);
              }
            });
    When(
        "^.*\"([^\"]+)\" exercises choice \"([^\"]+)\" on \"([^\"]*)\" with contract id \"([^\"]+)\"(?: and (expects failure))?$",
        (String party,
            String choiceName,
            String moduleAndEntityName,
            String contractIdKey,
            String expectedFailure) ->
            new LedgerExecutor(expectedFailure != null) {
              void run() throws InvalidProtocolBufferException {
                PackageUtils.TemplateType idWithArgs =
                    findTemplate(ledgerClient, moduleAndEntityName);
                ContractId contractId =
                    ledgerAdapter.valueStore.get(contractIdKey).asContractId().get();
                ledgerAdapter.exerciseChoice(
                    party(party), idWithArgs.identifier, contractId, choiceName, new Record());
              }
            });
    When(
        "^.*\"([^\"]+)\" exercises choice \"([^\"]+)\" on \"([^\"]+)\" with contract id \"([^\"]+)\" using values(?: and (expects failure))?$",
        (String party,
            String choiceName,
            String moduleAndEntityName,
            String contractIdKey,
            String expectedFailure,
            DataTable dataTable) ->
            new LedgerExecutor(expectedFailure != null) {
              void run() throws InvalidProtocolBufferException {
                PackageUtils.TemplateType idWithArgs =
                    findTemplate(ledgerClient, moduleAndEntityName);
                Record args = fieldsToArgs(dataTable.asList(), idWithArgs.choices.get(choiceName));
                ContractId contractId =
                    ledgerAdapter.valueStore.get(contractIdKey).asContractId().get();
                ledgerAdapter.exerciseChoice(
                    party(party), idWithArgs.identifier, contractId, choiceName, args);
              }
            });
    Then(
        "^.*\"([^\"]+)\" should observe the creation of \"([^\"]+)\"$",
        (String party, String moduleAndEntityName) -> {
          PackageUtils.TemplateType idWithArgs = findTemplate(ledgerClient, moduleAndEntityName);
          ledgerAdapter.getCreatedContractId(party(party), idWithArgs.identifier, ContractId::new);
        });
    Then(
        "^.*\"([^\"]+)\" should observe the creation of \"([^\"]+)\" with(?: contract id \"([^\"]+)\" and)? values$",
        (String party, String moduleAndEntityName, String contractId, DataTable dataTable) -> {
          PackageUtils.TemplateType idWithArgs = findTemplate(ledgerClient, moduleAndEntityName);
          // TODO: check datatable's dimension
          Record args = fieldsToArgs(dataTable.asList(), idWithArgs.createFields);
          ledgerAdapter.observeEvent(
              party,
              ContractCreated.expectContractWithArguments(
                  idWithArgs.identifier, "{CAPTURE:" + contractId + "}", args));
        });
    Then(
        "^.*\"([^\"]+)\" should observe the archival of \"([^\"]+)\" with contract id \"([^\"]+)\".*$",
        (String party, String moduleAndEntityName, String contractIdKey) -> {
          PackageUtils.TemplateType idWithArgs = findTemplate(ledgerClient, moduleAndEntityName);
          ContractId contractId = ledgerAdapter.valueStore.get(contractIdKey).asContractId().get();
          ledgerAdapter.observeEvent(
              party, ContractArchived.apply(idWithArgs.identifier.toString(), contractId));
        });
    Then(
        "^.*they should receive a technical failure (with|containing) message \\s*\"([^\"]*)\".*$",
        (String withOrContaining, String errorMessageRegex) -> {
          Throwable lastResult = resultHolder.getAndSet(null);
          if (lastResult == null) {
            throw new AssertionError("Failure was expected but not observed");
          }
          if (!withOrContaining.equals(WITH)) {
            errorMessageRegex = ".*" + errorMessageRegex + ".*";
          }
          assertTrue(
              "Expected error text [" + errorMessageRegex + "], but found [" + lastResult + "]",
              Pattern.compile(errorMessageRegex, Pattern.DOTALL)
                  .matcher(lastResult.toString())
                  .matches());
        });
  }

  abstract class LedgerExecutor {
    LedgerExecutor(boolean expectingError) throws InvalidProtocolBufferException {
      try {
        run();
      } catch (Throwable t) {
        if (!expectingError) throw t;
        resultHolder.getAndSet(t);
      }
    }

    abstract void run() throws InvalidProtocolBufferException;
  }
}
