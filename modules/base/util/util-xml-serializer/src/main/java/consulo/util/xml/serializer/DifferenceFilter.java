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
package consulo.util.xml.serializer;

import consulo.util.lang.Comparing;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.function.Predicate;

/**
 * @author peter
*/
public class DifferenceFilter<T> implements DefaultJDOMExternalizer.JDOMFilter, Predicate<Field> {
  private final T myThisSettings;
  private final T myParentSettings;

  public DifferenceFilter(final T object, final T parentObject) {
    myThisSettings = object;
    myParentSettings = parentObject;
  }

  @Override
  public final boolean isAccept(@Nonnull Field field) {
    return test(field);
  }

  @Override
  public boolean test(Field field) {
    try {
      Object thisValue = field.get(myThisSettings);
      Object parentValue = field.get(myParentSettings);
      return !Comparing.equal(thisValue, parentValue);
    }
    catch (Throwable e) {
      return true;
    }
  }
}
