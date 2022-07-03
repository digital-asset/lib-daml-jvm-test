/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing;

import com.daml.extensions.testing.junit4.Sandbox;
import com.daml.extensions.testing.junit4.SandboxTestExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.daml.extensions.testing.TestCommons.DAR_PATH;
import static com.daml.extensions.testing.TestCommons.PINGPONG_PATH;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SandboxTestExtension.class)
public class SetupAppCallbackIT {
  private static AtomicBoolean boolFlag = new AtomicBoolean(false);

  private static final Sandbox sandbox =
      Sandbox.builder()
          .damlRoot(PINGPONG_PATH)
          .dar(DAR_PATH)
          .setupAppCallback(client -> boolFlag.set(true))
          .build();

  public Sandbox getSandbox() {
    return sandbox;
  }

  @Test
  public void testSetupAppCallback() {
    assertTrue(boolFlag.get(), "Setup should set the boolFlag.");
  }
}
