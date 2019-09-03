package com.digitalasset.testing.cucumber.utils;

public class World {
  private int sandboxPort;

  public int getSandboxPort() {
    return sandboxPort;
  }

  public void setSandboxPort(int sandboxPort) {
    this.sandboxPort = sandboxPort;
  }

  @Override
  public String toString() {
    return "World{" + "sandboxPort=" + sandboxPort + '}';
  }
}
