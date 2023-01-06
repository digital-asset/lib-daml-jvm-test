/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing;

import com.daml.extensions.testing.junit5.Sandbox;
import com.daml.extensions.testing.junit5.SandboxTestExtension;

import com.daml.extensions.testing.junit5.TestSandbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.daml.extensions.testing.TestCommons.DAR_PATH;
import static com.daml.extensions.testing.TestCommons.PINGPONG_PATH;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SandboxTestExtension.class)
public class SetupAppCallbackIT {
  private static final AtomicBoolean boolFlag = new AtomicBoolean(false);

  @TestSandbox
  public static final Sandbox sandbox =
      Sandbox.builder()
          .damlRoot(PINGPONG_PATH)
          .dar(DAR_PATH)
          .setupAppCallback(client -> boolFlag.set(true))
          .build();

  @Test
  public void testSetupAppCallback() {
    assertTrue(boolFlag.get(), "Setup should set the boolFlag.");
  }
}
