/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.junit5;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class SandboxTestExtension implements AfterEachCallback, BeforeEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) throws IOException, InterruptedException, TimeoutException {
    Sandbox sandbox = getSandboxFromTestContext(context);
    sandbox.restart();
  }

  @Override
  public void afterEach(ExtensionContext context) {
    Sandbox sandbox = getSandboxFromTestContext(context);
    sandbox.stop();
  }

  private Sandbox getSandboxFromTestContext(ExtensionContext context){
    Object testInstance = getTestInstanceFromContext(context);
    return getSandboxFromTestInstance(testInstance);
  }

  private Object getTestInstanceFromContext(ExtensionContext context){
      return context.getRequiredTestInstance();
  }

  private Sandbox getSandboxFromTestInstance(Object testInstance) {
    List<Field> sandboxFields =
        AnnotationSupport.findAnnotatedFields(testInstance.getClass(), TestSandbox.class);
    Sandbox sandbox = null;
    for (Field sandboxField : sandboxFields) {
      try {
        sandbox = (Sandbox) sandboxField.get(testInstance);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return sandbox;
  }
}
