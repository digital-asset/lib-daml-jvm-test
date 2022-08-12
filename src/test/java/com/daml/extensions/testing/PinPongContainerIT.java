package com.daml.extensions.testing;

import com.daml.extensions.testing.junit5.Sandbox;
import com.daml.extensions.testing.junit5.SandboxTestExtension;
import com.daml.extensions.testing.junit5.TestSandbox;
import com.daml.extensions.testing.utils.ContractWithId;
import com.daml.ledger.javaapi.data.ContractId;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.Int64;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

import static com.daml.extensions.testing.Dsl.int64;
import static com.daml.extensions.testing.Dsl.record;
import static com.daml.extensions.testing.TestCommons.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SandboxTestExtension.class)
public class PinPongContainerIT {
  @TestSandbox
  public static final Sandbox sandbox =
      Sandbox.builder().dar(DAR_PATH).useContainers().parties(EXAMPLE_PARTIES).build();

  @Test
  public void testCreate() throws InvalidProtocolBufferException {
    sandbox
        .getLedgerAdapter()
        .createContract(
            sandbox.getPartyId(CHARLIE),
            sandbox.templateIdentifier(PING_PONG_MODULE, "MyPingPong", "MyPing"),
            record(sandbox.getPartyId(CHARLIE), sandbox.getPartyId(BOB), int64(777)));
    ContractWithId<ContractId> pingContract =
        sandbox
            .getLedgerAdapter()
            .getMatchedContract(
                sandbox.getPartyId(CHARLIE),
                sandbox.templateIdentifier(PING_PONG_MODULE, "MyPingPong", "MyPing"),
                ContractId::new);
    // Checking that the ping-pong counter is right
    Optional<DamlRecord> parameters = pingContract.record.asRecord();
    assertEquals(
        parameters.flatMap(p -> p.getFieldsMap().get("count").asInt64().map(Int64::getValue)),
        Optional.of(777L));
  }
}
