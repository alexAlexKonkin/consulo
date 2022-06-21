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
package consulo.ide.impl.idea.packageDependencies;

import consulo.annotation.component.Extension;
import consulo.annotation.component.ExtensionImpl;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.scratch.ScratchesNamedScope;
import consulo.ide.impl.psi.search.scope.packageSet.CustomScopesProviderEx;
import consulo.ide.impl.psi.search.scope.packageSet.FilePatternPackageSet;
import consulo.content.scope.AbstractPackageSet;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.ide.impl.psi.search.scope.NonProjectFilesScope;
import consulo.ide.impl.psi.search.scope.ProjectFilesScope;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * @author anna
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class DefaultScopesProvider extends CustomScopesProviderEx {
  private final NamedScope myProblemsScope;
  private final Project myProject;
  private final List<NamedScope> myScopes;

  public static DefaultScopesProvider getInstance(Project project) {
    return CUSTOM_SCOPES_PROVIDER.findExtensionOrFail(project, DefaultScopesProvider.class);
  }

  @Inject
  public DefaultScopesProvider(Project project) {
    myProject = project;
    final NamedScope projectScope = new ProjectFilesScope();
    final NamedScope nonProjectScope = new NonProjectFilesScope();
    final String text = FilePatternPackageSet.SCOPE_FILE + ":*//*";
    myProblemsScope = new NamedScope(IdeBundle.message("predefined.scope.problems.name"), new AbstractPackageSet(text) {
      @Override
      public boolean contains(VirtualFile file, Project project, NamedScopesHolder holder) {
        return project == myProject && WolfTheProblemSolver.getInstance(project).isProblemFile(file);
      }
    });
    myScopes = Arrays.asList(projectScope, getAllScope(), nonProjectScope, new ScratchesNamedScope());
  }

  @Override
  @Nonnull
  public List<NamedScope> getCustomScopes() {
    return myScopes;
  }

  public static NamedScope getAllScope() {
    return CustomScopesProviderEx.getAllScope();
  }

  public NamedScope getProblemsScope() {
    return myProblemsScope;
  }
}
