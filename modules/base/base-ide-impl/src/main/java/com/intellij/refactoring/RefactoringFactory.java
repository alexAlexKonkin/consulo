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
package com.intellij.refactoring;

import com.intellij.openapi.components.ServiceManager;
import consulo.language.psi.PsiElement;
import consulo.project.Project;

/**
 * @author dsl
 */
public abstract class RefactoringFactory {
  public static RefactoringFactory getInstance(Project project) {
    return ServiceManager.getService(project, RefactoringFactory.class);
  }

  public abstract RenameRefactoring createRename(PsiElement element, String newName);
  public abstract RenameRefactoring createRename(PsiElement element, String newName, boolean searchInComments, boolean searchInNonJavaFiles);

  public abstract SafeDeleteRefactoring createSafeDelete(PsiElement[] elements);
}
