// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.completion;

import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionType;
import com.intellij.codeInsight.template.CustomLiveTemplate;
import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.postfix.templates.PostfixLiveTemplate;
import consulo.codeEditor.Editor;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PostfixTemplateCompletionContributor extends CompletionContributor {
  public PostfixTemplateCompletionContributor() {
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new PostfixTemplatesCompletionProvider());
  }

  @Nullable
  public static PostfixLiveTemplate getPostfixLiveTemplate(@Nonnull PsiFile file, @Nonnull Editor editor) {
    PostfixLiveTemplate postfixLiveTemplate = CustomLiveTemplate.EP_NAME.findExtension(PostfixLiveTemplate.class);
    TemplateActionContext templateActionContext = TemplateActionContext.expanding(file, editor);
    return postfixLiveTemplate != null && TemplateManagerImpl.isApplicable(postfixLiveTemplate, templateActionContext) ? postfixLiveTemplate : null;
  }
}
