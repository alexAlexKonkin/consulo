// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.index;

import consulo.component.extension.ExtensionPointName;
import consulo.index.io.IndexExtension;
import com.intellij.util.indexing.SnapshotInputMappingIndex;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

//@ApiStatus.Experimental
public interface IndexImporterFactory {
  ExtensionPointName<IndexImporterFactory> EP_NAME = ExtensionPointName.create("consulo.indexImporterFactory");

  @Nullable
  <Key, Value, Input> SnapshotInputMappingIndex<Key, Value, Input> createImporter(@Nonnull IndexExtension<Key, Value, Input> extension);
}
