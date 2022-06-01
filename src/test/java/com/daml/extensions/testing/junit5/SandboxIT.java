/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.hamcrest.Matchers;

import static com.daml.extensions.testing.TestCommons.DAR_PATH;
import static com.daml.extensions.testing.TestCommons.PINGPONG_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SandboxTestExtension.class)
public class SandboxIT {

  @TestSandbox
  public static final Sandbox sandbox =
      Sandbox.builder()
          .damlRoot(PINGPONG_PATH)
          .dar(DAR_PATH)
          .ledgerId("sample-ledger")
          .logLevel(LogLevel.DEBUG) // implicitly test loglevel override
          .build();

  @Test
  public void portIsAssignedWhenSandboxIsStarted() {
    int sandboxPort = sandbox.getSandboxPort();
    assertsIsBetween(sandboxPort, 6860, 6890);
  }

  @Test
  public void ledgerIdSpecified() {
    assertThat(sandbox.getLedgerId(), is("sample-ledger"));
  }

  @Test
  public void portIsAssignedAndSandboxContainerIsStarted() {
    try {
      Sandbox sandbox =
              Sandbox.builder()
                      .damlRoot(RESOURCE_DIR.toAbsolutePath())
                      .dar(DAR_PATH.getFileName())
                      .useContainers()
                      .ledgerId("sample-ledger")
                      .logLevel(LogLevel.DEBUG) // implicitly test loglevel override
                      .build();
      sandbox.getSandboxManager().start();
      int sandboxPort = sandbox.getSandboxPort();
      Assert.assertTrue("Sandbox should be started", sandbox.isRunnning());
      assertsIsBetween(sandboxPort, 6860, 6890);
      assertThat(sandbox.getLedgerId(), is("sample-ledger"));
    } catch (Exception e) {
      Assert.assertTrue("Sandbox Exception " + e.getLocalizedMessage(),false);
    }
  }

  private void assertsIsBetween(int x, int low, int high) {
    String message = String.format("Expected '%d' to be between '%d' and '%d'", x, low, high);
    assertTrue(low <= x && x <= high, message);
  }
}
