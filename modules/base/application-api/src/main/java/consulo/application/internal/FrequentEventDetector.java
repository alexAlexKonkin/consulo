/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.application.internal;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;

import consulo.util.lang.ExceptionUtil;
import jakarta.annotation.Nonnull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author peter
 */
public class FrequentEventDetector {
  private static final Logger LOG = Logger.getInstance(FrequentEventDetector.class);

  public enum Level {INFO, WARN, ERROR}

  private long myStartedCounting = System.currentTimeMillis();
  private final AtomicInteger myEventsPosted = new AtomicInteger();
  private final AtomicInteger myLastTraceId = new AtomicInteger();
  private final Map<String, Integer> myRecentTraces = new LinkedHashMap<>() {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
      return size() > 50;
    }
  };
  private final int myEventCountThreshold;
  private final int myTimeSpanMs;
  private final Level myLevel;
  private static boolean enabled = true;

  public FrequentEventDetector(int eventCountThreshold, int timeSpanMs) {
    this(eventCountThreshold, timeSpanMs, Level.INFO);
  }

  public FrequentEventDetector(int eventCountThreshold, int timeSpanMs, @Nonnull Level level) {
    myEventCountThreshold = eventCountThreshold;
    myTimeSpanMs = timeSpanMs;
    myLevel = level;
  }

  public void eventHappened(@Nonnull Object event) {
    if (!enabled) return;
    if (myEventsPosted.incrementAndGet() > myEventCountThreshold) {
      boolean shouldLog = false;

      synchronized (myEventsPosted) {
        if (myEventsPosted.get() > myEventCountThreshold) {
          long timeNow = System.currentTimeMillis();
          shouldLog = timeNow - myStartedCounting < myTimeSpanMs;
          myEventsPosted.set(0);
          myStartedCounting = timeNow;
        }
      }

      if (shouldLog) {
        String trace = ExceptionUtil.getThrowableText(new Throwable());
        boolean logTrace;
        int traceId;
        synchronized (myEventsPosted) {
          Integer existingTraceId = myRecentTraces.get(trace);
          logTrace = existingTraceId == null;
          if (logTrace) {
            myRecentTraces.put(trace, traceId = myLastTraceId.incrementAndGet());
          }
          else {
            traceId = existingTraceId;
          }
        }

        String message = "Too many events posted, #" + traceId  + ". Event: "+event +
                         (logTrace ? "\n" + trace : "");
        if (myLevel == Level.INFO) {
          LOG.info(message);
        }
        else if (myLevel == Level.WARN) {
          LOG.warn(message);
        }
        else {
          LOG.error(message);
        }
      }
    }
  }

  public static void disableUntil(@Nonnull Disposable reenable) {
    enabled = false;
    Disposer.register(reenable, () -> enabled = true);
  }
}
