package com.digitalasset.testing.ledger;

import com.digitalasset.testing.utils.OS;

import java.nio.file.Path;
import java.util.Optional;

public class SandboxRunnerFactory {
  public static SandboxRunner getSandboxRunner(
      Path darPath,
      Optional<String> testModule,
      Optional<String> testScenario,
      int sandboxPort,
      boolean useWallclockTime) {
    if (OS.isWindows()) {
      return new WindowsSandboxRunner(
          darPath.toString(), testModule, testScenario, sandboxPort, useWallclockTime);
    } else {
      return new UnixSandboxRunner(
          darPath.toString(), testModule, testScenario, sandboxPort, useWallclockTime);
    }
  }
}
