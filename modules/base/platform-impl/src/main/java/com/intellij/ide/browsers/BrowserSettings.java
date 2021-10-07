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
package com.intellij.ide.browsers;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.swing.*;

public class BrowserSettings implements SearchableConfigurable, Configurable.NoScroll {
  private BrowserSettingsPanel myPanel;

  @Override
  @Nonnull
  public String getId() {
    return getHelpTopic();
  }

  @Override
  @Nls
  public String getDisplayName() {
    return IdeBundle.message("browsers.settings");
  }

  @Override
  @Nonnull
  public String getHelpTopic() {
    return "reference.settings.ide.settings.web.browsers";
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent(@Nonnull Disposable uiDisposable) {
    if (myPanel == null) {
      myPanel = new BrowserSettingsPanel(uiDisposable);
    }
    return myPanel.getComponent();
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myPanel != null && myPanel.isModified();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    if (myPanel != null) {
      myPanel.apply();
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    if (myPanel != null) {
      myPanel.reset();
    }
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myPanel = null;
  }

  public void selectBrowser(@Nonnull WebBrowser browser) {
    if(myPanel == null) {
      throw new IllegalArgumentException("not initialized ui");
    }
    myPanel.selectBrowser(browser);
  }
}
