package com.daml.extensions.testing.junit5;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.daml.extensions.testing.TestCommons.DAR_PATH;
import static com.daml.extensions.testing.TestCommons.PINGPONG_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SandboxTestExtension.class)
public class SandboxPortIT {
  private final int customPort = 6863;

  @TestSandbox
  public static final Sandbox sandbox =
      Sandbox.builder()
          .damlRoot(PINGPONG_PATH)
          .dar(DAR_PATH)
          .port(6863)
          .ledgerId("sample-ledger")
          .logLevel(LogLevel.DEBUG) // implicitly test loglevel override
          .build();

  @Test
  public void specifiedPortIsAssignedWhenSandboxIsStarted() {
    assertThat(sandbox.getSandboxPort(), Matchers.is(customPort));
  }
}
