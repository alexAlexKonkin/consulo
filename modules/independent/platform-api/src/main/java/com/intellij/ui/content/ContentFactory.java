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
package com.intellij.ui.content;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import consulo.annotations.DeprecationInfo;
import consulo.ui.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ContentFactory {
  @NotNull
  static ContentFactory getInstance() {
    return ServiceManager.getService(ContentFactory.class);
  }

  @NotNull
  ContentManager createContentManager(@NotNull ContentUI contentUI, boolean canCloseContents, @NotNull Project project);

  @NotNull
  ContentManager createContentManager(boolean canCloseContents, @NotNull Project project);

  /**
   * do not rename due it will be conflicted with deprecated method
   */
  @NotNull
  Content createUIContent(@Nullable Component component, String displayName, boolean isLockable);

  // TODO [VISTALL] AWT & Swing dependency
  // region AWT & Swing dependency
  @NotNull
  @Deprecated
  @DeprecationInfo("")
  Content createContent(javax.swing.JComponent component, String displayName, boolean isLockable);
  // endregion

  // region Deprecated staff
  @Deprecated
  class SERVICE {
    private SERVICE() {
    }

    @NotNull
    @Deprecated
    public static ContentFactory getInstance() {
      return ServiceManager.getService(ContentFactory.class);
    }
  }
  // endregion
}
