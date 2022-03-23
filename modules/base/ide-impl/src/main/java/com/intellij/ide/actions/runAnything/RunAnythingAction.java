// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import consulo.execution.executor.Executor;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import consulo.application.Application;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.MacKeymapUtil;
import com.intellij.openapi.keymap.impl.ModifierKeyDoubleClickHandler;
import consulo.application.dumb.DumbAware;
import com.intellij.openapi.util.NotNullLazyValue;
import consulo.application.util.SystemInfo;
import consulo.application.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import consulo.ui.ex.awt.FontUtil;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;

public class RunAnythingAction extends AnAction implements CustomComponentAction, DumbAware {
  public static final String RUN_ANYTHING_ACTION_ID = "RunAnything";
  public static final Key<Executor> EXECUTOR_KEY = Key.create("EXECUTOR_KEY");
  public static final AtomicBoolean SHIFT_IS_PRESSED = new AtomicBoolean(false);
  public static final AtomicBoolean ALT_IS_PRESSED = new AtomicBoolean(false);

  private boolean myIsDoubleCtrlRegistered;

  private static final NotNullLazyValue<Boolean> IS_ACTION_ENABLED = new NotNullLazyValue<>() {
    @Nonnull
    @Override
    protected Boolean compute() {
      return RunAnythingProvider.EP_NAME.hasAnyExtensions();
    }
  };

  public RunAnythingAction(@Nonnull Application application) {
    if(application.isSwingApplication()) {
      IdeEventQueue.getInstance().addPostprocessor(event -> {
        if (event instanceof KeyEvent) {
          final int keyCode = ((KeyEvent)event).getKeyCode();
          if (keyCode == KeyEvent.VK_SHIFT) {
            SHIFT_IS_PRESSED.set(event.getID() == KeyEvent.KEY_PRESSED);
          }
          else if (keyCode == KeyEvent.VK_ALT) {
            ALT_IS_PRESSED.set(event.getID() == KeyEvent.KEY_PRESSED);
          }
        }
        return false;
      }, null);
    }
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    if (Registry.is("ide.suppress.double.click.handler") && e.getInputEvent() instanceof KeyEvent) {
      if (((KeyEvent)e.getInputEvent()).getKeyCode() == KeyEvent.VK_CONTROL) {
        return;
      }
    }

    if (e.getData(CommonDataKeys.PROJECT) != null) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed(IdeActions.ACTION_RUN_ANYTHING);

      RunAnythingManager runAnythingManager = RunAnythingManager.getInstance(e.getData(CommonDataKeys.PROJECT));
      String text = GotoActionBase.getInitialTextForNavigation(e);
      runAnythingManager.show(text, e);
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    if (getActiveKeymapShortcuts(RUN_ANYTHING_ACTION_ID).getShortcuts().length == 0) {
      if (!myIsDoubleCtrlRegistered) {
        ModifierKeyDoubleClickHandler.getInstance().registerAction(RUN_ANYTHING_ACTION_ID, KeyEvent.VK_CONTROL, -1, false);
        myIsDoubleCtrlRegistered = true;
      }
    }
    else {
      if (myIsDoubleCtrlRegistered) {
        ModifierKeyDoubleClickHandler.getInstance().unregisterAction(RUN_ANYTHING_ACTION_ID);
        myIsDoubleCtrlRegistered = false;
      }
    }

    boolean isEnabled = IS_ACTION_ENABLED.getValue();
    e.getPresentation().setEnabledAndVisible(isEnabled);
  }

  @Nonnull
  @Override
  public JComponent createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
    return new ActionButton(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE) {

      @Nullable
      @Override
      protected String getShortcutText() {
        if (myIsDoubleCtrlRegistered) {
          return IdeBundle.message("run.anything.double.ctrl.shortcut", SystemInfo.isMac ? FontUtil.thinSpace() + MacKeymapUtil.CONTROL : "Ctrl");
        }
        //keymap shortcut is added automatically
        return null;
      }

      @Override
      public void setToolTipText(String s) {
        String shortcutText = getShortcutText();
        super.setToolTipText(StringUtil.isNotEmpty(shortcutText) ? (s + " (" + shortcutText + ")") : s);
      }
    };
  }
}