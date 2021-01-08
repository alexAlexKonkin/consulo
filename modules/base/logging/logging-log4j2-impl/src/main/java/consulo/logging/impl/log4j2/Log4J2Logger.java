/*
 * Copyright 2013-2018 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.logging.impl.log4j2;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.util.text.StringUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.logging.LoggerLevel;
import consulo.platform.impl.action.LastActionTracker;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.ThreeState;
import consulo.util.lang.reflect.ReflectionUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2018-08-15
 */
public class Log4J2Logger implements Logger {
  private static Supplier<String> ourApplicationInfoProvider = getInfoProvider();

  private final LoggerContext myLoggerContext;
  private final String myName;

  @Nullable
  private org.apache.logging.log4j.Logger myLogger;

  Log4J2Logger(LoggerContext loggerContext, String name) {
    myLoggerContext = loggerContext;
    myName = name;
  }

  @Nonnull
  private org.apache.logging.log4j.Logger logger() {
    if(myLogger == null) {
      org.apache.logging.log4j.core.Logger logger = myLoggerContext.getLogger(myName);
      myLogger = logger;
      return logger;
    }
    return myLogger;
  }

  @Override
  public void error(Object message) {
    if (message instanceof IdeaLoggingEvent) {
      logger().error(message);
    }
    else {
      error(String.valueOf(message));
    }
  }

  @Override
  public boolean isDebugEnabled() {
    return logger().isDebugEnabled();
  }

  @Override
  public void debug(String message) {
    logger().debug(message);
  }

  @Override
  public void debug(Throwable t) {
    logger().debug("", t);
  }

  @Override
  public void debug(String message, Throwable t) {
    logger().debug(message, t);
  }

  @Override
  public void error(String message, @Nullable Throwable t, String... details) {
    org.apache.logging.log4j.Logger logger = logger();
    
    if (t instanceof ControlFlowException) {
      logger.error(new Throwable("Do not log ProcessCanceledException").initCause(t));
      throw (RuntimeException)t;
    }

    if (t != null && t.getClass().getName().contains("ReparsedSuccessfullyException")) {
      logger.error(new Throwable("Do not log ReparsedSuccessfullyException").initCause(t));
      throw (RuntimeException)t;
    }

    String detailString = StringUtil.join(details, "\n");

    logger.error(message + (!detailString.isEmpty() ? "\nDetails: " + detailString : ""), t);
    logErrorHeader();
    if (t != null && t.getCause() != null) {
      logger.error("Original exception: ", t.getCause());
    }
  }

  private void logErrorHeader() {
    final String info = ourApplicationInfoProvider.get();

    org.apache.logging.log4j.Logger logger = logger();
    
    if (info != null) {
      logger.error(info);
    }

    logger.error("JDK: " + System.getProperties().getProperty("java.version", "unknown"));
    logger.error("VM: " + System.getProperties().getProperty("java.vm.name", "unknown"));
    logger.error("Vendor: " + System.getProperties().getProperty("java.vendor", "unknown"));
    logger.error("OS: " + System.getProperties().getProperty("os.name", "unknown"));

    Application application = ApplicationManager.getApplication();
    // only if app not disposed or not started disposing
    if (application != null && application.getDisposeState().get() == ThreeState.NO) {
      final String lastPreformedActionId = LastActionTracker.ourLastActionId;
      if (lastPreformedActionId != null) {
        logger.error("Last Action: " + lastPreformedActionId);
      }

      CommandProcessor commandProcessor = CommandProcessor.getInstance();
      final String currentCommandName = commandProcessor.getCurrentCommandName();
      if (currentCommandName != null) {
        logger.error("Current Command: " + currentCommandName);
      }
    }
  }

  @Override
  public void info(String message) {
    logger().info(message);
  }

  @Override
  public void info(String message, @Nullable Throwable t) {
    logger().info(message, t);
  }

  @Override
  public void warn(String message, @Nullable Throwable t) {
    logger().warn(message, t);
  }

  @Override
  public void setLevel(@Nonnull LoggerLevel level) throws IllegalAccessException {
    Class callerClass = ReflectionUtil.findCallerClass(1);
    if (callerClass == null) {
      throw new IllegalAccessException("There not caller class");
    }

    PluginDescriptor plugin = PluginManager.getPlugin(callerClass);
    if (plugin == null || !PluginIds.isPlatformPlugin(plugin.getPluginId())) {
      throw new IllegalAccessException("Plugin is not platform: " + plugin);
    }

    org.apache.logging.log4j.core.Logger logger = myLoggerContext.getLogger(myName);

    Level newLevel;
    switch (level) {

      case INFO:
        newLevel = Level.INFO;
        break;
      case WARNING:
        newLevel = Level.WARN;
        break;
      case ERROR:
        newLevel = Level.ERROR;
        break;
      case DEBUG:
        newLevel = Level.DEBUG;
        break;
      case TRACE:
        newLevel = Level.TRACE;
        break;
      default:
        throw new IllegalArgumentException("Wrong level: " + level);
    }

    logger.setLevel(newLevel);
  }

  private static Supplier<String> getInfoProvider() {
    return () -> {
      final ApplicationInfo info = ApplicationInfo.getInstance();
      return info.getFullApplicationName() + "  " + "Build #" + info.getBuild().asString();
    };
  }
}
