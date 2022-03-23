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
package com.intellij.ide.actions;

import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.application.dumb.DumbAware;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

/**
 * @author yole
 */
public abstract class SplitterActionBase extends AnAction implements DumbAware {
  public void update(final AnActionEvent event) {
    final Project project = event.getData(CommonDataKeys.PROJECT);
    final Presentation presentation = event.getPresentation();
    boolean enabled;
    if (project == null) {
      enabled = false;
    }
    else {
      enabled = isActionEnabled(project);
    }
    if (ActionPlaces.isPopupPlace(event.getPlace())) {
      presentation.setVisible(enabled);
    }
    else {
      presentation.setEnabled(enabled);
    }
  }

  protected boolean isActionEnabled(Project project) {
    final FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
    return fileEditorManager.isInSplitter();
  }
}
