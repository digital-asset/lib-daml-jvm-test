/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates.
// All rights reserved.

package com.daml.extensions.testing.cucumber.steps;

import com.daml.daml_lf_dev.DamlLf1;
import com.daml.extensions.testing.comparator.ledger.ContractArchived;
import com.daml.extensions.testing.comparator.ledger.ContractCreated;
import com.daml.extensions.testing.cucumber.utils.Config;
import com.daml.extensions.testing.ledger.SandboxManager;
import com.daml.extensions.testing.utils.PackageUtils;
import com.daml.ledger.javaapi.data.ContractId;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.Party;
import io.cucumber.datatable.DataTable;
import io.cucumber.java8.En;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static com.daml.extensions.testing.cucumber.utils.TableUtils.fieldsToArgs;
import static com.daml.extensions.testing.utils.PackageUtils.findPackageObject;
import static com.daml.extensions.testing.utils.PackageUtils.findTemplate;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Notes:
// - for optional parts, one needs to use the form (:? expecting (failure))? because otherwise
//   Cucumber will complain about having an extra argument in the lambda expression
@SuppressWarnings("unused")
public class LedgerInteractions implements En {
  private static final String WITH = "WITH";
  private final AtomicReference<Throwable> resultHolder = new AtomicReference<>();
  private SandboxManager sandboxManager;
  private static final Logger logger = LoggerFactory.getLogger(LedgerInteractions.class);

  private void startSandbox(Path damlRoot, Path[] darPath, String[] parties)
      throws InterruptedException, IOException, TimeoutException {
    sandboxManager =
        new SandboxManager(
            damlRoot,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Duration.ofSeconds(30),
            Duration.ofSeconds(10),
            parties,
            darPath,
            (client, channel) -> {},
            false);
    sandboxManager.start();
  }

  public LedgerInteractions(Config config) {
    Given(
        "^Sandbox is started in directory \"([^\"]+)\" with DAR \"([^\"]+)\" and the following parties$",
        (String damlRoot, String darPath, DataTable dataTable) -> {
          String[] parties = dataTable.asList().toArray(new String[] {});
          startSandbox(
              Paths.get(damlRoot).toAbsolutePath().normalize(),
              new Path[] { Paths.get(darPath).toAbsolutePath().normalize() },
              parties);
        });
      Given(
          "^Sandbox is started in directory \"([^\"]+)\" with parties \"([^\"]+)\" and the following dars$",
          (String damlRoot, String _parties, DataTable dataTable) -> {
              String[] _dars = dataTable.asList().toArray(new String[] {});
              Path[] dars = Arrays.asList(_dars).stream().map( darPath -> Paths.get(darPath).toAbsolutePath().normalize() )
                  .toArray(Path[]::new);
              String[] parties = _parties.trim ().length()==0 ? new String[0] :  _parties.split(",");
              startSandbox(
                  Paths.get(damlRoot).toAbsolutePath().normalize(),
                  dars,
                  parties);
          });
      Given(
        "^Sandbox is started with DAR \"([^\"]+)\" and the following parties$",
        (String darPathS, DataTable dataTable) -> {
          Path darPath = Paths.get(darPathS).toAbsolutePath().normalize();
          Path damlRoot = darPath.getParent();
          String[] parties = dataTable.asList().toArray(new String[] {});
          startSandbox(damlRoot, new Path[] { darPath }, parties);
        });
    After(
        () -> {
          if (sandboxManager != null) {
            sandboxManager.stop();
          }
        });
      Given(
          "^the following parties are allocated$",
          (DataTable dataTable) -> {
              String[] parties = dataTable.asList().toArray(new String[] {});
              sandboxManager.allocateParties(parties);
          });
    // Given - When - Then clauses :
    // ----------------------------
    When(
        "^.*\"([^\"]+)\" creates contract \"([^\"]+)\" using values(?: expecting (failure))?$",
        (String partyDisplayName,
            String moduleAndEntityName,
            String expectedFailure,
            DataTable dataTable) ->
            new LedgerExecutor(expectedFailure != null) {
              void run() throws IOException {
                PackageUtils.TemplateType idWithArgs =
                    findTemplate(sandboxManager.getClient(), moduleAndEntityName);
                DamlLf1.Package pkg =
                    findPackageObject(
                        sandboxManager.getClient(), idWithArgs.identifier.getModuleName());
                checkTableIsTwoOrManyRows(dataTable);
                for (int i = 1; i < dataTable.width(); i++) {
                  DamlRecord args =
                      fieldsToArgs(
                          dataTable.column(i), idWithArgs.createFields, pkg, sandboxManager);
                  sandboxManager
                      .getLedgerAdapter()
                      .createContract(
                          sandboxManager.getPartyId(partyDisplayName), idWithArgs.identifier, args);
                }
              }
            });
    When(
        "^.*\"([^\"]+)\" exercises choice \"([^\"]+)\" on \"([^\"]*)\" with contract id \"([^\"]+)\"(?: and (expects failure))?$",
        (String partyDisplayName,
            String choiceName,
            String moduleAndEntityName,
            String contractIdKey,
            String expectedFailure) ->
            new LedgerExecutor(expectedFailure != null) {
              void run() throws IOException {
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
                        sandboxManager.getPartyId(partyDisplayName),
                        idWithArgs.identifier,
                        contractId,
                        choiceName,
                        new DamlRecord());
              }
            });
    When(
        "^.*\"([^\"]+)\" exercises choice \"([^\"]+)\" on \"([^\"]+)\" with contract id \"([^\"]+)\" using values(?: and (expects failure))?$",
        (String partyDisplayName,
            String choiceName,
            String moduleAndEntityName,
            String contractIdKey,
            String expectedFailure,
            DataTable dataTable) ->
            new LedgerExecutor(expectedFailure != null) {
              void run() throws IOException {
                PackageUtils.TemplateType idWithArgs =
                    findTemplate(sandboxManager.getClient(), moduleAndEntityName);
                DamlLf1.Package pkg =
                    findPackageObject(
                        sandboxManager.getClient(), idWithArgs.identifier.getModuleName());
                DamlRecord args =
                    fieldsToArgs(
                        checkTableIsOneOrTwoRowsAndGet(dataTable),
                        idWithArgs.choices.get(choiceName),
                        pkg,
                        sandboxManager);
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
                        sandboxManager.getPartyId(partyDisplayName),
                        idWithArgs.identifier,
                        contractId,
                        choiceName,
                        args);
              }
            });
    Then(
        "^.*\"([^\"]+)\" should observe the creation of \"([^\"]+)\"$",
        (String partyDisplayName, String moduleAndEntityName) -> {
          PackageUtils.TemplateType idWithArgs =
              findTemplate(sandboxManager.getClient(), moduleAndEntityName);
          sandboxManager
              .getLedgerAdapter()
              .getCreatedContractId(
                  sandboxManager.getPartyId(partyDisplayName),
                  idWithArgs.identifier,
                  ContractId::new);
        });
    Then(
        "^.*\"([^\"]+)\" should observe the creation of \"([^\"]+)\" with(?: contract id \"([^\"]+)\" and)? values$",
        (String partyDisplayName,
            String moduleAndEntityName,
            String contractId,
            DataTable dataTable) -> {
          PackageUtils.TemplateType idWithArgs =
              findTemplate(sandboxManager.getClient(), moduleAndEntityName);
          DamlLf1.Package pkg =
              findPackageObject(sandboxManager.getClient(), idWithArgs.identifier.getModuleName());
          DamlRecord args =
              fieldsToArgs(
                  checkTableIsOneOrTwoRowsAndGet(dataTable),
                  idWithArgs.createFields,
                  pkg,
                  sandboxManager);
          sandboxManager
              .getLedgerAdapter()
              .observeEvent(
                  getPartyIdOnLedger(partyDisplayName),
                  ContractCreated.expectContractWithArguments(
                      idWithArgs.identifier, "{CAPTURE:" + contractId + "}", args));
        });
    Then(
        "^.*\"([^\"]+)\" should observe the archival of \"([^\"]+)\" with contract id \"([^\"]+)\".*$",
        (String partyDisplayName, String moduleAndEntityName, String contractIdKey) -> {
          PackageUtils.TemplateType idWithArgs =
              findTemplate(sandboxManager.getClient(), moduleAndEntityName);
          ContractId contractId =
              sandboxManager.getLedgerAdapter().valueStore.get(contractIdKey).asContractId().get();
          sandboxManager
              .getLedgerAdapter()
              .observeEvent(
                  getPartyIdOnLedger(partyDisplayName),
                  ContractArchived.apply(idWithArgs.identifier.toString(), contractId));
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
              Pattern.compile(errorMessageRegex, Pattern.DOTALL)
                  .matcher(lastResult.toString())
                  .matches(),
              "Expected error text [" + errorMessageRegex + "], but found [" + lastResult + "]");
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

  private String getPartyIdOnLedger(String partyDisplayName) {
    Party partyIdObj = sandboxManager.getPartyId(partyDisplayName);
    return partyIdObj.getValue();
  }

  abstract class LedgerExecutor {
    LedgerExecutor(boolean expectingError) throws IOException {
      try {
        run();
      } catch (Throwable t) {
        if (!expectingError) throw t;
        resultHolder.getAndSet(t);
      }
    }

    abstract void run() throws IOException;
  }
}
