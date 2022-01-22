/*
 * Copyright 2000-2019 JetBrains s.r.o.
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
package com.intellij.codeInsight.navigation;

import consulo.project.Project;
import com.intellij.openapi.ui.JBListUpdater;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Ref;
import consulo.language.psi.PsiElement;
import com.intellij.ui.components.JBList;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.usages.UsageView;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * @deprecated please use {@link BackgroundUpdaterTask}
 */
@Deprecated
public abstract class ListBackgroundUpdaterTask extends BackgroundUpdaterTask {

  protected AbstractPopup myPopup;

  /**
   * @deprecated Use {@link #ListBackgroundUpdaterTask(Project, String, Comparator)}
   */
  @Deprecated
  public ListBackgroundUpdaterTask(@Nullable final Project project, @Nonnull final String title) {
    this(project, title, null);
  }

  public ListBackgroundUpdaterTask(@Nullable final Project project, @Nonnull final String title, @Nullable Comparator<PsiElement> comparator) {
    super(project, title, comparator);
  }

  /**
   * @deprecated please use {@link BackgroundUpdaterTask}
   */
  @Deprecated
  public void init(@Nonnull AbstractPopup popup, @Nonnull Object component, @Nonnull Ref<UsageView> usageView) {
    myPopup = popup;
    if (component instanceof JBList) {
      init((JBPopup)myPopup, new JBListUpdater((JBList)component), usageView);
    }
  }
}
