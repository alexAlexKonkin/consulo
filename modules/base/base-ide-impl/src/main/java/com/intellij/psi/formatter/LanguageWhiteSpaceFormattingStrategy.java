/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.psi.formatter;

import consulo.language.LanguageExtension;
import consulo.container.plugin.PluginIds;

/**
 * Exposes pre-configured {@link WhiteSpaceFormattingStrategy} objects to use in a per-language manner.
 *
 * @author Denis Zhdanov
 * @since Sep 20, 2010 7:41:55 PM
 */
public class LanguageWhiteSpaceFormattingStrategy extends LanguageExtension<WhiteSpaceFormattingStrategy> {

  public static final LanguageWhiteSpaceFormattingStrategy INSTANCE = new LanguageWhiteSpaceFormattingStrategy();

  private LanguageWhiteSpaceFormattingStrategy() {
    super(PluginIds.CONSULO_BASE + ".lang.whiteSpaceFormattingStrategy");
  }
}
