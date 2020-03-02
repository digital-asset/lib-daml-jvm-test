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
