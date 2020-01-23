/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.utils;

public class OS {
  public static boolean isWindows() {
    return System.getProperty("os.name").contains("indows");
  }
}
