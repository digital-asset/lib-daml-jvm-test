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
import com.digitalasset.testing.ledger.SandboxCommunicator;
import com.digitalasset.testing.utils.PackageUtils;
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
  private SandboxCommunicator sandboxCommunicator;
  private static final Logger logger = LoggerFactory.getLogger(LedgerInteractions.class);

  public LedgerInteractions() {
    Given(
        "^Sandbox is started with DAR \"([^\"]+)\"$",
        (String darPath, DataTable dataTable) -> {
          String[] parties = dataTable.asList().toArray(new String[] {});
          sandboxCommunicator =
              new SandboxCommunicator(
                  Optional.empty(),
                  Optional.empty(),
                  Duration.ofSeconds(30),
                  parties,
                  Paths.get(darPath),
                  (client) -> {});
          sandboxCommunicator.startAll();
        });
    After(
        () -> {
          if (sandboxCommunicator != null) {
            sandboxCommunicator.stopAll();
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
                    findTemplate(sandboxCommunicator.getClient(), moduleAndEntityName);
                Record args = fieldsToArgs(checkTableIsList(dataTable), idWithArgs.createFields);
                sandboxCommunicator
                    .getLedgerAdapter()
                    .createContract(new Party(party), idWithArgs.identifier, args);
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
                    findTemplate(sandboxCommunicator.getClient(), moduleAndEntityName);
                ContractId contractId =
                    sandboxCommunicator
                        .getLedgerAdapter()
                        .valueStore
                        .get(contractIdKey)
                        .asContractId()
                        .get();
                sandboxCommunicator
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
                    findTemplate(sandboxCommunicator.getClient(), moduleAndEntityName);
                Record args = fieldsToArgs(dataTable.asList(), idWithArgs.choices.get(choiceName));
                ContractId contractId =
                    sandboxCommunicator
                        .getLedgerAdapter()
                        .valueStore
                        .get(contractIdKey)
                        .asContractId()
                        .get();
                sandboxCommunicator
                    .getLedgerAdapter()
                    .exerciseChoice(
                        party(party), idWithArgs.identifier, contractId, choiceName, args);
              }
            });
    Then(
        "^.*\"([^\"]+)\" should observe the creation of \"([^\"]+)\"$",
        (String party, String moduleAndEntityName) -> {
          PackageUtils.TemplateType idWithArgs =
              findTemplate(sandboxCommunicator.getClient(), moduleAndEntityName);
          sandboxCommunicator
              .getLedgerAdapter()
              .getCreatedContractId(party(party), idWithArgs.identifier, ContractId::new);
        });
    Then(
        "^.*\"([^\"]+)\" should observe the creation of \"([^\"]+)\" with(?: contract id \"([^\"]+)\" and)? values$",
        (String party, String moduleAndEntityName, String contractId, DataTable dataTable) -> {
          PackageUtils.TemplateType idWithArgs =
              findTemplate(sandboxCommunicator.getClient(), moduleAndEntityName);
          Record args = fieldsToArgs(checkTableIsList(dataTable), idWithArgs.createFields);
          sandboxCommunicator
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
              findTemplate(sandboxCommunicator.getClient(), moduleAndEntityName);
          ContractId contractId =
              sandboxCommunicator
                  .getLedgerAdapter()
                  .valueStore
                  .get(contractIdKey)
                  .asContractId()
                  .get();
          sandboxCommunicator
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

  private List<String> checkTableIsList(DataTable dataTable) {
    if (dataTable.height() == 1 & dataTable.width() > 0) {
      return dataTable.asList();
    }
    throw new IllegalArgumentException(
        "The provided data table must be a list. Current dimension: "
            + dataTable.height()
            + "x"
            + dataTable.width());
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
