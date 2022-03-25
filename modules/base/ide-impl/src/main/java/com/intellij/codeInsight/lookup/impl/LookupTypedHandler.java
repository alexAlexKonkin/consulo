// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.lookup.impl;

import consulo.language.editor.AutoPopupController;
import com.intellij.codeInsight.completion.CodeCompletionFeatures;
import com.intellij.codeInsight.completion.CompletionPhase;
import com.intellij.codeInsight.completion.CompletionProgressIndicator;
import consulo.language.editor.completion.PrefixMatcher;
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.codeInsight.editorActions.AutoHardWrapHandler;
import com.intellij.codeInsight.editorActions.TypedHandler;
import com.intellij.codeInsight.lookup.CharFilter;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.actions.ChooseItemAction;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase;
import com.intellij.featureStatistics.FeatureUsageTracker;
import consulo.dataContext.DataManager;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import consulo.codeEditor.action.TypedActionHandler;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.editor.internal.PsiUtilBase;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.List;

public class LookupTypedHandler extends TypedActionHandlerBase {
  private static final Logger LOG = Logger.getInstance(LookupTypedHandler.class);

  public LookupTypedHandler(@Nullable TypedActionHandler originalHandler) {
    super(originalHandler);
  }

  @Override
  public void execute(@Nonnull Editor originalEditor, char charTyped, @Nonnull DataContext dataContext) {
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    PsiFile file = project == null ? null : PsiUtilBase.getPsiFileInEditor(originalEditor, project);

    if (file == null) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(originalEditor, charTyped, dataContext);
      }
      return;
    }

    if (!EditorModificationUtil.checkModificationAllowed(originalEditor)) {
      return;
    }

    CompletionPhase oldPhase = CompletionServiceImpl.getCompletionPhase();
    if (oldPhase instanceof CompletionPhase.CommittingDocuments && oldPhase.indicator != null) {
      oldPhase.indicator.scheduleRestart();
    }

    Editor editor = TypedHandler.injectedEditorIfCharTypedIsSignificant(charTyped, originalEditor, file);
    if (editor != originalEditor) {
      file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    }

    if (originalEditor.isInsertMode() && beforeCharTyped(charTyped, project, originalEditor, editor, file)) {
      return;
    }

    if (myOriginalHandler != null) {
      myOriginalHandler.execute(originalEditor, charTyped, dataContext);
    }
  }

  private static boolean beforeCharTyped(final char charTyped, Project project, final Editor originalEditor, final Editor editor, PsiFile file) {
    final LookupImpl lookup = (LookupImpl)LookupManager.getActiveLookup(originalEditor);
    if (lookup == null) {
      return false;
    }

    if (charTyped == ' ' && ChooseItemAction.hasTemplatePrefix(lookup, TemplateSettings.SPACE_CHAR)) {
      return false;
    }

    final CharFilter.Result result = getLookupAction(charTyped, lookup);
    if (lookup.isLookupDisposed()) {
      return false;
    }

    if (result == CharFilter.Result.ADD_TO_PREFIX) {
      Document document = editor.getDocument();
      long modificationStamp = document.getModificationStamp();

      if (!lookup.performGuardedChange(() -> {
        lookup.fireBeforeAppendPrefix(charTyped);
        EditorModificationUtil.typeInStringAtCaretHonorMultipleCarets(originalEditor, String.valueOf(charTyped), true);
      })) {
        return true;
      }
      lookup.appendPrefix(charTyped);
      if (lookup.isStartCompletionWhenNothingMatches() && lookup.getItems().isEmpty()) {
        final CompletionProgressIndicator completion = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
        if (completion != null) {
          completion.scheduleRestart();
        }
        else {
          AutoPopupController.getInstance(editor.getProject()).scheduleAutoPopup(editor);
        }
      }

      AutoHardWrapHandler.getInstance().wrapLineIfNecessary(originalEditor, DataManager.getInstance().getDataContext(originalEditor.getContentComponent()), modificationStamp);

      final CompletionProgressIndicator completion = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
      if (completion != null) {
        completion.prefixUpdated();
      }
      return true;
    }

    if (result == CharFilter.Result.SELECT_ITEM_AND_FINISH_LOOKUP && lookup.isFocused()) {
      LookupElement item = lookup.getCurrentItem();
      if (item != null) {
        if (completeTillTypedCharOccurrence(charTyped, lookup, item)) {
          return true;
        }

        FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EDITING_COMPLETION_FINISH_BY_DOT_ETC);
        lookup.finishLookupInWritableFile(charTyped, item);
        return true;
      }
    }

    lookup.hide();
    TypedHandler.autoPopupCompletion(editor, charTyped, project, file);
    return false;
  }

  private static boolean completeTillTypedCharOccurrence(char charTyped, LookupImpl lookup, LookupElement item) {
    PrefixMatcher matcher = lookup.itemMatcher(item);
    final String oldPrefix = matcher.getPrefix() + lookup.getAdditionalPrefix();
    PrefixMatcher expanded = matcher.cloneWithPrefix(oldPrefix + charTyped);
    if (expanded.prefixMatches(item)) {
      for (String s : item.getAllLookupStrings()) {
        if (matcher.prefixMatches(s)) {
          int i = -1;
          while (true) {
            i = s.indexOf(charTyped, i + 1);
            if (i < 0) break;
            final String newPrefix = s.substring(0, i + 1);
            if (expanded.prefixMatches(newPrefix)) {
              lookup.replacePrefix(oldPrefix, newPrefix);
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  static CharFilter.Result getLookupAction(final char charTyped, final LookupImpl lookup) {
    CharFilter.Result filtersDecision = getFilterDecision(charTyped, lookup);
    if (filtersDecision != null) {
      return filtersDecision;
    }
    return CharFilter.Result.HIDE_LOOKUP;
  }

  @Nullable
  private static CharFilter.Result getFilterDecision(char charTyped, LookupImpl lookup) {
    lookup.checkValid();
    LookupElement item = lookup.getCurrentItem();
    int prefixLength = item == null ? lookup.getAdditionalPrefix().length() : lookup.itemPattern(item).length();

    for (CharFilter extension : getFilters()) {
      CharFilter.Result result = extension.acceptChar(charTyped, prefixLength, lookup);
      if (result != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(extension + " of " + extension.getClass() + " returned " + result);
        }
        return result;
      }
      if (lookup.isLookupDisposed()) {
        throw new AssertionError("Lookup disposed after " + extension);
      }
    }
    return null;
  }

  private static List<CharFilter> getFilters() {
    return CharFilter.EP_NAME.getExtensionList();
  }
}
