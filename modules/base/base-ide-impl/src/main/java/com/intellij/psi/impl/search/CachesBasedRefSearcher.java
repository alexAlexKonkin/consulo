// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.search;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import com.intellij.psi.impl.SyntheticFileSystemItem;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.meta.PsiMetaOwner;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import consulo.application.util.function.Processor;
import javax.annotation.Nonnull;

/**
 * @author max
 */
public class CachesBasedRefSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
  public CachesBasedRefSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@Nonnull ReferencesSearch.SearchParameters p, @Nonnull Processor<? super PsiReference> consumer) {
    final PsiElement refElement = p.getElementToSearch();
    boolean caseSensitive = refElement.getLanguage().isCaseSensitive();

    String text = null;
    if (refElement instanceof PsiFileSystemItem && !(refElement instanceof SyntheticFileSystemItem)) {
      final VirtualFile vFile = ((PsiFileSystemItem)refElement).getVirtualFile();
      if (vFile != null) {
        String fileNameWithoutExtension = vFile.getNameWithoutExtension();
        text = fileNameWithoutExtension.isEmpty() ? vFile.getName() : fileNameWithoutExtension;
      }
      // We must not look for file references with the file language's case-sensitivity, 
      // since case-sensitivity of the references themselves depends either on file system 
      // or on the rules of the language of reference
      caseSensitive = false;
    }
    else if (refElement instanceof PsiNamedElement) {
      text = ((PsiNamedElement)refElement).getName();
      if (refElement instanceof PsiMetaOwner) {
        final PsiMetaData metaData = ((PsiMetaOwner)refElement).getMetaData();
        if (metaData != null) text = metaData.getName();
      }
    }

    if (text == null && refElement instanceof PsiMetaOwner) {
      final PsiMetaData metaData = ((PsiMetaOwner)refElement).getMetaData();
      if (metaData != null) text = metaData.getName();
    }
    if (StringUtil.isNotEmpty(text)) {
      final SearchScope searchScope = p.getEffectiveSearchScope();
      p.getOptimizer().searchWord(text, searchScope, caseSensitive, refElement);
    }
  }
}