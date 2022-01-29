/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import com.intellij.ide.ui.LafManager;
import consulo.application.ui.UISettings;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import consulo.project.DumbAware;
import consulo.project.Project;
import consulo.util.concurrent.ActionCallback;
import consulo.project.ui.wm.ToolWindow;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.openapi.wm.impl.ToolWindowLayout;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.project.ui.wm.IdeFrameUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;
import java.util.Map;

/**
 * @author Konstantin Bulenkov
 */
public class TogglePresentationModeAction extends AnAction implements DumbAware {
  private static final Map<Object, Object> ourSavedValues = ContainerUtil.newLinkedHashMap();
  private static float ourSavedScaleFactor = JBUI.scale(1f);
  private static int ourSavedConsoleFontSize;

  @Override
  public void update(@Nonnull AnActionEvent e) {
    boolean selected = UISettings.getInstance().PRESENTATION_MODE;
    e.getPresentation().setText(selected ? "Exit Presentation Mode" : "Enter Presentation Mode");
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e){
    UISettings settings = UISettings.getInstance();
    Project project = e.getProject();

    setPresentationMode(project, !settings.PRESENTATION_MODE);
  }

  //public static void restorePresentationMode() {
  //  UISettings instance = UISettings.getInstance();
  //  tweakUIDefaults(instance, true);
  //  tweakEditorAndFireUpdateUI(instance, true);
  //}

  public static void setPresentationMode(final Project project, final boolean inPresentation) {
    final UISettings settings = UISettings.getInstance();
    settings.PRESENTATION_MODE = inPresentation;

    final boolean layoutStored = storeToolWindows(project);

    tweakUIDefaults(settings, inPresentation);

    ActionCallback callback = project == null ? ActionCallback.DONE : tweakFrameFullScreen(project, inPresentation);
    callback.doWhenProcessed(() -> {
      tweakEditorAndFireUpdateUI(settings, inPresentation);

      restoreToolWindows(project, layoutStored, inPresentation);
    });
  }

  private static ActionCallback tweakFrameFullScreen(Project project, boolean inPresentation) {
    IdeFrameEx frame = (IdeFrameEx)IdeFrameUtil.findActiveRootIdeFrame();
    if (frame != null) {
      PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
      if (inPresentation) {
        propertiesComponent.setValue("full.screen.before.presentation.mode", String.valueOf(frame.isInFullScreen()));
        return frame.toggleFullScreen(true);
      }
      else {
        if (frame.isInFullScreen()) {
          final String value = propertiesComponent.getValue("full.screen.before.presentation.mode");
          return frame.toggleFullScreen("true".equalsIgnoreCase(value));
        }
      }
    }
    return ActionCallback.DONE;
  }

  private static void tweakEditorAndFireUpdateUI(UISettings settings, boolean inPresentation) {
    EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();
    int fontSize = inPresentation ? settings.PRESENTATION_MODE_FONT_SIZE : globalScheme.getEditorFontSize();
    if (inPresentation) {
      ourSavedConsoleFontSize = globalScheme.getConsoleFontSize();
      globalScheme.setConsoleFontSize(fontSize);
    }
    else {
      globalScheme.setConsoleFontSize(ourSavedConsoleFontSize);
    }
    for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
      if (editor instanceof EditorEx) {
        ((EditorEx)editor).setFontSize(fontSize);
      }
    }
    UISettings.getInstance().fireUISettingsChanged();
    LafManager.getInstance().updateUI();
    EditorUtil.reinitSettings();
  }

  private static void tweakUIDefaults(UISettings settings, boolean inPresentation) {
    UIDefaults defaults = UIManager.getDefaults();
    Enumeration<Object> keys = defaults.keys();
    if (inPresentation) {
      while (keys.hasMoreElements()) {
        Object key = keys.nextElement();
        if (key instanceof String) {
          String name = (String)key;
          if (name.endsWith(".font")) {
            Font font = defaults.getFont(key);
            ourSavedValues.put(key, font);
          }
          else if (name.endsWith(".rowHeight")) {
            ourSavedValues.put(key, defaults.getInt(key));
          }
        }
      }
      float scaleFactor = settings.PRESENTATION_MODE_FONT_SIZE / UIUtil.DEF_SYSTEM_FONT_SIZE;
      ourSavedScaleFactor = JBUI.scale(1f);
      JBUI.setUserScaleFactor(scaleFactor);
      for (Object key : ourSavedValues.keySet()) {
        Object v = ourSavedValues.get(key);
        if (v instanceof Font) {
          Font font = (Font)v;
          defaults.put(key, new FontUIResource(font.getName(), font.getStyle(), JBUI.scale(font.getSize())));
        }
        else if (v instanceof Integer) {
          defaults.put(key, JBUI.scale(((Integer)v).intValue()));
        }
      }
    }
    else {
      for (Object key : ourSavedValues.keySet()) {
        defaults.put(key, ourSavedValues.get(key));
      }
      JBUI.setUserScaleFactor(ourSavedScaleFactor);
      ourSavedValues.clear();
    }
  }

  private static boolean hideAllToolWindows(ToolWindowManagerEx manager) {
    // to clear windows stack
    manager.clearSideStack();

    String[] ids = manager.getToolWindowIds();
    boolean hasVisible = false;
    for (String id : ids) {
      final ToolWindow toolWindow = manager.getToolWindow(id);
      if (toolWindow.isVisible()) {
        toolWindow.hide(null);
        hasVisible = true;
      }
    }
    return hasVisible;
  }

  static boolean storeToolWindows(@Nullable Project project) {
    if (project == null) return false;
    ToolWindowManagerEx manager = ToolWindowManagerEx.getInstanceEx(project);

    ToolWindowLayout layout = new ToolWindowLayout();
    layout.copyFrom(manager.getLayout());
    boolean hasVisible = hideAllToolWindows(manager);

    if (hasVisible) {
      manager.setLayoutToRestoreLater(layout);
      manager.activateEditorComponent();
    }
    return hasVisible;
  }

  static void restoreToolWindows(Project project, boolean needsRestore, boolean inPresentation) {
    if (project == null || !needsRestore) return;
    ToolWindowManagerEx manager = ToolWindowManagerEx.getInstanceEx(project);
    ToolWindowLayout restoreLayout = manager.getLayoutToRestoreLater();
    if (!inPresentation && restoreLayout != null) {
      manager.setLayout(restoreLayout);
    }
  }
}
