// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion.actions;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import consulo.language.editor.completion.CompletionType;
import com.intellij.codeInsight.hint.HintManagerImpl;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.ide.impl.psi.util.PsiUtilBase;
import javax.annotation.Nonnull;

import java.awt.event.InputEvent;

/**
 * @author peter
 */
public abstract class BaseCodeCompletionAction extends DumbAwareAction implements HintManagerImpl.ActionToIgnore {

  protected BaseCodeCompletionAction() {
    setEnabledInModalContext(true);
    setInjectedContext(true);
  }

  protected void invokeCompletion(AnActionEvent e, CompletionType type, int time) {
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    assert editor != null;
    Project project = editor.getProject();
    assert project != null;
    InputEvent inputEvent = e.getInputEvent();
    createHandler(type, true, false, true).invokeCompletion(project, editor, time, inputEvent != null && inputEvent.getModifiers() != 0);
  }

  @Nonnull
  public CodeCompletionHandlerBase createHandler(@Nonnull CompletionType completionType, boolean invokedExplicitly, boolean autopopup, boolean synchronous) {

    return new CodeCompletionHandlerBase(completionType, invokedExplicitly, autopopup, synchronous);
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    e.getPresentation().setEnabled(false);

    Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    if (editor == null) return;

    Project project = editor.getProject();
    PsiFile psiFile = project == null ? null : PsiUtilBase.getPsiFileInEditor(editor, project);
    if (psiFile == null) return;

    if (!ApplicationManager.getApplication().isHeadlessEnvironment() && !editor.getContentComponent().isShowing()) return;
    e.getPresentation().setEnabled(true);
  }
}
