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

/*
 * @author max
 */
package consulo.ide.impl.idea.ide.bookmarks.actions;

import consulo.ide.impl.idea.ide.bookmarks.BookmarkImpl;
import consulo.ide.impl.idea.ide.bookmarks.BookmarkItem;
import consulo.ide.impl.idea.ide.bookmarks.BookmarkManagerImpl;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.ide.impl.idea.ui.popup.util.DetailViewImpl;
import consulo.ide.impl.idea.ui.popup.util.ItemWrapper;
import consulo.ide.impl.idea.ui.popup.util.MasterDetailPopupBuilder;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.speedSearch.FilteringListModel;
import consulo.ui.ex.popup.JBPopup;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: remove duplication with BaseShowRecentFilesAction, there's quite a bit of it

public class BookmarksAction extends AnAction implements DumbAware, MasterDetailPopupBuilder.Delegate {

  private JBPopup myPopup;

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
    if (project == null) return;

    if (myPopup != null && myPopup.isVisible()) return;

    final DefaultListModel model = buildModel(project);

    final JBList list = new JBList(model);
    list.getEmptyText().setText("No Bookmarks");

    EditBookmarkDescriptionAction editDescriptionAction = new EditBookmarkDescriptionAction(project, list);
    DefaultActionGroup actions = new DefaultActionGroup();
    actions.add(editDescriptionAction);
    actions.add(new DeleteBookmarkAction(project, list));
    actions.add(new MoveBookmarkUpAction(project, list));
    actions.add(new MoveBookmarkDownAction(project, list));

    myPopup = new MasterDetailPopupBuilder(project)
      .setActionsGroup(actions)
      .setList(list)
      .setDetailView(new DetailViewImpl(project))
      .setCloseOnEnter(false)
      .setDoneRunnable(new Runnable() {
        @Override
        public void run() {
          myPopup.cancel();
        }
      })
      .setDelegate(this).createMasterDetailPopup();
    new AnAction() {
      @Override
      public void actionPerformed(AnActionEvent e) {
        Object selectedValue = list.getSelectedValue();
        if (selectedValue instanceof BookmarkItem) {
          itemChosen((BookmarkItem)selectedValue, project, myPopup, true);
        }
      }
    }.registerCustomShortcutSet(CommonShortcuts.getEditSource(), list);
    editDescriptionAction.setPopup(myPopup);
    myPopup.showCenteredInCurrentWindow(project);
    //todo[zaec] selection mode shouldn't be set in builder.setList() method
    list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
  }

  @Override
  public String getTitle() {
    return "Bookmarks";
  }

  @Override
  public void handleMnemonic(KeyEvent e, Project project, JBPopup popup) {
    char mnemonic = e.getKeyChar();
    final BookmarkImpl bookmark = BookmarkManagerImpl.getInstance(project).findBookmarkForMnemonic(mnemonic);
    if (bookmark != null) {
      popup.cancel();
      ProjectIdeFocusManager.getInstance(project).doWhenFocusSettlesDown(new Runnable() {
        @Override
        public void run() {
          bookmark.navigate(true);
        }
      });
    }
  }

  @Override
  @Nullable
  public JComponent createAccessoryView(Project project) {
    if (!BookmarkManagerImpl.getInstance(project).hasBookmarksWithMnemonics()) {
      return null;
    }
    final JLabel mnemonicLabel = new JLabel();
    mnemonicLabel.setFont(BookmarkImpl.MNEMONIC_FONT);

    mnemonicLabel.setPreferredSize(new JLabel("W.").getPreferredSize());
    mnemonicLabel.setOpaque(false);
    return mnemonicLabel;
  }

  @Override
  public Object[] getSelectedItemsInTree() {
    return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void itemChosen(ItemWrapper item, Project project, JBPopup popup, boolean withEnterOrDoubleClick) {
    if (item instanceof BookmarkItem && withEnterOrDoubleClick) {
      BookmarkImpl bookmark = ((BookmarkItem)item).getBookmark();
      popup.cancel();
      bookmark.navigate(true);
    }
  }

  @Override
  public void removeSelectedItemsInTree() {

  }

  private static DefaultListModel buildModel(Project project) {
    final DefaultListModel model = new DefaultListModel();

    for (BookmarkImpl bookmark : BookmarkManagerImpl.getInstance(project).getValidBookmarks()) {
      model.addElement(new BookmarkItem(bookmark));
    }

    return model;
  }

  protected static class BookmarkInContextInfo {
    private final DataContext myDataContext;
    private final Project myProject;
    private BookmarkImpl myBookmarkAtPlace;
    private VirtualFile myFile;
    private int myLine;

    public BookmarkInContextInfo(DataContext dataContext, Project project) {
      myDataContext = dataContext;
      myProject = project;
    }

    public BookmarkImpl getBookmarkAtPlace() {
      return myBookmarkAtPlace;
    }

    public VirtualFile getFile() {
      return myFile;
    }

    public int getLine() {
      return myLine;
    }

    public BookmarkInContextInfo invoke() {
      myBookmarkAtPlace = null;
      myFile = null;
      myLine = -1;


      BookmarkManagerImpl bookmarkManager = BookmarkManagerImpl.getInstance(myProject);
      if (ToolWindowManager.getInstance(myProject).isEditorComponentActive()) {
        Editor editor = myDataContext.getData(PlatformDataKeys.EDITOR);
        if (editor != null) {
          Document document = editor.getDocument();
          myLine = editor.getCaretModel().getLogicalPosition().line;
          myFile = FileDocumentManager.getInstance().getFile(document);
          myBookmarkAtPlace = bookmarkManager.findEditorBookmark(document, myLine);
        }
      }

      if (myFile == null) {
        myFile = myDataContext.getData(PlatformDataKeys.VIRTUAL_FILE);
        myLine = -1;

        if (myBookmarkAtPlace == null && myFile != null) {
          myBookmarkAtPlace = bookmarkManager.findFileBookmark(myFile);
        }
      }
      return this;
    }
  }

  static List<BookmarkImpl> getSelectedBookmarks(JList list) {
    List<BookmarkImpl> answer = new ArrayList<BookmarkImpl>();

    for (Object value : list.getSelectedValues()) {
      if (value instanceof BookmarkItem) {
        answer.add(((BookmarkItem)value).getBookmark());
      }
      else {
        return Collections.emptyList();
      }
    }

    return answer;
  }

  static boolean notFiltered(JList list) {
    if (!(list.getModel() instanceof FilteringListModel)) return true;
    final FilteringListModel model = (FilteringListModel)list.getModel();
    return model.getOriginalModel().getSize() == model.getSize();
  }

}
