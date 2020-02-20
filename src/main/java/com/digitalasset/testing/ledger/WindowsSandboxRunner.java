/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class WindowsSandboxRunner extends SandboxRunner {

  public WindowsSandboxRunner(
      String relativeDarPath,
      Optional<String> testModule,
      Optional<String> testScenario,
      Integer sandboxPort,
      boolean useWallclockTime,
      Optional<String> ledgerId) {
    super(relativeDarPath, testModule, testScenario, sandboxPort, useWallclockTime, ledgerId);
  }

  @Override
  protected String getDamlCommand() {
    return "daml.cmd";
  }

  @Override
  protected void closeSandbox(Process sandbox) throws IOException {
    // Do not use destroy method, otherwise subprocesses cannot be stopped properly on Windows.
    // Closing the output stream is treated as signal for graceful termination.
    sandbox.getOutputStream().close();
  }

  @Override
  protected void addCustomCommands(List<String> commands) {
    commands.add("--shutdown-stdin-close");
  }
}
