/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.desktop.awt.editor;

import consulo.codeEditor.internal.RealEditor;
import org.intellij.lang.annotations.JdkConstants;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * @author VISTALL
 * @since 15/01/2023
 */
// TODO move it to desktop-awt-ide-impl
public interface DesktopAWTEditor extends RealEditor {
  @Nonnull
  FontMetrics getFontMetrics(@JdkConstants.FontStyle int fontType);
}
