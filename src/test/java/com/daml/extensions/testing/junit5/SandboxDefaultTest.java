/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.daml.extensions.testing.JvmTestLibCommon.SANDBOX_PORT_RANGE;
import static com.daml.extensions.testing.TestCommons.DAR_PATH;
import static com.daml.extensions.testing.TestCommons.PINGPONG_PATH;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SandboxTestExtension.class)
public class SandboxDefaultTest {

  @TestSandbox
  public static final Sandbox sandbox =
      Sandbox.builder().damlRoot(PINGPONG_PATH).dar(DAR_PATH).build();

  @Test
  public void defaultPortIsAssignedWhenSandboxIsStarted() {
    int sandboxPort = sandbox.getSandboxPort();
    assertsIsBetween(
        sandboxPort, SANDBOX_PORT_RANGE.lowerEndpoint(), SANDBOX_PORT_RANGE.upperEndpoint());
  }

  @Test
  public void defaultLedgerIdSpecified() {
    assertTrue(sandbox.getLedgerId() != null);
  }

  private void assertsIsBetween(int x, int low, int high) {
    String message = String.format("Expected '%d' to be between '%d' and '%d'", x, low, high);
    assertTrue(low <= x && x <= high, message);
  }
}
