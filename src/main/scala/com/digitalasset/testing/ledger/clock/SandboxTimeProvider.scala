/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger.clock

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

import com.digitalasset.ledger.api.v1.testing.time_service.{
  GetTimeRequest,
  GetTimeResponse,
  SetTimeRequest,
  TimeServiceGrpc
}
import com.google.protobuf.timestamp.Timestamp
import io.grpc.ManagedChannel
import io.grpc.stub.StreamObserver

import scala.concurrent.Await
import scala.concurrent.duration._

object SandboxTimeProviderFactory extends TimeProviderFactory {
  override def getTimeProvider(channel: ManagedChannel,
                               ledgerId: String): TimeProvider =
    new SandboxTimeProvider(TimeServiceGrpc.stub(channel), ledgerId)
}

class SandboxTimeProvider(stub: TimeServiceGrpc.TimeServiceStub,
                          ledgerId: String)
    extends TimeProvider {

  stub.getTime(
    new GetTimeRequest(ledgerId),
    new StreamObserver[GetTimeResponse] {
      override def onNext(timeResp: GetTimeResponse): Unit =
        actualTime.set(
          Instant.ofEpochSecond(timeResp.getCurrentTime.seconds,
                                timeResp.getCurrentTime.nanos))

      override def onError(t: Throwable): Unit = {
        println(t.getMessage)
        throw t
      }

      override def onCompleted(): Unit = ()
    }
  )

  private var actualTime: AtomicReference[Instant] =
    new AtomicReference[Instant](Instant.EPOCH)

  override def getCurrentTime: Instant = actualTime.get()

  override def setCurrentTime(time: Instant): Unit = {
    val aT = actualTime.get()
    val setFut = stub.setTime(
      SetTimeRequest(ledgerId,
                     Some(new Timestamp(aT.getEpochSecond, aT.getNano)),
                     Some(new Timestamp(time.getEpochSecond, time.getNano))))
    Await.result(setFut, 5000 millis)
  }
}
