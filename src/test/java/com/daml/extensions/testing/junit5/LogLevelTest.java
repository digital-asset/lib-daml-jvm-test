/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.junit5;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LogLevelTest {

  @Test
  public void testToString() {
    assertThat(LogLevel.DEBUG.toString(), is("debug"));
    assertThat(LogLevel.TRACE.toString(), is("trace"));
    assertThat(LogLevel.ERROR.toString(), is("error"));
    assertThat(LogLevel.INFO.toString(), is("info"));
    assertThat(LogLevel.WARN.toString(), is("warn"));
  }
}
