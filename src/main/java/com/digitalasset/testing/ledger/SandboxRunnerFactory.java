/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import com.digitalasset.testing.junit4.LogLevel;
import com.digitalasset.testing.utils.OS;
import java.nio.file.Path;
import java.util.Optional;

public class SandboxRunnerFactory {
  public static SandboxRunner getSandboxRunner(
      Path damlRoot,
      Path darPath,
      Optional<String> testModule,
      Optional<String> testStartScript,
      int sandboxPort,
      boolean useWallclockTime,
      Optional<String> ledgerId,
      Optional<LogLevel> logLevel) {
    if (OS.isWindows()) {
      return new WindowsSandboxRunner(
          damlRoot,
          darPath,
          testModule,
          testStartScript,
          sandboxPort,
          useWallclockTime,
          ledgerId,
          logLevel);
    } else {
      return new UnixSandboxRunner(
          damlRoot,
          darPath,
          testModule,
          testStartScript,
          sandboxPort,
          useWallclockTime,
          ledgerId,
          logLevel);
    }
  }
}
