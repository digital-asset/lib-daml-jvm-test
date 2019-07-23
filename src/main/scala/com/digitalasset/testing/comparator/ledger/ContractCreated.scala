/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.comparator.ledger

import com.daml.ledger.javaapi.data.{
  Identifier => JavaIdentifier,
  Record => JavaRecord
}
import com.digitalasset.ledger.api.v1.transaction.TreeEvent
import com.digitalasset.ledger.api.v1.value.Value
import com.digitalasset.ledger.api.v1.value.Value.Sum
import com.digitalasset.testing.ast.toAst
import com.digitalasset.testing.comparator.MessageTester
import com.digitalasset.testing.comparator.MessageTester.{Irrelevant, Same}
import grizzled.slf4j.Logging
import scalaz.syntax.monoid.ToSemigroupOps

object ContractCreated extends Logging {
  private def apply(
      expectedTemplate: JavaIdentifier,
      expectedContractId: String,
      captureOrexpectedArgumentsOpt: Either[String, Option[JavaRecord]])
    : MessageTester[TreeEvent] =
    new MessageTester[TreeEvent] {
      override def prettyPrintExpected: String =
        s"${expectedTemplate} - ${expectedContractId}"
      override def prettyPrintActual(event: TreeEvent): String = event.toString

      override def test(event: TreeEvent): MessageTester.ComparisonResult =
        if (event.kind.isCreated && compareIdentifier(
              event.getCreated.getTemplateId,
              expectedTemplate)) {
          val contractId = event.getCreated.contractId
          val createArguments = event.getCreated.getCreateArguments
          val valueDiff =
            compareValues(expectedContractId, contractId, "contractId")
          captureOrexpectedArgumentsOpt match {
            case Right(Some(expectedArguments)) =>
              val expVal =
                Value.parseFrom(expectedArguments.toProto.toByteArray)
              valueDiff |+| compareAst(
                toAst(expVal),
                toAst(Value.of(Sum.Record(createArguments))))
            case Right(None) =>
              valueDiff
            case Left(capture) =>
              valueDiff |+| Same(
                capture -> Value(Value.Sum.Record(createArguments)))
          }
        } else {
          Irrelevant
        }
    }

  def apply(expectedTemplate: JavaIdentifier,
            expectedContractId: String,
            expectedArguments: JavaRecord): MessageTester[TreeEvent] =
    apply(expectedTemplate, expectedContractId, Right(Some(expectedArguments)))

  def apply(expectedTemplate: JavaIdentifier,
            expectedContractId: String): MessageTester[TreeEvent] =
    apply(expectedTemplate, expectedContractId, Right(None))

  def apply(expectedTemplate: JavaIdentifier,
            expectedContractId: String,
            capture: String): MessageTester[TreeEvent] =
    apply(expectedTemplate, expectedContractId, Left(capture))
}
