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
package com.maddyhome.idea.copyright.psi;

import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.ui.TemplateCommentPanel;
import javax.annotation.Nonnull;
import consulo.copyright.config.CopyrightFileConfig;

/**
 * @author yole
 */
public abstract class UpdateCopyrightsProvider<T extends CopyrightFileConfig> {
  @Nonnull
  public abstract UpdatePsiFileCopyright<T> createInstance(@Nonnull PsiFile file, @Nonnull CopyrightProfile copyrightProfile);

  @Nonnull
  public abstract T createDefaultOptions();

  @Nonnull
  public abstract TemplateCommentPanel createConfigurable(@Nonnull Project project, @Nonnull TemplateCommentPanel parentPane, @Nonnull FileType fileType);

  public boolean isAllowSeparator() {
    return true;
  }
}
