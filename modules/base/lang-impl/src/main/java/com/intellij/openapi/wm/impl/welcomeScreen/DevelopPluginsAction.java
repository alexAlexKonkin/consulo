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
package com.intellij.openapi.wm.impl.welcomeScreen;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class DevelopPluginsAction extends AnAction implements DumbAware {
  @NonNls
  private static final String PLUGIN_WEBSITE = "https://github.com/consulo/consulo/wiki/Plugin-Development-Starter-Guide";

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    BrowserUtil.browse(PLUGIN_WEBSITE);

  }
}