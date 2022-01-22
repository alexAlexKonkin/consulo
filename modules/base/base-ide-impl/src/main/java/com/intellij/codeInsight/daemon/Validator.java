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

package com.intellij.codeInsight.daemon;

import consulo.language.psi.PsiElement;
import org.intellij.lang.annotations.MagicConstant;
import javax.annotation.Nonnull;

/**
 * @author Maxim.Mossienko
 */
public interface Validator<T extends PsiElement> {
  interface ValidationHost {
    int WARNING = 0;
    int ERROR = 1;
    int INFO = 2;

    enum ErrorType {
      WARNING, ERROR, INFO
    }

    /**
     * @deprecated Use {@link #addMessage(PsiElement, String, ErrorType)} instead
     */
    void addMessage(PsiElement context, String message, @MagicConstant(intValues = {INFO, WARNING, ERROR}) int type);
    void addMessage(PsiElement context, String message, @Nonnull ErrorType type);
  }


  void validate(@Nonnull T context, @Nonnull ValidationHost host);
}
