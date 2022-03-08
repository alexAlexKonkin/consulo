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

package com.intellij.internal;

import consulo.language.editor.completion.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.ui.ex.action.Presentation;
import consulo.logging.Logger;
import consulo.codeEditor.Editor;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.application.dumb.DumbAware;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import consulo.ui.annotation.RequiredUIAccess;

import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Map;

/**
 * @author peter
 */
public class DumpLookupElementWeights extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(DumpLookupElementWeights.class);

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    dumpLookupElementWeights((LookupImpl)LookupManager.getActiveLookup(editor));
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull final AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    presentation.setEnabled(editor != null && LookupManager.getActiveLookup(editor) != null);
  }

  public static void dumpLookupElementWeights(final LookupImpl lookup) {
    LookupElement selected = lookup.getCurrentItem();
    String sb = "selected: " + selected;
    if (selected != null) {
      sb += "\nprefix: " + lookup.itemPattern(selected);
    }
    sb += "\nweights:\n" + StringUtil.join(getLookupElementWeights(lookup, true), "\n");
    System.out.println(sb);
    LOG.info(sb);
    try {
      CopyPasteManager.getInstance().setContents(new StringSelection(sb));
    } catch (Exception ignore){}
  }

  public static List<String> getLookupElementWeights(LookupImpl lookup, boolean hideSingleValued) {
    final Map<LookupElement, List<Pair<String, Object>>> weights = lookup.getRelevanceObjects(lookup.getItems(), hideSingleValued);
    return ContainerUtil.map(weights.entrySet(), new Function<Map.Entry<LookupElement, List<Pair<String, Object>>>, String>() {
      @Override
      public String fun(Map.Entry<LookupElement, List<Pair<String, Object>>> entry) {
        return entry.getKey().getLookupString() + "\t" + StringUtil.join(entry.getValue(), new Function<Pair<String, Object>, String>() {
          @Override
          public String fun(Pair<String, Object> pair) {
            return pair.first + "=" + pair.second;
          }
        }, ", ");
      }
    });
  }
}