// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions.enter;

import com.intellij.codeInsight.editorActions.EnterHandler;
import consulo.codeEditor.EditorEx;
import consulo.language.Language;
import consulo.codeEditor.Editor;
import consulo.codeEditor.HighlighterIterator;
import consulo.language.psi.PsiFile;
import consulo.language.ast.IElementType;
import javax.annotation.Nonnull;

public abstract class EnterBetweenBracesNoCommitDelegate extends EnterBetweenBracesDelegate {
  @Override
  public boolean bracesAreInTheSameElement(@Nonnull PsiFile file, @Nonnull Editor editor, int lBraceOffset, int rBraceOffset) {
    final HighlighterIterator it = createBeforeIterator(editor, lBraceOffset + 1);
    while (!it.atEnd() && it.getStart() < rBraceOffset) {
      it.advance();
      if (!it.atEnd() && it.getStart() == rBraceOffset) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isInComment(@Nonnull PsiFile file, @Nonnull Editor editor, int offset) {
    final HighlighterIterator it = createBeforeIterator(editor, offset);
    return !it.atEnd() && isCommentType((IElementType)it.getTokenType());
  }

  public abstract boolean isCommentType(IElementType type);

  @Override
  protected void formatAtOffset(@Nonnull PsiFile file, @Nonnull Editor editor, int offset, Language language) {
    EnterHandler.adjustLineIndentNoCommit(language, editor.getDocument(), editor, offset);
  }

  @Nonnull
  public static HighlighterIterator createBeforeIterator(@Nonnull Editor editor, int caretOffset) {
    return ((EditorEx)editor).getHighlighter().createIterator(caretOffset == 0 ? 0 : caretOffset - 1);
  }
}