package com.digitalasset.testing.junit4;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

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
