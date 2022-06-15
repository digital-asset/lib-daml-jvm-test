/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing;

import com.daml.extensions.testing.comparator.ledger.ContractCreated;
import com.daml.extensions.testing.junit4.LogLevel;
import com.daml.extensions.testing.junit4.Sandbox;
import com.daml.extensions.testing.ledger.DefaultLedgerAdapter;
import com.daml.extensions.testing.utils.ContractWithId;
import com.daml.ledger.javaapi.data.*;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.StatusRuntimeException;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static com.daml.extensions.testing.Dsl.*;
import static com.daml.extensions.testing.TestCommons.*;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class PingPongIT {
  private static final Sandbox sandbox =
      Sandbox.builder()
          .damlRoot(PINGPONG_PATH)
          .dar(DAR_PATH)
          .logLevel(LogLevel.DEBUG)
          .moduleAndScript("Test", "testSetup")
          .build();

  @ClassRule public static ExternalResource sandboxClassRule = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  private DefaultLedgerAdapter ledger() {
    return sandbox.getLedgerAdapter();
  }

  @Ignore
  @Test
  public void testCreate() throws InvalidProtocolBufferException {
    ledger().createContract(sandbox.getPartyId(CHARLIE), pingTemplateId(), record(sandbox.getPartyId(CHARLIE), sandbox.getPartyId(BOB), int64(777)));

    ContractWithId<ContractId> pingContract =
        ledger().getMatchedContract(sandbox.getPartyId(CHARLIE), pingTemplateId(), ContractId::new);
    // Checking that the ping-pong counter is right
    Optional<DamlRecord> parameters = pingContract.record.asRecord();
    assertThat(
        parameters.flatMap(p -> p.getFieldsMap().get("count").asInt64().map(Int64::getValue)),
        is(optionalWithValue(equalTo(777L))));
  }

  @Ignore
  @Test
  public void testObservationWithMatcher() throws InvalidProtocolBufferException {
    Party aliceId = sandbox.getPartyId(ALICE);
    Party bobId = sandbox.getPartyId(BOB);
    DamlRecord recordMatcher =
        record(field("sender", aliceId), field("receiver", bobId), field("count", int64(2)));

    ledger().getCreatedContractId(bobId, pingTemplateId(), recordMatcher, ContractId::new);
  }

  @Ignore
  @Test
  public void testObservationWithoutMatcher() throws InvalidProtocolBufferException {
    Party bobId = sandbox.getPartyId(BOB);
    ledger().getCreatedContractId(bobId, pingTemplateId(), ContractId::new);
  }

  @Ignore
  @Test
  public void testSimpleObservation() throws InvalidProtocolBufferException {
    String key = "mykey";
    TreeEvent evt =
        ledger()
            .observeEvent(
                BOB.getValue(),
                ContractCreated.expectContract(pingTemplateId(), "{CAPTURE:" + key + "}"));
    Value capturedValue = ledger().valueStore.get(key);
    assertNotNull(capturedValue);
  }

  @Ignore
  @Test
  public void testObservation() throws InvalidProtocolBufferException {
    ContractWithId<ContractId> contract =
        ledger().getMatchedContract(sandbox.getPartyId(BOB), pingTemplateId(), ContractId::new);
    assertNotNull(contract);
  }

  @Ignore
  @Test
  public void testExercise() throws InvalidProtocolBufferException {
    ContractWithId<ContractId> contract =
        ledger().getMatchedContract(BOB, pingTemplateId(), ContractId::new);
    ledger()
        .exerciseChoice(
            BOB, pingTemplateId(), contract.contractId, "RespondPong", emptyDamlRecord());
  }

  @Ignore
  @Test
  public void testExerciseCommand() throws InvalidProtocolBufferException {
    ContractWithId<ContractId> contract =
        ledger().getMatchedContract(BOB, pingTemplateId(), ContractId::new);
    ledger()
        .exerciseChoice(
            BOB,
            new ExerciseCommand(
                pingTemplateId(),
                contract.contractId.getValue(),
                "RespondPong",
                emptyDamlRecord()));
  }

  @Ignore
  @Test(expected = TimeoutException.class)
  public void testDoubleObservationNotPossible() throws InvalidProtocolBufferException {
    ledger().getMatchedContract(BOB, pingTemplateId(), ContractId::new);
    ledger().getMatchedContract(BOB, pingTemplateId(), ContractId::new);
  }

  @Ignore
  @Test
  public void testPingPongFullWorkflow() throws InvalidProtocolBufferException {
    // PingPong workflow
    // Bob's turn
    DamlRecord recordMatcher =
        record(field("sender", ALICE), field("receiver", BOB), field("count", int64(2)));

    ContractId pingCid =
        ledger().getCreatedContractId(BOB, pingTemplateId(), recordMatcher, ContractId::new);
    ledger().exerciseChoice(BOB, pingTemplateId(), pingCid, "RespondPong", emptyDamlRecord());

    // Alice's turn
    ContractId pongCid = ledger().getCreatedContractId(ALICE, pongTemplateId(), ContractId::new);
    ledger().exerciseChoice(ALICE, pongTemplateId(), pongCid, "RespondPing", emptyDamlRecord());
    ContractWithId<ContractId> pingContract =
        ledger().getMatchedContract(BOB, pingTemplateId(), ContractId::new);

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
        ledger().getMatchedContract(ALICE, pingTemplateId(), ContractId::new);

    assertThat(
        pingContract2
            .record
            .asRecord()
            .flatMap(r -> r.getFieldsMap().get("count").asInt64())
            .map(Int64::getValue),
        is(optionalWithValue(equalTo(2L))));
  }

  @Ignore
  @Test
  public void testPingPongFullWorkflowWAlternativeApiCalls() throws InvalidProtocolBufferException {
    ContractId pingCid = ledger().getCreatedContractId(BOB, pingTemplateId(), ContractId::new);
    ledger().exerciseChoice(BOB, pingTemplateId(), pingCid, "RespondPong", emptyDamlRecord());

    // Alice's turn sending an exercise command, directly
    ContractId pongCid = ledger().getCreatedContractId(ALICE, pongTemplateId(), ContractId::new);
    ExerciseCommand exerciseCmd =
        new ExerciseCommand(pongTemplateId(), pongCid.getValue(), "RespondPing", emptyDamlRecord());
    sandbox.getLedgerAdapter().exerciseChoice(ALICE, exerciseCmd);
    DamlRecord recordMatcher =
        record(field("sender", ALICE), field("receiver", BOB), field("count", int64(4)));

    ContractId pingCid2 =
        ledger().getCreatedContractId(BOB, pingTemplateId(), recordMatcher, ContractId::new);
  }

  private Identifier pingTemplateId() throws InvalidProtocolBufferException {
    return sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Ping");
  }

  private Identifier pongTemplateId() throws InvalidProtocolBufferException {
    return sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Pong");
  }

  @Ignore
  @Test(expected = StatusRuntimeException.class)
  public void testTimedOperationFailsIfTimeIsWrong() throws InvalidProtocolBufferException {
    Instant futureTime = Instant.ofEpochSecond(5000);
    Identifier timedPingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "TimedPing");

    Timestamp timestamp = Timestamp.fromInstant(futureTime);
    sandbox
        .getLedgerAdapter()
        .createContract(CHARLIE, timedPingTid, record(timestamp, CHARLIE, BOB, int64(777)));

    ContractId timedPingCid =
        sandbox.getLedgerAdapter().getCreatedContractId(CHARLIE, timedPingTid, ContractId::new);

    ExerciseCommand exerciseCmd =
        new ExerciseCommand(
            timedPingTid, timedPingCid.getValue(), "TimedPingRespondPong", emptyDamlRecord());

    sandbox.getLedgerAdapter().exerciseChoice(CHARLIE, exerciseCmd);
  }

  @Ignore
  @Test
  public void testTimedOperationIfTimeIsOk()
      throws InvalidProtocolBufferException, InterruptedException {
    Instant futureTime = Instant.ofEpochSecond(5000);
    Identifier timedPingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "TimedPing");

    Timestamp timestamp = Timestamp.fromInstant(futureTime);
    sandbox
        .getLedgerAdapter()
        .createContract(CHARLIE, timedPingTid, record(timestamp, CHARLIE, BOB, int64(777)));

    ContractId timedPingCid =
        sandbox.getLedgerAdapter().getCreatedContractId(CHARLIE, timedPingTid, ContractId::new);

    ExerciseCommand exerciseCmd =
        new ExerciseCommand(
            timedPingTid, timedPingCid.getValue(), "TimedPingRespondPong", emptyDamlRecord());

    sandbox.getLedgerAdapter().setCurrentTime(futureTime.plusSeconds(1000));
    sandbox.getLedgerAdapter().exerciseChoice(BOB, exerciseCmd);
  }

  private Identifier numericTemplateId() throws InvalidProtocolBufferException {
    return sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "NumericTester");
  }

  @Ignore
  @Test
  public void testNumeric() throws InvalidProtocolBufferException {
    ledger()
        .createContract(
            CHARLIE, numericTemplateId(), record(CHARLIE, numeric("3.14"), numeric("1.234")));
    ContractWithId<ContractId> numericTesterContract =
        ledger().getMatchedContract(CHARLIE, numericTemplateId(), ContractId::new);
    // Checking that the ping-pong counter is right
    Optional<DamlRecord> parameters = numericTesterContract.record.asRecord();
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
}
