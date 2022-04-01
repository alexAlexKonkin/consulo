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

package com.intellij.packageDependencies.actions;

import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.AnalysisScopeBundle;
import com.intellij.analysis.BaseAnalysisAction;
import com.intellij.analysis.BaseAnalysisActionDialog;
import consulo.ui.ex.awt.scopeChooser.ScopeChooserCombo;
import consulo.project.Project;
import consulo.content.scope.SearchScope;
import consulo.disposer.Disposer;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;

/**
 * User: anna
 * Date: Jan 16, 2005
 */
public class BackwardDependenciesAction extends BaseAnalysisAction {
  private AdditionalSettingsPanel myPanel;


  public BackwardDependenciesAction() {
    super(AnalysisScopeBundle.message("action.backward.dependency.analysis"), AnalysisScopeBundle.message("action.analysis.noun"));
  }

  @Override
  protected void analyze(@Nonnull final Project project, final AnalysisScope scope) {
    scope.setSearchInLibraries(true); //find library usages in project
    final SearchScope selectedScope = myPanel.myCombo.getSelectedScope();
    new BackwardDependenciesHandler(project, scope, selectedScope != null ? new AnalysisScope(selectedScope, project) : new AnalysisScope(project)).analyze();
    dispose();
  }

  @Override
  protected boolean acceptNonProjectDirectories() {
    return true;
  }

  @Override
  protected void canceled() {
    super.canceled();
    dispose();
  }

  private void dispose() {
    Disposer.dispose(myPanel.myCombo);
    myPanel = null;
  }

  @Override
  @Nullable
  protected JComponent getAdditionalActionSettings(final Project project, final BaseAnalysisActionDialog dialog) {
    myPanel = new AdditionalSettingsPanel();
    myPanel.myCombo.init(project, null);
    return myPanel.myWholePanel;
  }

  private static class AdditionalSettingsPanel {
    private ScopeChooserCombo myCombo;
    private JPanel myWholePanel;
  }
}
