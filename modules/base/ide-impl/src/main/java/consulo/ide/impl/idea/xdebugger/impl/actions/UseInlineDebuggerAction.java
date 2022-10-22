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
package consulo.ide.impl.idea.xdebugger.impl.actions;

import consulo.application.dumb.DumbAware;
import consulo.execution.debug.XDebuggerUtil;
import consulo.ide.impl.idea.xdebugger.impl.settings.XDebuggerSettingManagerImpl;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;

/**
 * @author Konstantin Bulenkov
 */
public class UseInlineDebuggerAction extends ToggleAction implements DumbAware {
  @Override
  public boolean isSelected(AnActionEvent e) {
    return XDebuggerSettingManagerImpl.getInstanceImpl().getDataViewSettings().isShowValuesInline();
  }

  @Override
  public void setSelected(AnActionEvent e, boolean state) {
    XDebuggerSettingManagerImpl.getInstanceImpl().getDataViewSettings().setShowValuesInline(state);
    XDebuggerUtil.getInstance().rebuildAllSessionsViews(e.getData(CommonDataKeys.PROJECT));
  }
}
