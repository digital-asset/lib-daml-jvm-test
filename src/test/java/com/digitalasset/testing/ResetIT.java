package com.digitalasset.testing;

import com.daml.ledger.javaapi.data.*;
import com.digitalasset.daml_lf.DamlLf1;
import com.digitalasset.testing.comparator.ledger.ContractCreated;
import com.digitalasset.testing.junit4.Sandbox;
import com.digitalasset.testing.ledger.DefaultLedgerAdapter;
import com.digitalasset.testing.utils.ContractWithId;
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

import static com.digitalasset.testing.Dsl.*;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ResetIT {
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
                  .useReset()
                  .parties(ALICE.getValue(), BOB.getValue(), CHARLIE.getValue())
                  .build();

  @ClassRule
  public static ExternalResource sandboxClassRule = sandbox.getClassRule();
  @Rule
  public ExternalResource sandboxRule = sandbox.getRule();

  @Test
  public void testEmptyLedger() throws InvalidProtocolBufferException {
    ContractWithId<ContractId> pingContract =
            sandbox.getLedgerAdapter().getMatchedContract(CHARLIE, pingTemplateId(), ContractId::new);
  }

  @Test
  public void testCreate() throws InvalidProtocolBufferException {
    sandbox.getLedgerAdapter()
            .createContract(CHARLIE, pingTemplateId(), record(CHARLIE, BOB, int64(777)));

    ContractWithId<ContractId> pingContract =
            sandbox.getLedgerAdapter().getMatchedContract(CHARLIE, pingTemplateId(), ContractId::new);
    // Checking that the ping-pong counter is right
    Optional<Record> parameters = pingContract.record.asRecord();
    assertThat(
            parameters.flatMap(p -> p.getFieldsMap().get("count").asInt64().map(Int64::getValue)),
            is(optionalWithValue(equalTo(777L))));
  }

  private Identifier pingTemplateId() throws InvalidProtocolBufferException {
    return sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Ping");
  }
}
