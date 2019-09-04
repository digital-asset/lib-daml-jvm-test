/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.cucumber.utils;

import com.digitalasset.testing.utils.SandboxUtils;

public class World {
  private int sandboxPort;

  public World() {
    this.sandboxPort = SandboxUtils.getSandboxPort();
  }

  public int getSandboxPort() {
    return sandboxPort;
  }

  @Override
  public String toString() {
    return "World{" + "sandboxPort=" + sandboxPort + '}';
  }
}
