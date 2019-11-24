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
package com.intellij.openapi.options.ex;

import com.intellij.CommonBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ex.IdeFocusTraversalPolicy;
import com.intellij.util.Alarm;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ide.base.BaseShowSettingsUtil;
import consulo.options.ConfigurableUIMigrationUtil;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class SingleConfigurableEditor extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.options.ex.SingleConfigurableEditor");
  private Project myProject;
  private Component myParentComponent;
  private Configurable myConfigurable;
  private JComponent myCenterPanel;
  private String myDimensionKey;
  private final boolean myShowApplyButton;
  private boolean myChangesWereApplied;

  @RequiredUIAccess
  public SingleConfigurableEditor(@Nullable Project project, Configurable configurable, @NonNls String dimensionKey, final boolean showApplyButton, final IdeModalityType ideModalityType) {
    this(project, configurable, null, dimensionKey, showApplyButton, ideModalityType);
  }

  @RequiredUIAccess
  public SingleConfigurableEditor(@Nullable Project project,
                                  Configurable configurable,
                                  @Nullable String title,
                                  @NonNls String dimensionKey,
                                  final boolean showApplyButton,
                                  final IdeModalityType ideModalityType) {
    super(project, true, ideModalityType);
    myDimensionKey = dimensionKey;
    myShowApplyButton = showApplyButton;
    setTitle(title);

    myProject = project;
    myConfigurable = configurable;
    init();
    myConfigurable.reset();
  }

  @RequiredUIAccess
  public SingleConfigurableEditor(Component parent, Configurable configurable, String dimensionServiceKey, final boolean showApplyButton, final IdeModalityType ideModalityType) {
    this(parent, configurable, null, dimensionServiceKey, showApplyButton, ideModalityType);

  }

  @RequiredUIAccess
  public SingleConfigurableEditor(Component parent,
                                  Configurable configurable,
                                  @Nullable String title,
                                  String dimensionServiceKey,
                                  final boolean showApplyButton,
                                  final IdeModalityType ideModalityType) {
    super(parent, true);
    myDimensionKey = dimensionServiceKey;
    myShowApplyButton = showApplyButton;
    setTitle(StringUtil.notNullize(title, createTitleString(configurable)));

    myParentComponent = parent;
    myConfigurable = configurable;
    init();
    myConfigurable.reset();
  }

  public SingleConfigurableEditor(@Nullable Project project, Configurable configurable, @NonNls String dimensionKey, final boolean showApplyButton) {
    this(project, configurable, dimensionKey, showApplyButton, IdeModalityType.IDE);
  }

  public SingleConfigurableEditor(Component parent, Configurable configurable, String dimensionServiceKey, final boolean showApplyButton) {
    this(parent, configurable, dimensionServiceKey, showApplyButton, IdeModalityType.IDE);
  }

  public SingleConfigurableEditor(@Nullable Project project, Configurable configurable, @NonNls String dimensionKey, IdeModalityType ideModalityType) {
    this(project, configurable, dimensionKey, true, ideModalityType);
  }

  public SingleConfigurableEditor(@Nullable Project project, Configurable configurable, @NonNls String dimensionKey) {
    this(project, configurable, dimensionKey, true);
  }

  public SingleConfigurableEditor(Component parent, Configurable configurable, String dimensionServiceKey) {
    this(parent, configurable, dimensionServiceKey, true);
  }

  public SingleConfigurableEditor(@Nullable Project project, Configurable configurable, IdeModalityType ideModalityType) {
    this(project, configurable, BaseShowSettingsUtil.createDimensionKey(configurable), ideModalityType);
  }

  public SingleConfigurableEditor(@Nullable Project project, Configurable configurable) {
    this(project, configurable, BaseShowSettingsUtil.createDimensionKey(configurable));
  }

  public SingleConfigurableEditor(Component parent, Configurable configurable) {
    this(parent, configurable, BaseShowSettingsUtil.createDimensionKey(configurable));
  }

  public Configurable getConfigurable() {
    return myConfigurable;
  }

  public Project getProject() {
    return myProject;
  }

  private static String createTitleString(Configurable configurable) {
    String displayName = configurable.getDisplayName();
    LOG.assertTrue(displayName != null, configurable.getClass().getName());
    return displayName.replaceAll("\n", " ");
  }

  @Override
  protected String getDimensionServiceKey() {
    if (myDimensionKey == null) {
      return super.getDimensionServiceKey();
    }
    else {
      return myDimensionKey;
    }
  }

  @Override
  @Nonnull
  protected Action[] createActions() {
    List<Action> actions = new ArrayList<>();
    actions.add(getOKAction());
    actions.add(getCancelAction());
    if (myShowApplyButton) {
      actions.add(new ApplyAction());
    }
    if (myConfigurable.getHelpTopic() != null) {
      actions.add(getHelpAction());
    }
    return actions.toArray(new Action[actions.size()]);
  }

  @Override
  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp(myConfigurable.getHelpTopic());
  }

  @Override
  @RequiredUIAccess
  public void doCancelAction() {
    if (myChangesWereApplied) {
      ApplicationManager.getApplication().saveAll();
    }
    super.doCancelAction();
  }

  @Override
  @RequiredUIAccess
  protected void doOKAction() {
    try {
      if (myConfigurable.isModified()) myConfigurable.apply();

      ApplicationManager.getApplication().saveAll();
    }
    catch (ConfigurationException e) {
      if (e.getMessage() != null) {
        if (myProject != null) {
          Messages.showMessageDialog(myProject, e.getMessage(), e.getTitle(), Messages.getErrorIcon());
        }
        else {
          Messages.showMessageDialog(myParentComponent, e.getMessage(), e.getTitle(), Messages.getErrorIcon());
        }
      }
      return;
    }
    super.doOKAction();
  }

  protected static String createDimensionKey(Configurable configurable) {
    String displayName = configurable.getDisplayName();
    displayName = displayName.replaceAll("\n", "_").replaceAll(" ", "_");
    return "#" + displayName;
  }

  protected class ApplyAction extends AbstractAction {
    private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

    public ApplyAction() {
      super(CommonBundle.getApplyButtonText());
      final Runnable updateRequest = new Runnable() {
        @Override
        public void run() {
          if (!SingleConfigurableEditor.this.isShowing()) return;
          try {
            ApplyAction.this.setEnabled(myConfigurable != null && myConfigurable.isModified());
          }
          catch (IndexNotReadyException ignored) {
          }
          addUpdateRequest(this);
        }
      };

      // invokeLater necessary to make sure dialog is already shown so we calculate modality state correctly.
      SwingUtilities.invokeLater(() -> addUpdateRequest(updateRequest));
    }

    private void addUpdateRequest(final Runnable updateRequest) {
      myUpdateAlarm.addRequest(updateRequest, 500, ModalityState.stateForComponent(getWindow()));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(ActionEvent event) {
      if (myPerformAction) return;
      try {
        myPerformAction = true;
        if (myConfigurable.isModified()) {
          myConfigurable.apply();
          myChangesWereApplied = true;
          setCancelButtonText(CommonBundle.getCloseButtonText());
        }
      }
      catch (ConfigurationException e) {
        if (myProject != null) {
          Messages.showMessageDialog(myProject, e.getMessage(), e.getTitle(), Messages.getErrorIcon());
        }
        else {
          Messages.showMessageDialog(myParentComponent, e.getMessage(), e.getTitle(), Messages.getErrorIcon());
        }
      }
      finally {
        myPerformAction = false;
      }
    }
  }

  @Override
  @RequiredUIAccess
  protected JComponent createCenterPanel() {
    myCenterPanel = ConfigurableUIMigrationUtil.createComponent(myConfigurable);
    return myCenterPanel;
  }

  @RequiredUIAccess
  @Override
  public JComponent getPreferredFocusedComponent() {
    if (myConfigurable instanceof Configurable.HoldPreferredFocusedComponent) {
      JComponent preferred = ConfigurableUIMigrationUtil.getPreferredFocusedComponent((Configurable.HoldPreferredFocusedComponent)myConfigurable);
      if (preferred != null) return preferred;
    }
    return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myCenterPanel);
  }

  @Override
  @RequiredUIAccess
  public void dispose() {
    super.dispose();
    myConfigurable.disposeUIResources();
    myConfigurable = null;
  }
}
