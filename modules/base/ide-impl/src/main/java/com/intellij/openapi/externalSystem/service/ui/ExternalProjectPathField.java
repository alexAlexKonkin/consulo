/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.ui;

import consulo.language.editor.completion.CompletionResultSet;
import consulo.application.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingModel;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.externalSystem.service.task.ui.ExternalSystemNode;
import com.intellij.openapi.externalSystem.service.task.ui.ExternalSystemTasksTree;
import com.intellij.openapi.externalSystem.service.task.ui.ExternalSystemTasksTreeModel;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.project.Project;
import consulo.ui.ex.awt.ComponentWithBrowseButton;
import consulo.ui.ex.awt.FixedSizeButton;
import consulo.ui.ex.popup.JBPopup;
import consulo.ide.ui.impl.PopupChooserBuilder;
import consulo.util.lang.ref.Ref;
import com.intellij.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.ui.EditorTextField;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.TextAccessor;
import consulo.ui.ex.awt.tree.Tree;
import com.intellij.util.Consumer;
import com.intellij.util.TextFieldCompletionProvider;
import com.intellij.util.TextFieldCompletionProviderDumbAware;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.ui.GridBag;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.fileChooser.FileChooser;

import javax.annotation.Nonnull;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Denis Zhdanov
 * @since 24.05.13 19:13
 */
public class ExternalProjectPathField extends ComponentWithBrowseButton<ExternalProjectPathField.MyPathAndProjectButtonPanel>
  implements TextAccessor
{

  @Nonnull
  private static final String PROJECT_FILE_TO_START_WITH_KEY = "external.system.task.project.file.to.start";

  @Nonnull
  private final Project         myProject;
  @Nonnull
  private final ProjectSystemId myExternalSystemId;

  public ExternalProjectPathField(@Nonnull Project project,
                                  @Nonnull ProjectSystemId externalSystemId,
                                  @Nonnull FileChooserDescriptor descriptor,
                                  @Nonnull String fileChooserTitle)
  {
    super(createPanel(project, externalSystemId), new MyBrowseListener(descriptor, fileChooserTitle, project));
    ActionListener[] listeners = getButton().getActionListeners();
    for (ActionListener listener : listeners) {
      if (listener instanceof MyBrowseListener) {
        ((MyBrowseListener)listener).setPathField(getChildComponent().getTextField());
        break;
      }
    }
    myProject = project;
    myExternalSystemId = externalSystemId;
  }

  @Nonnull
  private static MyPathAndProjectButtonPanel createPanel(@Nonnull final Project project, @Nonnull final ProjectSystemId externalSystemId) {
    final EditorTextField textField = createTextField(project, externalSystemId);
    
    final FixedSizeButton selectRegisteredProjectButton = new FixedSizeButton();
    selectRegisteredProjectButton.setIcon(TargetAWT.to(AllIcons.Actions.Module));
    String tooltipText = ExternalSystemBundle.message("run.configuration.tooltip.choose.registered.project",
                                                      externalSystemId.getReadableName());
    selectRegisteredProjectButton.setToolTipText(tooltipText);
    selectRegisteredProjectButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final Ref<JBPopup> popupRef = new Ref<JBPopup>();
        final Tree tree = buildRegisteredProjectsTree(project, externalSystemId);
        tree.setBorder(IdeBorderFactory.createEmptyBorder(8));
        Runnable treeSelectionCallback = new Runnable() {
          @Override
          public void run() {
            TreePath path = tree.getSelectionPath();
            if (path != null) {
              Object lastPathComponent = path.getLastPathComponent();
              if (lastPathComponent instanceof ExternalSystemNode) {
                Object e = ((ExternalSystemNode)lastPathComponent).getDescriptor().getElement();
                if (e instanceof ExternalProjectPojo) {
                  ExternalProjectPojo pojo = (ExternalProjectPojo)e;
                  textField.setText(pojo.getPath());
                  Editor editor = textField.getEditor();
                  if (editor != null) {
                    collapseIfPossible(editor, externalSystemId, project);
                  }
                }
              }
            }
            popupRef.get().closeOk(null); 
          }
        };
        JBPopup popup = new PopupChooserBuilder(tree)
          .setTitle(ExternalSystemBundle.message("run.configuration.title.choose.registered.project", externalSystemId.getReadableName()))
          .setResizable(true)
          .setItemChoosenCallback(treeSelectionCallback)
          .setAutoselectOnMouseMove(true)
          .setCloseOnEnter(false)
          .createPopup();
        popupRef.set(popup);
        popup.showUnderneathOf(selectRegisteredProjectButton);
      }
    });
    return new MyPathAndProjectButtonPanel(textField, selectRegisteredProjectButton);
  }

  @Nonnull
  private static Tree buildRegisteredProjectsTree(@Nonnull Project project, @Nonnull ProjectSystemId externalSystemId) {
    ExternalSystemTasksTreeModel model = new ExternalSystemTasksTreeModel(externalSystemId);
    ExternalSystemTasksTree result = new ExternalSystemTasksTree(model, ContainerUtilRt.<String, Boolean>newHashMap(), project, externalSystemId);
    
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    assert manager != null;
    AbstractExternalSystemLocalSettings settings = manager.getLocalSettingsProvider().fun(project);
    Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> projects = settings.getAvailableProjects();
    List<ExternalProjectPojo> rootProjects = ContainerUtilRt.newArrayList(projects.keySet());
    ContainerUtil.sort(rootProjects);
    for (ExternalProjectPojo rootProject : rootProjects) {
      model.ensureSubProjectsStructure(rootProject, projects.get(rootProject));
    }
    return result;
  }
  
  @Nonnull
  private static EditorTextField createTextField(@Nonnull final Project project, @Nonnull final ProjectSystemId externalSystemId) {
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    assert manager != null;
    final AbstractExternalSystemLocalSettings settings = manager.getLocalSettingsProvider().fun(project);
    final ExternalSystemUiAware uiAware = ExternalSystemUiUtil.getUiAware(externalSystemId);
    TextFieldCompletionProvider provider = new TextFieldCompletionProviderDumbAware() {
      @Override
      protected void addCompletionVariants(@Nonnull String text, int offset, @Nonnull String prefix, @Nonnull CompletionResultSet result) {
        for (Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>> entry : settings.getAvailableProjects().entrySet()) {
          String rootProjectPath = entry.getKey().getPath();
          String rootProjectName = uiAware.getProjectRepresentationName(rootProjectPath, null);
          ExternalProjectPathLookupElement rootProjectElement = new ExternalProjectPathLookupElement(rootProjectName, rootProjectPath);
          result.addElement(rootProjectElement);
          for (ExternalProjectPojo subProject : entry.getValue()) {
            String p = subProject.getPath();
            if (rootProjectPath.equals(p)) {
              continue;
            }
            String subProjectName = uiAware.getProjectRepresentationName(p, rootProjectPath);
            ExternalProjectPathLookupElement subProjectElement = new ExternalProjectPathLookupElement(subProjectName, p);
            result.addElement(subProjectElement);
          }
        }
        result.stopHere();
      }
    };
    EditorTextField result = provider.createEditor(project, false, new Consumer<Editor>() {
      @Override
      public void consume(Editor editor) {
        collapseIfPossible(editor, externalSystemId, project);
        editor.getSettings().setShowIntentionBulb(false);
      }
    });
    result.setBorder(UIUtil.getTextFieldBorder());
    result.setOneLineMode(true);
    result.setOpaque(true);
    result.setBackground(UIUtil.getTextFieldBackground());
    return result;
  }
  
  @Override
  public void setText(final String text) {
    getChildComponent().getTextField().setText(text);

    Editor editor = getChildComponent().getTextField().getEditor();
    if (editor != null) {
      collapseIfPossible(editor, myExternalSystemId, myProject);
    }
  }

  private static void collapseIfPossible(@Nonnull final Editor editor,
                                         @Nonnull ProjectSystemId externalSystemId,
                                         @Nonnull Project project)
  {
    ExternalSystemManager<?,?,?,?,?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    assert manager != null;
    final AbstractExternalSystemLocalSettings settings = manager.getLocalSettingsProvider().fun(project);
    final ExternalSystemUiAware uiAware = ExternalSystemUiUtil.getUiAware(externalSystemId);

    String rawText = editor.getDocument().getText();
    for (Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>> entry : settings.getAvailableProjects().entrySet()) {
      if (entry.getKey().getPath().equals(rawText)) {
        collapse(editor, uiAware.getProjectRepresentationName(entry.getKey().getPath(), null));
        return;
      }
      for (ExternalProjectPojo pojo : entry.getValue()) {
        if (pojo.getPath().equals(rawText)) {
          collapse(editor, uiAware.getProjectRepresentationName(pojo.getPath(), entry.getKey().getPath()));
          return;
        }
      }
    }
  }

  private static void collapse(@Nonnull final Editor editor, @Nonnull final String placeholder) {
    final FoldingModel foldingModel = editor.getFoldingModel();
    foldingModel.runBatchFoldingOperation(new Runnable() {
      @Override
      public void run() {
        for (FoldRegion region : foldingModel.getAllFoldRegions()) {
          foldingModel.removeFoldRegion(region);
        }
        FoldRegion region = foldingModel.addFoldRegion(0, editor.getDocument().getTextLength(), placeholder);
        if (region != null) {
          region.setExpanded(false);
        }
      }
    });
  }

  @Override
  public String getText() {
    return getChildComponent().getTextField().getText();
  }
  
  private static class MyBrowseListener implements ActionListener {
    
    @Nonnull
    private final FileChooserDescriptor myDescriptor;
    @Nonnull
    private final Project myProject;
    private EditorTextField myPathField;
    
    MyBrowseListener(@Nonnull final FileChooserDescriptor descriptor,
                     @Nonnull final String fileChooserTitle,
                     @Nonnull final Project project)
    {
      descriptor.setTitle(fileChooserTitle);
      myDescriptor = descriptor;
      myProject = project;
    }

    private void setPathField(@Nonnull EditorTextField pathField) {
      myPathField = pathField;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(ActionEvent e) {
      if (myPathField == null) {
        assert false;
        return;
      }
      PropertiesComponent component = PropertiesComponent.getInstance(myProject);
      String pathToStart = myPathField.getText();
      if (StringUtil.isEmpty(pathToStart)) {
        pathToStart = component.getValue(PROJECT_FILE_TO_START_WITH_KEY);
      }
      VirtualFile fileToStart = null;
      if (!StringUtil.isEmpty(pathToStart)) {
        fileToStart = LocalFileSystem.getInstance().findFileByPath(pathToStart);
      }

      FileChooser.chooseFile(myDescriptor, myProject, fileToStart).doWhenDone(file -> {
        String path = ExternalSystemApiUtil.getLocalFileSystemPath(file);
        myPathField.setText(path);
        component.setValue(PROJECT_FILE_TO_START_WITH_KEY, path);
      });
    }
  }
  
  public static class MyPathAndProjectButtonPanel extends JPanel {

    @Nonnull
    private final EditorTextField myTextField;
    @Nonnull
    private final FixedSizeButton myRegisteredProjectsButton;

    public MyPathAndProjectButtonPanel(@Nonnull EditorTextField textField,
                                       @Nonnull FixedSizeButton registeredProjectsButton)
    {
      super(new GridBagLayout());
      myTextField = textField;
      myRegisteredProjectsButton = registeredProjectsButton;
      add(myTextField, new GridBag().weightx(1).fillCellHorizontally());
      add(myRegisteredProjectsButton, new GridBag().insets(0, 3, 0, 0));
    }

    @Nonnull
    public EditorTextField getTextField() {
      return myTextField;
    }
  }
}
