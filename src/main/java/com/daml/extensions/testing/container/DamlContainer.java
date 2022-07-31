/*
 * Copyright 2022 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.daml.extensions.testing.container;

import org.testcontainers.containers.GenericContainer;

/** Needed to make public the fixedPorts method inside GenericContainer */
public class DamlContainer extends GenericContainer<DamlContainer> {
  public DamlContainer(String image) {
    super(image);
  }

  // make public this otherwise protected method
  public DamlContainer fixedPorts(int extPort, int dockPort) {
    addFixedExposedPort(extPort, dockPort);
    return self();
  }
}
