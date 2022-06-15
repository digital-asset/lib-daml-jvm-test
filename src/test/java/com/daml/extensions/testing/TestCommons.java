/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing;

import com.daml.daml_lf_dev.DamlLf1;
import com.daml.ledger.javaapi.data.Party;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.daml.extensions.testing.Dsl.party;

public class TestCommons {
  public static final Path RESOURCE_DIR = Paths.get("src", "test", "resources").toAbsolutePath();
  public static final Path PINGPONG_PATH = RESOURCE_DIR.resolve("ping-pong").toAbsolutePath();
  public static final Path DAR_PATH = RESOURCE_DIR.resolve("ping-pong.dar").toAbsolutePath();
  public static final String PING_PONG_MODULE_NAME = "PingPong";
  public static final DamlLf1.DottedName PING_PONG_MODULE =
      DamlLf1.DottedName.newBuilder().addSegments(PING_PONG_MODULE_NAME).build();
  public static final Party ALICE = party("Alice");
  public static final Party BOB = party("Bob");
  public static final Party CHARLIE = party("Charlie");
}
