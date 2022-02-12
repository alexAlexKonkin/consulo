// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.todo;

import com.intellij.ide.IdeBundle;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.fileTypes.FileTypeEvent;
import com.intellij.openapi.fileTypes.FileTypeListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import consulo.application.progress.ProcessCanceledException;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsListener;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.project.ui.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import com.intellij.util.ObjectUtils;
import consulo.component.messagebus.MessageBusConnection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.OptionTag;
import consulo.application.AccessRule;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

@State(name = "TodoView", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class TodoView implements PersistentStateComponent<TodoView.State>, Disposable {
  private final Project myProject;

  private ContentManager myContentManager;
  private TodoPanel myAllTodos;
  private final List<TodoPanel> myPanels = new ArrayList<>();
  private final List<Content> myNotAddedContent = new ArrayList<>();

  private State state = new State();

  private Content myChangeListTodosContent;

  private final MyVcsListener myVcsListener = new MyVcsListener();

  public TodoView(@Nonnull Project project) {
    myProject = project;

    state.all.arePackagesShown = true;
    state.all.isAutoScrollToSource = true;

    state.current.isAutoScrollToSource = true;

    MessageBusConnection connection = project.getMessageBus().connect(this);
    connection.subscribe(TodoConfiguration.PROPERTY_CHANGE, new MyPropertyChangeListener());
    connection.subscribe(FileTypeManager.TOPIC, new MyFileTypeListener());
    connection.subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, myVcsListener);
  }

  static class State {
    @Attribute(value = "selected-index")
    public int selectedIndex;

    @OptionTag(value = "selected-file", nameAttribute = "id", tag = "todo-panel", valueAttribute = "")
    public TodoPanelSettings current = new TodoPanelSettings();

    @OptionTag(value = "all", nameAttribute = "id", tag = "todo-panel", valueAttribute = "")
    public TodoPanelSettings all = new TodoPanelSettings();

    @OptionTag(value = "default-changelist", nameAttribute = "id", tag = "todo-panel", valueAttribute = "")
    public TodoPanelSettings changeList = new TodoPanelSettings();
  }

  @Override
  public void loadState(@Nonnull State state) {
    this.state = state;
  }

  @Override
  public State getState() {
    if (myContentManager != null) {
      // all panel were constructed
      Content content = myContentManager.getSelectedContent();
      state.selectedIndex = content == null ? -1 : myContentManager.getIndexOfContent(content);
    }
    return state;
  }

  @Override
  public void dispose() {
  }

  public void initToolWindow(@Nonnull ToolWindow toolWindow) {
    // Create panels
    ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
    Content allTodosContent = contentFactory.createContent(null, IdeBundle.message("title.project"), false);
    myAllTodos = new TodoPanel(myProject, state.all, false, allTodosContent) {
      @Override
      protected TodoTreeBuilder createTreeBuilder(JTree tree, Project project) {
        AllTodosTreeBuilder builder = createAllTodoBuilder(tree, project);
        builder.init();
        return builder;
      }
    };
    allTodosContent.setComponent(myAllTodos);
    Disposer.register(this, myAllTodos);
    if (toolWindow instanceof ToolWindowEx) {
      DefaultActionGroup group = new DefaultActionGroup() {
        {
          getTemplatePresentation().setText(IdeBundle.message("group.view.options"));
          setPopup(true);
          add(myAllTodos.createAutoScrollToSourceAction());
          addSeparator();
          addAll(myAllTodos.createGroupByActionGroup());
        }
      };
      ((ToolWindowEx)toolWindow).setAdditionalGearActions(group);
    }

    Content currentFileTodosContent = contentFactory.createContent(null, IdeBundle.message("title.todo.current.file"), false);
    CurrentFileTodosPanel currentFileTodos = new CurrentFileTodosPanel(myProject, state.current, currentFileTodosContent) {
      @Override
      protected TodoTreeBuilder createTreeBuilder(JTree tree, Project project) {
        CurrentFileTodosTreeBuilder builder = new CurrentFileTodosTreeBuilder(tree, project);
        builder.init();
        return builder;
      }
    };
    Disposer.register(this, currentFileTodos);
    currentFileTodosContent.setComponent(currentFileTodos);

    String tabName = getTabNameForChangeList(ChangeListManager.getInstance(myProject).getDefaultChangeList().getName());
    myChangeListTodosContent = contentFactory.createContent(null, tabName, false);
    ChangeListTodosPanel changeListTodos = new ChangeListTodosPanel(myProject, state.current, myChangeListTodosContent) {
      @Override
      protected TodoTreeBuilder createTreeBuilder(JTree tree, Project project) {
        ChangeListTodosTreeBuilder builder = new ChangeListTodosTreeBuilder(tree, project);
        builder.init();
        return builder;
      }
    };
    Disposer.register(this, changeListTodos);
    myChangeListTodosContent.setComponent(changeListTodos);

    Content scopeBasedTodoContent = contentFactory.createContent(null, "Scope Based", false);
    ScopeBasedTodosPanel scopeBasedTodos = new ScopeBasedTodosPanel(myProject, state.current, scopeBasedTodoContent);
    Disposer.register(this, scopeBasedTodos);
    scopeBasedTodoContent.setComponent(scopeBasedTodos);

    myContentManager = toolWindow.getContentManager();

    myContentManager.addContent(allTodosContent);
    myContentManager.addContent(currentFileTodosContent);
    myContentManager.addContent(scopeBasedTodoContent);

    if (ProjectLevelVcsManager.getInstance(myProject).hasActiveVcss()) {
      myVcsListener.myIsVisible = true;
      myContentManager.addContent(myChangeListTodosContent);
    }
    for (Content content : myNotAddedContent) {
      myContentManager.addContent(content);
    }

    myChangeListTodosContent.setCloseable(false);
    allTodosContent.setCloseable(false);
    currentFileTodosContent.setCloseable(false);
    scopeBasedTodoContent.setCloseable(false);
    Content content = myContentManager.getContent(state.selectedIndex);
    myContentManager.setSelectedContent(content == null ? allTodosContent : content);

    myPanels.add(myAllTodos);
    myPanels.add(changeListTodos);
    myPanels.add(currentFileTodos);
    myPanels.add(scopeBasedTodos);
  }

  @Nonnull
  static String getTabNameForChangeList(@Nonnull String changelistName) {
    changelistName = changelistName.trim();
    String suffix = "Changelist";
    return StringUtil.endsWithIgnoreCase(changelistName, suffix) ? changelistName : changelistName + " " + suffix;
  }

  @Nonnull
  protected AllTodosTreeBuilder createAllTodoBuilder(JTree tree, Project project) {
    return new AllTodosTreeBuilder(tree, project);
  }

  private final class MyVcsListener implements VcsListener {
    private boolean myIsVisible;

    @Override
    public void directoryMappingChanged() {
      ApplicationManager.getApplication().invokeLater(() -> {
        if (myContentManager == null || myProject.isDisposed()) {
          // was not initialized yet
          return;
        }

        boolean hasActiveVcss = ProjectLevelVcsManager.getInstance(myProject).hasActiveVcss();
        if (myIsVisible && !hasActiveVcss) {
          myContentManager.removeContent(myChangeListTodosContent, false);
          myIsVisible = false;
        }
        else if (!myIsVisible && hasActiveVcss) {
          myContentManager.addContent(myChangeListTodosContent);
          myIsVisible = true;
        }
      }, ModalityState.NON_MODAL);
    }
  }

  private final class MyPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent e) {
      if (TodoConfiguration.PROP_TODO_PATTERNS.equals(e.getPropertyName()) || TodoConfiguration.PROP_TODO_FILTERS.equals(e.getPropertyName())) {
        _updateFilters();
      }
    }

    private void _updateFilters() {
      try {
        if (!DumbService.isDumb(myProject)) {
          updateFilters();
          return;
        }
      }
      catch (ProcessCanceledException ignore) {
      }
      DumbService.getInstance(myProject).smartInvokeLater(this::_updateFilters);
    }

    private void updateFilters() {
      for (TodoPanel panel : myPanels) {
        panel.updateTodoFilter();
      }
    }
  }

  private final class MyFileTypeListener implements FileTypeListener {
    @Override
    public void fileTypesChanged(@Nonnull FileTypeEvent e) {
      refresh();
    }
  }

  public void refresh() {
    Map<TodoPanel, Set<VirtualFile>> files = new HashMap<>();

    AccessRule.readAsync(() -> {
      if (myAllTodos == null) {
        return;
      }
      for (TodoPanel panel : myPanels) {
        panel.myTodoTreeBuilder.collectFiles(virtualFile -> {
          files.computeIfAbsent(panel, p -> new HashSet<>()).add(virtualFile);
          return true;
        });
      }
    }).doWhenDone(() -> {
      Application.get().invokeLater(() -> {
        for (TodoPanel panel : myPanels) {
          panel.rebuildCache(ObjectUtils.notNull(files.get(panel), new HashSet<>()));
          panel.updateTree();
        }
      }, ModalityState.NON_MODAL);
    });
  }

  public void addCustomTodoView(final TodoTreeBuilderFactory factory, final String title, final TodoPanelSettings settings) {
    Content content = ContentFactory.SERVICE.getInstance().createContent(null, title, true);
    final ChangeListTodosPanel panel = new ChangeListTodosPanel(myProject, settings, content) {
      @Override
      protected TodoTreeBuilder createTreeBuilder(JTree tree, Project project) {
        TodoTreeBuilder todoTreeBuilder = factory.createTreeBuilder(tree, project);
        todoTreeBuilder.init();
        return todoTreeBuilder;
      }
    };
    content.setComponent(panel);
    Disposer.register(this, panel);

    if (myContentManager == null) {
      myNotAddedContent.add(content);
    }
    else {
      myContentManager.addContent(content);
    }
    myPanels.add(panel);
    content.setCloseable(true);
    content.setDisposer(new Disposable() {
      @Override
      public void dispose() {
        myPanels.remove(panel);
      }
    });
  }
}
