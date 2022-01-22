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
package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import consulo.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import consulo.language.util.IncorrectOperationException;
import javax.annotation.Nonnull;

/**
 * @author peter
*/
class RenameFileReferenceIntentionAction implements IntentionAction, LocalQuickFix {
  private final String myExistingElementName;
  private final FileReference myFileReference;

  public RenameFileReferenceIntentionAction(final String existingElementName, final FileReference fileReference) {
    myExistingElementName = existingElementName;
    myFileReference = fileReference;
  }

  @Override
  @Nonnull
  public String getText() {
    return CodeInsightBundle.message("rename.file.reference.text", myExistingElementName);
  }

  @Override
  @Nonnull
  public String getName() {
    return getText();
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return CodeInsightBundle.message("rename.file.reference.family");
  }

  @Override
  public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
    if (isAvailable(project, null, null)) {
      new WriteCommandAction(project) {
        @Override
        protected void run(Result result) throws Throwable {
          invoke(project, null, descriptor.getPsiElement().getContainingFile());
        }
      }.execute();
    }
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
    myFileReference.handleElementRename(myExistingElementName);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
