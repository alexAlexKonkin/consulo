// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.cache;

import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import consulo.util.dataholder.UserDataHolder;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CodeStyleCachingService {

  static CodeStyleCachingService getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, CodeStyleCachingService.class);
  }

  @Nullable
  CodeStyleSettings tryGetSettings(@Nonnull PsiFile file);

  void scheduleWhenSettingsComputed(@Nonnull PsiFile file, @Nonnull Runnable runnable);

  @Nullable
  UserDataHolder getDataHolder(@Nonnull VirtualFile virtualFile);
}
