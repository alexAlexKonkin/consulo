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
package com.intellij.vcs.log.ui.actions;

import consulo.component.extension.Extensions;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.data.VcsLogUiProperties;
import com.intellij.vcs.log.ui.VcsLogHighlighterFactory;
import com.intellij.vcs.log.ui.VcsLogInternalDataKeys;
import com.intellij.vcs.log.ui.VcsLogUiImpl;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

import static com.intellij.vcs.log.data.MainVcsLogUiProperties.VcsLogHighlighterProperty;

public class HighlightersActionGroup extends ActionGroup {
  @Nonnull
  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    List<AnAction> actions = ContainerUtil.newArrayList();

    if (e != null) {
      if (e.getData(VcsLogInternalDataKeys.LOG_UI_PROPERTIES) != null) {
        actions.add(new AnSeparator("Highlight"));
        for (VcsLogHighlighterFactory factory : Extensions.getExtensions(VcsLogUiImpl.LOG_HIGHLIGHTER_FACTORY_EP, e.getData(CommonDataKeys.PROJECT))) {
          if (factory.showMenuItem()) {
            actions.add(new EnableHighlighterAction(factory));
          }
        }
      }
    }

    return actions.toArray(new AnAction[actions.size()]);
  }

  private static class EnableHighlighterAction extends BooleanPropertyToggleAction {
    @Nonnull
    private final VcsLogHighlighterFactory myFactory;

    private EnableHighlighterAction(@Nonnull VcsLogHighlighterFactory factory) {
      super(factory.getTitle());
      myFactory = factory;
    }

    @Override
    protected VcsLogUiProperties.VcsLogUiProperty<Boolean> getProperty() {
      return VcsLogHighlighterProperty.get(myFactory.getId());
    }
  }
}
