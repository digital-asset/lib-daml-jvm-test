/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger

import java.io.{File, IOException}
import java.util.Optional
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

import com.daml.ledger.rxjava.DamlLedgerClient
import com.digitalasset.testing.ledger.clock.SandboxTimeProviderFactory
import com.digitalasset.testing.store.DefaultValueStore
import grizzled.slf4j.Logging

import scala.util.Try

/**
  * A class that can run a DAML Sandbox instance. A ledger client and a ledger adapter are initialized as well.
  * - To start, initialize the client and the adapter, call startSandbox.
  * - To stop, shutdown the client and the adapter, call stopSandbox.
  */
class SandboxRunner(val relativeDarPath: String,
                    val testModule: String,
                    val testScenario: String,
                    val sandboxPort: Int = 6865,
                    val waitTimeout: Int = 20,
                    val parties: Array[String] = Array.empty,
                    val setupApplication: Consumer[DamlLedgerClient])
    extends Logging {

  val damlLedgerClient: DamlLedgerClient =
    DamlLedgerClient.forHostWithLedgerIdDiscovery("localhost",
                                                  sandboxPort,
                                                  Optional.empty())
  val ledgerAdapter: DefaultLedgerAdapter =
    new DefaultLedgerAdapter(new DefaultValueStore(),
                             sandboxHost = "localhost",
                             sandboxPort = sandboxPort,
                             timeProviderFactory = SandboxTimeProviderFactory)
  private var sandbox: Process = null

  private def getSandboxRunner(scenario: String) =
    new ProcessBuilder("daml",
                       "sandbox",
                       "--",
                       "-p",
                       sandboxPort.toString,
                       "--scenario",
                       String.format("%s:%s", testModule, scenario),
                       relativeDarPath)
      .redirectError(new File("integration-test-sandbox.log"))
      .redirectOutput(new File("integration-test-sandbox.log"))

  def waitForConnection(damlLedgerClient: DamlLedgerClient,
                        seconds: Int): Unit = {
    var connected = false
    var time = seconds
    while ({
      !connected && 0 <= time
    }) try {
      damlLedgerClient.connect()
      connected = true
    } catch {
      case _ignored: Exception =>
        logger.info(s"Connecting to sandbox at localhost:${sandboxPort}")
        Try(Thread.sleep(1000))
    }
    if (!connected) {
      throw new TimeoutException(
        s"Can't connect to sandbox at localhost:${sandboxPort}")
    }
  }

  /**
    * Start DAML Sandbox, initialize the client and the adapter
    * @throws IOException
    */
  @throws[IOException]
  def startSandbox(): Unit = {
    sandbox = getSandboxRunner(testScenario).start
    waitForConnection(damlLedgerClient, waitTimeout)
    setupApplication.accept(damlLedgerClient)
    ledgerAdapter.start(parties)
  }

  /*
   * Stop DAML Sandbox, shutdown the client and the adapter
   */
  def stopSandbox(): Unit = {
    damlLedgerClient.close()
    if (sandbox != null) {
      sandbox.destroy()
      sandbox.waitFor()
    }
    sandbox = null
  }
}
