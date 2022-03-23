/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.command.impl;

import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.CommandProcessor;
import com.intellij.openapi.command.undo.BasicUndoableAction;
import consulo.document.DocumentReference;
import consulo.document.DocumentReferenceManager;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * User: anna
 * Date: 11/8/11
 */
public class FinishMarkAction extends BasicUndoableAction {
  private @Nonnull
  final StartMarkAction myStartAction;
  private boolean myGlobal = false;
  private String myCommandName;
  private DocumentReference myReference;

  private FinishMarkAction(DocumentReference reference, @Nonnull StartMarkAction action) {
    super(reference);
    myReference = reference;
    myStartAction = action;
  }

  public void undo() {
  }

  public void redo() {
  }

  public boolean isGlobal() {
    return myGlobal;
  }

  public void setGlobal(boolean isGlobal) {
    myStartAction.setGlobal(isGlobal);
    myGlobal = isGlobal;
  }

  public void setCommandName(String commandName) {
    myStartAction.setCommandName(commandName);
    myCommandName = commandName;
  }

  public String getCommandName() {
    return myCommandName;
  }

  public DocumentReference getAffectedDocument() {
    return myReference;
  }

  public static void finish(final Project project, final Editor editor, @Nullable final StartMarkAction startAction) {
    if (startAction == null) return;
    CommandProcessor.getInstance().executeCommand(project, new Runnable() {
      public void run() {
        DocumentReference reference = DocumentReferenceManager.getInstance().create(editor.getDocument());
        ProjectUndoManager.getInstance(project).undoableActionPerformed(new FinishMarkAction(reference, startAction));
        StartMarkAction.markFinished(project);
      }
    }, "finish", null);
  }
}
