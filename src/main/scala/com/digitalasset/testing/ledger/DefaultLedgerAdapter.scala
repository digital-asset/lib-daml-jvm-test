/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import com.daml.ledger.javaapi.data.{
  ContractId,
  Party,
  ExerciseCommand => JavaExerciseCommand,
  Identifier => JavaIdentifier,
  Record => JavaRecord,
  Value => JavaValue
}
import com.digitalasset.ledger.api.v1.command_service.{
  CommandServiceGrpc,
  SubmitAndWaitRequest
}
import com.digitalasset.ledger.api.v1.commands.Command.Command.{
  Create,
  Exercise
}
import com.digitalasset.ledger.api.v1.commands.{
  Command,
  Commands,
  CreateCommand,
  ExerciseCommand
}
import com.digitalasset.ledger.api.v1.ledger_identity_service.{
  GetLedgerIdentityRequest,
  LedgerIdentityServiceGrpc
}
import com.digitalasset.ledger.api.v1.ledger_offset.LedgerOffset
import com.digitalasset.ledger.api.v1.ledger_offset.LedgerOffset.LedgerBoundary.LEDGER_BEGIN
import com.digitalasset.ledger.api.v1.ledger_offset.LedgerOffset.Value.Boundary
import com.digitalasset.ledger.api.v1.transaction.TreeEvent
import com.digitalasset.ledger.api.v1.transaction_filter.{
  Filters,
  TransactionFilter
}
import com.digitalasset.ledger.api.v1.transaction_service.{
  GetLedgerEndRequest,
  GetTransactionTreesResponse,
  GetTransactionsRequest,
  TransactionServiceGrpc
}
import com.digitalasset.ledger.api.v1.value.{Identifier, Record, Value}
import com.digitalasset.testing.comparator.MessageTester
import com.digitalasset.testing.ledger.DefaultLedgerAdapter._
import com.digitalasset.testing.ledger.clock.{
  SystemTimeProviderFactory,
  TimeProvider,
  TimeProviderFactory
}
import com.digitalasset.testing.logging.Dump
import com.digitalasset.testing.store.{InMemoryMessageStorage, ValueStore}
import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.{ByteString, CodedInputStream}
import grizzled.slf4j.Logging
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import javax.annotation.concurrent.GuardedBy

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * A class that implements a ledger adapter and a ledger controller over a DAML Sandbox instance.
  * @param valueStore a structure used to store observed events
  * @param timeProviderFactory a factory that can create a time provider (e.g. a clock)
  * @param sandboxHost DAML Sandbox host
  * @param sandboxPort DAML Sandbox port
  * @param timeout time to wait for startup
  */
class DefaultLedgerAdapter(
    val valueStore: ValueStore,
    val timeProviderFactory: TimeProviderFactory = SystemTimeProviderFactory,
    val sandboxHost: String = FTSandboxHost,
    val sandboxPort: Int = FTSandboxPort,
    val timeout: FiniteDuration = DefaultLedgerAdapter.TIMEOUT)
    extends LedgerAdapter
    with LedgerController
    with TimeProvider
    with Logging {

  def this(valueStore: ValueStore) = {
    this(valueStore,
         SystemTimeProviderFactory,
         FTSandboxHost,
         FTSandboxPort,
         DefaultLedgerAdapter.TIMEOUT)
  }

  @GuardedBy("this")
  private var timeProvider: TimeProvider = _
  @GuardedBy("this")
  private var channel: ManagedChannel = _
  @GuardedBy("this")
  private var ledgerId: String = _
  @GuardedBy("this")
  private var startOffset: LedgerOffset = _
  @GuardedBy("this")
  private var storageByParty
    : mutable.Map[String, InMemoryMessageStorage[TreeEvent]] = _

  override def start(explicitParties: Seq[String],
                     suggestStartOffset: LedgerOffset): Unit = synchronized {
    assert(channel == null, "Ledger Client is already started")
    info("Starting Ledger Client")
    this.channel = initChannel
    this.ledgerId = initLedgerId
    this.startOffset = initStartOffset(suggestStartOffset)
    this.timeProvider =
      timeProviderFactory.getTimeProvider(channel, this.ledgerId)
    storageByParty = new mutable.HashMap()
    explicitParties.foreach(getStorage)
    info("Ledger Client started")
  }

  override def stop(): Unit = synchronized {
    if (channel != null) {
      info("Stopping Ledger Client")
      channel.shutdown().awaitTermination(5L, TimeUnit.SECONDS)
      channel = null
      info(s"Ledger Client stopped")
    }
  }

  override def createContract(party: Party,
                              templateId: JavaIdentifier,
                              payload: JavaRecord): Unit = synchronized {
    ensureStarted()
    debug(s"Attempting to create a contract $templateId")
    val parsedId = Identifier.parseFrom(templateId.toProto.toByteArray);

    val parsedRecord = Record.parseFrom(payload.toProtoRecord.toByteArray)
    val command = Create(CreateCommand(Some(parsedId), Some(parsedRecord)))
    submit(party.getValue, command)
    debug(s"Contract $templateId created")
  }

  override def exerciseChoice(
      party: Party,
      templateId: JavaIdentifier,
      contractId: ContractId,
      choice: String,
      payload: JavaValue
  ): Unit = synchronized {
    ensureStarted()
    debug(s"Attempting to exercise $choice on $templateId")
    val command =
      Exercise(
        ExerciseCommand(
          Some(Identifier.parseFrom(templateId.toProto.toByteArray)),
          contractId.getValue,
          choice,
          Some(Value.parseFrom(payload.toProto.toByteArray))))
    submit(party.getValue, command)
    debug(s"Choice $choice on $templateId exercised")
  }

  override def exerciseChoice(
      party: Party,
      exerciseCmd: JavaExerciseCommand
  ): Unit = synchronized {
    ensureStarted()
    val command =
      Exercise(
        ExerciseCommand(
          Some(Identifier.parseFrom(
            exerciseCmd.getTemplateId.toProto.toByteArray)),
          exerciseCmd.getContractId,
          exerciseCmd.getChoice,
          Some(
            Value.parseFrom(exerciseCmd.getChoiceArgument.toProto.toByteArray))
        ))
    submit(party.getValue, command)
  }

  // Observe the NEXT matching event.
  override def observeEvent(party: String,
                            eventTester: MessageTester[TreeEvent]): Unit = {
    val event = getStorage(party).observe(timeout, eventTester)
    Dump.dump(interactionLogger, ObserveEvent(party, event))
  }

  override def assertDidntHappen(party: String,
                                 eventTester: MessageTester[TreeEvent]): Unit =
    getStorage(party).assertDidntHappen(eventTester)

  @GuardedBy("this")
  private def ensureStarted(): Unit =
    if (channel == null) start(Seq.empty, LedgerOffset(Boundary(LEDGER_BEGIN)))

  private def getStorage(party: String) = synchronized {
    ensureStarted()
    storageByParty.getOrElseUpdate(party, initStorageAndStartListening(party))
  }

  private def initChannel: ManagedChannel =
    ManagedChannelBuilder
      .forAddress(sandboxHost, sandboxPort)
      .usePlaintext()
      .maxInboundMessageSize(Integer.MAX_VALUE)
      .build()

  private def initLedgerId =
    LedgerIdentityServiceGrpc
      .blockingStub(channel)
      .getLedgerIdentity(GetLedgerIdentityRequest())
      .ledgerId

  private def initStartOffset(suggestStartOffset: LedgerOffset) =
    if (suggestStartOffset.value.boundary.contains(
          LedgerOffset.LedgerBoundary.LEDGER_END)) {
      TransactionServiceGrpc
        .blockingStub(channel)
        .getLedgerEnd(GetLedgerEndRequest(ledgerId))
        .getOffset
    } else {
      suggestStartOffset
    }

  private def initStorageAndStartListening(party: String) = {
    val storage = new InMemoryMessageStorage[TreeEvent](
      DefaultLedgerAdapter.ChannelName,
      valueStore)
    val observer = new StreamObserver[GetTransactionTreesResponse] {
      override def onNext(response: GetTransactionTreesResponse): Unit =
        onMessage(response, party, storage)
      override def onError(t: Throwable): Unit = {
        info(t.getMessage)
        trace(s"Error occurred in stream handler for part $party", t)
      }
      override def onCompleted(): Unit =
        trace(s"Stream processing completed for party $party")
    }
    val request = GetTransactionsRequest(
      ledgerId = ledgerId,
      begin = Some(startOffset),
      filter = Some(TransactionFilter(Map(party -> Filters()))),
      verbose = true
    )
    TransactionServiceGrpc.stub(channel).getTransactionTrees(request, observer)
    storage
  }

  private def onMessage(
      event: GetTransactionTreesResponse,
      party: String,
      storage: InMemoryMessageStorage[TreeEvent]
  ): Unit = synchronized {
    if (channel != null) {
      for (tree <- event.transactions; event <- tree.eventsById.values) {
        Dump.dump(wireLogger, ObserveEvent(party, event))
        storage.onMessage(event)
      }
    }
  }

  private def submit(party: String, command: Command.Command): Unit = {
    val let = timeProvider.getCurrentTime
    val mrt = let.plusMillis(TTL.toMillis)
    val cmdId = UUID.randomUUID().toString
    val commands = Commands(
      ledgerId = ledgerId,
      workflowId = s"$APP_ID:$cmdId",
      applicationId = APP_ID,
      commandId = cmdId,
      party = party,
      ledgerEffectiveTime = Some(toProtobufTimestamp(let)),
      maximumRecordTime = Some(toProtobufTimestamp(mrt)),
      commands = Seq(Command(command))
    )

    lazy val event = CommandEvent(cmdId, party, command)
    Dump.dump(wireLogger, event)
    CommandServiceGrpc
      .blockingStub(channel)
      .submitAndWait(SubmitAndWaitRequest(commands = Some(commands)))
    Dump.dump(interactionLogger, event)
  }

  override def getCurrentTime: Instant = timeProvider.getCurrentTime

  override def setCurrentTime(time: Instant): Unit =
    timeProvider.setCurrentTime(time)
}

object DefaultLedgerAdapter {
  private val APP_ID = "func-test"
  private val TIMEOUT: FiniteDuration = 10 seconds
  private val TTL = 10 seconds
  private val ChannelName = "ledger"
  private val FTSandboxHost =
    Option(System.getProperty("ft.sandbox.hostname"))
      .filter(_.trim.nonEmpty)
      .getOrElse("localhost")
  private val FTSandboxPort = Integer.getInteger("ft.sandbox.port", 6865)
  private val wireLogger = "LEDGER.WIRE"
  private val interactionLogger = "LEDGER.INTERACTION"

  private def toCodedInputStream(input: ByteString) = {
    val cos = CodedInputStream.newInstance(input.asReadOnlyByteBuffer())
    cos.setRecursionLimit(1000)
    cos
  }

  private def toProtobufTimestamp(instant: Instant): Timestamp =
    Timestamp(instant.toEpochMilli / 1000,
              ((instant.toEpochMilli % 1000) * 1000).toInt)
}
