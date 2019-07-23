/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.logging

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, StandardOpenOption}

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.apache.commons.io.FileUtils

class DumpAppender extends AppenderBase[ILoggingEvent] {
  var workDir: File = new File("ft-logs")
  def setWorkDir(workDir: String): Unit = { this.workDir = new File(workDir) }

  var pruneLogs: Boolean = false
  def setPruneLogs(pruneLogs: Boolean): Unit = { this.pruneLogs = pruneLogs }

  override def start(): Unit = {
    if (pruneLogs) FileUtils.deleteDirectory(workDir)
    super.start()
  }

  override def append(eventObject: ILoggingEvent): Unit = {
    eventObject.getArgumentArray match {
      case Array(e: LogEvent, context: String) =>
        val prefix =
          if (context != null && context.nonEmpty) s"$context/" else ""
        val file = new File(workDir, s"$prefix${e.filename}")
        file.getParentFile.mkdirs()
        Files.write(
          file.toPath,
          e.filePrettyMsg.getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.CREATE,
          StandardOpenOption.WRITE,
          StandardOpenOption.TRUNCATE_EXISTING
        )

      case _ => println(s"DumpAppender: context not provided")
    }
  }
}
