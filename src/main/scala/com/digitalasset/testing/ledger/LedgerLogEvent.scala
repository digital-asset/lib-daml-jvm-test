/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger

import com.digitalasset.ledger.api.v1.commands.Command.Command
import com.digitalasset.ledger.api.v1.commands.Command.Command.{
  Create,
  Exercise
}
import com.digitalasset.ledger.api.v1.commands.{CreateCommand, ExerciseCommand}
import com.digitalasset.ledger.api.v1.event.{CreatedEvent, ExercisedEvent}
import com.digitalasset.ledger.api.v1.transaction.TreeEvent
import com.digitalasset.ledger.api.v1.transaction.TreeEvent.Kind
import com.digitalasset.ledger.api.v1.value.{Record, Value}
import com.digitalasset.testing.Patterns.{Fqn, IdentifierOps}
import com.digitalasset.testing.ast.toAst
import com.digitalasset.testing.ledger.Utils._
import com.digitalasset.testing.logging.{LogEvent, _}
import com.digitalasset.testing.utils.AstHelper
import scalapb.json4s.JsonFormat

sealed trait LedgerLogEvent extends LogEvent

case class CommandEvent(cmdId: String, party: String, cmd: Command)
    extends LedgerLogEvent {
  override lazy val consolePrettyMsg: String = s">>> CMD $party " + (cmd match {
    case Create(CreateCommand(Some(Fqn(template)), createArguments)) =>
      s"Create $template\n${createArguments.pretty}---"
    case Exercise(
        ExerciseCommand(Some(Fqn(template)),
                        contractId,
                        choice,
                        choiceArgument)) =>
      s"Exercise $template#$choice on $contractId\n${choiceArgument.pretty}---"
    case _ => UNKNOWN
  })

  override lazy val filename: String =
    s"ledger/$party/commands/$now-$postfix.txt"
  override lazy val filePrettyMsg: String = cmd match {
    case Create(CreateCommand(Some(Fqn(template)), createArguments)) =>
      s"""Create contract:
         |Template: $template
         |Payload:
         |${createArguments.pretty}---
         |
         |Protobuf:
         |${createArguments.proto}
       """.stripMargin
    case Exercise(
        ExerciseCommand(Some(Fqn(template)),
                        contractId,
                        choice,
                        choiceArgument)) =>
      s"""Exercise choice:
         |Template: $template
         |Choice: $choice
         |Contract id: $contractId
         |Payload:
         |${choiceArgument.pretty}---
         |
         |Protobuf:
         |${choiceArgument.proto}
       """.stripMargin
    case _ => UNKNOWN
  }

  private def postfix = cmd match {
    case Create(CreateCommand(Some(templateId), _)) =>
      s"create-${templateId.entName}"
    case Exercise(ExerciseCommand(Some(templateId), _, choice, _)) =>
      s"exercise-${templateId.entName}#$choice"
    case _ => UNKNOWN
  }
}

case class ObserveEvent(party: String, event: TreeEvent)
    extends LedgerLogEvent {
  override lazy val consolePrettyMsg
    : String = s"<<< EVT $party " + (event.kind match {
    case Kind.Created(
        CreatedEvent(_,
                     contractId,
                     Some(Fqn(template)),
                     createArguments,
                     _,
                     _,
                     _,
                     _,
                     _)) =>
      s"Created $template $contractId\n${createArguments.pretty}---"
    case Kind.Exercised(
        ExercisedEvent(_,
                       contractId,
                       Some(Fqn(template)),
                       _,
                       choice,
                       choiceArgument,
                       _,
                       _,
                       _,
                       _,
                       _)) =>
      s"Exercised $template#$choice $contractId\n${choiceArgument.pretty}---"
    case _ => UNKNOWN
  })

  override lazy val filename: String = s"ledger/$party/events/$now-$postfix.txt"
  override lazy val filePrettyMsg: String = event.kind match {
    case Kind.Created(
        CreatedEvent(eventId,
                     contractId,
                     Some(Fqn(template)),
                     createArguments,
                     _,
                     _,
                     _,
                     _,
                     _)) =>
      s"""Created contract:
         |Event id: $eventId
         |Template: $template
         |Contract id: $contractId
         |Payload:
         |${createArguments.pretty}---
         |
         |Protobuf:
         |${createArguments.proto}
       """.stripMargin
    case Kind.Exercised(
        ExercisedEvent(eventId,
                       contractId,
                       Some(Fqn(template)),
                       _,
                       choice,
                       choiceArgument,
                       _,
                       consuming,
                       _,
                       _,
                       _)) =>
      s"""Exercised choice:
         |Event id: $eventId
         |Template: $template
         |Choice: $choice
         |Consuming: $consuming
         |Contract id: $contractId
         |Payload:
         |${choiceArgument.pretty}---
         |
         |Protobuf:
         |${choiceArgument.proto}
       """.stripMargin
    case _ =>
      UNKNOWN
  }

  private def postfix = event.kind match {
    case Kind.Created(CreatedEvent(_, _, Some(templateId), _, _, _, _, _, _)) =>
      s"created-${templateId.entName}"
    case Kind.Exercised(
        ExercisedEvent(_,
                       _,
                       Some(templateId),
                       _,
                       choice,
                       _,
                       _,
                       consuming,
                       _,
                       _,
                       _)) =>
      s"exercised-${templateId.entName}#$choice${if (consuming) "!" else ""}"
    case _ =>
      UNKNOWN
  }
}

object Utils {
  implicit class OptionValueOpts(val obj: Option[Value]) {
    def pretty: String = obj.fold(EMPTY)(a => AstHelper.prettyPrint(toAst(a)))
    def proto: String = obj.fold(EMPTY)(JsonFormat.toJsonString)
  }
  implicit class OptionRecordOpts(val obj: Option[Record]) {
    def pretty: String =
      obj.fold(EMPTY)(a =>
        AstHelper.prettyPrint(toAst(Value(Value.Sum.Record(a)))))
    def proto: String = obj.fold(EMPTY)(JsonFormat.toJsonString)
  }
}
