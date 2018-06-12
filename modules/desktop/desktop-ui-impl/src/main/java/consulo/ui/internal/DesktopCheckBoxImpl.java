/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.internal;

import com.intellij.ui.components.JBCheckBox;
import consulo.awt.TargetAWT;
import consulo.ui.CheckBox;
import consulo.ui.KeyCode;
import consulo.ui.RequiredUIAccess;
import consulo.ui.ValueComponent;
import consulo.ui.internal.base.SwingComponentDelegate;
import consulo.ui.util.MnemonicInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class DesktopCheckBoxImpl extends SwingComponentDelegate<JBCheckBox> implements CheckBox, SwingWrapper {
  public DesktopCheckBoxImpl() {
    myComponent = new JBCheckBox();
  }

  @Nonnull
  @Override
  public Boolean getValue() {
    return myComponent.isSelected();
  }

  @RequiredUIAccess
  @Override
  public void setValue(@Nullable Boolean value, boolean fireEvents) {
    if (value == null) {
      throw new IllegalArgumentException();
    }

    myComponent.setSelected(value);
  }

  @Nonnull
  @Override
  public String getText() {
    return myComponent.getText();
  }

  @RequiredUIAccess
  @Override
  public void setText(@Nonnull String text) {
    MnemonicInfo mnemonicInfo = MnemonicInfo.parse(text);
    if (mnemonicInfo == null) {
      myComponent.setText(text);

      setMnemonicKey(null);
      setMnemonicTextIndex(-1);
    }
    else {
      myComponent.setText(mnemonicInfo.getText());
      setMnemonicKey(mnemonicInfo.getKeyCode());
      setMnemonicTextIndex(mnemonicInfo.getIndex());
    }
  }

  @Override
  public void addValueListener(@Nonnull ValueComponent.ValueListener<Boolean> valueListener) {
    myComponent.addItemListener(new DesktopValueListenerAsItemListenerImpl<>(this, valueListener, false));
  }

  @Override
  public void removeValueListener(@Nonnull ValueComponent.ValueListener<Boolean> valueListener) {
    myComponent.removeItemListener(new DesktopValueListenerAsItemListenerImpl<>(this, valueListener, false));
  }

  @Override
  public void setMnemonicKey(@Nullable KeyCode key) {
    myComponent.setMnemonic(key == null ? 0 : TargetAWT.to(key));
  }

  @Override
  public void setMnemonicTextIndex(int index) {
    myComponent.setDisplayedMnemonicIndex(index);
  }
}
