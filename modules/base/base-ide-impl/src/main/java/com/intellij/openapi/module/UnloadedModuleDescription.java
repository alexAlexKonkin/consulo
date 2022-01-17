/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.module;

import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import javax.annotation.Nonnull;

import java.util.List;

/**
 * Represents a module which is unloaded from the project. Such modules aren't shown in UI (except for a special 'Load/Unload Modules' dialog),
 * all of their contents is excluded from the project so it isn't indexed or compiled.
 *
 * @author nik
 */
public interface UnloadedModuleDescription extends ModuleDescription {
  @Nonnull
  List<VirtualFilePointer> getContentRoots();

  @Nonnull
  List<String> getGroupPath();
}
