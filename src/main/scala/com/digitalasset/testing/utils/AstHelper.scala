/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.utils

import com.digitalasset.testing.ast.Ast
import org.yaml.snakeyaml.DumperOptions.FlowStyle
import org.yaml.snakeyaml.{DumperOptions, Yaml}

import scala.collection.JavaConverters._

object AstHelper {
  def prettyPrint(ast: => Ast): String = getYaml.dump(toJavaPrimitives(ast))

  private def toJavaPrimitives(ast: Ast): AnyRef = ast match {
    case Ast.Map(map)     => map.mapValues(toJavaPrimitives).asJava
    case Ast.Seq(seq)     => seq.map(toJavaPrimitives).asJava
    case Ast.Value(value) => value
    case Ast.Null         => null
  }

  private def getYaml = {
    val opts = new DumperOptions()
    opts.setDefaultFlowStyle(FlowStyle.BLOCK)
    new Yaml(opts)
  }
}
