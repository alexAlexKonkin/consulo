/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.fileTemplate.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.language.file.FileTypeManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;


/**
 * @author Dmitry Avdeev
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "ExportableFileTemplateSettings", storages = @Storage(file = StoragePathMacros.APP_CONFIG + "/" + ExportableFileTemplateSettings.EXPORTABLE_SETTINGS_FILE))
public class ExportableFileTemplateSettings extends FileTemplateSettingsBase {
  @Inject
  public ExportableFileTemplateSettings(@Nonnull FileTypeManager typeManager) {
    super(typeManager, null);
  }
}