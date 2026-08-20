package util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import java.io.ByteArrayOutputStream;
import org.slf4j.LoggerFactory;

/** Captures logback output produced while running an action. */
public final class LogCapture {

  private LogCapture() {}

  public static String capture(Runnable action) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern("%msg%n");
    encoder.start();
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<>();
    appender.setContext(context);
    appender.setEncoder(encoder);
    appender.setOutputStream(buffer);
    appender.start();
    Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
    root.addAppender(appender);
    try {
      action.run();
    } finally {
      root.detachAppender(appender);
      appender.stop();
    }
    return buffer.toString();
  }
}
