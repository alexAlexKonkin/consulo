/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.template.impl.editorActions;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.template.impl.TemplateSettings;
import consulo.language.editor.action.TypedHandlerDelegate;
import consulo.language.editor.template.TemplateManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;

@ExtensionImpl(id = "space")
public class SpaceHandler extends TypedHandlerDelegate {
  @Override
  public Result beforeCharTyped(char charTyped, Project project, Editor editor, PsiFile file, FileType fileType) {
    if (charTyped == TemplateSettings.SPACE_CHAR && TemplateManager.getInstance(project).startTemplate(editor, TemplateSettings.SPACE_CHAR)) {
      return Result.STOP;
    }

    return super.beforeCharTyped(charTyped, project, editor, file, fileType);
  }
}
