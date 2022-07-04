/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.ledger;

import com.daml.extensions.testing.junit5.LogLevel;
import com.daml.extensions.testing.utils.OS;
import java.nio.file.Path;
import java.util.Optional;

public class SandboxRunnerFactory {
  public static SandboxRunner getSandboxRunner(
      Path damlRoot,
      Path darPath,
      int sandboxPort,
      boolean useWallclockTime,
      Optional<String> ledgerId,
      Optional<LogLevel> logLevel) {
    if (OS.isWindows()) {
      return new WindowsSandboxRunner(
          damlRoot, darPath, sandboxPort, useWallclockTime, ledgerId, logLevel);
    } else {
      return new UnixSandboxRunner(
          damlRoot, darPath, sandboxPort, useWallclockTime, ledgerId, logLevel);
    }
  }
}
