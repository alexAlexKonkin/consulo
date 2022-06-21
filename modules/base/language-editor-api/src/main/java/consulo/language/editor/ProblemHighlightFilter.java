/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.language.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * This filters can be used to prevent error highlighting (invalid code, unresolved references etc.) in files outside of project scope.
 *
 * Filter implementations should be permissive - i.e. should prevent highlighting only for files it absolutely knows about,
 * and return true otherwise.
 */
@Extension(ComponentScope.APPLICATION)
public abstract class ProblemHighlightFilter {
  public static final ExtensionPointName<ProblemHighlightFilter> EP_NAME = ExtensionPointName.create(ProblemHighlightFilter.class);

  /**
   * @param psiFile file to decide about
   * @return false if this filter disables highlighting for given file, true if filter enables highlighting or can't decide
   */
  public abstract boolean shouldHighlight(@Nonnull PsiFile psiFile);

  public boolean shouldProcessInBatch(@Nonnull PsiFile psiFile) {
    return shouldHighlight(psiFile);
  }

  public static boolean shouldHighlightFile(@Nullable final PsiFile psiFile) {
    return shouldProcess(psiFile, true);
  }

  public static boolean shouldProcessFileInBatch(@Nullable final PsiFile psiFile) {
    return shouldProcess(psiFile, false);
  }

  private static boolean shouldProcess(PsiFile psiFile, boolean onTheFly) {
    if (psiFile == null) return true;

    final List<ProblemHighlightFilter> filters = EP_NAME.getExtensionList();
    for (ProblemHighlightFilter filter : filters) {
      if (onTheFly ? !filter.shouldHighlight(psiFile) : !filter.shouldProcessInBatch(psiFile)) return false;
    }

    return true;
  }
}
