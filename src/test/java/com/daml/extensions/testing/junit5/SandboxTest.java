/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.junit5;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.daml.extensions.testing.TestCommons.DAR_PATH;
import static com.daml.extensions.testing.TestCommons.PINGPONG_PATH;
import static com.daml.extensions.testing.TestCommons.RESOURCE_DIR;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Optional;

@ExtendWith(SandboxTestExtension.class)
public class SandboxTest {

  private final int customPort = 6863;

  @TestSandbox
  public static final Sandbox sandbox =
          Sandbox.builder()
                  .damlRoot(PINGPONG_PATH)
                  .dar(DAR_PATH)
                  .port(6863)
                  .ledgerId("sample-ledger")
                  .logLevel(LogLevel.DEBUG) // implicitly test loglevel override
                  .build();

  @Test
  public void logLevelIsSet() {
    Sandbox sandbox =
        Sandbox.builder()
            .damlRoot(PINGPONG_PATH)
            .dar(DAR_PATH)
            .ledgerId("sample-ledger")
            .logLevel(LogLevel.TRACE)
            .build();
    assertThat(sandbox.getLogLevel(), is(Optional.of(LogLevel.TRACE)));
  }

  @Test
  public void darPathIsRequiredForSandbox() {
    Assertions.assertThrows(IllegalStateException.class, () -> Sandbox.builder().build());
  }

  @Test
  public void damlRootIsRequiredForSandbox() {
    Assertions.assertThrows(
        IllegalStateException.class,
        () -> Sandbox.builder().dar(DAR_PATH).damlRoot(RESOURCE_DIR).build());
  }

  @Test
  public void specifiedPortIsAssignedWhenSandboxIsStarted() {
    assertThat(sandbox.getSandboxPort(), Matchers.is(customPort));
  }
}
