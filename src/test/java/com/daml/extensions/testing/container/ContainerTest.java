/*
 * Copyright 2022 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.container;

import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import static com.daml.extensions.testing.TestCommons.RESOURCE_DIR;

/**
 * @author Jean Safdar
 */
public class ContainerTest {
    public static final String IMAGE = "digitalasset/daml-sdk:2.1.1";
    public static final int PORT_DEFAULT = 6865;
    public static final String darFile = "ping-pong.dar";
    public static final String CONTAINER_DAR_PATH = "/release";
    @Test
    public void testContainer() {
        GenericContainer test = new GenericContainer(IMAGE);
        String filePath = RESOURCE_DIR.toAbsolutePath().toString();
        System.out.println ("Filepath " + filePath);
        String command = "daml sandbox --dar " + CONTAINER_DAR_PATH + "/" + darFile ;
//        String command = "daml sandbox --dar " + DamlContainer.CONTAINER_DAR_PATH + "/" + darFile ;
        System.out.println ("Command " + command);
        test.withFileSystemBind(filePath, CONTAINER_DAR_PATH, BindMode.READ_ONLY)
                .withExposedPorts(PORT_DEFAULT) // this means that ports will be dynamically allocated
                .withCommand(command)
                .start();
        System.err.println(test.getLogs());
        Assert.assertTrue("the container should now be running on ledger port " + test.getMappedPort(PORT_DEFAULT), test.isRunning());
        test.close();
    }
}
