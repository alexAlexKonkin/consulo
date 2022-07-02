/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.lineMarker;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.Pass;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProviderDescriptor;
import consulo.application.AllIcons;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.psi.PsiElement;
import consulo.ide.impl.idea.util.Function;
import consulo.annotation.access.RequiredReadAction;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class RunLineMarkerProvider extends LineMarkerProviderDescriptor {

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }

  @RequiredReadAction
  @Nullable
  @Override
  public LineMarkerInfo getLineMarkerInfo(@Nonnull PsiElement element) {
    List<RunLineMarkerContributor> contributors = RunLineMarkerContributor.forLanguage(element.getLanguage());
    DefaultActionGroup actionGroup = null;
    Image icon = null;
    final List<RunLineMarkerContributor.Info> infos = new ArrayList<>();
    for (RunLineMarkerContributor contributor : contributors) {
      RunLineMarkerContributor.Info info = contributor.getInfo(element);
      if (info == null) {
        continue;
      }
      if (icon == null) {
        icon = info.icon;
      }
      if (actionGroup == null) {
        actionGroup = new DefaultActionGroup();
      }
      infos.add(info);
      for (AnAction action : info.actions) {
        actionGroup.add(new LineMarkerActionWrapper(element, action));
      }
      actionGroup.add(new AnSeparator());
    }
    if (icon == null) return null;

    final DefaultActionGroup finalActionGroup = actionGroup;
    Function<PsiElement, String> tooltipProvider = element1 -> {
      final StringBuilder tooltip = new StringBuilder();
      for (RunLineMarkerContributor.Info info : infos) {
        if (info.tooltipProvider != null) {
          String string = info.tooltipProvider.fun(element1);
          if (string == null) continue;
          if (tooltip.length() != 0) {
            tooltip.append("\n");
          }
          tooltip.append(string);
        }
      }

      return tooltip.length() == 0 ? null : tooltip.toString();
    };
    return new LineMarkerInfo<PsiElement>(element, element.getTextRange(), icon, Pass.LINE_MARKERS, tooltipProvider, null, GutterIconRenderer.Alignment.CENTER) {
      @Nullable
      @Override
      public GutterIconRenderer createGutterRenderer() {
        return new LineMarkerGutterIconRenderer<PsiElement>(this) {
          @Override
          public AnAction getClickAction() {
            return null;
          }

          @Override
          public boolean isNavigateAction() {
            return true;
          }

          @Nullable
          @Override
          public ActionGroup getPopupMenuActions() {
            return finalActionGroup;
          }
        };
      }
    };
  }

  @Nonnull
  @Override
  public String getName() {
    return "Run line marker";
  }

  @Nullable
  @Override
  public Image getIcon() {
    return AllIcons.RunConfigurations.TestState.Run;
  }
}