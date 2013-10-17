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

/*
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Sep 27, 2002
 * Time: 3:10:17 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.codeInsight.highlighting;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.hint.EditorFragmentComponent;
import com.intellij.concurrency.Job;
import com.intellij.concurrency.JobLauncher;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Alarm;
import com.intellij.util.Processor;
import com.intellij.util.containers.WeakHashMap;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BraceHighlightingHandler {
  private static final Key<List<RangeHighlighter>> BRACE_HIGHLIGHTERS_IN_EDITOR_VIEW_KEY = Key.create("BraceHighlighter.BRACE_HIGHLIGHTERS_IN_EDITOR_VIEW_KEY");
  private static final Key<RangeHighlighter> LINE_MARKER_IN_EDITOR_KEY = Key.create("BraceHighlighter.LINE_MARKER_IN_EDITOR_KEY");

  /**
   * Holds weak references to the editors that are being processed at non-EDT.
   * <p/>
   * Is intended to be used to avoid submitting unnecessary new processing request from EDT, i.e. it's assumed that the collection
   * is accessed from the single thread (EDT).
   */
  private static final Set<Editor> PROCESSED_EDITORS = Collections.newSetFromMap(new WeakHashMap<Editor, Boolean>());

  private final Project myProject;
  private final Editor myEditor;
  private final Alarm myAlarm;

  private final DocumentEx myDocument;
  private final PsiFile myPsiFile;
  // private final FileType myFileType;
  private final CodeInsightSettings myCodeInsightSettings;

  private BraceHighlightingHandler(@NotNull Project project, @NotNull Editor editor, @NotNull Alarm alarm, PsiFile psiFile) {
    myProject = project;

    myEditor = editor;
    myAlarm = alarm;
    myDocument = (DocumentEx)myEditor.getDocument();

    myPsiFile = psiFile;
    myCodeInsightSettings = CodeInsightSettings.getInstance();
    // myFileType = myPsiFile == null ? null : myPsiFile.getFileType();
  }

  static void lookForInjectedAndMatchBracesInOtherThread(@NotNull final Editor editor, @NotNull final Alarm alarm, @NotNull final Processor<BraceHighlightingHandler> processor) {
    if (!PROCESSED_EDITORS.add(editor)) {
      // Skip processing if that is not really necessary.
      // Assuming to be in EDT here.
      return;
    }
    final Project project = editor.getProject();
    if (project == null) return;
    final int offset = editor.getCaretModel().getOffset();
    JobLauncher.getInstance().submitToJobThread(Job.DEFAULT_PRIORITY, new Runnable() {
      @Override
      public void run() {
        final PsiFile injected;
        try {
          injected = ApplicationManager.getApplication().runReadAction(new Computable<PsiFile>() {
            @Override
            public PsiFile compute() {
              if (isReallyDisposed(editor, project)) return null;
              PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
              return psiFile == null || psiFile instanceof PsiCompiledElement
                     ? null : getInjectedFileIfAny(editor, project, offset, psiFile, alarm);
            }
          });
        }
        catch (RuntimeException e) {
          // Reset processing flag in case of unexpected exception.
          ApplicationManager.getApplication().invokeLater(new DumbAwareRunnable() {
            @Override
            public void run() {
              PROCESSED_EDITORS.remove(editor);
            }
          });
          throw e;
        }
        ApplicationManager.getApplication().invokeLater(new DumbAwareRunnable() {
          @Override
          public void run() {
            try {
              if (!isReallyDisposed(editor, project)) {
                Editor newEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(editor, injected);
                BraceHighlightingHandler handler = new BraceHighlightingHandler(project, newEditor, alarm, injected);
                processor.process(handler);
              }
            }
            finally {
              PROCESSED_EDITORS.remove(editor);
            }
          }
        }, ModalityState.stateForComponent(editor.getComponent()));
      }
    });
  }

  private static boolean isReallyDisposed(Editor editor, Project project) {
    Project editorProject = editor.getProject();
    return editorProject == null ||
           editorProject.isDisposed() || project.isDisposed() || !editor.getComponent().isShowing() || editor.isViewer();
  }

  @NotNull
  private static PsiFile getInjectedFileIfAny(@NotNull final Editor editor, @NotNull final Project project, int offset, @NotNull PsiFile psiFile, @NotNull final Alarm alarm) {
    Document document = editor.getDocument();
    // when document is committed, try to highlight braces in injected lang - it's fast
    if (!PsiDocumentManager.getInstance(project).isUncommited(document)) {
      final PsiElement injectedElement = InjectedLanguageUtil.findInjectedElementNoCommit(psiFile, offset);
      if (injectedElement != null /*&& !(injectedElement instanceof PsiWhiteSpace)*/) {
        final PsiFile injected = injectedElement.getContainingFile();
        if (injected != null) {
          return injected;
        }
      }
    }
    else {
      PsiDocumentManager.getInstance(project).performForCommittedDocument(document, new Runnable() {
        @Override
        public void run() {
          if (!project.isDisposed() && !editor.isDisposed()) {
            BraceHighlighter.updateBraces(editor, alarm);
          }
        }
      });
    }
    return psiFile;
  }

  public void updateBraces() {
    if (myPsiFile == null || !myPsiFile.isValid()) return;

    clearBraceHighlighters();

    if (!myCodeInsightSettings.HIGHLIGHT_BRACES) return;

    if (myEditor.getSelectionModel().hasSelection()) return;
    
    if (myEditor.getSoftWrapModel().isInsideOrBeforeSoftWrap(myEditor.getCaretModel().getVisualPosition())) return;

    int offset = myEditor.getCaretModel().getOffset();
    final CharSequence chars = myEditor.getDocument().getCharsSequence();

    //if (myEditor.offsetToLogicalPosition(offset).column != myEditor.getCaretModel().getLogicalPosition().column) {
    //  // we are in virtual space
    //  final int caretLineNumber = myEditor.getCaretModel().getLogicalPosition().line;
    //  if (caretLineNumber >= myDocument.getLineCount()) return;
    //  offset = myDocument.getLineEndOffset(caretLineNumber) + myDocument.getLineSeparatorLength(caretLineNumber);
    //}

    final int originalOffset = offset;

    HighlighterIterator iterator = getEditorHighlighter().createIterator(offset);
    FileType fileType = PsiUtilBase.getPsiFileAtOffset(myPsiFile, offset).getFileType();

    if (iterator.atEnd()) {
      offset--;
    }
    else if (BraceMatchingUtil.isRBraceToken(iterator, chars, fileType)) {
      offset--;
    }
    else if (!BraceMatchingUtil.isLBraceToken(iterator, chars, fileType)) {
      offset--;

      if (offset >= 0) {
        final HighlighterIterator i = getEditorHighlighter().createIterator(offset);
        if (!BraceMatchingUtil.isRBraceToken(i, chars, getFileTypeByIterator(i))) offset++;
      }
    }

    if (offset < 0) {
      removeLineMarkers();
      return;
    }

    iterator = getEditorHighlighter().createIterator(offset);
    fileType = getFileTypeByIterator(iterator);

    myAlarm.cancelAllRequests();

    if (BraceMatchingUtil.isLBraceToken(iterator, chars, fileType) ||
        BraceMatchingUtil.isRBraceToken(iterator, chars, fileType)) {
      doHighlight(offset, originalOffset, fileType);
    }
    else if (offset > 0 && offset < chars.length()) {
      // There is a possible case that there are paired braces nearby the caret position and the document contains only white
      // space symbols between them. We want to highlight such braces as well.
      // Example: 
      //     public void test() { <caret>
      //     }
      char c = chars.charAt(offset);
      boolean searchForward = c != '\n';

      // Try to find matched brace backwards.
      if (offset >= originalOffset && (c == ' ' || c == '\t' || c == '\n')) {
        int backwardNonWsOffset = CharArrayUtil.shiftBackward(chars, offset - 1, "\t ");
        if (backwardNonWsOffset >= 0) {
          iterator = getEditorHighlighter().createIterator(backwardNonWsOffset);
          FileType newFileType = getFileTypeByIterator(iterator);
          if (BraceMatchingUtil.isLBraceToken(iterator, chars, newFileType) ||
              BraceMatchingUtil.isRBraceToken(iterator, chars, newFileType)) {
            offset = backwardNonWsOffset;
            searchForward = false;
            doHighlight(backwardNonWsOffset, originalOffset, newFileType);
          }
        }
      }

      // Try to find matched brace forward.
      if (searchForward) {
        int forwardOffset = CharArrayUtil.shiftForward(chars, offset, "\t ");
        if (forwardOffset > offset || c == ' ' || c == '\t') {
          iterator = getEditorHighlighter().createIterator(forwardOffset);
          FileType newFileType = getFileTypeByIterator(iterator);
          if (BraceMatchingUtil.isLBraceToken(iterator, chars, newFileType) ||
              BraceMatchingUtil.isRBraceToken(iterator, chars, newFileType)) {
            offset = forwardOffset;
            doHighlight(forwardOffset, originalOffset, newFileType);
          }
        }
      }
    }

    //highlight scope
    if (!myCodeInsightSettings.HIGHLIGHT_SCOPE) {
      removeLineMarkers();
      return;
    }

    final int _offset = offset;
    final FileType _fileType = fileType;
    myAlarm.addRequest(new Runnable() {
      @Override
      public void run() {
        if (!myProject.isDisposed() && !myEditor.isDisposed()) {
          highlightScope(_offset, _fileType);
        }
      }
    }, 300);
  }

  private FileType getFileTypeByIterator(HighlighterIterator iterator) {
    return PsiUtilBase.getPsiFileAtOffset(myPsiFile, iterator.getStart()).getFileType();
  }

  private FileType getFileTypeByOffset(int offset) {
    return PsiUtilBase.getPsiFileAtOffset(myPsiFile, offset).getFileType();
  }

  private EditorHighlighter getEditorHighlighter() {
    return ((EditorEx)myEditor).getHighlighter();
  }

  private void highlightScope(int offset, FileType fileType) {
    if (myEditor.getFoldingModel().isOffsetCollapsed(offset)) return;
    if (myEditor.getDocument().getTextLength() <= offset) return;

    HighlighterIterator iterator = getEditorHighlighter().createIterator(offset);
    final CharSequence chars = myDocument.getCharsSequence();

    if (!BraceMatchingUtil.isStructuralBraceToken(fileType, iterator, chars)) {
//      if (BraceMatchingUtil.isRBraceTokenToHighlight(myFileType, iterator) || BraceMatchingUtil.isLBraceTokenToHighlight(myFileType, iterator)) return;
    }
    else {
      if (BraceMatchingUtil.isRBraceToken(iterator, chars, fileType) ||
          BraceMatchingUtil.isLBraceToken(iterator, chars, fileType)) return;
    }

    if (!BraceMatchingUtil.findStructuralLeftBrace(fileType, iterator, chars)) {
      removeLineMarkers();
      return;
    }

    highlightLeftBrace(iterator, true, fileType);
  }

  private void doHighlight(int offset, int originalOffset, FileType fileType) {
    if (myEditor.getFoldingModel().isOffsetCollapsed(offset)) return;

    HighlighterIterator iterator = getEditorHighlighter().createIterator(offset);
    final CharSequence chars = myDocument.getCharsSequence();

    if (BraceMatchingUtil.isLBraceToken(iterator, chars, fileType)) {
      IElementType tokenType = iterator.getTokenType();

      iterator.advance();
      if (!iterator.atEnd() && BraceMatchingUtil.isRBraceToken(iterator, chars, fileType)) {
        if (BraceMatchingUtil.isPairBraces(tokenType, iterator.getTokenType(), fileType) &&
            originalOffset == iterator.getStart()) return;
      }

      iterator.retreat();
      highlightLeftBrace(iterator, false, fileType);

      if (offset > 0) {
        iterator = getEditorHighlighter().createIterator(offset - 1);
        if (BraceMatchingUtil.isRBraceToken(iterator, chars, fileType)) {
          highlightRightBrace(iterator, fileType);
        }
      }
    }
    else if (BraceMatchingUtil.isRBraceToken(iterator, chars, fileType)) {
      highlightRightBrace(iterator, fileType);
    }
  }

  private void highlightRightBrace(HighlighterIterator iterator, FileType fileType) {
    TextRange brace1 = TextRange.create(iterator.getStart(), iterator.getEnd());

    boolean matched = BraceMatchingUtil.matchBrace(myDocument.getCharsSequence(), fileType, iterator, false);

    TextRange brace2 = iterator.atEnd() ? null : TextRange.create(iterator.getStart(), iterator.getEnd());

    highlightBraces(brace2, brace1, matched, false, fileType);
  }

  private void highlightLeftBrace(HighlighterIterator iterator, boolean scopeHighlighting, FileType fileType) {
    TextRange brace1Start = TextRange.create(iterator.getStart(), iterator.getEnd());
    boolean matched = BraceMatchingUtil.matchBrace(myDocument.getCharsSequence(), fileType, iterator, true);

    TextRange brace2End = iterator.atEnd() ? null : TextRange.create(iterator.getStart(), iterator.getEnd());

    highlightBraces(brace1Start, brace2End, matched, scopeHighlighting, fileType);
  }

  private void highlightBraces(final TextRange lBrace, TextRange rBrace, boolean matched, boolean scopeHighlighting, FileType fileType) {
    if (!matched && fileType == FileTypes.PLAIN_TEXT) {
      return;
    }

    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    final TextAttributes attributes =
      matched ? scheme.getAttributes(CodeInsightColors.MATCHED_BRACE_ATTRIBUTES)
              : scheme.getAttributes(CodeInsightColors.UNMATCHED_BRACE_ATTRIBUTES);

    if (rBrace != null && !scopeHighlighting) {
      highlightBrace(rBrace, matched);
    }

    if (lBrace != null && !scopeHighlighting) {
      highlightBrace(lBrace, matched);
    }

    if (!myEditor.equals(FileEditorManager.getInstance(myProject).getSelectedTextEditor())) {
      return;
    }

    if (lBrace != null && rBrace !=null) {
      final int startLine = myEditor.offsetToLogicalPosition(lBrace.getStartOffset()).line;
      final int endLine = myEditor.offsetToLogicalPosition(rBrace.getEndOffset()).line;
      if (endLine - startLine > 0) {
        final Runnable runnable = new Runnable() {
          @Override
          public void run() {
            if (myProject.isDisposed() || myEditor.isDisposed()) return;
            Color color = attributes.getBackgroundColor();
            if (color == null) return;
            color = color.darker();
            lineMarkFragment(startLine, endLine, color);
          }
        };

        if (!scopeHighlighting) {
          myAlarm.addRequest(runnable, 300);
        }
        else {
          runnable.run();
        }
      }
      else {
        if (!myCodeInsightSettings.HIGHLIGHT_SCOPE) {
          removeLineMarkers();
        }
      }

      if (!scopeHighlighting) {
        showScopeHint(lBrace.getStartOffset(), lBrace.getEndOffset());
      }
    }
    else {
      if (!myCodeInsightSettings.HIGHLIGHT_SCOPE) {
        removeLineMarkers();
      }
    }
  }

  private void highlightBrace(TextRange braceRange, boolean matched) {
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    final TextAttributes attributes =
        matched ? scheme.getAttributes(CodeInsightColors.MATCHED_BRACE_ATTRIBUTES)
        : scheme.getAttributes(CodeInsightColors.UNMATCHED_BRACE_ATTRIBUTES);


    RangeHighlighter rbraceHighlighter =
        myEditor.getMarkupModel().addRangeHighlighter(
          braceRange.getStartOffset(), braceRange.getEndOffset(), HighlighterLayer.LAST + 1, attributes, HighlighterTargetArea.EXACT_RANGE);
    rbraceHighlighter.setGreedyToLeft(false);
    rbraceHighlighter.setGreedyToRight(false);
    registerHighlighter(rbraceHighlighter);
  }

  private void registerHighlighter(RangeHighlighter highlighter) {
    getHighlightersList().add(highlighter);
  }

  @NotNull
  private List<RangeHighlighter> getHighlightersList() {
    // braces are highlighted across the whole editor, not in each injected editor separately
    Editor editor = myEditor instanceof EditorWindow ? ((EditorWindow)myEditor).getDelegate() : myEditor;
    List<RangeHighlighter> highlighters = editor.getUserData(BRACE_HIGHLIGHTERS_IN_EDITOR_VIEW_KEY);
    if (highlighters == null) {
      highlighters = new ArrayList<RangeHighlighter>();
      editor.putUserData(BRACE_HIGHLIGHTERS_IN_EDITOR_VIEW_KEY, highlighters);
    }
    return highlighters;
  }

  private void showScopeHint(final int lbraceStart, final int lbraceEnd) {
    LogicalPosition bracePosition = myEditor.offsetToLogicalPosition(lbraceStart);
    Point braceLocation = myEditor.logicalPositionToXY(bracePosition);
    final int y = braceLocation.y;
    myAlarm.addRequest(
        new Runnable() {
          @Override
          public void run() {
            if (!myEditor.getComponent().isShowing()) return;
            Rectangle viewRect = myEditor.getScrollingModel().getVisibleArea();
            if (y < viewRect.y) {
              int start = lbraceStart;
              if (!(myPsiFile instanceof PsiPlainTextFile) && myPsiFile.isValid()) {
                PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                start = BraceMatchingUtil.getBraceMatcher(getFileTypeByOffset(lbraceStart), PsiUtilCore
                  .getLanguageAtOffset(myPsiFile, lbraceStart)).getCodeConstructStart(myPsiFile, lbraceStart);
              }
              TextRange range = new TextRange(start, lbraceEnd);
              int line1 = myDocument.getLineNumber(range.getStartOffset());
              int line2 = myDocument.getLineNumber(range.getEndOffset());
              line1 = Math.max(line1, line2 - 5);
              range = new TextRange(myDocument.getLineStartOffset(line1), range.getEndOffset());
              EditorFragmentComponent.showEditorFragmentHint(myEditor, range, true, true);
            }
          }
        },
        300, ModalityState.stateForComponent(myEditor.getComponent()));
  }

  public void clearBraceHighlighters() {
    List<RangeHighlighter> highlighters = getHighlightersList();
    for (final RangeHighlighter highlighter : highlighters) {
      highlighter.dispose();
    }
    highlighters.clear();
  }

  private void lineMarkFragment(int startLine, int endLine, Color color) {
    removeLineMarkers();

    if (startLine >= endLine || endLine >= myDocument.getLineCount()) return;

    int startOffset = myDocument.getLineStartOffset(startLine);
    int endOffset = myDocument.getLineStartOffset(endLine);

    RangeHighlighter highlighter = myEditor.getMarkupModel().addRangeHighlighter(startOffset, endOffset, 0, null, HighlighterTargetArea.LINES_IN_RANGE);
    highlighter.setLineMarkerRenderer(new MyLineMarkerRenderer(color));
    myEditor.putUserData(LINE_MARKER_IN_EDITOR_KEY, highlighter);
  }

  private void removeLineMarkers() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    RangeHighlighter marker = myEditor.getUserData(LINE_MARKER_IN_EDITOR_KEY);
    if (marker != null && ((MarkupModelEx)myEditor.getMarkupModel()).containsHighlighter(marker)) {
      marker.dispose();
    }
    myEditor.putUserData(LINE_MARKER_IN_EDITOR_KEY, null);
  }

  private static class MyLineMarkerRenderer implements LineMarkerRenderer {
    private static final int DEEPNESS = 2;
    private static final int THICKNESS = 2;
    private final Color myColor;

    private MyLineMarkerRenderer(Color color) {
      myColor = color;
    }

    @Override
    public void paint(Editor editor, Graphics g, Rectangle r) {
      int height = r.height + editor.getLineHeight();
      g.setColor(myColor);
      g.fillRect(r.x, r.y, THICKNESS, height);
      g.fillRect(r.x + THICKNESS, r.y, DEEPNESS, THICKNESS);
      g.fillRect(r.x + THICKNESS, r.y + height - THICKNESS, DEEPNESS, THICKNESS);
    }
  }
}
