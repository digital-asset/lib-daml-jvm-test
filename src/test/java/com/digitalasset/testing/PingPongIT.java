package com.digitalasset.testing;

import static com.digitalasset.testing.Dsl.emptyRecord;
import static com.digitalasset.testing.Dsl.field;
import static com.digitalasset.testing.Dsl.int64;
import static com.digitalasset.testing.Dsl.party;
import static com.digitalasset.testing.Dsl.record;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.daml.ledger.javaapi.data.*;
import com.digitalasset.daml_lf.DamlLf1;
import com.digitalasset.testing.comparator.ledger.ContractCreated;
import com.digitalasset.testing.junit4.Sandbox;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import com.digitalasset.testing.utils.ContractWithId;

import com.daml.ledger.javaapi.data.ContractId;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Int64;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Record;
import com.daml.ledger.javaapi.data.TreeEvent;
import com.daml.ledger.javaapi.data.Value;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.StatusRuntimeException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public class PingPongIT {
  private static final Path RESOURCE_DIR = Paths.get("src", "test", "resources").toAbsolutePath();
  private static final Path PINGPONG_PATH = RESOURCE_DIR.resolve("ping-pong").toAbsolutePath();
  private static final Path DAR_PATH = RESOURCE_DIR.resolve("ping-pong.dar").toAbsolutePath();
  private static final String PING_PONG_MODULE_NAME = "PingPong";
  private static final DamlLf1.DottedName PING_PONG_MODULE =
      DamlLf1.DottedName.newBuilder().addSegments(PING_PONG_MODULE_NAME).build();
  private static final Party ALICE = party("Alice");
  private static final Party BOB = party("Bob");
  private static final Party CHARLIE = party("Charlie");

  private static Sandbox sandbox =
      Sandbox.builder()
          .dar(DAR_PATH)
          .projectDir(PINGPONG_PATH)
          .module("Test")
          .scenario("testSetup")
          .parties(ALICE.getValue(), BOB.getValue(), CHARLIE.getValue())
          .build();

  @ClassRule
  public static ExternalResource sandboxClassRule = sandbox.getClassRule();
  @Rule
  public ExternalResource sandboxRule = sandbox.getRule();

  private DefaultLedgerAdapter ledger() {
    return sandbox.getLedgerAdapter();
  }

  @Test
  public void testCreate() throws InvalidProtocolBufferException {
    ledger().createContract(CHARLIE, pingTemplateId(), record(CHARLIE, BOB, int64(777)));

    ContractWithId<ContractId> pingContract =
        ledger().getMatchedContract(CHARLIE, pingTemplateId(), ContractId::new);
    // Checking that the ping-pong counter is right
    Optional<Record> parameters = pingContract.record.asRecord();
    assertThat(
        parameters.flatMap(p -> p.getFieldsMap().get("count").asInt64().map(Int64::getValue)),
        is(optionalWithValue(equalTo(777L))));
  }

  @Test
  public void testObservationWithMatcher() throws InvalidProtocolBufferException {
    Record recordMatcher =
        record(field("sender", ALICE), field("receiver", BOB), field("count", int64(2)));

    ledger().getCreatedContractId(BOB, pingTemplateId(), recordMatcher, ContractId::new);
  }

  @Test
  public void testObservationWithoutMatcher() throws InvalidProtocolBufferException {
    ledger().getCreatedContractId(BOB, pingTemplateId(), ContractId::new);
  }

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

  @Test
  public void testObservation() throws InvalidProtocolBufferException {
    ContractWithId<ContractId> contract =
        ledger().getMatchedContract(BOB, pingTemplateId(), ContractId::new);
    assertNotNull(contract);
  }

  @Test
  public void testExercise() throws InvalidProtocolBufferException {
    ContractWithId<ContractId> contract =
        ledger().getMatchedContract(BOB, pingTemplateId(), ContractId::new);
    ledger()
        .exerciseChoice(BOB, pingTemplateId(), contract.contractId, "RespondPong", emptyRecord());
  }

  @Test
  public void testExerciseCommand() throws InvalidProtocolBufferException {
    ContractWithId<ContractId> contract =
        ledger().getMatchedContract(BOB, pingTemplateId(), ContractId::new);
    ledger()
        .exerciseChoice(
            BOB,
            new ExerciseCommand(
                pingTemplateId(), contract.contractId.getValue(), "RespondPong", emptyRecord()));
  }

  @Test(expected = TimeoutException.class)
  public void testDoubleObservationNotPossible() throws InvalidProtocolBufferException {
    ledger().getMatchedContract(BOB, pingTemplateId(), ContractId::new);
    ledger().getMatchedContract(BOB, pingTemplateId(), ContractId::new);
  }

  @Test
  public void testPingPongFullWorkflow() throws InvalidProtocolBufferException {
    // PingPong workflow
    // Bob's turn
    Record recordMatcher =
        record(field("sender", ALICE), field("receiver", BOB), field("count", int64(2)));

    ContractId pingCid =
        ledger().getCreatedContractId(BOB, pingTemplateId(), recordMatcher, ContractId::new);
    ledger().exerciseChoice(BOB, pingTemplateId(), pingCid, "RespondPong", emptyRecord());

    // Alice's turn
    ContractId pongCid = ledger().getCreatedContractId(ALICE, pongTemplateId(), ContractId::new);
    ledger().exerciseChoice(ALICE, pongTemplateId(), pongCid, "RespondPing", emptyRecord());
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

  @Test
  public void testPingPongFullWorkflowWAlternativeApiCalls() throws InvalidProtocolBufferException {
    ContractId pingCid = ledger().getCreatedContractId(BOB, pingTemplateId(), ContractId::new);
    ledger().exerciseChoice(BOB, pingTemplateId(), pingCid, "RespondPong", emptyRecord());

    // Alice's turn sending an exercise command, directly
    ContractId pongCid = ledger().getCreatedContractId(ALICE, pongTemplateId(), ContractId::new);
    ExerciseCommand exerciseCmd =
        new ExerciseCommand(pongTemplateId(), pongCid.getValue(), "RespondPing", emptyRecord());
    sandbox.getLedgerAdapter().exerciseChoice(ALICE, exerciseCmd);
    Record recordMatcher =
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
            timedPingTid, timedPingCid.getValue(), "TimedPingRespondPong", emptyRecord());

    sandbox.getLedgerAdapter().exerciseChoice(CHARLIE, exerciseCmd);
  }

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
            timedPingTid, timedPingCid.getValue(), "TimedPingRespondPong", emptyRecord());

    sandbox.getLedgerAdapter().setCurrentTime(futureTime.plusSeconds(1000));
    sandbox.getLedgerAdapter().exerciseChoice(BOB, exerciseCmd);
  }
}
