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

package com.intellij.openapi.editor.highlighter;

import consulo.document.Document;
import com.intellij.openapi.editor.markup.TextAttributes;
import consulo.language.ast.IElementType;


public interface HighlighterIterator {
  TextAttributes getTextAttributes();
  int getStart();
  int getEnd();
  IElementType getTokenType();
  void advance();
  void retreat();
  boolean atEnd();
  Document getDocument();
}
