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
package com.intellij.packaging.impl.artifacts;

import consulo.util.collection.MultiValuesMap;
import com.intellij.openapi.util.io.FileUtil;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ModifiableArtifactModel;
import com.intellij.packaging.impl.elements.FileOrDirectoryCopyPackagingElement;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import com.intellij.util.PathUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VirtualFileAdapter;
import consulo.virtualFileSystem.event.VirtualFileMoveEvent;
import consulo.virtualFileSystem.event.VirtualFilePropertyEvent;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * @author nik
 */
public class ArtifactVirtualFileListener extends VirtualFileAdapter {
  private final CachedValue<MultiValuesMap<String, Artifact>> myParentPathsToArtifacts;
  private final ArtifactManagerImpl myArtifactManager;

  public ArtifactVirtualFileListener(Project project, final ArtifactManagerImpl artifactManager) {
    myArtifactManager = artifactManager;
    myParentPathsToArtifacts =
      CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<MultiValuesMap<String, Artifact>>() {
        public Result<MultiValuesMap<String, Artifact>> compute() {
          MultiValuesMap<String, Artifact> result = computeParentPathToArtifactMap();
          return Result.createSingleDependency(result, artifactManager.getModificationTracker());
        }
      }, false);
  }

  private MultiValuesMap<String, Artifact> computeParentPathToArtifactMap() {
    final MultiValuesMap<String, Artifact> result = new MultiValuesMap<String, Artifact>();
    for (final Artifact artifact : myArtifactManager.getArtifacts()) {
      ArtifactUtil.processFileOrDirectoryCopyElements(artifact, new PackagingElementProcessor<FileOrDirectoryCopyPackagingElement<?>>() {
        @Override
        public boolean process(@Nonnull FileOrDirectoryCopyPackagingElement<?> element, @Nonnull PackagingElementPath pathToElement) {
          String path = element.getFilePath();
          while (path.length() > 0) {
            result.put(path, artifact);
            path = PathUtil.getParentPath(path);
          }
          return true;
        }
      }, myArtifactManager.getResolvingContext(), false);
    }
    return result;
  }


  @Override
  public void fileMoved(@Nonnull VirtualFileMoveEvent event) {
    final String oldPath = event.getOldParent().getPath() + "/" + event.getFileName();
    filePathChanged(oldPath, event.getNewParent().getPath() + "/" + event.getFileName());
  }

  private void filePathChanged(@Nonnull final String oldPath, @Nonnull final String newPath) {
    final Collection<Artifact> artifacts = myParentPathsToArtifacts.getValue().get(oldPath);
    if (artifacts != null) {
      final ModifiableArtifactModel model = myArtifactManager.createModifiableModel();
      for (Artifact artifact : artifacts) {
        final Artifact copy = model.getOrCreateModifiableArtifact(artifact);
        ArtifactUtil.processFileOrDirectoryCopyElements(copy, new PackagingElementProcessor<FileOrDirectoryCopyPackagingElement<?>>() {
          @Override
          public boolean process(@Nonnull FileOrDirectoryCopyPackagingElement<?> element, @Nonnull PackagingElementPath pathToElement) {
            final String path = element.getFilePath();
            if (FileUtil.startsWith(path, oldPath)) {
              element.setFilePath(newPath + path.substring(oldPath.length()));
            }
            return true;
          }
        }, myArtifactManager.getResolvingContext(), false);
      }
      model.commit();
    }
  }

  @Override
  public void propertyChanged(@Nonnull VirtualFilePropertyEvent event) {
    if (VirtualFile.PROP_NAME.equals(event.getPropertyName())) {
      final VirtualFile parent = event.getParent();
      if (parent != null) {
        filePathChanged(parent.getPath() + "/" + event.getOldValue(), parent.getPath() + "/" + event.getNewValue());
      }
    }
  }
}
