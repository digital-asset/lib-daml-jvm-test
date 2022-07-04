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

import java.lang.reflect.Field;
import java.util.List;

public class SandboxTestExtension implements AfterEachCallback, BeforeEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    context
        .getTestInstance()
        .ifPresent(
            testInstance -> {
              Sandbox sandbox = getSandboxFromTestInstance(testInstance);
              try {
                sandbox.restart();
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }

  @Override
  public void afterEach(ExtensionContext context) {
    context
        .getTestInstance()
        .ifPresent(
            testInstance -> {
              Sandbox sandbox = getSandboxFromTestInstance(testInstance);
              try {
                sandbox.stop();
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
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
