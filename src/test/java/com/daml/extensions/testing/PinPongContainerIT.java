package com.daml.extensions.testing;

import com.daml.extensions.testing.junit4.Sandbox;
import com.daml.extensions.testing.utils.ContractWithId;
import com.daml.ledger.javaapi.data.ContractId;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.Int64;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import java.util.Optional;

import static com.daml.extensions.testing.Dsl.int64;
import static com.daml.extensions.testing.Dsl.record;
import static com.daml.extensions.testing.TestCommons.*;

public class PinPongContainerIT {
    private static final Sandbox sandbox =
            Sandbox.builder()
                    .damlRoot(PINGPONG_PATH)
                    .dar(DAR_PATH)
                    .useContainers()
                    .parties(CHARLIE, BOB, ALICE)
                    .moduleAndScript("Test", "testSetup")
                    .build();

    @ClassRule
    public static ExternalResource sandboxClassRule = sandbox.getClassRule();
    @Rule
    public ExternalResource sandboxRule = sandbox.getRule();

    @Test
    public void testCreate() throws InvalidProtocolBufferException {
        sandbox.getLedgerAdapter()
                .createContract(
                        sandbox.getPartyId(CHARLIE),
                        sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "MyPing"),
                        record(
                                sandbox.getPartyId(CHARLIE),
                                sandbox.getPartyId(BOB),
                                int64(777))
                );
        ContractWithId<ContractId> pingContract =
                sandbox.getLedgerAdapter().getMatchedContract(
                        sandbox.getPartyId(CHARLIE),
                        sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "MyPing"),
                        ContractId::new);
        // Checking that the ping-pong counter is right
        Optional<DamlRecord> parameters = pingContract.record.asRecord();
        Assert.assertEquals(
                parameters.flatMap(p -> p.getFieldsMap().get("count").asInt64().map(Int64::getValue)),
                Optional.of(777L));
    }
}
