/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.utils

import com.daml.extensions.testing.ast.Ast

object AstHelper {
  def prettyPrint(ast: => Ast): String = toJavaPrimitives(ast).toString

  private def toJavaPrimitives(ast: Ast): AnyRef = ast match {
    case Ast.Map(map)     => map.view.mapValues(toJavaPrimitives)
    case Ast.Seq(seq)     => seq.map(toJavaPrimitives)
    case Ast.Value(value) => value
    case Ast.Null         => null
  }
}
