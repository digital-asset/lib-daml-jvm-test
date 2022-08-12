/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.ledger

import com.daml.extensions.testing.Patterns.IdentifierOps
import com.daml.extensions.testing.ast.toAst
import com.daml.extensions.testing.ledger.Utils._
import com.daml.extensions.testing.logging.LogEvent
import com.daml.extensions.testing.utils.AstHelper
import com.daml.ledger.javaapi.data._
import com.google.protobuf.TextFormat

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

sealed trait LedgerLogEvent extends LogEvent

case class CommandEvent(cmdId: String, party: String, cmd: Command)
    extends LedgerLogEvent {
  override lazy val consolePrettyMsg: String = s">>> CMD $party " + (cmd match {
    case cc: CreateCommand =>
      s"Create ${cc.getTemplateId.fqn}\n${cc.getCreateArguments.pretty}---"
    case ec: ExerciseCommand =>
      s"Exercise ${ec.getTemplateId.fqn}#${ec.getChoice} on ${ec.getContractId}\n${ec.getChoiceArgument.pretty}---"
    case _ => UNKNOWN
  })

  override lazy val filename: String =
    s"ledger/$party/commands/$now-$postfix.txt"
  override lazy val filePrettyMsg: String = cmd match {
    case cc: CreateCommand =>
      s"""Create contract:
         |Template: ${cc.getTemplateId.fqn}
         |Payload:
         |${cc.getCreateArguments.pretty}---
         |
         |Protobuf:
         |${cc.getCreateArguments.proto}
       """.stripMargin
    case ec: ExerciseCommand =>
      s"""Exercise choice:
         |Template: ${ec.getTemplateId.fqn}
         |Choice: ${ec.getChoice}
         |Contract id: ${ec.getContractId}
         |Payload:
         |${ec.getChoiceArgument.pretty}---
         |
         |Protobuf:
         |${ec.getChoiceArgument.proto}
       """.stripMargin
    case _ => UNKNOWN
  }

  private def postfix = cmd match {
    case cc: CreateCommand => s"create-${cc.getTemplateId.entName}"
    case ec: ExerciseCommand =>
      s"exercise-${ec.getTemplateId.entName}#${ec.getChoice}"
    case _ => UNKNOWN
  }
}

case class ObserveEvent(party: String, event: TreeEvent)
    extends LedgerLogEvent {
  override lazy val consolePrettyMsg
    : String = s"<<< EVT $party " + (event match {
    case ce: CreatedEvent =>
      s"Created ${ce.getTemplateId.fqn} ${ce.getContractId}\n${ce.getArguments.pretty}---"
    case ee: ExercisedEvent =>
      s"Exercised ${ee.getTemplateId.fqn}#${ee.getChoice} ${ee.getContractId}\n${ee.getChoiceArgument.pretty}---"
    case _ => UNKNOWN
  })

  override lazy val filename: String = s"ledger/$party/events/$now-$postfix.txt"
  override lazy val filePrettyMsg: String = event match {
    case ce: CreatedEvent =>
      s"""Created contract:
         |Event id: ${ce.getEventId}
         |Template: ${ce.getTemplateId.fqn}
         |Contract id: ${ce.getContractId}
         |Payload:
         |${ce.getArguments.pretty}---
         |
         |Protobuf:
         |${ce.getArguments.proto}
       """.stripMargin
    case ee: ExercisedEvent =>
      s"""Exercised choice:
         |Event id: ${ee.getEventId}
         |Template: ${ee.getTemplateId.fqn}
         |Choice: ${ee.getChoice}
         |Consuming: ${ee.isConsuming}
         |Contract id: ${ee.getContractId}
         |Payload:
         |${ee.getChoiceArgument.pretty}---
         |
         |Protobuf:
         |${ee.getChoiceArgument.proto}
       """.stripMargin
    case _ =>
      UNKNOWN
  }

  private def postfix = event match {
    case ce: CreatedEvent => s"created-${ce.getTemplateId.entName}"
    case ee: ExercisedEvent =>
      s"exercised-${ee.getTemplateId.entName}#${ee.getChoice}${if (ee.isConsuming) "!"
      else ""}"
    case _ => UNKNOWN
  }
}

object Utils {
  val UNKNOWN: String = "UNKNOWN"
  val EMPTY: String = "<EMPTY>"

  private val FORMATTER = new DateTimeFormatterBuilder()
    .appendValue(ChronoField.YEAR, 4)
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .appendLiteral('-')
    .appendValue(ChronoField.HOUR_OF_DAY, 2)
    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
    .appendFraction(ChronoField.NANO_OF_SECOND, 3, 3, true)
    .toFormatter

  def now: String = FORMATTER.format(ZonedDateTime.now())

  implicit class ValueOpts(val value: Value) {
    def pretty: String = AstHelper.prettyPrint(toAst(value))
    def proto: String = TextFormat.shortDebugString(value.toProto)
  }

  implicit class OptionValueOpts(val obj: Option[Value]) {
    def pretty: String = obj.fold(EMPTY)(v => AstHelper.prettyPrint(toAst(v)))
    def proto: String =
      obj.fold(EMPTY)(v => TextFormat.shortDebugString(v.toProto))
  }

  implicit class RecordOpts(record: DamlRecord) {
    def pretty: String = AstHelper.prettyPrint(toAst(record))
    def proto: String = TextFormat.shortDebugString(record.toProto)
  }

  implicit class OptionRecordOpts(val obj: Option[DamlRecord]) {
    def pretty: String = obj.fold(EMPTY)(r => AstHelper.prettyPrint(toAst(r)))
    def proto: String =
      obj.fold(EMPTY)(r => TextFormat.shortDebugString(r.toProto))
  }
}
