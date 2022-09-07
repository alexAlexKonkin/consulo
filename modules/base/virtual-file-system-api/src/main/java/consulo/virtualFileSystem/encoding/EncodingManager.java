// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.encoding;

import consulo.annotation.DeprecationInfo;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.util.Collection;

/**
 * @see ApplicationEncodingManager
 * @see EncodingProjectManager
 */
public abstract class EncodingManager extends EncodingRegistry {
  public static final String PROP_NATIVE2ASCII_SWITCH = "native2ascii";
  public static final String PROP_PROPERTIES_FILES_ENCODING = "propertiesFilesEncoding";

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use ApplicationEncodingManager class")
  public static EncodingManager getInstance() {
    return ApplicationEncodingManager.getInstance();
  }

  @Nonnull
  public abstract Collection<Charset> getFavorites();

  @Override
  public abstract boolean isNative2AsciiForPropertiesFiles();

  public abstract void setNative2AsciiForPropertiesFiles(VirtualFile virtualFile, boolean native2Ascii);

  /**
   * @return returns empty for system default
   */
  @Nonnull
  public abstract String getDefaultCharsetName();

  public void setDefaultCharsetName(@Nonnull String name) {
    throw new UnsupportedOperationException("Not implemented");
  }

  /**
   * @return null for system-default
   */
  @Override
  @Nullable
  public abstract Charset getDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile);

  public abstract void setDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile, @Nullable Charset charset);

  /**
   * @return encoding used by default in {@link GeneralCommandLine}
   */
  @Override
  @Nonnull
  public abstract Charset getDefaultConsoleEncoding();

  public boolean shouldAddBOMForNewUtf8File() {
    return false;
  }
}
