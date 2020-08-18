/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.comparator

import com.daml.ledger.javaapi.data.{ContractId, Identifier => JavaIdentifier}
import com.digitalasset.testing.Patterns._
import com.digitalasset.testing.ast.Ast
import com.digitalasset.testing.comparator.MessageTester._
import scalaz.std.list.listInstance
import scalaz.syntax.foldable.ToFoldableOps

package object ledger {

  def compareValues(expected: String,
                    actual: String,
                    path: String): ComparisonResult =
    expected match {
      case RegexMatchRegex(pattern) =>
        if (actual matches unescape(pattern))
          Same()
        else
          Diff(
            s"$path: Actual value [$actual] doesn't match against the expected pattern [$pattern]")

      case IgnoreRegex()              => Same()
      case CaptureVariableRegex(name) => Same(name -> new ContractId(actual))
      case `actual`                   => Same()
      case _                          => Diff(s"$path: Expected [$expected] but got [$actual]")
    }

  def compareAst(expected: Ast,
                 actual: Ast,
                 path: String = "ROOT"): ComparisonResult =
    (expected, actual) match {
      case (Ast.Value(IgnoreRegex()), _) => Same()

      case (Ast.Map(exp), Ast.Map(act))
          if exp.size == act.size && exp.keySet == act.keySet =>
        (exp.toList.sortBy(_._1) zip act.toList.sortBy(_._1)).map {
          case ((expK, expV), (_, actV)) =>
            compareAst(expV, actV, s"$path.$expK")
        }.suml

      case (Ast.Value(constructor), Ast.Map(kv))
          if kv.size == 1 && kv.values.head == Ast.Null =>
        compareValues(constructor, kv.keys.head, s"$path.$constructor")

      case (Ast.Map(expMap), Ast.Map(actMap)) =>
        val expectedKeys = expMap.keySet diff actMap.keySet
        val unexpectedKeys = actMap.keySet diff expMap.keySet
        val expected =
          if (expectedKeys.isEmpty) ""
          else
            expectedKeys.mkString(" Expected keys which where not present: [",
                                  ", ",
                                  "].")
        val unexpected =
          if (unexpectedKeys.isEmpty) ""
          else unexpectedKeys.mkString(" Unexpected keys: [", ", ", "].")
        Diff(s"$path: Records have different key sets.$expected$unexpected")

      case (Ast.Seq(exp), Ast.Seq(act)) if exp.size == act.size =>
        (exp zip act).zipWithIndex
          .map { case ((e, a), ix) => compareAst(e, a, s"$path[$ix]") }
          .toList
          .suml

      case (Ast.Seq(exp), Ast.Seq(act)) =>
        Diff(
          s"$path: Expected list of size ${exp.size}, but got list of size ${act.size}")

      case (Ast.Value(exp), Ast.Value(act)) =>
        compareValues(exp, act, path)

      case (Ast.Null, Ast.Null) =>
        Same()

      case _ =>
        Error(s"$path: Unexpected or unsupported case: $expected, $actual")
    }
}
