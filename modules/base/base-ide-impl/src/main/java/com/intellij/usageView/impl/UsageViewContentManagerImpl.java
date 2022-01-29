// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.usageView.impl;

import com.intellij.find.FindBundle;
import com.intellij.find.FindSettings;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.impl.ContentManagerWatcher;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareToggleAction;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.project.ui.wm.ToolWindow;
import consulo.project.ui.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.UIBundle;
import com.intellij.ui.content.*;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usageView.UsageViewContentManager;
import com.intellij.usages.UsageView;
import com.intellij.usages.UsageViewSettings;
import com.intellij.usages.rules.UsageFilteringRuleProvider;
import com.intellij.util.containers.ContainerUtil;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

import jakarta.inject.Singleton;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Singleton
public class UsageViewContentManagerImpl extends UsageViewContentManager {
  private final Key<Boolean> REUSABLE_CONTENT_KEY = Key.create("UsageTreeManager.REUSABLE_CONTENT_KEY");
  private final Key<Boolean> NOT_REUSABLE_CONTENT_KEY = Key.create("UsageTreeManager.NOT_REUSABLE_CONTENT_KEY");        //todo[myakovlev] dont use it
  private final Key<UsageView> NEW_USAGE_VIEW_KEY = Key.create("NEW_USAGE_VIEW_KEY");
  private final ContentManager myFindContentManager;

  @Inject
  public UsageViewContentManagerImpl(@Nonnull Project project, @Nonnull ToolWindowManager toolWindowManager) {
    ToolWindow toolWindow = toolWindowManager.registerToolWindow(ToolWindowId.FIND, true, ToolWindowAnchor.BOTTOM, project, true);
    //toolWindow.setHelpId(UsageViewImpl.HELP_ID);
    toolWindow.setToHideOnEmptyContent(true);
    toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowFind);

    DumbAwareToggleAction toggleNewTabAction = new DumbAwareToggleAction(FindBundle.message("find.open.in.new.tab.action")) {
      @Override
      public boolean isSelected(@Nonnull AnActionEvent e) {
        return FindSettings.getInstance().isShowResultsInSeparateView();
      }

      @Override
      public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        FindSettings.getInstance().setShowResultsInSeparateView(state);
      }
    };

    DumbAwareToggleAction toggleSortAction = new DumbAwareToggleAction(UsageViewBundle.message("sort.alphabetically.action.text"), null, AllIcons.ObjectBrowser.Sorted) {
      @Override
      public boolean isSelected(@Nonnull AnActionEvent e) {
        return UsageViewSettings.getInstance().isSortAlphabetically();
      }

      @Override
      public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        UsageViewSettings.getInstance().setSortAlphabetically(state);
        project.getMessageBus().syncPublisher(UsageFilteringRuleProvider.RULES_CHANGED).run();
      }
    };

    DumbAwareToggleAction toggleAutoscrollAction = new DumbAwareToggleAction(UIBundle.message("autoscroll.to.source.action.name"), UIBundle.message("autoscroll.to.source.action.description"), AllIcons.General.AutoscrollToSource) {
      @Override
      public boolean isSelected(@Nonnull AnActionEvent e) {
        return UsageViewSettings.getInstance().isAutoScrollToSource();
      }

      @Override
      public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        UsageViewSettings.getInstance().setAutoScrollToSource(state);
      }
    };

    DefaultActionGroup gearActions = new DefaultActionGroup(IdeBundle.message("group.view.options"), true);
    gearActions.addAll(toggleAutoscrollAction, toggleSortAction, toggleNewTabAction);
    ((ToolWindowEx)toolWindow).setAdditionalGearActions(gearActions);

    myFindContentManager = toolWindow.getContentManager();
    myFindContentManager.addContentManagerListener(new ContentManagerAdapter() {
      @Override
      public void contentRemoved(@Nonnull ContentManagerEvent event) {
        event.getContent().release();
      }
    });
    new ContentManagerWatcher(toolWindow, myFindContentManager);
  }

  @Nonnull
  @Override
  public Content addContent(@Nonnull String contentName, boolean reusable, @Nonnull final JComponent component, boolean toOpenInNewTab, boolean isLockable) {
    return addContent(contentName, null, null, reusable, component, toOpenInNewTab, isLockable);
  }

  @Nonnull
  @Override
  public Content addContent(@Nonnull String contentName, String tabName, String toolwindowTitle, boolean reusable, @Nonnull final JComponent component, boolean toOpenInNewTab, boolean isLockable) {
    Key<Boolean> contentKey = reusable ? REUSABLE_CONTENT_KEY : NOT_REUSABLE_CONTENT_KEY;
    Content selectedContent = getSelectedContent();
    toOpenInNewTab |= selectedContent != null && selectedContent.isPinned();

    Content contentToDelete = null;
    int indexToAdd = -1;
    if (!toOpenInNewTab && reusable) {
      List<Content> contents = ContainerUtil.newArrayList(myFindContentManager.getContents());
      if (selectedContent != null) {
        contents.remove(selectedContent);
        contents.add(selectedContent);// Selected content has to be the last (and the best) candidate to be deleted
      }

      for (Content content : contents) {
        if (!content.isPinned() && content.getUserData(contentKey) != null) {
          UsageView usageView = content.getUserData(NEW_USAGE_VIEW_KEY);
          if (usageView == null || !usageView.isSearchInProgress()) {
            contentToDelete = content;
            indexToAdd = myFindContentManager.getIndexOfContent(contentToDelete);
          }
        }
      }
    }
    Content content = ContentFactory.getInstance().createContent(component, contentName, isLockable);
    content.setTabName(tabName);
    content.setToolwindowTitle(toolwindowTitle);
    content.putUserData(contentKey, Boolean.TRUE);
    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);

    myFindContentManager.addContent(content, indexToAdd);
    if (contentToDelete != null) {
      myFindContentManager.removeContent(contentToDelete, true);
    }
    myFindContentManager.setSelectedContent(content);

    return content;
  }

  @Override
  public int getReusableContentsCount() {
    return getContentCount(true);
  }

  private int getContentCount(boolean reusable) {
    Key<Boolean> contentKey = reusable ? REUSABLE_CONTENT_KEY : NOT_REUSABLE_CONTENT_KEY;
    Content[] contents = myFindContentManager.getContents();
    return (int)Arrays.stream(contents).filter(content -> content.getUserData(contentKey) != null).count();
  }

  @Override
  public Content getSelectedContent(boolean reusable) {
    Key<Boolean> contentKey = reusable ? REUSABLE_CONTENT_KEY : NOT_REUSABLE_CONTENT_KEY;
    Content selectedContent = myFindContentManager.getSelectedContent();
    return selectedContent == null || selectedContent.getUserData(contentKey) == null ? null : selectedContent;
  }

  @Override
  public Content getSelectedContent() {
    return myFindContentManager == null ? null : myFindContentManager.getSelectedContent();
  }

  @Override
  public void closeContent(@Nonnull Content content) {
    myFindContentManager.removeContent(content, true);
    content.release();
  }
}
