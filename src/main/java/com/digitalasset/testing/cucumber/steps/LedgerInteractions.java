/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// Copyright (c) 2019, Digital Asset (Switzerland) GmbH and/or its affiliates.
// All rights reserved.

package com.digitalasset.testing.cucumber.steps;

import com.daml.ledger.javaapi.data.ContractId;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Record;
import com.digitalasset.testing.comparator.ledger.ContractArchived;
import com.digitalasset.testing.comparator.ledger.ContractCreated;
import com.digitalasset.testing.cucumber.utils.World;
import com.digitalasset.testing.ledger.SandboxManager;
import com.digitalasset.testing.utils.PackageUtils;
import com.digitalasset.testing.utils.SandboxUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import cucumber.api.java8.En;
import io.cucumber.datatable.DataTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static com.digitalasset.testing.Dsl.party;
import static com.digitalasset.testing.cucumber.utils.TableUtils.fieldsToArgs;
import static com.digitalasset.testing.utils.PackageUtils.findTemplate;
import static org.junit.Assert.assertTrue;

// Notes:
// - for optional parts, one needs to use the form (:? expecting (failure))? because otherwise
//   Cucumber will complain about having an extra argument in the lambda expression
@SuppressWarnings("unused")
public class LedgerInteractions implements En {
  private static final String WITH = "WITH";
  private final AtomicReference<Throwable> resultHolder = new AtomicReference<>();
  private SandboxManager sandboxManager;
  private static final Logger logger = LoggerFactory.getLogger(LedgerInteractions.class);
  private World world;

  public LedgerInteractions(World world) {
    this.world = world;
    Given(
        "^Sandbox is started with DAR \"([^\"]+)\" and the following parties$",
        (String darPath, DataTable dataTable) -> {
          String[] parties = dataTable.asList().toArray(new String[] {});
          sandboxManager =
              new SandboxManager(
                  Optional.empty(),
                  Optional.empty(),
                  Duration.ofSeconds(30),
                  parties,
                  Paths.get(darPath),
                  (client) -> {});
          world.setSandboxPort(SandboxUtils.getSandboxPort());
          sandboxManager.start(world.getSandboxPort());
        });
    After(
        () -> {
          if (sandboxManager != null) {
            sandboxManager.stop();
          }
        });

    // Given - When - Then clauses :
    // ----------------------------
    When(
        "^.*\"([^\"]+)\" creates contract \"([^\"]+)\" using values(?: expecting (failure))?$",
        (String party, String moduleAndEntityName, String expectedFailure, DataTable dataTable) ->
            new LedgerExecutor(expectedFailure != null) {
              void run() throws InvalidProtocolBufferException {
                PackageUtils.TemplateType idWithArgs =
                    findTemplate(sandboxManager.getClient(), moduleAndEntityName);
                checkTableIsTwoOrManyRows(dataTable);
                for (int i = 1; i < dataTable.width(); i++) {
                  Record args = fieldsToArgs(dataTable.column(i), idWithArgs.createFields);
                  sandboxManager
                      .getLedgerAdapter()
                      .createContract(new Party(party), idWithArgs.identifier, args);
                }
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
                    findTemplate(sandboxManager.getClient(), moduleAndEntityName);
                ContractId contractId =
                    sandboxManager
                        .getLedgerAdapter()
                        .valueStore
                        .get(contractIdKey)
                        .asContractId()
                        .get();
                sandboxManager
                    .getLedgerAdapter()
                    .exerciseChoice(
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
                    findTemplate(sandboxManager.getClient(), moduleAndEntityName);
                Record args =
                    fieldsToArgs(
                        checkTableIsOneOrTwoRowsAndGet(dataTable),
                        idWithArgs.choices.get(choiceName));
                ContractId contractId =
                    sandboxManager
                        .getLedgerAdapter()
                        .valueStore
                        .get(contractIdKey)
                        .asContractId()
                        .get();
                sandboxManager
                    .getLedgerAdapter()
                    .exerciseChoice(
                        party(party), idWithArgs.identifier, contractId, choiceName, args);
              }
            });
    Then(
        "^.*\"([^\"]+)\" should observe the creation of \"([^\"]+)\"$",
        (String party, String moduleAndEntityName) -> {
          PackageUtils.TemplateType idWithArgs =
              findTemplate(sandboxManager.getClient(), moduleAndEntityName);
          sandboxManager
              .getLedgerAdapter()
              .getCreatedContractId(party(party), idWithArgs.identifier, ContractId::new);
        });
    Then(
        "^.*\"([^\"]+)\" should observe the creation of \"([^\"]+)\" with(?: contract id \"([^\"]+)\" and)? values$",
        (String party, String moduleAndEntityName, String contractId, DataTable dataTable) -> {
          PackageUtils.TemplateType idWithArgs =
              findTemplate(sandboxManager.getClient(), moduleAndEntityName);
          Record args =
              fieldsToArgs(checkTableIsOneOrTwoRowsAndGet(dataTable), idWithArgs.createFields);
          sandboxManager
              .getLedgerAdapter()
              .observeEvent(
                  party,
                  ContractCreated.expectContractWithArguments(
                      idWithArgs.identifier, "{CAPTURE:" + contractId + "}", args));
        });
    Then(
        "^.*\"([^\"]+)\" should observe the archival of \"([^\"]+)\" with contract id \"([^\"]+)\".*$",
        (String party, String moduleAndEntityName, String contractIdKey) -> {
          PackageUtils.TemplateType idWithArgs =
              findTemplate(sandboxManager.getClient(), moduleAndEntityName);
          ContractId contractId =
              sandboxManager.getLedgerAdapter().valueStore.get(contractIdKey).asContractId().get();
          sandboxManager
              .getLedgerAdapter()
              .observeEvent(
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

  private void checkTableIsTwoOrManyRows(DataTable dataTable) {
    if (dataTable.width() <= 1) {
      throw new IllegalArgumentException(
          "The provided data table must be contain at least two columns: "
              + "the argument names' columns and a columns with arguments. Current column count: "
              + dataTable.width());
    }
  }

  private List<String> checkTableIsOneOrTwoRowsAndGet(DataTable dataTable) {
    if (dataTable.width() == 1) {
      return dataTable.column(0);
    }
    if (dataTable.width() == 2) {
      return dataTable.column(1);
    }
    throw new IllegalArgumentException(
        "The provided data table must contain one (arguments) or two (names with arguments) columns."
            + " Current column count: "
            + dataTable.width());
  }

  protected int getSandboxPort() {
    return sandboxManager.getPort();
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
