/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing;

import com.daml.daml_lf_dev.DamlLf1;
import com.daml.extensions.testing.ledger.SandboxManager;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestCommons {
  public static final Path RESOURCE_DIR = Paths.get("src", "test", "resources").toAbsolutePath();
  public static final Path PINGPONG_PATH = RESOURCE_DIR.resolve("ping-pong").toAbsolutePath();
  public static final String DAR_FILE = "ping-pong.dar";
  public static final Path DAR_PATH = RESOURCE_DIR.resolve(DAR_FILE).toAbsolutePath();
  public static final String PING_PONG_MODULE_NAME = "MyPingPong";
  public static final DamlLf1.DottedName PING_PONG_MODULE =
      DamlLf1.DottedName.newBuilder().addSegments(PING_PONG_MODULE_NAME).build();
  public static final String ALICE = "Alice";
  public static final String BOB = "Bob";
  public static final String CHARLIE = "Charlie";
  public static final String[] EXAMPLE_PARTIES = new String[]{ALICE, BOB, CHARLIE};
  public static void checkIfExamplePartiesAllocated(SandboxManager sandbox){
    for(String displayName: EXAMPLE_PARTIES){
      assertNotNull(sandbox.getPartyId(displayName), displayName + " is not allocated");
    }
  }

}
