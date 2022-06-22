/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing;

import com.daml.extensions.testing.junit4.Sandbox;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.daml.extensions.testing.TestCommons.*;
import static org.junit.Assert.assertTrue;

public class SetupAppCallbackWithRestartIT {
  private static AtomicBoolean boolFlag = new AtomicBoolean(false);

  private static Sandbox sandbox =
      Sandbox.builder()
          .damlRoot(PINGPONG_PATH)
          .dar(DAR_PATH)
          .parties(ALICE, BOB, CHARLIE)
          .setupAppCallback(client -> boolFlag.set(true))
          .build();

  @ClassRule public static ExternalResource sandboxClassRule = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  @Test
  public void testSetupAppCallbackWithRestart() {
    assertTrue("Setup should set the boolFlag.", boolFlag.get());
  }
}
