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

      case (Ast.Seq(exp), Ast.Seq(act)) if exp.size == act.size =>
        (exp zip act).zipWithIndex
          .map { case ((e, a), ix) => compareAst(e, a, s"$path[$ix]") }
          .toList
          .suml

      case (Ast.Seq(exp), Ast.Seq(act)) =>
        Diff(
          s"$path: Expected list of size ${exp.size}, but got list of size ${act.size}")

      case (Ast.Value(expectedCtor),
            Ast.Constructor(actualCtor, actualParams)) =>
        compareCtor(expectedCtor, Ast.Null, actualCtor, actualParams, path)

      case (Ast.Constructor(expectedCtor, expectedParams),
            Ast.Value(actualCtor)) =>
        compareCtor(expectedCtor, expectedParams, actualCtor, Ast.Null, path)

      case (Ast.Constructor(expectedCtor, expectedParams),
            Ast.Constructor(actualCtor, actualParams)) =>
        compareCtor(expectedCtor,
                    expectedParams,
                    actualCtor,
                    actualParams,
                    path)

      case (Ast.Map(expMap), Ast.Map(actMap)) =>
        (expMap.keySet ++ actMap.keySet).toList.sorted.foldMap { key =>
          (expMap.get(key), actMap.get(key)) match {
            case (Some(expV), Some(actV)) =>
              compareAst(expV, actV, s"$path.$key")
            case (None, Some(actV)) => compareAst(Ast.Null, actV, s"$path.$key")
            case (Some(expV), None) => compareAst(expV, Ast.Null, s"$path.$key")
            case (None, None)       => Error("Internal bug")
          }
        }

      case (Ast.Value(exp), Ast.Value(act)) =>
        compareValues(exp, act, path)

      case (Ast.Null, Ast.Null) =>
        Same()

      case (Ast.Null, Ast.Value(act)) =>
        Diff(s"$path: Value [$act] not expected")

      case (Ast.Value(exp), Ast.Null) =>
        Diff(s"$path: Value [$exp] expected, but it is missing")

      case (Ast.Null, _) =>
        Diff(s"$path: Value [$actual] not expected")

      case (_, Ast.Null) =>
        Diff(s"$path: Value [$expected] expected, but it is missing")

      case _ =>
        Error(s"$path: Unexpected or unsupported case: [$expected], [$actual]")
    }

  private def compareCtor(expectedCtor: String,
                          expectedParams: Ast,
                          actualCtor: String,
                          actualParams: Ast,
                          path: String): ComparisonResult =
    (expectedCtor, expectedParams, actualCtor, actualParams) match {
      case (e, ep, a, ap) if e != a =>
        Diff(
          s"$path: Actual constructor / singleton map $a => $ap doesn't match expected constructor / singleton map $e => $ep")
      case (c, e, _, a) => compareAst(e, a, s"$path.$c")
    }
}
