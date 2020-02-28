package com.digitalasset.testing.ledger;

import com.digitalasset.testing.junit4.Sandbox;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.*;

public class SandboxRunnerTest {

  @Test
  public void commandsAppearsToBeValidArgumentList() {

    SandboxRunner sandboxRunner = SandboxRunnerFactory.getSandboxRunner(Paths.get("path-to-dar"), Optional.of("testModule"), Optional.of("testScenario"), 123, true, Optional.of("ledgerId"), Optional.of(Sandbox.LogLevel.TRACE));
    assertEquals(
            Arrays.asList("daml", "sandbox", "--", "-p", "123", "-w", "--scenario", "testModule:testScenario", "--ledgerid", "ledgerId", "--log-level", "trace", "path-to-dar"), sandboxRunner.commands()
    );
  }
}
