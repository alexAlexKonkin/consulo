// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.actions.GotoFileAction;
import com.intellij.ide.util.gotoByName.FilteringGotoByModel;
import com.intellij.ide.util.gotoByName.GotoFileConfiguration;
import com.intellij.ide.util.gotoByName.GotoFileModel;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import com.intellij.openapi.fileEditor.OpenFileDescriptorImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.file.FileTypeManager;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import com.intellij.ui.IdeUICustomization;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Konstantin Bulenkov
 * @author Mikhail Sokolov
 */
public class FileSearchEverywhereContributor extends AbstractGotoSEContributor {
  private final GotoFileModel myModelForRenderer;
  private final PersistentSearchEverywhereContributorFilter<FileType> myFilter;

  public FileSearchEverywhereContributor(@Nullable Project project, @Nullable PsiElement context) {
    super(project, context);
    myModelForRenderer = project == null ? null : new GotoFileModel(project);
    myFilter = project == null ? null : createFileTypeFilter(project);
  }

  @Nonnull
  @Override
  public String getGroupName() {
    return "Files";
  }

  public String includeNonProjectItemsText() {
    return IdeBundle.message("checkbox.include.non.project.files", IdeUICustomization.getInstance().getProjectConceptName());
  }

  @Override
  public int getSortWeight() {
    return 200;
  }

  @Override
  public int getElementPriority(@Nonnull Object element, @Nonnull String searchPattern) {
    return super.getElementPriority(element, searchPattern) + 2;
  }

  @Nonnull
  @Override
  protected FilteringGotoByModel<FileType> createModel(@Nonnull Project project) {
    GotoFileModel model = new GotoFileModel(project);
    if (myFilter != null) {
      model.setFilterItems(myFilter.getSelectedElements());
    }
    return model;
  }

  @Nonnull
  @Override
  public List<AnAction> getActions(@Nonnull Runnable onChanged) {
    return doGetActions(includeNonProjectItemsText(), myFilter, onChanged);
  }

  @Nonnull
  @Override
  public ListCellRenderer<Object> getElementsRenderer() {
    //noinspection unchecked
    return new SERenderer() {
      @Nonnull
      @Override
      protected ItemMatchers getItemMatchers(@Nonnull JList list, @Nonnull Object value) {
        ItemMatchers defaultMatchers = super.getItemMatchers(list, value);
        if (!(value instanceof PsiFileSystemItem) || myModelForRenderer == null) {
          return defaultMatchers;
        }

        return GotoFileModel.convertToFileItemMatchers(defaultMatchers, (PsiFileSystemItem)value, myModelForRenderer);
      }
    };
  }

  @Override
  public boolean processSelectedItem(@Nonnull Object selected, int modifiers, @Nonnull String searchText) {
    if (selected instanceof PsiFile) {
      VirtualFile file = ((PsiFile)selected).getVirtualFile();
      if (file != null && myProject != null) {
        Pair<Integer, Integer> pos = getLineAndColumn(searchText);
        OpenFileDescriptorImpl descriptor = new OpenFileDescriptorImpl(myProject, file, pos.first, pos.second);
        descriptor.setUseCurrentWindow(openInCurrentWindow(modifiers));
        if (descriptor.canNavigate()) {
          descriptor.navigate(true);
          return true;
        }
      }
    }

    return super.processSelectedItem(selected, modifiers, searchText);
  }

  @Override
  public Object getDataForItem(@Nonnull Object element, @Nonnull Key dataId) {
    if (CommonDataKeys.PSI_FILE == dataId && element instanceof PsiFile) {
      return element;
    }

    if (SearchEverywhereDataKeys.ITEM_STRING_DESCRIPTION == dataId && element instanceof PsiFile) {
      String path = ((PsiFile)element).getVirtualFile().getPath();
      path = FileUtil.toSystemIndependentName(path);
      if (myProject != null) {
        String basePath = myProject.getBasePath();
        if (basePath != null) {
          path = FileUtil.getRelativePath(basePath, path, '/');
        }
      }
      return path;
    }

    return super.getDataForItem(element, dataId);
  }

  public static class Factory implements SearchEverywhereContributorFactory<Object> {
    @Nonnull
    @Override
    public SearchEverywhereContributor<Object> createContributor(@Nonnull AnActionEvent initEvent) {
      return new FileSearchEverywhereContributor(initEvent.getData(CommonDataKeys.PROJECT), GotoActionBase.getPsiContext(initEvent));
    }
  }

  @Nonnull
  public static PersistentSearchEverywhereContributorFilter<FileType> createFileTypeFilter(@Nonnull Project project) {
    List<FileType> items = Stream.of(FileTypeManager.getInstance().getRegisteredFileTypes()).sorted(GotoFileAction.FileTypeComparator.INSTANCE).collect(Collectors.toList());
    return new PersistentSearchEverywhereContributorFilter<>(items, GotoFileConfiguration.getInstance(project), FileType::getName, FileType::getIcon);
  }
}
