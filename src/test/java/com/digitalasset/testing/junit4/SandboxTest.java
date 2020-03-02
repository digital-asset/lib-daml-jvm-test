/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.junit4;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SandboxTest {

  @Test
  public void logLevelIsSet() {
    Sandbox sandbox =
        Sandbox.builder()
            .dar(Paths.get("./src/test/resources/ping-pong.dar"))
            .ledgerId("sample-ledger")
            .logLevel(LogLevel.TRACE)
            .build();
    assertThat(sandbox.getLogLevel(), is(Optional.of(LogLevel.TRACE)));
  }
}
