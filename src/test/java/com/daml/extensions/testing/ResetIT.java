/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing;

import com.daml.extensions.testing.junit4.Sandbox;
import com.daml.extensions.testing.utils.ContractWithId;
import com.daml.ledger.javaapi.data.ContractId;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Int64;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hamcrest.CoreMatchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.ExternalResource;

import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static com.daml.extensions.testing.Dsl.int64;
import static com.daml.extensions.testing.Dsl.record;
import static com.daml.extensions.testing.TestCommons.*;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ResetIT {
  private static Sandbox sandbox =
      Sandbox.builder()
          .damlRoot(PINGPONG_PATH)
          .dar(DAR_PATH)
          .useReset()
          .parties(ALICE.getValue(), BOB.getValue(), CHARLIE.getValue())
          .build();

  @ClassRule public static ExternalResource sandboxClassRule = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  @Rule public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testEmptyLedger() throws InvalidProtocolBufferException {
    expectedException.expect(TimeoutException.class);
    expectedException.expectMessage(
        CoreMatchers.containsString(
            "Timed out while waiting for the correct message to be observed."));

    sandbox.getLedgerAdapter().getMatchedContract(CHARLIE, pingTemplateId(), ContractId::new);
  }

  @Test
  public void testCreate() throws InvalidProtocolBufferException {
    sandbox
        .getLedgerAdapter()
        .createContract(CHARLIE, pingTemplateId(), record(CHARLIE, BOB, int64(777)));

    ContractWithId<ContractId> pingContract =
        sandbox.getLedgerAdapter().getMatchedContract(CHARLIE, pingTemplateId(), ContractId::new);
    // Checking that the ping-pong counter is right
    Optional<DamlRecord> parameters = pingContract.record.asRecord();
    assertThat(
        parameters.flatMap(p -> p.getFieldsMap().get("count").asInt64().map(Int64::getValue)),
        is(optionalWithValue(equalTo(777L))));
  }

  private Identifier pingTemplateId() throws InvalidProtocolBufferException {
    return sandbox.templateIdentifier(PING_PONG_MODULE, "PingPong", "Ping");
  }
}
