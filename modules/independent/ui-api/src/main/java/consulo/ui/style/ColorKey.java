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
package consulo.ui.style;

import consulo.ui.shared.ColorValue;
import consulo.ui.shared.RGBColor;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 22-Jun-16
 */
public interface ColorKey extends ColorValue {
  @NotNull
  @Override
  default RGBColor toRGB() {
    return StyleManager.get().getCurrentStyle().getColor(this).toRGB();
  }
}
