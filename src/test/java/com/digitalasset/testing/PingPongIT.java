/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing;

import static com.digitalasset.testing.Dsl.*;
import static com.digitalasset.testing.junit4.Sandbox.getUniqueParty;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.daml.ledger.javaapi.data.*;
import com.digitalasset.testing.comparator.ledger.ContractCreated;
import com.digitalasset.testing.junit4.Sandbox;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import com.digitalasset.testing.utils.ContractWithId;

import com.daml.ledger.javaapi.data.ContractId;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Int64;
import com.daml.ledger.javaapi.data.Record;
import com.daml.ledger.javaapi.data.Value;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.StatusRuntimeException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import static com.digitalasset.testing.TestCommons.*;

public class PingPongIT {
  private static final Sandbox sandbox =
      Sandbox.builder()
          .damlRoot(PINGPONG_PATH)
          .dar(DAR_PATH)
          // .parties(alice, bob, charlie)
          // .moduleAndScript("Test", "testSetup")
          .build();

  @ClassRule public static ExternalResource sandboxRule = sandbox.getRule();

  private DefaultLedgerAdapter ledger() {
    return sandbox.getLedgerAdapter();
  }

  @Test
  public void testCreate() throws IOException, InterruptedException {
    Party alice = getUniqueParty("Alice");
    Party bob = getUniqueParty("Bob");
    Party charlie = getUniqueParty("Charlie");

    sandbox.runScript("Test", "testSetup", alice, bob, charlie);

    ledger().createContract(charlie, pingTemplateId(), record(charlie, bob, int64(777)));

    ContractWithId<ContractId> pingContract =
        ledger().getMatchedContract(charlie, pingTemplateId(), ContractId::new);
    // Checking that the ping-pong counter is right
    Optional<Record> parameters = pingContract.record.asRecord();
    assertThat(
        parameters.flatMap(p -> p.getFieldsMap().get("count").asInt64().map(Int64::getValue)),
        is(optionalWithValue(equalTo(777L))));
  }

  @Test
  public void testObservationWithMatcher() throws IOException, InterruptedException {
    Party alice = getUniqueParty("Alice");
    Party bob = getUniqueParty("Bob");
    Party charlie = getUniqueParty("Charlie");

    sandbox.runScript("Test", "testSetup", alice, bob, charlie);

    Record recordMatcher =
        record(field("sender", alice), field("receiver", bob), field("count", int64(2)));

    ledger().getCreatedContractId(bob, pingTemplateId(), recordMatcher, ContractId::new);
  }

  @Test
  public void testObservationWithoutMatcher() throws IOException, InterruptedException {
    Party alice = getUniqueParty("Alice");
    Party bob = getUniqueParty("Bob");
    Party charlie = getUniqueParty("Charlie");

    sandbox.runScript("Test", "testSetup", alice, bob, charlie);

    ledger().getCreatedContractId(bob, pingTemplateId(), ContractId::new);
  }

  @Test
  public void testSimpleObservation() throws IOException, InterruptedException {
    Party alice = getUniqueParty("Alice");
    Party bob = getUniqueParty("Bob");
    Party charlie = getUniqueParty("Charlie");

    sandbox.runScript("Test", "testSetup", alice, bob, charlie);

    String key = "mykey";
    ledger()
        .observeEvent(
            bob.getValue(),
            ContractCreated.expectContract(pingTemplateId(), "{CAPTURE:" + key + "}"));
    Value capturedValue = ledger().valueStore.get(key);
    assertNotNull(capturedValue);
  }

  @Test
  public void testObservation() throws IOException, InterruptedException {
    Party alice = getUniqueParty("Alice");
    Party bob = getUniqueParty("Bob");
    Party charlie = getUniqueParty("Charlie");

    sandbox.runScript("Test", "testSetup", alice, bob, charlie);

    ContractWithId<ContractId> contract =
        ledger().getMatchedContract(bob, pingTemplateId(), ContractId::new);
    assertNotNull(contract);
  }

  @Test
  public void testExercise() throws IOException, InterruptedException {
    Party alice = getUniqueParty("Alice");
    Party bob = getUniqueParty("Bob");
    Party charlie = getUniqueParty("Charlie");

    sandbox.runScript("Test", "testSetup", alice, bob, charlie);

    ContractWithId<ContractId> contract =
        ledger().getMatchedContract(bob, pingTemplateId(), ContractId::new);
    ledger()
        .exerciseChoice(bob, pingTemplateId(), contract.contractId, "RespondPong", emptyRecord());
  }

  @Test
  public void testExerciseCommand() throws IOException, InterruptedException {
    Party alice = getUniqueParty("Alice");
    Party bob = getUniqueParty("Bob");
    Party charlie = getUniqueParty("Charlie");

    sandbox.runScript("Test", "testSetup", alice, bob, charlie);

    ContractWithId<ContractId> contract =
        ledger().getMatchedContract(bob, pingTemplateId(), ContractId::new);
    ledger()
        .exerciseChoice(
            bob,
            new ExerciseCommand(
                pingTemplateId(), contract.contractId.getValue(), "RespondPong", emptyRecord()));
  }

  @Test(expected = TimeoutException.class)
  public void testDoubleObservationNotPossible() throws IOException, InterruptedException {
    Party alice = getUniqueParty("Alice");
    Party bob = getUniqueParty("Bob");
    Party charlie = getUniqueParty("Charlie");

    sandbox.runScript("Test", "testSetup", alice, bob, charlie);

    ledger().getMatchedContract(bob, pingTemplateId(), ContractId::new);
    ledger().getMatchedContract(bob, pingTemplateId(), ContractId::new);
  }

  @Test
  public void testPingPongFullWorkflow() throws IOException, InterruptedException {
    Party alice = getUniqueParty("Alice");
    Party bob = getUniqueParty("Bob");
    Party charlie = getUniqueParty("Charlie");

    sandbox.runScript("Test", "testSetup", alice, bob, charlie);

    // PingPong workflow
    // Bob's turn
    Record recordMatcher =
        record(field("sender", alice), field("receiver", bob), field("count", int64(2)));

    ContractId pingCid =
        ledger().getCreatedContractId(bob, pingTemplateId(), recordMatcher, ContractId::new);
    ledger().exerciseChoice(bob, pingTemplateId(), pingCid, "RespondPong", emptyRecord());

    // Alice's turn
    ContractId pongCid = ledger().getCreatedContractId(alice, pongTemplateId(), ContractId::new);
    ledger().exerciseChoice(alice, pongTemplateId(), pongCid, "RespondPing", emptyRecord());
    ContractWithId<ContractId> pingContract =
        ledger().getMatchedContract(bob, pingTemplateId(), ContractId::new);

    assertThat(
        pingContract
            .record
            .asRecord()
            .flatMap(ps -> ps.getFieldsMap().get("count").asInt64())
            .map(Int64::getValue),
        is(optionalWithValue(equalTo(4L))));

    // Alice tries to observe Ping.
    // Note that Alice hasn't observer any Ping before
    // So it will start with the first.
    ContractWithId<ContractId> pingContract2 =
        ledger().getMatchedContract(alice, pingTemplateId(), ContractId::new);

    assertThat(
        pingContract2
            .record
            .asRecord()
            .flatMap(r -> r.getFieldsMap().get("count").asInt64())
            .map(Int64::getValue),
        is(optionalWithValue(equalTo(2L))));
  }

  @Test
  public void testPingPongFullWorkflowWAlternativeApiCalls()
      throws IOException, InterruptedException {
    Party alice = getUniqueParty("Alice");
    Party bob = getUniqueParty("Bob");
    Party charlie = getUniqueParty("Charlie");

    sandbox.runScript("Test", "testSetup", alice, bob, charlie);

    ContractId pingCid = ledger().getCreatedContractId(bob, pingTemplateId(), ContractId::new);
    ledger().exerciseChoice(bob, pingTemplateId(), pingCid, "RespondPong", emptyRecord());

    // Alice's turn sending an exercise command, directly
    ContractId pongCid = ledger().getCreatedContractId(alice, pongTemplateId(), ContractId::new);
    ExerciseCommand exerciseCmd =
        new ExerciseCommand(pongTemplateId(), pongCid.getValue(), "RespondPing", emptyRecord());
    sandbox.getLedgerAdapter().exerciseChoice(alice, exerciseCmd);
    Record recordMatcher =
        record(field("sender", alice), field("receiver", bob), field("count", int64(4)));

    ledger().getCreatedContractId(bob, pingTemplateId(), recordMatcher, ContractId::new);
  }

  @Test(expected = StatusRuntimeException.class)
  public void testTimedOperationFailsIfTimeIsWrong() throws IOException, InterruptedException {
    Party alice = getUniqueParty("Alice");
    Party bob = getUniqueParty("Bob");
    Party charlie = getUniqueParty("Charlie");

    sandbox.runScript("Test", "testSetup", alice, bob, charlie);

    Instant futureTime = Instant.ofEpochSecond(5000);
    Identifier timedPingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "TimedPing");

    Timestamp timestamp = Timestamp.fromInstant(futureTime);
    sandbox
        .getLedgerAdapter()
        .createContract(charlie, timedPingTid, record(timestamp, charlie, bob, int64(777)));

    ContractId timedPingCid =
        sandbox.getLedgerAdapter().getCreatedContractId(charlie, timedPingTid, ContractId::new);

    ExerciseCommand exerciseCmd =
        new ExerciseCommand(
            timedPingTid, timedPingCid.getValue(), "TimedPingRespondPong", emptyRecord());

    sandbox.getLedgerAdapter().exerciseChoice(charlie, exerciseCmd);
  }

  @Test
  public void testTimedOperationIfTimeIsOk() throws IOException, InterruptedException {
    Party alice = getUniqueParty("Alice");
    Party bob = getUniqueParty("Bob");
    Party charlie = getUniqueParty("Charlie");

    sandbox.runScript("Test", "testSetup", alice, bob, charlie);

    Instant futureTime = Instant.ofEpochSecond(5000);
    Identifier timedPingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "TimedPing");

    Timestamp timestamp = Timestamp.fromInstant(futureTime);
    sandbox
        .getLedgerAdapter()
        .createContract(charlie, timedPingTid, record(timestamp, charlie, bob, int64(777)));

    ContractId timedPingCid =
        sandbox.getLedgerAdapter().getCreatedContractId(charlie, timedPingTid, ContractId::new);

    ExerciseCommand exerciseCmd =
        new ExerciseCommand(
            timedPingTid, timedPingCid.getValue(), "TimedPingRespondPong", emptyRecord());

    sandbox.getLedgerAdapter().setCurrentTime(futureTime.plusSeconds(1000));
    sandbox.getLedgerAdapter().exerciseChoice(bob, exerciseCmd);
  }

  @Test
  public void testNumeric() throws IOException, InterruptedException {
    Party alice = getUniqueParty("Alice");
    Party bob = getUniqueParty("Bob");
    Party charlie = getUniqueParty("Charlie");

    sandbox.runScript("Test", "testSetup", alice, bob, charlie);

    ledger()
        .createContract(
            charlie, numericTemplateId(), record(charlie, numeric("3.14"), numeric("1.234")));
    ContractWithId<ContractId> numericTesterContract =
        ledger().getMatchedContract(charlie, numericTemplateId(), ContractId::new);
    // Checking that the ping-pong counter is right
    Optional<Record> parameters = numericTesterContract.record.asRecord();
    assertThat(
        parameters
            .flatMap(p -> p.getFieldsMap().get("x").asNumeric().map(Numeric::getValue))
            .map(BigDecimal::toPlainString),
        is(optionalWithValue(equalTo("3.1400000000")))); // DAML / Decimal := Numeric 10
    assertThat(
        parameters
            .flatMap(p -> p.getFieldsMap().get("y").asNumeric().map(Numeric::getValue))
            .map(BigDecimal::toPlainString),
        is(optionalWithValue(equalTo("1.2340"))));
  }

  private Identifier numericTemplateId() throws InvalidProtocolBufferException {
    return sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "NumericTester");
  }

  private Identifier pingTemplateId() throws InvalidProtocolBufferException {
    return sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Ping");
  }

  private Identifier pongTemplateId() throws InvalidProtocolBufferException {
    return sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Pong");
  }
}
