/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.codeInspection;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import javax.annotation.Nonnull;

public class LocalQuickFixAsIntentionAdapter implements IntentionAction {
  private final LocalQuickFix myFix;
  @Nonnull
  private final ProblemDescriptor myProblemDescriptor;

  public LocalQuickFixAsIntentionAdapter(@Nonnull LocalQuickFix fix, @Nonnull ProblemDescriptor problemDescriptor) {
    myFix = fix;
    myProblemDescriptor = problemDescriptor;
  }

  @Nonnull
  @Override
  public String getText() {
    return myFix.getName();
  }

  @Nonnull
  @Override
  public String getFamilyName() {
    return myFix.getFamilyName();
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return myProblemDescriptor.getStartElement() != null;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    myFix.applyFix(project, myProblemDescriptor);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}

