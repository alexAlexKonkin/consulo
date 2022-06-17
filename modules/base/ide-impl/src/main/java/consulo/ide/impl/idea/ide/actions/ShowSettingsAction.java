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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.CommonBundle;
import consulo.application.AllIcons;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.application.util.SystemInfo;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class ShowSettingsAction extends AnAction implements DumbAware {
  private final Provider<ShowSettingsUtil> myShowSettingsUtil;

  @Inject
  public ShowSettingsAction(Provider<ShowSettingsUtil> showSettingsUtil) {
    super(CommonBundle.settingsAction(), CommonBundle.settingsActionDescription(), AllIcons.General.Settings);
    myShowSettingsUtil = showSettingsUtil;
  }

  @Override
  public void update(AnActionEvent e) {
    if (SystemInfo.isMac && e.getPlace().equals(ActionPlaces.MAIN_MENU)) {
      // It's called from Preferences in App menu.
      e.getPresentation().setVisible(false);
    }
    if (e.getPlace().equals(ActionPlaces.WELCOME_SCREEN)) {
      e.getPresentation().setText(CommonBundle.settingsTitle());
    }
  }

  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      project = ProjectManager.getInstance().getDefaultProject();
    }

    myShowSettingsUtil.get().showSettingsDialog(project);
  }
}
