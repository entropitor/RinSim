/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.core.model.time;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verifyNotNull;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.annotation.Nullable;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ComparisonChain;
import com.google.common.primitives.Doubles;

/**
 *
 * @author Rinde van Lon
 */
public final class GCLogMonitor {
  static final long S_TO_NS = 1000000000L;
  static final long HISTORY_LENGTH = 30 * S_TO_NS;
  static final long LOG_PARSER_DELAY = 200L;
  static final int QUEUE_EXPECTED_SIZE = 50;
  static final String FILTER =
      "Total time for which application threads were stopped";

  @Nullable
  private static volatile GCLogMonitor instance;

  LogListener accum;
  long startTimeNS;

  Deque<PauseTime> pauseTimes;

  GCLogMonitor() {
    accum = new LogListener();
    Tailer.create(new File("gclog.txt"), accum, LOG_PARSER_DELAY);

    pauseTimes = new ConcurrentLinkedDeque<>();
    startTimeNS = ManagementFactory.getRuntimeMXBean().getStartTime() * 1000000;
  }

  public static GCLogMonitor getInstance() {
    if (instance != null) {
      return instance;
    }
    return instance = new GCLogMonitor();
  }

  boolean hasSurpassed(long timeNs) {
    return !pauseTimes.isEmpty()
        && pauseTimes.peekLast().getTime() > timeNs - startTimeNS;
  }

  long getPauseTimeInInterval(long ts1, long ts2) {
    final long vmt1 = ts1 - startTimeNS;
    final long vmt2 = ts2 - startTimeNS;
    long duration = 0;

    // iterator starts with oldest times
    for (final PauseTime pt : pauseTimes) {
      if (vmt2 > pt.getTime()) {
        break;
      }
      if (vmt1 > pt.getTime()) {
        duration += pt.getDuration();
      }
    }
    return duration;
  }

  class LogListener extends TailerListenerAdapter {

    LogListener() {}

    @Override
    public void handle(@Nullable String line) {
      if (line != null && line.contains(FILTER)) {
        final String[] parts = line.split(": ");

        // System.out.println(parts[0] + " " + parts[2]);

        final Double t = Doubles.tryParse(parts[0]);
        if (t == null) {
          return;
        }
        final long time = (long) (S_TO_NS * t);

        final Double d =
            Doubles.tryParse(parts[2].substring(0, parts[2].length() - 8));
        if (d == null) {
          return;
        }
        final long duration = (long) (S_TO_NS * d);

        checkState(pauseTimes.peekLast().getTime() <= time,
            "Time inconsistency detected in the gc log.");
        // add new info at the back
        pauseTimes.add(PauseTime.create(time, duration));

        // remove old info at the front
        while (time - pauseTimes.peekFirst().getTime() > HISTORY_LENGTH) {
          pauseTimes.pollFirst();
        }
        // System.out.println("queue size: " + pauseTimes.size());
      }
    }
  }

  @AutoValue
  abstract static class PauseTime implements Comparable<PauseTime> {
    abstract long getTime();

    abstract long getDuration();

    @Override
    public int compareTo(@Nullable PauseTime o) {
      final PauseTime other = verifyNotNull(o);
      return ComparisonChain.start().compare(getTime(), other.getTime())
          .compare(getDuration(), other.getDuration())
          .result();
    }

    static PauseTime create(long time, long duration) {
      return new AutoValue_GCLogMonitor_PauseTime(time, duration);
    }
  }
}
