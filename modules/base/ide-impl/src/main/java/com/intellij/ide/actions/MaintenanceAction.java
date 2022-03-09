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
package com.intellij.ide.actions;

import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.JBPopupFactory;

public class MaintenanceAction extends AnAction {

  public MaintenanceAction() {
    super("Maintenance");
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final ActionGroup group = (ActionGroup)ActionManager.getInstance().getAction("MaintenanceGroup");
    JBPopupFactory.getInstance().
      createActionGroupPopup("Maintenance", group, e.getDataContext(), JBPopupFactory.ActionSelectionAid.NUMBERING, true).
      showInFocusCenter();
  }
}