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
  public void afterEach(ExtensionContext context) {
    context
        .getTestInstance()
        .ifPresent(
            testInstance -> {
              List<Field> sandboxFields =
                  AnnotationSupport.findAnnotatedFields(testInstance.getClass(), TestSandbox.class);
              for (Field sandboxField : sandboxFields) {
                try {
                  Sandbox sandbox = (Sandbox) sandboxField.get(testInstance);
                  sandbox.stop();
                } catch (IllegalAccessException e) {
                  e.printStackTrace();
                }
              }
            });
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    context
        .getTestInstance()
        .ifPresent(
            testInstance -> {
              List<Field> sandboxFields =
                  AnnotationSupport.findAnnotatedFields(testInstance.getClass(), TestSandbox.class);
              for (Field sandboxField : sandboxFields) {
                try {
                  Sandbox sandbox = (Sandbox) sandboxField.get(testInstance);
                  sandbox.restart();
                } catch (IllegalAccessException
                    | IOException
                    | InterruptedException
                    | TimeoutException e) {
                  e.printStackTrace();
                }
              }
            });
  }
}
