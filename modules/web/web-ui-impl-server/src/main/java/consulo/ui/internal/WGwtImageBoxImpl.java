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

import com.vaadin.ui.AbstractComponent;
import consulo.ui.Component;
import consulo.ui.ImageBox;
import consulo.ui.RequiredUIAccess;
import consulo.ui.shared.Size;
import consulo.ui.image.Image;
import consulo.ui.internal.image.WGwtImageUrlCache;
import consulo.ui.internal.image.WGwtImageWithState;
import consulo.web.gwt.shared.ui.state.ImageBoxState;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
public class WGwtImageBoxImpl extends AbstractComponent implements ImageBox, VaadinWrapper {
  private WGwtImageWithState myImage;

  public WGwtImageBoxImpl(Image image) {
    myImage = WGwtImageUrlCache.map(image);

    getState().myImageState = myImage.getState();
  }

  @Override
  protected ImageBoxState getState() {
    return (ImageBoxState)super.getState();
  }

  @Nonnull
  @Override
  public Image getImage() {
    return myImage;
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {
  }
}
