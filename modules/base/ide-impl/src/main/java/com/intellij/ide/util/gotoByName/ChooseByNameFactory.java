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
package com.intellij.ide.util.gotoByName;

import consulo.ide.ServiceManager;
import consulo.project.Project;
import javax.annotation.Nonnull;

/**
 * User: anna
 * Date: Jan 26, 2005
 */
public abstract class ChooseByNameFactory {
  public static ChooseByNameFactory getInstance(Project project){
    return ServiceManager.getService(project, ChooseByNameFactory.class);
  }

  public abstract ChooseByNamePopupComponent createChooseByNamePopupComponent(@Nonnull ChooseByNameModel model);
}
