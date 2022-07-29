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

package consulo.ide.impl.idea.history.integration.ui.actions;

import consulo.ide.impl.idea.history.core.LocalHistoryFacade;
import consulo.ide.impl.idea.history.integration.IdeaGateway;
import consulo.ide.impl.idea.history.integration.ui.views.SelectionHistoryDialog;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.vcs.action.VcsContext;
import consulo.ide.impl.idea.openapi.vcs.actions.VcsContextWrapper;
import consulo.virtualFileSystem.VirtualFile;
import consulo.vcs.history.VcsSelection;
import consulo.ide.impl.idea.vcsUtil.VcsSelectionUtil;
import javax.annotation.Nullable;

public class ShowSelectionHistoryAction extends ShowHistoryAction {
  @Override
  protected void showDialog(Project p, IdeaGateway gw, VirtualFile f, AnActionEvent e) {
    VcsSelection sel = getSelection(e);

    int from = sel.getSelectionStartLineNumber();
    int to = sel.getSelectionEndLineNumber();

    new SelectionHistoryDialog(p, gw, f, from, to).show();
  }

  @Override
  protected String getText(AnActionEvent e) {
    VcsSelection sel = getSelection(e);
    return sel == null ? super.getText(e) : sel.getActionName();
  }

  @Override
  public void update(AnActionEvent e) {
    if (e.getData(PlatformDataKeys.EDITOR) == null) {
      e.getPresentation().setVisible(false);
    }
    else {
      super.update(e);
    }
  }

  @Override
  protected boolean isEnabled(LocalHistoryFacade vcs, IdeaGateway gw, VirtualFile f, AnActionEvent e) {
    return super.isEnabled(vcs, gw, f, e) && !f.isDirectory() && getSelection(e) != null;
  }

  @Nullable
  private static VcsSelection getSelection(AnActionEvent e) {
    VcsContext c = VcsContextWrapper.createCachedInstanceOn(e);
    return VcsSelectionUtil.getSelection(c);
  }
}
