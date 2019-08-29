/*
 * Copyright 2013-2019 consulo.io
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
package consulo.application;

import consulo.annotations.DeprecationInfo;
import consulo.platform.Platform;

/**
 * @author VISTALL
 * @since 2019-05-09
 */
public class WriteThreadOption {
  @Deprecated
  @DeprecationInfo("Always enabled")
  public static boolean isSubWriteThreadSupported() {
    return true;
  }

  public static boolean isSeparateWriteThreadSuppored() {
    return Platform.current().isWebService();
  }
}
