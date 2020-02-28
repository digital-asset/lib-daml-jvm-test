/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import com.digitalasset.testing.junit4.Sandbox;

import java.util.Optional;

public class UnixSandboxRunner extends SandboxRunner {

  public UnixSandboxRunner(
      String relativeDarPath,
      Optional<String> testModule,
      Optional<String> testScenario,
      Integer sandboxPort,
      boolean useWallclockTime,
      Optional<String> ledgerId,
      Optional<Sandbox.LogLevel> logLevel) {
    super(
        relativeDarPath,
        testModule,
        testScenario,
        sandboxPort,
        useWallclockTime,
        ledgerId,
        logLevel);
  }

  @Override
  protected String getDamlCommand() {
    return "daml";
  }

  @Override
  protected void closeSandbox(Process sandbox) {
    sandbox.destroy();
  }
}
