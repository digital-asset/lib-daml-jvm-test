/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import com.digitalasset.testing.junit4.LogLevel;
import java.nio.file.Path;
import java.util.Optional;

public class UnixSandboxRunner extends SandboxRunner {

  public UnixSandboxRunner(
      Path damlRoot,
      Path relativeDarPath,
      Optional<String> testModule,
      Optional<String> testStartScript,
      Integer sandboxPort,
      boolean useWallclockTime,
      Optional<String> ledgerId,
      Optional<LogLevel> logLevel) {
    super(damlRoot, relativeDarPath, sandboxPort, useWallclockTime, ledgerId, logLevel);
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
