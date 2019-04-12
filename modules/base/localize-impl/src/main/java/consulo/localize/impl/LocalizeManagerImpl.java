/*
 * Copyright 2013-2019 consulo.io
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
package consulo.localize.impl;

import consulo.localize.LocalizeLibraryBuilder;
import consulo.localize.LocalizeManager;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public class LocalizeManagerImpl extends LocalizeManager {
  @Nonnull
  @Override
  public LocalizeLibraryBuilder newBuilder(@Nonnull String pluginId, @Nonnull String id, @Nonnull Class<?> bundleClass) {
    return new LocalizeLibraryBuilderImpl(pluginId, id, bundleClass);
  }
}
