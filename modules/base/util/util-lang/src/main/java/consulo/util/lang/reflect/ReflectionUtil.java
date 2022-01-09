/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.util.lang.reflect;

import consulo.util.lang.ref.SimpleReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * Based on IDEA version
 */
public class ReflectionUtil {
  private static final Logger LOG = LoggerFactory.getLogger(ReflectionUtil.class);

  public static boolean isAssignable(@Nonnull Class<?> ancestor, @Nonnull Class<?> descendant) {
    return ancestor == descendant || ancestor.isAssignableFrom(descendant);
  }

  public static <T> T getStaticFieldValue(@Nonnull Class objectClass, @Nullable Class<T> fieldType, @Nonnull String fieldName) {
    try {
      final Field field = findAssignableField(objectClass, fieldType, fieldName);
      if (!Modifier.isStatic(field.getModifiers())) {
        throw new IllegalArgumentException("Field " + objectClass + "." + fieldName + " is not static");
      }
      return (T)field.get(null);
    }
    catch (NoSuchFieldException | IllegalAccessException e) {
      LOG.debug(e.getMessage(), e);
      return null;
    }
  }

  @Nonnull
  public static <T> T newInstance(@Nonnull Class<? extends T> clazz) {
    try {
      Constructor<? extends T> declaredConstructor = clazz.getDeclaredConstructor();
      declaredConstructor.setAccessible(true);
      return declaredConstructor.newInstance();
    }
    catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  public static Field findAssignableField(@Nonnull Class<?> clazz, @Nullable final Class<?> fieldType, @Nonnull final String fieldName) throws NoSuchFieldException {
    Field result = processFields(clazz, field -> fieldName.equals(field.getName()) && (fieldType == null || fieldType.isAssignableFrom(field.getType())));
    if (result != null) return result;
    throw new NoSuchFieldException("Class: " + clazz + " fieldName: " + fieldName + " fieldType: " + fieldType);
  }

  private static Field processFields(@Nonnull Class clazz, @Nonnull Predicate<Field> checker) {
    SimpleReference<Field> fieldRef = SimpleReference.create();

    processClasses(clazz, it -> {
      for (Field field : it.getDeclaredFields()) {
        if (checker.test(field)) {
          fieldRef.set(field);
          return false;
        }
      }

      return true;
    });

    return fieldRef.get();
  }

  protected static void processClasses(Class<?> clazz, Predicate<Class> classConsumer) {
    processClasses(clazz, classConsumer, new HashSet<>());
  }

  protected static void processClasses(Class<?> clazz, Predicate<Class> classConsumer, Set<Class> processed) {
    if (processed.add(clazz)) {
      if (!classConsumer.test(clazz)) {
        return;
      }
    }

    for (Class<?> intf : clazz.getInterfaces()) {
      processClasses(intf, classConsumer, processed);
    }

    Class<?> superclass = clazz.getSuperclass();
    if (superclass != null) {
      processClasses(superclass, classConsumer, processed);
    }
  }

  @Nonnull
  public static Class forName(@Nonnull String fqn) {
    try {
      return Class.forName(fqn);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  public static Class findClassOrNull(@Nonnull String fqn, @Nonnull ClassLoader classLoader) {
    try {
      return Class.forName(fqn, true, classLoader);
    }
    catch (ClassNotFoundException ignored) {
      return null;
    }
  }

  /**
   * Returns the class this method was called 'framesToSkip' frames up the caller hierarchy.
   * <p/>
   * NOTE:
   * <b>Extremely expensive!
   * Please consider not using it.
   * These aren't the droids you're looking for!</b>
   */
  @Nullable
  public static Class findCallerClass(int framesToSkip) {
    StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    StackWalker.StackFrame frame = walker.walk(it -> {
      Optional<StackWalker.StackFrame> first = it.skip(framesToSkip).findFirst();
      return first.get();
    });

    return frame.getDeclaringClass();
  }

  @Nonnull
  public static Class<?> getRawType(@Nonnull Type type) {
    if (type instanceof Class) {
      return (Class)type;
    }
    if (type instanceof ParameterizedType) {
      return getRawType(((ParameterizedType)type).getRawType());
    }
    if (type instanceof GenericArrayType) {
      //todo[peter] don't create new instance each time
      return Array.newInstance(getRawType(((GenericArrayType)type).getGenericComponentType()), 0).getClass();
    }
    assert false : type;
    return null;
  }

  @Nonnull
  public static Class<?> boxType(@Nonnull Class<?> type) {
    if (!type.isPrimitive()) return type;
    if (type == boolean.class) return Boolean.class;
    if (type == byte.class) return Byte.class;
    if (type == short.class) return Short.class;
    if (type == int.class) return Integer.class;
    if (type == long.class) return Long.class;
    if (type == float.class) return Float.class;
    if (type == double.class) return Double.class;
    if (type == char.class) return Character.class;
    return type;
  }

  @Nonnull
  public static List<Method> getClassPublicMethods(@Nonnull Class aClass) {
    return getClassPublicMethods(aClass, false);
  }

  @Nonnull
  public static List<Method> getClassPublicMethods(@Nonnull Class aClass, boolean includeSynthetic) {
    Method[] methods = aClass.getMethods();
    return includeSynthetic ? Arrays.asList(methods) : filterRealMethods(methods);
  }

  @Nonnull
  public static List<Method> getClassDeclaredMethods(@Nonnull Class aClass) {
    return getClassDeclaredMethods(aClass, false);
  }

  @Nonnull
  public static List<Method> getClassDeclaredMethods(@Nonnull Class aClass, boolean includeSynthetic) {
    Method[] methods = aClass.getDeclaredMethods();
    return includeSynthetic ? Arrays.asList(methods) : filterRealMethods(methods);
  }

  @Nonnull
  public static List<Field> getClassDeclaredFields(@Nonnull Class aClass) {
    Field[] fields = aClass.getDeclaredFields();
    return Arrays.asList(fields);
  }

  @Nonnull
  private static List<Method> filterRealMethods(@Nonnull Method[] methods) {
    List<Method> result = new ArrayList<>();
    for (Method method : methods) {
      if (!method.isSynthetic()) {
        result.add(method);
      }
    }
    return result;
  }
}
