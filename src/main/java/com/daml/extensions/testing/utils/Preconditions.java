/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.utils;

public class Preconditions {
  public static void require(boolean expression, String message) {
    if (!expression) {
      throw new IllegalStateException(message);
    }
  }
}
