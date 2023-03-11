/*
 * Copyright 2013-2023 consulo.io
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
package consulo.language.editor.internal.inspection;

import consulo.language.editor.inspection.InspectionToolState;
import consulo.util.lang.ObjectUtil;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 04/03/2023
 */
public class DummyInspectionToolState implements InspectionToolState<Object> {
  public static final DummyInspectionToolState INSTANCE = new DummyInspectionToolState();

  @Nullable
  @Override
  public Object getState() {
    return ObjectUtil.NULL;
  }

  @Override
  public void loadState(Object state) {

  }
}
