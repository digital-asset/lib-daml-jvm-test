package com.digitalasset.testing.junit4;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class SandboxTest {

  @Test
  public void logLevelIsSet()  {
    Sandbox sandbox = Sandbox.builder()
            .dar(Paths.get("./src/test/resources/ping-pong.dar"))
            .ledgerId("sample-ledger")
            .logLevel(Sandbox.LogLevel.TRACE).build();

    assertThat(sandbox.getLogLevel(), is(Optional.of(Sandbox.LogLevel.DEBUG)));
  }

}
