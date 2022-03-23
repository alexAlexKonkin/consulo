/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.diff.actions.impl;

import com.intellij.diff.DiffContext;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.diff.util.DiffUtil;
import com.intellij.ide.actions.EditSourceAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.navigation.Navigatable;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OpenInEditorAction extends EditSourceAction implements DumbAware {
  public static final Key<OpenInEditorAction> KEY = Key.create("DiffOpenInEditorAction");

  @Nullable private final Runnable myAfterRunnable;

  public OpenInEditorAction(@javax.annotation.Nullable Runnable afterRunnable) {
    ActionUtil.copyFrom(this, "EditSource");
    myAfterRunnable = afterRunnable;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    if (!e.isFromActionToolbar()) {
      e.getPresentation().setEnabledAndVisible(true);
      return;
    }

    DiffRequest request = e.getData(DiffDataKeys.DIFF_REQUEST);
    DiffContext context = e.getData(DiffDataKeys.DIFF_CONTEXT);

    if (DiffUtil.isUserDataFlagSet(DiffUserDataKeys.GO_TO_SOURCE_DISABLE, request, context)) {
      e.getPresentation().setVisible(false);
      e.getPresentation().setEnabled(false);
    }

    if (e.getData(CommonDataKeys.PROJECT) == null) {
      e.getPresentation().setVisible(true);
      e.getPresentation().setEnabled(false);
      return;
    }

    Navigatable[] navigatables = e.getData(DiffDataKeys.NAVIGATABLE_ARRAY);
    if (navigatables == null || !ContainerUtil.exists(navigatables, Navigatable::canNavigate)) {
      e.getPresentation().setVisible(true);
      e.getPresentation().setEnabled(false);
      return;
    }

    e.getPresentation().setEnabledAndVisible(true);
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;

    Navigatable[] navigatables = e.getData(DiffDataKeys.NAVIGATABLE_ARRAY);
    if (navigatables == null) return;

    openEditor(project, navigatables);
  }

  public void openEditor(@Nonnull Project project, @Nonnull Navigatable navigatable) {
    openEditor(project, new Navigatable[]{navigatable});
  }

  public void openEditor(@Nonnull Project project, @Nonnull Navigatable[] navigatables) {
    boolean success = false;
    for (Navigatable navigatable : navigatables) {
      if (navigatable.canNavigate()) {
        navigatable.navigate(true);
        success = true;
      }
    }
    if (success && myAfterRunnable != null) myAfterRunnable.run();
  }
}
