package com.intellij.coverage.view;

import com.intellij.coverage.CoverageDataManager;
import com.intellij.coverage.CoverageOptionsProvider;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.icons.AllIcons;
import com.intellij.ide.impl.ContentManagerWatcher;
import consulo.application.ApplicationManager;
import com.intellij.openapi.components.*;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindow;
import consulo.project.ui.wm.ToolWindowAnchor;
import consulo.project.ui.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.disposer.Disposer;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/**
 * User: anna
 * Date: 1/2/12
 */
@Singleton
@State(
    name = "CoverageViewManager",
    storages = {@Storage( file = StoragePathMacros.WORKSPACE_FILE)}
)
public class CoverageViewManager implements PersistentStateComponent<CoverageViewManager.StateBean> {
  private static final Logger LOG = Logger.getInstance(CoverageViewManager.class);
  public static final String TOOLWINDOW_ID = "Coverage";
  private Project myProject;
  private final CoverageDataManager myDataManager;
  private ContentManager myContentManager;
  private StateBean myStateBean = new StateBean();
  private Map<String, CoverageView> myViews = new HashMap<String, CoverageView>();
  private boolean myReady;

  @Inject
  public CoverageViewManager(Project project, ToolWindowManager toolWindowManager, CoverageDataManager dataManager) {
    myProject = project;
    myDataManager = dataManager;

    ToolWindow toolWindow = toolWindowManager.registerToolWindow(TOOLWINDOW_ID, true, ToolWindowAnchor.RIGHT, myProject);
    toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowCoverage);
    toolWindow.setSplitMode(true, null);
    myContentManager = toolWindow.getContentManager();
    new ContentManagerWatcher(toolWindow, myContentManager);
  }

  public StateBean getState() {
    return myStateBean;
  }

  public void loadState(StateBean state) {
    myStateBean = state;
  }

  public CoverageView getToolwindow(CoverageSuitesBundle suitesBundle) {
    return myViews.get(getDisplayName(suitesBundle));
  }

  public void activateToolwindow(CoverageView view, boolean requestFocus) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(TOOLWINDOW_ID);
    if (requestFocus) {
      myContentManager.setSelectedContent(myContentManager.getContent(view));
      LOG.assertTrue(toolWindow != null);
      toolWindow.activate(null, false);
    }
  }

  public static CoverageViewManager getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, CoverageViewManager.class);
  }

  public void createToolWindow(String displayName, boolean defaultFileProvider) {
    closeView(displayName);

    final CoverageView coverageView = new CoverageView(myProject, myDataManager, myStateBean);
    myViews.put(displayName, coverageView);
    Content content = myContentManager.getFactory().createContent(coverageView, displayName, true);
    myContentManager.addContent(content);
    myContentManager.setSelectedContent(content);

    if (CoverageOptionsProvider.getInstance(myProject).activateViewOnRun() && defaultFileProvider) {
      activateToolwindow(coverageView, true);
    }
  }

  void closeView(String displayName) {
    final CoverageView oldView = myViews.get(displayName);
    if (oldView != null) {
      final Content content = myContentManager.getContent(oldView);
      final Runnable runnable = new Runnable() {
        public void run() {
          if (content != null) {
            myContentManager.removeContent(content, true);
          }
          Disposer.dispose(oldView);
        }
      };
      ApplicationManager.getApplication().invokeLater(runnable);
    }
    setReady(false);
  }

  public boolean isReady() {
    return myReady;
  }

  public void setReady(boolean ready) {
    myReady = ready;
  }

  public static String getDisplayName(CoverageSuitesBundle suitesBundle) {
    final RunConfigurationBase configuration = suitesBundle.getRunConfiguration();
    return configuration != null ? configuration.getName() : suitesBundle.getPresentableName();
  }

  public static class StateBean {
    public boolean myFlattenPackages = false;
    public boolean myAutoScrollToSource = false;
    public boolean myAutoScrollFromSource = false;
  }
}
