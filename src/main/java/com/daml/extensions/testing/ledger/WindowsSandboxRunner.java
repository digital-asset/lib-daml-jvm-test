/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.ledger;

import com.daml.extensions.testing.junit5.LogLevel;

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
      Optional<Path> customConfigPath,
      Optional<String> ledgerId,
      Optional<LogLevel> logLevel) {
    super(
        damlRoot,
        relativeDarPath,
        sandboxPort,
        useWallclockTime,
        customConfigPath,
        ledgerId,
        logLevel);
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
