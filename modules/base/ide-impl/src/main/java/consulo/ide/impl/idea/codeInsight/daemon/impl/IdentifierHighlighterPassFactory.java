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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.Pass;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPassFactory;
import consulo.language.editor.CodeInsightSettings;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
@ExtensionImpl
public class IdentifierHighlighterPassFactory implements TextEditorHighlightingPassFactory {
  public static boolean ourTestingIdentifierHighlighting = false;

  @Override
  public void register(@Nonnull Registrar registrar) {
    registrar.registerTextEditorHighlightingPass(this, null, new int[]{Pass.UPDATE_ALL}, false, -1);
  }

  @Override
  public TextEditorHighlightingPass createHighlightingPass(@Nonnull final PsiFile file, @Nonnull final Editor editor) {
    if (editor.isOneLineMode()) return null;

    if (CodeInsightSettings.getInstance().HIGHLIGHT_IDENTIFIER_UNDER_CARET && (!ApplicationManager.getApplication().isHeadlessEnvironment() || ourTestingIdentifierHighlighting)) {
      return new IdentifierHighlighterPass(file.getProject(), file, editor);
    }
    return null;
  }
}
