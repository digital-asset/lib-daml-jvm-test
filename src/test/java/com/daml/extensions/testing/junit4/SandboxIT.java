/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.junit4;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.hamcrest.Matchers;

import static com.daml.extensions.testing.TestCommons.DAR_PATH;
import static com.daml.extensions.testing.TestCommons.PINGPONG_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SandboxTestExtension.class)
public class SandboxIT {

  private final int customPort = 6863;
  private static final Sandbox sandbox =
      Sandbox.builder()
          .damlRoot(PINGPONG_PATH)
          .dar(DAR_PATH)
          .port(6863)
          .ledgerId("sample-ledger")
          .logLevel(LogLevel.DEBUG) // implicitly test loglevel override
          .build();

  public Sandbox getSandbox() {
    return sandbox;
  }

  @Test
  public void specifiedPortIsAssignedWhenSandboxIsStarted() {
    assertThat(sandbox.getSandboxPort(), Matchers.is(customPort));
  }

  @Test
  public void ledgerIdSpecified() {
    assertThat(sandbox.getLedgerId(), is("sample-ledger"));
  }

}
