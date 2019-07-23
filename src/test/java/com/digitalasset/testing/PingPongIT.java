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

import com.digitalasset.daml_lf.DamlLf1;
import com.digitalasset.testing.comparator.ledger.ContractCreated;
import com.digitalasset.testing.junit4.Sandbox;
import com.digitalasset.testing.utils.ContractWithId;

import com.daml.ledger.javaapi.data.ContractId;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Int64;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Record;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import java.nio.file.Path;
import java.nio.file.Paths;
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

  private static Sandbox sandboxC =
      Sandbox.builder()
          .dar(DAR_PATH)
          .projectDir(PINGPONG_PATH)
          .module("Test")
          .scenario("testSetup")
          .parties(ALICE.getValue(), BOB.getValue(), CHARLIE.getValue())
          .build();

  @ClassRule public static ExternalResource compile = sandboxC.compilation();
  @Rule public Sandbox.Process sandbox = sandboxC.process();

  @Test
  public void testCreate() throws InvalidProtocolBufferException {
    Identifier pingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Ping");

    sandbox.getLedgerAdapter().createContract(CHARLIE, pingTid, record(CHARLIE, BOB, int64(777)));

    ContractWithId<ContractId> pingContract =
        sandbox.getMatchedContract(CHARLIE, pingTid, ContractId::new);
    // Checking that the ping-pong counter is right
    Optional<Record> parameters = pingContract.record.asRecord();
    assertThat(
        parameters.flatMap(p -> p.getFieldsMap().get("count").asInt64().map(Int64::getValue)),
        is(optionalWithValue(equalTo(777L))));
  }

  @Test
  public void testObservationWithMatcher() throws InvalidProtocolBufferException {
    Identifier pingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Ping");
    Record recordMatcher =
        record(field("sender", ALICE), field("receiver", BOB), field("count", int64(2)));

    sandbox.getCreatedContractId(BOB, pingTid, recordMatcher, ContractId::new);
  }

  @Test
  public void testObservationWithoutMatcher() throws InvalidProtocolBufferException {
    Identifier pingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Ping");
    sandbox.getCreatedContractId(BOB, pingTid, ContractId::new);
  }

  @Test
  public void testSimpleObservation() throws InvalidProtocolBufferException {
    String key = "mykey";
    Identifier pingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Ping");
    sandbox
        .getLedgerAdapter()
        .observeEvent(BOB.getValue(), ContractCreated.apply(pingTid, "{CAPTURE:" + key + "}"));
    com.digitalasset.ledger.api.v1.value.Value capturedValue =
        sandbox.getLedgerAdapter().valueStore().get(key);
    assertNotNull(capturedValue);
  }

  @Test
  public void testObservation() throws InvalidProtocolBufferException {
    Identifier pingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Ping");
    ContractWithId<ContractId> contract = sandbox.getMatchedContract(BOB, pingTid, ContractId::new);
    assertNotNull(contract);
  }

  @Test
  public void testExercise() throws InvalidProtocolBufferException {
    Identifier pingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Ping");
    ContractWithId<ContractId> contract = sandbox.getMatchedContract(BOB, pingTid, ContractId::new);
    sandbox
        .getLedgerAdapter()
        .exerciseChoice(BOB, pingTid, contract.contractId, "RespondPong", emptyRecord());
  }

  @Test
  public void testExerciseCommand() throws InvalidProtocolBufferException {
    Identifier pingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Ping");
    ContractWithId<ContractId> contract = sandbox.getMatchedContract(BOB, pingTid, ContractId::new);
    ExerciseCommand exerciseCmd =
        new ExerciseCommand(pingTid, contract.contractId.getValue(), "RespondPong", emptyRecord());
    sandbox.getLedgerAdapter().exerciseChoice(BOB, exerciseCmd);
  }

  @Test(expected = TimeoutException.class)
  public void testDoubleObservationNotPossible() throws InvalidProtocolBufferException {
    Identifier pingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Ping");
    sandbox.getMatchedContract(BOB, pingTid, ContractId::new);
    sandbox.getMatchedContract(BOB, pingTid, ContractId::new);
  }

  @Test
  public void testPingPongFullWorkflow() throws InvalidProtocolBufferException {
    // PingPong workflow
    // Bob's turn
    Identifier pingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Ping");
    Record recordMatcher =
        record(field("sender", ALICE), field("receiver", BOB), field("count", int64(2)));

    ContractId pingCid = sandbox.getCreatedContractId(BOB, pingTid, recordMatcher, ContractId::new);
    sandbox.getLedgerAdapter().exerciseChoice(BOB, pingTid, pingCid, "RespondPong", emptyRecord());

    // Alice's turn
    Identifier pongTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Pong");
    ContractId pongCid = sandbox.getCreatedContractId(ALICE, pongTid, ContractId::new);
    sandbox
        .getLedgerAdapter()
        .exerciseChoice(ALICE, pongTid, pongCid, "RespondPing", emptyRecord());
    ContractWithId<ContractId> pingContract =
        sandbox.getMatchedContract(BOB, pingTid, ContractId::new);

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
        sandbox.getMatchedContract(ALICE, pingTid, ContractId::new);

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
    Identifier pingTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Ping");
    ContractId pingCid = sandbox.getCreatedContractId(BOB, pingTid, ContractId::new);
    sandbox.getLedgerAdapter().exerciseChoice(BOB, pingTid, pingCid, "RespondPong", emptyRecord());

    // Alice's turn sending an exercise command, directly
    Identifier pongTid = sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Pong");
    ContractId pongCid = sandbox.getCreatedContractId(ALICE, pongTid, ContractId::new);
    ExerciseCommand exerciseCmd =
        new ExerciseCommand(pongTid, pongCid.getValue(), "RespondPing", emptyRecord());
    sandbox.getLedgerAdapter().exerciseChoice(ALICE, exerciseCmd);
    Record recordMatcher =
        record(field("sender", ALICE), field("receiver", BOB), field("count", int64(4)));

    ContractId pingCid2 =
        sandbox.getCreatedContractId(BOB, pingTid, recordMatcher, ContractId::new);
  }
}
