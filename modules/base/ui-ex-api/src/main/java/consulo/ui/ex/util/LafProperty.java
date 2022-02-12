/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.util;

import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 12-Feb-22
 */
public final class LafProperty {
  public static Color getListBackground() {
    return UIManager.getColor("List.background");
  }

  /**
   * @deprecated use {@link #getListSelectionForeground(boolean)}
   */
  @Nonnull
  @Deprecated
  public static Color getListSelectionForeground() {
    return getListSelectionForeground(true);
  }

  public static Color getListForeground() {
    return UIManager.getColor("List.foreground");
  }

  @Nonnull
  public static Color getListSelectionForeground(boolean focused) {
    Color foreground = UIManager.getColor(focused ? "List.selectionForeground" : "List.selectionInactiveForeground");
    if (focused && foreground == null) foreground = UIManager.getColor("List[Selected].textForeground");  // Nimbus
    return foreground != null ? foreground : getListForeground();
  }

  public static Color getActiveTextColor() {
    return UIManager.getColor("textActiveText");
  }

  public static Color getInactiveTextColor() {
    return UIManager.getColor("textInactiveText");
  }

  public static Color getLabelForeground() {
    return JBColor.namedColor("Label.foreground", new JBColor(Gray._0, Gray.xBB));
  }
}
