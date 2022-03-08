/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.projectRoots.impl;

import consulo.ide.ServiceManager;
import com.intellij.util.NotNullFunction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.concurrent.Future;

/**
 * @author nik
 */
@Deprecated
public abstract class JdkVersionDetector {
  @Nonnull
  public static JdkVersionDetector getInstance() {
    return ServiceManager.getService(JdkVersionDetector.class);
  }

  @Nullable
  public abstract String detectJdkVersion(String homePath);

  @Nullable
  public abstract String detectJdkVersion(String homePath, NotNullFunction<Runnable, Future<?>> actionRunner);

  @javax.annotation.Nullable
  public abstract String readVersionFromProcessOutput(String homePath, String[] command, String versionLineMarker,
                                                      NotNullFunction<Runnable, Future<?>> actionRunner);
}
