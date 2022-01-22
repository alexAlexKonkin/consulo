/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.openapi.fileEditor.impl.text;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.TextEditorBackgroundHighlighter;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import com.intellij.openapi.module.ModuleUtilCore;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import com.intellij.ui.EditorNotifications;
import consulo.fileEditor.impl.text.TextEditorComponentContainerFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;

public class PsiAwareTextEditorImpl extends TextEditorImpl {
  private TextEditorBackgroundHighlighter myBackgroundHighlighter;

  @RequiredUIAccess
  public PsiAwareTextEditorImpl(@Nonnull final Project project, @Nonnull final VirtualFile file, final TextEditorProviderImpl provider) {
    super(project, file, provider);
  }

  @Nonnull
  @Override
  public Runnable loadEditorInBackground() {
    Runnable baseAction = super.loadEditorInBackground();
    PsiFile psiFile = PsiManager.getInstance(myProject).findFile(myFile);
    Document document = FileDocumentManager.getInstance().getDocument(myFile);
    CodeFoldingState foldingState = document != null && !myProject.isDefault()
                                    ? CodeFoldingManager.getInstance(myProject).buildInitialFoldings(document)
                                    : null;
    return () -> {
      baseAction.run();
      if (foldingState != null) {
        foldingState.setToEditor(getEditor());
      }
      if (psiFile != null && psiFile.isValid()) {
        DaemonCodeAnalyzer.getInstance(myProject).restart(psiFile);
      }
      EditorNotifications.getInstance(myProject).updateNotifications(myFile);
    };
  }

  @Nonnull
  @Override
  protected TextEditorComponent createEditorComponent(final Project project, final VirtualFile file) {
    return new PsiAwareTextEditorComponent(project, file, this, myTextEditorComponentContainerFactory);
  }

  @Override
  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    if (!AsyncEditorLoader.isEditorLoaded(getEditor())) {
      return null;
    }

    if (myBackgroundHighlighter == null) {
      myBackgroundHighlighter = new TextEditorBackgroundHighlighter(myProject, getEditor());
    }
    return myBackgroundHighlighter;
  }

  private static class PsiAwareTextEditorComponent extends TextEditorComponent {
    private final Project myProject;
    private final VirtualFile myFile;

    private PsiAwareTextEditorComponent(@Nonnull  Project project,
                                        @Nonnull  VirtualFile file,
                                        @Nonnull  TextEditorImpl textEditor,
                                        @Nonnull TextEditorComponentContainerFactory factory) {
      super(project, file, textEditor, factory);
      myProject = project;
      myFile = file;
    }

    @Override
    public void dispose() {
      CodeFoldingManager foldingManager = CodeFoldingManager.getInstance(myProject);
      if (foldingManager != null) {
        foldingManager.releaseFoldings(getEditor());
      }
      super.dispose();
    }

    @Override
    public Object getData(@Nonnull final Key<?> dataId) {
      if (PlatformDataKeys.DOMINANT_HINT_AREA_RECTANGLE == dataId) {
        final LookupImpl lookup = (LookupImpl)LookupManager.getInstance(myProject).getActiveLookup();
        if (lookup != null && lookup.isVisible()) {
          return lookup.getBounds();
        }
      }
      if (LangDataKeys.MODULE == dataId) {
        return ModuleUtilCore.findModuleForFile(myFile, myProject);
      }
      return super.getData(dataId);
    }
  }
}
