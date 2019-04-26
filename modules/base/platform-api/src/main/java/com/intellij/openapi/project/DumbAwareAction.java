/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.openapi.project;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import consulo.annotations.DeprecationInfo;
import consulo.ui.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.migration.SwingImageRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.function.Consumer;

/**
 * @author nik
 */
public abstract class DumbAwareAction extends AnAction implements DumbAware {
  @Nonnull
  public static DumbAwareAction create(@Nonnull Consumer<? super AnActionEvent> actionPerformed) {
    return new DumbAwareAction() {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        actionPerformed.accept(e);
      }
    };
  }

  @Nonnull
  public static DumbAwareAction create(@Nullable String text, @Nonnull Consumer<? super AnActionEvent> actionPerformed) {
    return new DumbAwareAction(text) {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        actionPerformed.accept(e);
      }
    };
  }

  protected DumbAwareAction() {
  }

  protected DumbAwareAction(SwingImageRef icon) {
    super(icon);
  }

  protected DumbAwareAction(Image icon) {
    super(icon);
  }

  @Deprecated
  @DeprecationInfo("Use contructor with ui image")
  protected DumbAwareAction(Icon icon) {
    super(icon);
  }

  protected DumbAwareAction(@Nullable String text) {
    super(text);
  }

  protected DumbAwareAction(@Nullable String text, @Nullable String description, @Nullable SwingImageRef icon) {
    super(text, description, icon);
  }

  protected DumbAwareAction(@Nullable String text, @Nullable String description, @Nullable Image icon) {
    super(text, description, icon);
  }

  @Deprecated
  @DeprecationInfo("Use contructor with ui image")
  protected DumbAwareAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
    super(text, description, icon);
  }
}