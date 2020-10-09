/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.junit4;

import static com.digitalasset.testing.TestCommons.DAR_PATH;
import static com.digitalasset.testing.TestCommons.PINGPONG_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class SandboxIT {

  private static Sandbox sandbox =
      Sandbox.builder()
          .damlRoot(PINGPONG_PATH)
          .darMavenCoordinates(MavenCoordinates.builder()
                  .repoUrl("https://nexus.liquid-share.io/repository/liquidshare-maven")
                  .group("io.liquidshare.daml")
                  .darArtifact("liquidshare-daml")
                  .yamlArtifact("liquidshare-daml-manifest")
                  .version("0.28.0")
                  .mavenCredentials(MavenCredentials.builder()
                          .userName("emil.kirschner")
                          .password("uAU@UHQJQcE8_uZmed!RfJrY_JB6K2MVL4zzQLE@@3hfxsqz")
                          .build())
                  .build())
          .ledgerId("sample-ledger")
          .logLevel(LogLevel.DEBUG) // implicitly test loglevel override
              .sandboxWaitTimeout(Duration.of(1, ChronoUnit.MINUTES))
          .build();

  @ClassRule public static ExternalResource classRule = sandbox.getClassRule();

  @Rule public ExternalResource rule = sandbox.getRule();

  @Test
  public void portIsAssignedWhenSandboxIsStarted() {
    int sandboxPort = sandbox.getSandboxPort();
    assertsIsBetween(sandboxPort, 6860, 6890);
  }

  @Test
  public void ledgerIdSpecified() {
    assertThat(sandbox.getLedgerId(), is("sample-ledger"));
  }

  private void assertsIsBetween(int x, int low, int high) {
    String message = String.format("Expected '%d' to be between '%d' and '%d'", x, low, high);
    Assert.assertTrue(message, low <= x && x <= high);
  }
}
