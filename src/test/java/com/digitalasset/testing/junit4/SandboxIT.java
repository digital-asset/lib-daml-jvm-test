/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.junit4;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class SandboxIT {

  private static Sandbox sandbox =
      Sandbox.builder()
          .dar(Paths.get("./src/test/resources/ping-pong.dar"))
          .ledgerId("sample-ledger")
          .build();

  @ClassRule public static ExternalResource classRule = sandbox.getClassRule();

  @Rule public ExternalResource rule = sandbox.getRule();

  @Test
  public void portIsAssignedWhenSandboxIsStarted() {
    int sandboxPort = sandbox.getSandboxPort();
    assertsIsBetween(sandboxPort, 6860, 6890);
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
