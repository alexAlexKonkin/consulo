/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.ide.hierarchy;

import consulo.application.AllIcons;
import com.intellij.ide.impl.ContentManagerWatcher;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.content.ContentManager;
import consulo.component.persist.PersistentStateComponent;
import jakarta.inject.Inject;

import jakarta.inject.Singleton;

@Singleton
@State(
  name="HierarchyBrowserManager",
  storages= {
    @Storage(
      file = StoragePathMacros.WORKSPACE_FILE
    )}
)
public final class HierarchyBrowserManager implements PersistentStateComponent<HierarchyBrowserManager.State> {
  public static class State {
    public boolean IS_AUTOSCROLL_TO_SOURCE;
    public boolean SORT_ALPHABETICALLY;
    public boolean HIDE_CLASSES_WHERE_METHOD_NOT_IMPLEMENTED;
    public String SCOPE;
  }

  private State myState = new State();

  private final ContentManager myContentManager;

  @Inject
  public HierarchyBrowserManager(final Project project) {
    final ToolWindowManager toolWindowManager=ToolWindowManager.getInstance(project);
    final ToolWindow toolWindow = toolWindowManager.registerToolWindow(ToolWindowId.HIERARCHY, true, ToolWindowAnchor.RIGHT, project);
    myContentManager = toolWindow.getContentManager();
    toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowHierarchy);
    new ContentManagerWatcher(toolWindow,myContentManager);
  }

  public final ContentManager getContentManager() {
    return myContentManager;
  }

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(final State state) {
    myState = state;
  }

  public static HierarchyBrowserManager getInstance(final Project project) {
    return ServiceManager.getService(project, HierarchyBrowserManager.class);
  }
}
