/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.store
import java.io.File

import com.daml.extensions.testing.store.DefaultDictionary.RootPath
import grizzled.slf4j.Logging

import scala.io.Source

class DefaultDictionary extends ReferenceDictionary with Logging {
  private val dictionaries: Map[String, Map[String, String]] = initialize

  override def get(dictionary: String, key: String): String =
    dictionaries
      .getOrElse(dictionary,
                 throw new IllegalArgumentException(
                   s"Can't find dictionary [$dictionary]"))
      .getOrElse(key,
                 throw new IllegalArgumentException(
                   s"Can't find key [$key] in dictionary [$dictionary]"))

  private def initialize: Map[String, Map[String, String]] = {
    val files = for {
      resource <- Option(getClass.getResource(RootPath))
      fileList <- Option(new File(resource.getPath).listFiles)
    } yield fileList

    val (good, bad) = files.toList.flatten
      .partition(_.getName.matches("^[a-z][a-zA-Z0-9_-]*\\.csv$"))

    if (bad.nonEmpty) {
      warn(
        s"There are files in dictionary folder that don't correspond to correct naming convention:\n" +
          bad.mkString("\n"))
    }

    good
      .map(file => file.getName.replaceAll("\\.csv$", "") -> parseFile(file))
      .toMap
  }

  private def parseFile(file: File): Map[String, String] = {
    val src = Source.fromFile(file)
    try {
      val entries = for {
        line <- src.getLines() if line.contains(",")
        Array(key, value) = line.split(",", 2)
      } yield key -> value
      entries.toMap
    } finally {
      src.close()
    }
  }
}

object DefaultDictionary {
  val RootPath = "/dictionary"
}
