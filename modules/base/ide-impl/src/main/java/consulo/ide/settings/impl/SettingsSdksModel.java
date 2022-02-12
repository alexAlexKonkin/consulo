/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.settings.impl;

import consulo.ui.ex.action.DefaultActionGroup;
import consulo.configurable.ConfigurationException;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModel;
import consulo.content.bundle.SdkTypeId;
import com.intellij.openapi.ui.MasterDetailsComponent;
import consulo.util.lang.function.Condition;
import com.intellij.util.Consumer;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2020-01-20
 */
public interface SettingsSdksModel extends SdkModel {
  void reset();

  Map<Sdk, Sdk> getModifiedSdksMap();

  boolean isModified();

  void apply(MasterDetailsComponent masterDetailsComponent) throws ConfigurationException;

  void disposeUIResources();

  void doAdd(Sdk newSdk, @Nullable Consumer<Sdk> updateTree);

  void removeSdk(Sdk sdk);

  default void createAddActions(DefaultActionGroup group, final JComponent parent, final Consumer<Sdk> updateTree) {
    createAddActions(group, parent, updateTree, null);
  }

  void createAddActions(DefaultActionGroup group, final JComponent parent, final Consumer<Sdk> updateTree, @Nullable Condition<SdkTypeId> filter);
}
