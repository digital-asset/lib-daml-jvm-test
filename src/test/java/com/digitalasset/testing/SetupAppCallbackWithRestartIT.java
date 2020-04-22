/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing;

import static com.digitalasset.testing.TestCommons.ALICE;
import static com.digitalasset.testing.TestCommons.BOB;
import static com.digitalasset.testing.TestCommons.CHARLIE;
import static com.digitalasset.testing.TestCommons.DAR_PATH;
import static com.digitalasset.testing.TestCommons.PINGPONG_PATH;
import static org.junit.Assert.assertTrue;

import com.digitalasset.testing.junit4.Sandbox;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import java.util.concurrent.atomic.AtomicBoolean;

public class SetupAppCallbackWithRestartIT {
  private static AtomicBoolean boolFlag = new AtomicBoolean(false);

  private static Sandbox sandbox =
      Sandbox.builder()
          .damlRoot(PINGPONG_PATH)
          .dar(DAR_PATH)
          .parties(ALICE.getValue(), BOB.getValue(), CHARLIE.getValue())
          .useReset()
          .setupAppCallback(client -> boolFlag.set(true))
          .build();

  @ClassRule public static ExternalResource sandboxClassRule = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  @Test
  public void testSetupAppCallbackWithRestart() {
    assertTrue("Setup should set the boolFlag.", boolFlag.get());
  }
}
