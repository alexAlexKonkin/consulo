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
package com.intellij.refactoring.changeSignature.inplace;

import consulo.language.LanguageExtension;
import com.intellij.refactoring.changeSignature.ChangeInfo;
import consulo.container.plugin.PluginIds;

public class LanguageChangeSignatureDetectors extends LanguageExtension<LanguageChangeSignatureDetector<ChangeInfo>> {
  public static final LanguageChangeSignatureDetectors INSTANCE = new LanguageChangeSignatureDetectors();

  LanguageChangeSignatureDetectors() {
    super(PluginIds.CONSULO_BASE + ".changeSignatureDetector");
  }

}
