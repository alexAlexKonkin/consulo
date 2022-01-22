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
package com.intellij.psi.codeStyle.lineIndent;

import consulo.language.Language;
import consulo.component.extension.ExtensionPointName;
import javax.annotation.Nullable;

/**
 * Line indent provider extension point
 */
public class LineIndentProviderEP {
  public final static ExtensionPointName<LineIndentProvider> EP_NAME = ExtensionPointName.create("consulo.lineIndentProvider");

  @Nullable
  public static LineIndentProvider findLineIndentProvider(@Nullable Language language) {
    for (LineIndentProvider provider : EP_NAME.getExtensions()) {
      if (provider.isSuitableFor(language)) {
        return provider;
      }
    }
    return null;
  }
}
