/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.junit4;

public enum LogLevel {
  INFO,
  TRACE,
  DEBUG,
  WARN,
  ERROR;

  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }
}
