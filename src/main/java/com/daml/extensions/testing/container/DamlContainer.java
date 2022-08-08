/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.daml.extensions.testing.container;

import org.testcontainers.containers.GenericContainer;

/** Needed to make public the fixedPorts method inside GenericContainer */
public class DamlContainer extends GenericContainer<DamlContainer> {
  public DamlContainer(String image) {
    super(image);
  }
}
