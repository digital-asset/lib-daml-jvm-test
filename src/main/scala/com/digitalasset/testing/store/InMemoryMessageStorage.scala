/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.store

import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{CountDownLatch, TimeUnit, TimeoutException}

import com.digitalasset.testing.comparator.MessageTester
import com.digitalasset.testing.comparator.MessageTester.{Diff, Error, Same}
import grizzled.slf4j.Logging
import javax.annotation.concurrent.GuardedBy

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.FiniteDuration

class InMemoryMessageStorage[T](val channel: String, val valueStore: ValueStore)
    extends Logging {
  @GuardedBy("this")
  private val observedMessages: ListBuffer[T] = ListBuffer()
  @GuardedBy("this")
  private var claimedUpToIx: Int = 0
  @GuardedBy("this")
  private val listeners
    : mutable.Map[MessageTester[T], (CountDownLatch, AtomicReference[T])] =
    mutable.HashMap()

  def onMessage(msg: T): Unit = synchronized {
    listeners
      .find { case (predicate, _) => predicate.test(msg).success }
      .fold[Unit] {
        trace(
          "adding to observed messages, since no one claimed to have expected it")
        observedMessages += msg
      } {
        case (tester, (latch, ref)) =>
          trace(
            "one tester claimed to be waiting for the message; catching the message, and counting down the latch")
          listeners -= tester
          claimedUpToIx = observedMessages.size
          ref.set(msg)
          latch.countDown()
      }
  }

  def observe(duration: Duration, tester: MessageTester[T]): T = {
    observe(FiniteDuration(duration.getSeconds, TimeUnit.SECONDS), tester)
  }

  def observe(time: FiniteDuration, tester: MessageTester[T]): T = {
    val latch = new CountDownLatch(1) // latch to block until we see a message
    val ref = new AtomicReference[T]() // to catch the message
    try {
      synchronized {
        // check if we have seen it first
        val existingMsg = observedMessages.find(tester.test(_).success)
        existingMsg.fold[Unit] {
          trace("didn't see the message yet, adding our listener")
          listeners.put(tester, (latch, ref))
        } { msg =>
          trace(
            "seen the message, counting down the latch, removing the message")
          val ix = observedMessages.indexOf(msg)
          observedMessages.remove(ix)
          claimedUpToIx = (claimedUpToIx max ix) min observedMessages.size
          ref.set(msg)
          latch.countDown()
        }
      }

      trace("waiting for the message to come")
      if (latch.await(time.toSeconds, TimeUnit.SECONDS)) {
        trace("waiting on latch finished successfully")
        val result = ref.get()
        debug("Testing " + tester.prettyPrintExpected + " against " + result)
        tester.test(result) match {
          case r: Same =>
            r.capturedValues.foreach((valueStore.put _).tupled)
          case d =>
            debug("Diff: " + d)
        }
        result
      } else {
        trace("waiting on latch didn't finish successfully")
        val description = logExpectedAndActualMessages(tester)
        throw new TimeoutException(
          s"Timed out while waiting for the correct message to be observed.\n$description")
      }
    } finally {
      synchronized { listeners.remove(tester) }
    }
  }

  def assertDidntHappen(tester: MessageTester[T]): Unit = synchronized {
    observedMessages
      .drop(claimedUpToIx)
      .find(tester.test(_).success)
      .fold {
        // do nothing, all good
      } { actual =>
        throw new IllegalStateException(
          s"""Expected message to not have happened:
             |${tester.prettyPrintExpected}
             |But it actually happened:
             |${tester.prettyPrintActual(actual)}""".stripMargin
        )
      }
  }

  def logExpectedAndActualMessages(predicate: MessageTester[T]): String =
    synchronized {
      val diffs = observedMessages.map(actual =>
        (() => predicate.prettyPrintActual(actual), predicate.test(actual)))
      val observedDiffs = diffs.flatMap {
        case (actual, Diff(diff)) =>
          Seq(s"""${actual()}\n\nDiffs:\n${diff.mkString("\n")}""")
        case (actual, Error(err)) =>
          Seq(s"""${actual()}\n\nError during comparison: $err""")
        case _ => Seq()
      }
      val relevantMessageCount = observedDiffs.size
      val irrelevantMessageCount = diffs.size - relevantMessageCount

      val prettyDiffs = observedDiffs.mkString("\n\n---\n\n")

      val actualOutcome = (relevantMessageCount, irrelevantMessageCount) match {
        case (0, 0) =>
          s"But no events observed via $channel channel"
        case (0, irrelevant) =>
          s"But no events observed (among $irrelevant irrelevant events) via $channel channel"
        case (total, 0) =>
          s"However, $total other events observed via $channel channel:\n\n$prettyDiffs"
        case (total, irrelevant) =>
          s"However, $total other events observed (along with $irrelevant irrelevant events) via $channel channel:\n\n$prettyDiffs"
      }

      s"""Expected:
       |
       |${predicate.prettyPrintExpected}
       |
       |$actualOutcome
     """.stripMargin
    }
}
