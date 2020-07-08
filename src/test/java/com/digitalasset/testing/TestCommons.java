/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing;

import com.daml.daml_lf_dev.DamlLf1;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestCommons {
  public static final Path RESOURCE_DIR = Paths.get("src", "test", "resources").toAbsolutePath();
  public static final Path PINGPONG_PATH = RESOURCE_DIR.resolve("ping-pong").toAbsolutePath();
  public static final Path DAR_PATH = RESOURCE_DIR.resolve("ping-pong.dar").toAbsolutePath();
  public static final String PING_PONG_MODULE_NAME = "PingPong";
  public static final DamlLf1.DottedName PING_PONG_MODULE =
      DamlLf1.DottedName.newBuilder().addSegments(PING_PONG_MODULE_NAME).build();
}
