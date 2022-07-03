/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing;

import com.daml.extensions.testing.junit4.Sandbox;
import com.daml.extensions.testing.junit4.SandboxTestExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.daml.extensions.testing.TestCommons.*;

@ExtendWith(SandboxTestExtension.class)
public class PartiesAllocationTest {
  private static final Sandbox sandbox =
      Sandbox.builder().damlRoot(PINGPONG_PATH).dar(DAR_PATH).parties(ALICE, BOB, CHARLIE).build();

  public Sandbox getSandbox() {
    return sandbox;
  }

  @Test
  public void testPartiesAreAllocated() {
    sandbox.getPartyId(ALICE);
    sandbox.getPartyId(BOB);
    sandbox.getPartyId(CHARLIE);
  }

  @Test
  public void notAllocatedPartyThrows() throws NullPointerException {
    Assertions.assertThrows(
        NullPointerException.class,
        () -> {
          sandbox.getPartyId("notAllocatedParty");
        });
  }
}
