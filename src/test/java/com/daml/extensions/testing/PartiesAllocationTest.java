/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing;

import com.daml.extensions.testing.junit4.Sandbox;
import com.daml.ledger.javaapi.data.Party;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import static com.daml.extensions.testing.TestCommons.*;

public class PartiesAllocationTest {
  private static final Sandbox sandbox =
      Sandbox.builder()
          .damlRoot(PINGPONG_PATH)
          .dar(DAR_PATH)
          .parties(ALICE.getValue(), BOB.getValue(), CHARLIE.getValue())
          .build();

  @ClassRule public static ExternalResource sandboxClassRule = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  @Test
  public void testPartiesAreAllocated() {
    sandbox.getPartyId(ALICE);
    sandbox.getPartyId(BOB);
    sandbox.getPartyId(CHARLIE);
  }

  @Test(expected = NullPointerException.class)
  public void notAllocatedPartyThrows() throws NullPointerException {
    sandbox.getPartyId(new Party("notAllocatedParty"));
  }

}


