
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

import com.intellij.ide.IdeBundle;
import consulo.language.editor.CommonDataKeys;
import consulo.undoRedo.CommandProcessor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import consulo.project.Project;
import consulo.application.dumb.DumbAware;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

public class PrevSplitAction extends AnAction implements DumbAware {
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    final CommandProcessor commandProcessor = CommandProcessor.getInstance();
    commandProcessor.executeCommand(
      project, new Runnable(){
        public void run() {
          final FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
          manager.setCurrentWindow(manager.getPrevWindow(manager.getCurrentWindow()));
        }
      }, IdeBundle.message("command.go.to.prev.split"), null
    );
  }
  
  public void update(final AnActionEvent event){
    final Project project = event.getData(CommonDataKeys.PROJECT);
    final Presentation presentation = event.getPresentation();
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }
    final FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
    final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    presentation.setEnabled (toolWindowManager.isEditorComponentActive() && manager.isInSplitter() && manager.getCurrentWindow() != null);
  }
}
