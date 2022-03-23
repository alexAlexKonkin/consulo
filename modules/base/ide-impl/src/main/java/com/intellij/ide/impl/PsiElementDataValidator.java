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

package com.intellij.ide.impl;

import consulo.language.editor.CommonDataKeys;
import consulo.util.dataholder.Key;
import consulo.language.psi.PsiElement;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PsiElementDataValidator implements DataValidator<PsiElement> {
  @Nonnull
  @Override
  public Key<PsiElement> getKey() {
    return CommonDataKeys.PSI_ELEMENT;
  }

  @Nullable
  @Override
  public PsiElement findInvalid(Key<PsiElement> key, PsiElement psiElement, Object dataSource) {
    return psiElement.isValid() ? null : psiElement;
  }
}