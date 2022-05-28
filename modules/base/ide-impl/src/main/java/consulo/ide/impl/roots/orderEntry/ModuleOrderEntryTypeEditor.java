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
package consulo.ide.impl.roots.orderEntry;

import consulo.ide.setting.module.OrderEntryTypeEditor;
import consulo.module.impl.internal.layer.orderEntry.ModuleOrderEntryImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.ColoredTextContainer;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
public class ModuleOrderEntryTypeEditor implements OrderEntryTypeEditor<ModuleOrderEntryImpl> {
  @Nonnull
  @Override
  public Consumer<ColoredTextContainer> getRender(@Nonnull ModuleOrderEntryImpl orderEntry) {
    return it -> {
      it.setIcon(PlatformIconGroup.nodesModule());
      it.append(orderEntry.getPresentableName());
    };
  }
}
