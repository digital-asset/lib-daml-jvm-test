/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.junit4;

import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.ExternalResource;

import static com.daml.extensions.testing.TestCommons.DAR_PATH;
import static com.daml.extensions.testing.TestCommons.PINGPONG_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SandboxIT {

  private final int customPort = 6863;
  private static Sandbox sandbox =
      Sandbox.builder()
          .damlRoot(PINGPONG_PATH)
          .dar(DAR_PATH)
          .port(6863)
          .ledgerId("sample-ledger")
          .logLevel(LogLevel.DEBUG) // implicitly test loglevel override
          .build();

  @ClassRule public static ExternalResource classRule = sandbox.getClassRule();

  @Rule public ExternalResource rule = sandbox.getRule();

  @Test
  public void specifiedPortIsAssignedWhenSandboxIsStarted() {
    assertThat(sandbox.getSandboxPort(), Matchers.is(customPort));
  }

  @Test
  public void ledgerIdSpecified() {
    assertThat(sandbox.getLedgerId(), is("sample-ledger"));
  }

  private void assertsIsBetween(int x, int low, int high) {
    String message = String.format("Expected '%d' to be between '%d' and '%d'", x, low, high);
    Assert.assertTrue(message, low <= x && x <= high);
  }
}
