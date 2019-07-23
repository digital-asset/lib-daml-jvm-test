/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing

import com.digitalasset.ledger.api.v1.value.Identifier

import scala.util.matching.Regex

object Patterns {
  val varName = "[a-z][a-zA-Z0-9_-]*"

  // interpolators
  val XegerMatchRegex: Regex = "\\{XEGER:((?:\\\\[{}\\\\]|[^{}\\\\])+?)\\}".r
  val ReferVariableRegex: Regex = s"\\{($varName)\\}".r
  val DictionaryRegex: Regex = s"\\{($varName):($varName)\\}".r

  // egress
  val IgnoreRegex: Regex = "\\{IGNORE\\}".r
  val CaptureVariableRegex: Regex = s"\\{CAPTURE:(.+?)\\}".r
  val RegexMatchRegex: Regex = "\\{REGEX:((?:\\\\[{}\\\\]|[^{}\\\\])+?)\\}".r

  def unescape(pattern: String): String =
    pattern.replaceAll("""\\([{}\\])""", "$1")

  /* Unapply patterns, used in pattern matching */

  object Fqn {
    def unapply(identifier: Identifier): Option[String] = Some(identifier.fqn)
  }

  /* Rich objects */

  implicit class IdentifierOps(id: Identifier) {
    def fqn: String =
      if (id.name != "") id.name else s"${id.moduleName}.${id.entityName}"
    def entName: String =
      if (id.name != "") id.name.split('.').last else id.entityName
  }

  val SomeCtor = "Some"
  val NoneCtor = "None"
}
