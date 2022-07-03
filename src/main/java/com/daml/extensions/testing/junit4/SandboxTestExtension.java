/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.junit4;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.InvocationTargetException;

public class SandboxTestExtension implements AfterEachCallback, BeforeEachCallback {

  @Override
  public void afterEach(ExtensionContext context)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Object testInstance = context.getRequiredTestInstance();
    Sandbox sandbox =
        (Sandbox) testInstance.getClass().getMethod("getSandbox", null).invoke(testInstance, null);
    sandbox.stop();
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    Object testInstance = context.getRequiredTestInstance();
    Sandbox sandbox =
        (Sandbox) testInstance.getClass().getMethod("getSandbox", null).invoke(testInstance, null);
    sandbox.restart();
  }
}
