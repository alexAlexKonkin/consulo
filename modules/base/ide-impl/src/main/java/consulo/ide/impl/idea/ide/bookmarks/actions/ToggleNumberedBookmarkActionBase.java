/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

/*
 * @author max
 */
package consulo.ide.impl.idea.ide.bookmarks.actions;

import consulo.ide.impl.idea.ide.bookmarks.BookmarkImpl;
import consulo.ide.impl.idea.ide.bookmarks.BookmarkManagerImpl;
import consulo.dataContext.DataContext;
import consulo.application.dumb.DumbAware;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

public abstract class ToggleNumberedBookmarkActionBase extends AnAction implements DumbAware {
  private final int myNumber;

  public ToggleNumberedBookmarkActionBase(int n) {
    myNumber = n;
  }

  @Override
  public void update(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    e.getPresentation().setEnabled(project != null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);

    BookmarksAction.BookmarkInContextInfo info = new BookmarksAction.BookmarkInContextInfo(dataContext, project).invoke();
    if (info.getFile() == null) return;

    final BookmarkImpl oldBookmark = info.getBookmarkAtPlace();

    final BookmarkManagerImpl manager = BookmarkManagerImpl.getInstance(project);
    if (oldBookmark != null) {
      manager.removeBookmark(oldBookmark);
    }

    if (oldBookmark == null || oldBookmark.getMnemonic() != '0' + myNumber) {
      final BookmarkImpl bookmark = manager.addTextBookmark(info.getFile(), info.getLine(), "");
      manager.setMnemonic(bookmark, (char)('0' + myNumber));
    }
  }
}
