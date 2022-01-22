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

package com.intellij.lang.folding;

import consulo.language.ast.ASTNode;
import consulo.language.Language;
import consulo.language.LanguageExtension;
import consulo.document.Document;
import consulo.project.DumbService;
import consulo.language.psi.PsiElement;
import consulo.annotation.access.RequiredReadAction;
import consulo.container.plugin.PluginIds;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * @author yole
 * @author Konstantin Bulenkov
 */
public class LanguageFolding extends LanguageExtension<FoldingBuilder> {
  public static final LanguageFolding INSTANCE = new LanguageFolding();

  private LanguageFolding() {
    super(PluginIds.CONSULO_BASE + ".lang.foldingBuilder");
  }

  @Override
  public FoldingBuilder forLanguage(@Nonnull Language l) {
    FoldingBuilder cached = l.getUserData(getLanguageCache());
    if (cached != null) return cached;

    List<FoldingBuilder> extensions = forKey(l);
    FoldingBuilder result;
    if (extensions.isEmpty()) {

      Language base = l.getBaseLanguage();
      if (base != null) {
        result = forLanguage(base);
      }
      else {
        result = getDefaultImplementation();
      }
    }
    else {
      return extensions.size() == 1 ? extensions.get(0) : new CompositeFoldingBuilder(extensions);
    }

    l.putUserData(getLanguageCache(), result);
    return result;
  }

  @Nonnull
  @Override
  public List<FoldingBuilder> allForLanguage(@Nonnull Language l) {
    FoldingBuilder result = forLanguage(l);
    if (result == null) return Collections.emptyList();
    return result instanceof CompositeFoldingBuilder ? ((CompositeFoldingBuilder)result).getAllBuilders() : Collections.singletonList(result);
  }

  @RequiredReadAction
  public static FoldingDescriptor[] buildFoldingDescriptors(FoldingBuilder builder, PsiElement root, Document document, boolean quick) {
    if (!DumbService.isDumbAware(builder) && DumbService.getInstance(root.getProject()).isDumb()) {
      return FoldingDescriptor.EMPTY;
    }

    if (builder instanceof FoldingBuilderEx) {
      return ((FoldingBuilderEx)builder).buildFoldRegions(root, document, quick);
    }
    final ASTNode astNode = root.getNode();
    if (astNode == null || builder == null) {
      return FoldingDescriptor.EMPTY;
    }

    return builder.buildFoldRegions(astNode, document);
  }
}
