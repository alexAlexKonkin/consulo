/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide;

import consulo.language.psi.PsiElement;
import consulo.annotation.access.RequiredReadAction;
import consulo.container.plugin.PluginIds;
import consulo.extensions.CompositeExtensionPointName;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 0:20/19.07.13
 */
public interface IconDescriptorUpdater {
  CompositeExtensionPointName<IconDescriptorUpdater> EP_NAME =
          CompositeExtensionPointName.projectPoint(PluginIds.CONSULO_BASE + ".iconDescriptorUpdater", IconDescriptorUpdater.class);

  @RequiredReadAction
  void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags);
}
