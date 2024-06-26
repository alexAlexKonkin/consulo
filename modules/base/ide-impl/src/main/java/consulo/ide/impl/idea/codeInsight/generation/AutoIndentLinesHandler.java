/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.generation;

import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.logging.Logger;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.project.Project;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.document.util.DocumentUtil;
import consulo.language.util.IncorrectOperationException;
import jakarta.annotation.Nonnull;

public class AutoIndentLinesHandler implements CodeInsightActionHandler {
  private static final Logger LOG = Logger.getInstance(AutoIndentLinesHandler.class);

  @Override
  public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    Document document = editor.getDocument();
    int startOffset;
    int endOffset;
    boolean hasSelection = editor.getSelectionModel().hasSelection();
    if (hasSelection) {
      startOffset = editor.getSelectionModel().getSelectionStart();
      endOffset = editor.getSelectionModel().getSelectionEnd() - 1;
    }
    else {
      startOffset = endOffset = editor.getCaretModel().getOffset();
    }
    int line1 = editor.offsetToLogicalPosition(startOffset).line;
    int col = editor.getCaretModel().getLogicalPosition().column;

    try {
      adjustLineIndent(file, document, startOffset, endOffset, line1, project);
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }

    if (!hasSelection) {
      if (line1 < document.getLineCount() - 1) {
        if (document.getLineStartOffset(line1 + 1) + col >= document.getTextLength()) {
          col = document.getLineEndOffset(line1 + 1) - document.getLineStartOffset(line1 + 1);
        }
        LogicalPosition pos = new LogicalPosition(line1 + 1, col);
        editor.getCaretModel().moveToLogicalPosition(pos);
        editor.getSelectionModel().removeSelection();
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
      }
    }
  }

  private static void adjustLineIndent(PsiFile file, Document document, int startOffset, int endOffset, int line, Project project) {
    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
    if (startOffset == endOffset) {
      int lineStart = document.getLineStartOffset(line);
      if (codeStyleManager.isLineToBeIndented(file, lineStart)) {
        codeStyleManager.adjustLineIndent(file, lineStart);
      }
    }
    else {
      codeStyleManager.adjustLineIndent(file, new TextRange(DocumentUtil.getLineStartOffset(startOffset, document), endOffset));
    }
  }
}
