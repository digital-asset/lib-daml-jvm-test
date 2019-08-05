package com.digitalasset.testing.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class DumpAppender extends AppenderBase<ILoggingEvent> {
  private File workDir = new File("ft-logs");
  private boolean pruneLogs = false;

  @Override
  public void start() {
    if (pruneLogs) {
      try {
        FileUtils.deleteDirectory(workDir);
      } catch (IOException ignored) {
      }
    }
    super.start();
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    Object[] args = eventObject.getArgumentArray();
    if (args.length == 2 && args[0] instanceof LogEvent && args[1] instanceof String) {
      LogEvent e = (LogEvent) args[0];
      String context = (String) args[1];

      String prefix;
      if (!context.isEmpty()) { prefix = context + "/"; } else { prefix = ""; }

      File file = new File(workDir, prefix + e.filename());
      file.getParentFile().mkdirs();
      try {
        Files.write(
                file.toPath(),
                e.filePrettyMsg().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    } else {
      System.out.println("DumpAppender: context not provided");
    }
  }

  public void setWorkDir(File workDir) {
    this.workDir = workDir;
  }

  public void setPruneLogs(boolean pruneLogs) {
    this.pruneLogs = pruneLogs;
  }
}
