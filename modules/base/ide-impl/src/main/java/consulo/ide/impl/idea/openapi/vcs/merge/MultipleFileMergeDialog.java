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

package consulo.ide.impl.idea.openapi.vcs.merge;

import consulo.application.CommonBundle;
import consulo.diff.DiffManager;
import consulo.ide.impl.idea.diff.DiffRequestFactory;
import consulo.ide.impl.idea.diff.InvalidDiffRequestException;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.ide.impl.idea.diff.merge.MergeUtil;
import consulo.ide.impl.idea.diff.util.DiffUtil;
import consulo.vcs.merge.*;
import consulo.virtualFileSystem.VirtualFilePresentation;
import consulo.application.ApplicationManager;
import consulo.undoRedo.CommandProcessor;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.project.internal.ProjectManagerEx;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.util.lang.ref.Ref;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.vcs.VcsBundle;
import consulo.vcs.VcsException;
import consulo.vcs.change.VcsDirtyScopeManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.ColoredTableCellRenderer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.table.TableView;
import consulo.ide.impl.idea.util.Consumer;
import consulo.util.collection.SmartList;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.ex.awt.UIUtil;
import consulo.logging.Logger;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;

/**
 * @author yole
 */
public class MultipleFileMergeDialog extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(MultipleFileMergeDialog.class);

  private JPanel myRootPanel;
  private JButton myAcceptYoursButton;
  private JButton myAcceptTheirsButton;
  private JButton myMergeButton;
  private TableView<VirtualFile> myTable;
  private JBLabel myDescriptionLabel;
  private final MergeProvider myProvider;
  private final MergeSession myMergeSession;
  private final List<VirtualFile> myFiles;
  private final ListTableModel<VirtualFile> myModel;
  @Nullable
  private final Project myProject;
  private final ProjectManagerEx myProjectManager;
  private final List<VirtualFile> myProcessedFiles = new SmartList<VirtualFile>();
  private final Set<VirtualFile> myBinaryFiles = new HashSet<VirtualFile>();
  private final MergeDialogCustomizer myMergeDialogCustomizer;

  private final VirtualFileRenderer myVirtualFileRenderer = new VirtualFileRenderer();

  public MultipleFileMergeDialog(@Nullable Project project, @Nonnull final List<VirtualFile> files, @Nonnull final MergeProvider provider,
                                 @Nonnull MergeDialogCustomizer mergeDialogCustomizer) {
    super(project);

    myProject = project;
    myProjectManager = ProjectManagerEx.getInstanceEx();
    myProjectManager.blockReloadingProjectOnExternalChanges();
    myFiles = new ArrayList<VirtualFile>(files);
    myProvider = provider;
    myMergeDialogCustomizer = mergeDialogCustomizer;

    final String description = myMergeDialogCustomizer.getMultipleFileMergeDescription(files);
    if (!StringUtil.isEmptyOrSpaces(description)) {
      myDescriptionLabel.setText(description);
    }

    List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
    columns.add(new ColumnInfo<VirtualFile, VirtualFile>(VcsBundle.message("multiple.file.merge.column.name")) {
      @Override
      public VirtualFile valueOf(final VirtualFile virtualFile) {
        return virtualFile;
      }

      @Override
      public TableCellRenderer getRenderer(final VirtualFile virtualFile) {
        return myVirtualFileRenderer;
      }
    });
    columns.add(new ColumnInfo<VirtualFile, String>(VcsBundle.message("multiple.file.merge.column.type")) {
      @Override
      public String valueOf(final VirtualFile virtualFile) {
        return myBinaryFiles.contains(virtualFile)
               ? VcsBundle.message("multiple.file.merge.type.binary")
               : VcsBundle.message("multiple.file.merge.type.text");
      }

      @Override
      public String getMaxStringValue() {
        return VcsBundle.message("multiple.file.merge.type.binary");
      }

      @Override
      public int getAdditionalWidth() {
        return 10;
      }
    });
    if (myProvider instanceof MergeProvider2) {
      myMergeSession = ((MergeProvider2)myProvider).createMergeSession(files);
      Collections.addAll(columns, myMergeSession.getMergeInfoColumns());
    }
    else {
      myMergeSession = null;
    }
    myModel = new ListTableModel<VirtualFile>(columns.toArray(new ColumnInfo[columns.size()]));
    myModel.setItems(files);
    myTable.setModelAndUpdateColumns(myModel);
    myVirtualFileRenderer.setFont(UIUtil.getListFont());
    myTable.setRowHeight(myVirtualFileRenderer.getPreferredSize().height);
    setTitle(myMergeDialogCustomizer.getMultipleFileDialogTitle());
    init();
    myAcceptYoursButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@Nonnull ActionEvent e) {
        acceptRevision(true);
      }
    });
    myAcceptTheirsButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@Nonnull ActionEvent e) {
        acceptRevision(false);
      }
    });
    myTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(@Nonnull final ListSelectionEvent e) {
        updateButtonState();
      }
    });
    for (VirtualFile file : files) {
      if (file.getFileType().isBinary() || provider.isBinary(file)) {
        myBinaryFiles.add(file);
      }
    }
    myTable.getSelectionModel().setSelectionInterval(0, 0);
  }

  private void updateButtonState() {
    boolean haveSelection = myTable.getSelectedRowCount() > 0;
    boolean haveUnmergeableFiles = false;
    for (VirtualFile file : myTable.getSelection()) {
      if (myMergeSession != null) {
        boolean canMerge = myMergeSession.canMerge(file);
        if (!canMerge) {
          haveUnmergeableFiles = true;
          break;
        }
      }
    }
    myAcceptYoursButton.setEnabled(haveSelection);
    myAcceptTheirsButton.setEnabled(haveSelection);
    myMergeButton.setEnabled(haveSelection && !haveUnmergeableFiles);
  }

  @Override
  @Nullable
  protected JComponent createCenterPanel() {
    return myRootPanel;
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    return new Action[]{getCancelAction()};
  }

  @Nonnull
  @Override
  protected Action getCancelAction() {
    Action action = super.getCancelAction();
    action.putValue(Action.NAME, CommonBundle.getCloseButtonText());
    return action;
  }

  @Override
  protected void dispose() {
    myProjectManager.unblockReloadingProjectOnExternalChanges();
    super.dispose();
  }

  protected boolean beforeResolve(Collection<VirtualFile> files) {
    return true;
  }

  @Override
  @NonNls
  protected String getDimensionServiceKey() {
    return "MultipleFileMergeDialog";
  }

  private void acceptRevision(final boolean isCurrent) {
    FileDocumentManager.getInstance().saveAllDocuments();
    final Collection<VirtualFile> files = myTable.getSelection();
    if (!beforeResolve(files)) {
      return;
    }

    for (final VirtualFile file : files) {
      final Ref<Exception> ex = new Ref<Exception>();
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
            @Override
            public void run() {
              try {
                if (!(myProvider instanceof MergeProvider2) || myMergeSession.canMerge(file)) {
                  if (!DiffUtil.makeWritable(myProject, file)) {
                    throw new IOException("File is read-only: " + file.getPresentableName());
                  }
                  MergeData data = myProvider.loadRevisions(file);
                  if (isCurrent) {
                    file.setBinaryContent(data.CURRENT);
                  }
                  else {
                    file.setBinaryContent(data.LAST);
                    checkMarkModifiedProject(file);
                  }
                }
                markFileProcessed(file, isCurrent ? MergeSession.Resolution.AcceptedYours : MergeSession.Resolution.AcceptedTheirs);
              }
              catch (Exception e) {
                ex.set(e);
              }
            }
          }, "Accept " + (isCurrent ? "Yours" : "Theirs"), null);
        }
      });
      if (!ex.isNull()) {
        //noinspection ThrowableResultOfMethodCallIgnored
        Messages.showErrorDialog(myRootPanel, "Error saving merged data: " + ex.get().getMessage());
        break;
      }
    }
    updateModelFromFiles();
  }

  private void markFileProcessed(@Nonnull VirtualFile file, @Nonnull MergeSession.Resolution resolution) {
    myFiles.remove(file);
    if (myProvider instanceof MergeProvider2) {
      myMergeSession.conflictResolvedForFile(file, resolution);
    }
    else {
      myProvider.conflictResolvedForFile(file);
    }
    myProcessedFiles.add(file);
    if (myProject != null) {
      VcsDirtyScopeManager.getInstance(myProject).fileDirty(file);
    }
  }

  private void updateModelFromFiles() {
    if (myFiles.isEmpty()) {
      doCancelAction();
    }
    else {
      int selIndex = myTable.getSelectionModel().getMinSelectionIndex();
      myModel.setItems(myFiles);
      if (selIndex >= myFiles.size()) {
        selIndex = myFiles.size() - 1;
      }
      myTable.getSelectionModel().setSelectionInterval(selIndex, selIndex);
    }
  }

  private void showMergeDialog() {
    DiffRequestFactory requestFactory = DiffRequestFactory.getInstance();
    Collection<VirtualFile> files = myTable.getSelection();
    if (!beforeResolve(files)) {
      return;
    }

    for (final VirtualFile file : files) {
      final MergeData mergeData;
      try {
        mergeData = myProvider.loadRevisions(file);
      }
      catch (VcsException ex) {
        Messages.showErrorDialog(myRootPanel, "Error loading revisions to merge: " + ex.getMessage());
        break;
      }

      if (mergeData.CURRENT == null || mergeData.LAST == null || mergeData.ORIGINAL == null) {
        Messages.showErrorDialog(myRootPanel, "Error loading revisions to merge");
        break;
      }

      String leftTitle = myMergeDialogCustomizer.getLeftPanelTitle(file);
      String baseTitle = myMergeDialogCustomizer.getCenterPanelTitle(file);
      String rightTitle = myMergeDialogCustomizer.getRightPanelTitle(file, mergeData.LAST_REVISION_NUMBER);
      String title = myMergeDialogCustomizer.getMergeWindowTitle(file);

      final List<byte[]> byteContents = ContainerUtil.list(mergeData.CURRENT, mergeData.ORIGINAL, mergeData.LAST);
      List<String> contentTitles = ContainerUtil.list(leftTitle, baseTitle, rightTitle);

      Consumer<MergeResult> callback = new Consumer<MergeResult>() {
        @Override
        public void consume(final MergeResult result) {
          Document document = FileDocumentManager.getInstance().getCachedDocument(file);
          if (document != null) FileDocumentManager.getInstance().saveDocument(document);
          checkMarkModifiedProject(file);

          if (result != MergeResult.CANCEL) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
              @Override
              public void run() {
                markFileProcessed(file, getSessionResolution(result));
              }
            });
          }
        }
      };

      MergeRequest request;
      try {
        if (myProvider.isBinary(file)) { // respect MIME-types in svn
          request = requestFactory.createBinaryMergeRequest(myProject, file, byteContents, title, contentTitles, callback);
        }
        else {
          request = requestFactory.createMergeRequest(myProject, file, byteContents, title, contentTitles, callback);
        }
      }
      catch (InvalidDiffRequestException e) {
        LOG.error(e);
        Messages.showErrorDialog(myRootPanel, "Can't show merge dialog");
        break;
      }

      DiffManager.getInstance().showMerge(myProject, request);
    }
    updateModelFromFiles();
  }

  @Nonnull
  private static MergeSession.Resolution getSessionResolution(@Nonnull MergeResult result) {
    switch (result) {
      case LEFT:
        return MergeSession.Resolution.AcceptedYours;
      case RIGHT:
        return MergeSession.Resolution.AcceptedTheirs;
      case RESOLVED:
        return MergeSession.Resolution.Merged;
      default:
        throw new IllegalArgumentException(result.name());
    }
  }

  private void checkMarkModifiedProject(@Nonnull VirtualFile file) {
    MergeUtil.reportProjectFileChangeIfNeeded(myProject, file);
  }

  private void createUIComponents() {
    Action mergeAction = new AbstractAction() {
      @Override
      public void actionPerformed(@Nonnull ActionEvent e) {
        showMergeDialog();
      }
    };
    mergeAction.putValue(DEFAULT_ACTION, Boolean.TRUE);
    myMergeButton = createJButtonForAction(mergeAction);
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myTable;
  }

  @Nonnull
  public List<VirtualFile> getProcessedFiles() {
    return myProcessedFiles;
  }

  private static class VirtualFileRenderer extends ColoredTableCellRenderer {
    @Override
    protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
      VirtualFile vf = (VirtualFile)value;
      setIcon(VirtualFilePresentation.getIcon(vf));
      append(vf.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
      final VirtualFile parent = vf.getParent();
      if (parent != null) {
        append(" (" + FileUtil.toSystemDependentName(parent.getPresentableUrl()) + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }
    }
  }
}
