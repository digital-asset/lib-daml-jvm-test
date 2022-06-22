/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing;

import com.daml.extensions.testing.comparator.ledger.ContractCreated;
import com.daml.extensions.testing.junit4.Sandbox;
import com.daml.extensions.testing.ledger.DefaultLedgerAdapter;
import com.daml.extensions.testing.utils.ContractWithId;
import com.daml.ledger.javaapi.data.*;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.StatusRuntimeException;
import org.junit.ClassRule;
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
          .moduleAndScript("Test", "testSetup")
          .build();

  @ClassRule public static ExternalResource sandboxClassRule = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  private DefaultLedgerAdapter ledger() {
    return sandbox.getLedgerAdapter();
  }

  private Party charliePartyId() {
    return sandbox.getPartyId(CHARLIE);
  }

  private Party bobPartyId() {
    return sandbox.getPartyId(BOB);
  }

  private Party alicePartyId() {
    return sandbox.getPartyId(ALICE);
  }

  @Test
  public void testCreate() throws InvalidProtocolBufferException {
    ledger()
        .createContract(
            charliePartyId(), pingTemplateId(), record(charliePartyId(), bobPartyId(), int64(777)));

    ContractWithId<ContractId> pingContract =
        ledger().getMatchedContract(charliePartyId(), pingTemplateId(), ContractId::new);
    // Checking that the ping-pong counter is right
    Optional<DamlRecord> parameters = pingContract.record.asRecord();
    assertThat(
        parameters.flatMap(p -> p.getFieldsMap().get("count").asInt64().map(Int64::getValue)),
        is(optionalWithValue(equalTo(777L))));
  }

  @Test
  public void testObservationWithMatcher() throws InvalidProtocolBufferException {
    DamlRecord recordMatcher =
        record(
            field("sender", alicePartyId()),
            field("receiver", bobPartyId()),
            field("count", int64(2)));

    ledger().getCreatedContractId(bobPartyId(), pingTemplateId(), recordMatcher, ContractId::new);
  }

  @Test
  public void testObservationWithoutMatcher() throws InvalidProtocolBufferException {
    ledger().getCreatedContractId(bobPartyId(), pingTemplateId(), ContractId::new);
  }

  @Test
  public void testSimpleObservation() throws InvalidProtocolBufferException {
    String key = "mykey";
    TreeEvent evt =
        ledger()
            .observeEvent(
                bobPartyId().getValue(),
                ContractCreated.expectContract(pingTemplateId(), "{CAPTURE:" + key + "}"));
    Value capturedValue = ledger().valueStore.get(key);
    assertNotNull(capturedValue);
  }

  @Test
  public void testObservation() throws InvalidProtocolBufferException {
    ContractWithId<ContractId> contract =
        ledger().getMatchedContract(bobPartyId(), pingTemplateId(), ContractId::new);
    assertNotNull(contract);
  }

  @Test
  public void testExercise() throws InvalidProtocolBufferException {
    ContractWithId<ContractId> contract =
        ledger().getMatchedContract(bobPartyId(), pingTemplateId(), ContractId::new);
    ledger()
        .exerciseChoice(
            bobPartyId(), pingTemplateId(), contract.contractId, "RespondPong", emptyDamlRecord());
  }

  @Test
  public void testExerciseCommand() throws InvalidProtocolBufferException {
    ContractWithId<ContractId> contract =
        ledger().getMatchedContract(bobPartyId(), pingTemplateId(), ContractId::new);
    ledger()
        .exerciseChoice(
            bobPartyId(),
            new ExerciseCommand(
                pingTemplateId(),
                contract.contractId.getValue(),
                "RespondPong",
                emptyDamlRecord()));
  }

  @Test(expected = TimeoutException.class)
  public void testDoubleObservationNotPossible() throws InvalidProtocolBufferException {
    ledger().getMatchedContract(bobPartyId(), pingTemplateId(), ContractId::new);
    ledger().getMatchedContract(bobPartyId(), pingTemplateId(), ContractId::new);
  }

  @Test(expected = NullPointerException.class)
  public void testNotAllocatedPartyHasNoId()
      throws NullPointerException, InvalidProtocolBufferException {
    ledger()
        .getMatchedContract(
            sandbox.getPartyId("NonAllocatedPartyName"), pingTemplateId(), ContractId::new);
  }

  @Test
  public void testPingPongFullWorkflow() throws InvalidProtocolBufferException {
    // PingPong workflow
    // Bob's turn
    DamlRecord recordMatcher =
        record(
            field("sender", alicePartyId()),
            field("receiver", bobPartyId()),
            field("count", int64(2)));

    ContractId pingCid =
        ledger()
            .getCreatedContractId(bobPartyId(), pingTemplateId(), recordMatcher, ContractId::new);
    ledger()
        .exerciseChoice(bobPartyId(), pingTemplateId(), pingCid, "RespondPong", emptyDamlRecord());

    // Alice's turn
    ContractId pongCid =
        ledger().getCreatedContractId(alicePartyId(), pongTemplateId(), ContractId::new);
    ledger()
        .exerciseChoice(
            alicePartyId(), pongTemplateId(), pongCid, "RespondPing", emptyDamlRecord());
    ContractWithId<ContractId> pingContract =
        ledger().getMatchedContract(bobPartyId(), pingTemplateId(), ContractId::new);

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
        ledger().getMatchedContract(alicePartyId(), pingTemplateId(), ContractId::new);

    assertThat(
        pingContract2
            .record
            .asRecord()
            .flatMap(r -> r.getFieldsMap().get("count").asInt64())
            .map(Int64::getValue),
        is(optionalWithValue(equalTo(2L))));
  }

  @Test
  public void testPingPongFullWorkflowWAlternativeApiCalls() throws InvalidProtocolBufferException {
    ContractId pingCid =
        ledger().getCreatedContractId(bobPartyId(), pingTemplateId(), ContractId::new);
    ledger()
        .exerciseChoice(bobPartyId(), pingTemplateId(), pingCid, "RespondPong", emptyDamlRecord());

    // Alice's turn sending an exercise command, directly
    ContractId pongCid =
        ledger().getCreatedContractId(alicePartyId(), pongTemplateId(), ContractId::new);
    ExerciseCommand exerciseCmd =
        new ExerciseCommand(pongTemplateId(), pongCid.getValue(), "RespondPing", emptyDamlRecord());
    sandbox.getLedgerAdapter().exerciseChoice(alicePartyId(), exerciseCmd);
    DamlRecord recordMatcher =
        record(
            field("sender", alicePartyId()),
            field("receiver", bobPartyId()),
            field("count", int64(4)));

    ContractId pingCid2 =
        ledger()
            .getCreatedContractId(bobPartyId(), pingTemplateId(), recordMatcher, ContractId::new);
  }

  private Identifier pingTemplateId() throws InvalidProtocolBufferException {
    return sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "MyPing");
  }

  private Identifier pongTemplateId() throws InvalidProtocolBufferException {
    return sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "MyPong");
  }

  @Test(expected = StatusRuntimeException.class)
  public void testTimedOperationFailsIfTimeIsWrong() throws InvalidProtocolBufferException {
    Instant futureTime = Instant.ofEpochSecond(5000);
    Identifier timedPingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "TimedPing");

    Timestamp timestamp = Timestamp.fromInstant(futureTime);
    sandbox
        .getLedgerAdapter()
        .createContract(
            charliePartyId(),
            timedPingTid,
            record(timestamp, charliePartyId(), bobPartyId(), int64(777)));

    ContractId timedPingCid =
        sandbox
            .getLedgerAdapter()
            .getCreatedContractId(charliePartyId(), timedPingTid, ContractId::new);

    ExerciseCommand exerciseCmd =
        new ExerciseCommand(
            timedPingTid, timedPingCid.getValue(), "TimedPingRespondPong", emptyDamlRecord());

    sandbox.getLedgerAdapter().exerciseChoice(charliePartyId(), exerciseCmd);
  }

  @Test
  public void testTimedOperationIfTimeIsOk()
      throws InvalidProtocolBufferException, InterruptedException {
    Instant futureTime = Instant.ofEpochSecond(5000);
    Identifier timedPingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "TimedPing");

    Timestamp timestamp = Timestamp.fromInstant(futureTime);
    sandbox
        .getLedgerAdapter()
        .createContract(
            charliePartyId(),
            timedPingTid,
            record(timestamp, charliePartyId(), bobPartyId(), int64(777)));

    ContractId timedPingCid =
        sandbox
            .getLedgerAdapter()
            .getCreatedContractId(charliePartyId(), timedPingTid, ContractId::new);

    ExerciseCommand exerciseCmd =
        new ExerciseCommand(
            timedPingTid, timedPingCid.getValue(), "TimedPingRespondPong", emptyDamlRecord());

    sandbox.getLedgerAdapter().setCurrentTime(futureTime.plusSeconds(1000));
    sandbox.getLedgerAdapter().exerciseChoice(bobPartyId(), exerciseCmd);
  }

  private Identifier numericTemplateId() throws InvalidProtocolBufferException {
    return sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "NumericTester");
  }

  @Test
  public void testNumeric() throws InvalidProtocolBufferException {
    ledger()
        .createContract(
            charliePartyId(),
            numericTemplateId(),
            record(charliePartyId(), numeric("3.14"), numeric("1.234")));
    ContractWithId<ContractId> numericTesterContract =
        ledger().getMatchedContract(charliePartyId(), numericTemplateId(), ContractId::new);
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
