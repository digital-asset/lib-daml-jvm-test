/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.cucumber.utils;

import com.digitalasset.testing.utils.SandboxUtils;

public class Config {
  private int sandboxPort;

  public Config() {
    this.sandboxPort = SandboxUtils.getSandboxPort();
  }

  public int getSandboxPort() {
    return sandboxPort;
  }

  @Override
  public String toString() {
    return "Config{" + "sandboxPort=" + sandboxPort + '}';
  }
}
