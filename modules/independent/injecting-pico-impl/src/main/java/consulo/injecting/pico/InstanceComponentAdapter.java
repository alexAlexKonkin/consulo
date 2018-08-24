/*
 * Copyright 2013-2018 consulo.io
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
package consulo.injecting.pico;

import com.intellij.util.pico.AssignableToComponentAdapter;
import consulo.injecting.key.InjectingKey;
import org.picocontainer.PicoContainer;
import org.picocontainer.PicoInitializationException;
import org.picocontainer.PicoIntrospectionException;
import org.picocontainer.PicoVisitor;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-08-24
 */
public class InstanceComponentAdapter<T> implements AssignableToComponentAdapter {
  private final InjectingKey<T> myInterfaceKey;
  private final T myValue;

  public InstanceComponentAdapter(InjectingKey<T> interfaceKey, T value) {
    myInterfaceKey = interfaceKey;
    myValue = value;
  }

  @Override
  public String getComponentKey() {
    return myInterfaceKey.getTargetClassName();
  }

  @Override
  public Class getComponentImplementation() {
    return myValue.getClass();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object getComponentInstance(@Nonnull PicoContainer container) throws PicoInitializationException, PicoIntrospectionException {
    return myValue;
  }

  @Override
  public void verify(final PicoContainer container) throws PicoIntrospectionException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void accept(final PicoVisitor visitor) {
    visitor.visitComponentAdapter(this);
  }

  @Override
  public String getAssignableToClassName() {
    return myValue.getClass().getName();
  }

  @Override
  public String toString() {
    return "InstanceComponentAdapter[" + myInterfaceKey.getTargetClassName() + "]";
  }
}