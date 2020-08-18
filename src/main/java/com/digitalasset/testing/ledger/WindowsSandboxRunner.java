/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import com.digitalasset.testing.junit4.LogLevel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class WindowsSandboxRunner extends SandboxRunner {

  WindowsSandboxRunner(
      Path damlRoot,
      Path relativeDarPath,
      Integer sandboxPort,
      boolean useWallclockTime,
      Optional<String> ledgerId,
      Optional<LogLevel> logLevel) {
    super(damlRoot, relativeDarPath, sandboxPort, useWallclockTime, ledgerId, logLevel);
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
