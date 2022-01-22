/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.util.gist;

import consulo.language.psi.PsiFile;
import javax.annotation.Nonnull;

/**
 * Calculates some data based on {@link PsiFile} content, persists it between IDE restarts,
 * and updates it when the content is changed. The data is calculated lazily, when needed.<p/>
 * <p>
 * Obtained using {@link GistManager#newPsiFileGist}.<p/>
 * <p>
 * The difference to {@link VirtualFileGist} is that PSI content is used here. So if an uncommitted document is saved onto disk,
 * this class will use the last committed content of the PSI file, while {@link VirtualFileGist} will use the saved virtual file content.<p/>
 * <p>
 * Please note that VirtualFileGist is used inside, so using PsiFileGist has the same performance implications (see {@link VirtualFileGist} documentation).
 *
 * @author peter
 */
public interface PsiFileGist<Data> {

  /**
   * Calculate or get the cached data by the current PSI content.
   */
  Data getFileData(@Nonnull PsiFile file);
}
