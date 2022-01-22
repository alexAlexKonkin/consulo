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

/*
 * @author max
 */
package com.intellij.psi.tree;

import consulo.language.ast.ASTNode;
import consulo.language.Language;
import consulo.language.util.CharTable;
import consulo.language.ast.IElementType;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class CustomParsingType extends IElementType implements ICustomParsingType {
  public CustomParsingType(@Nonnull @NonNls String debugName, @Nullable Language language) {
    super(debugName, language);
  }

  public abstract ASTNode parse(CharSequence text, CharTable table);
}
