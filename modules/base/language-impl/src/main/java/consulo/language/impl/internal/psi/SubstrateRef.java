/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.impl.internal.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationManager;
import consulo.component.ProcessCanceledException;
import consulo.language.ast.ASTNode;
import consulo.language.ast.FileASTNode;
import consulo.language.impl.ast.SharedImplUtil;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiInvalidElementAccessException;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.stub.PsiFileStub;
import consulo.language.psi.stub.Stub;
import consulo.language.psi.stub.StubElement;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public abstract class SubstrateRef {
  private static final Logger LOG = Logger.getInstance(SubstrateRef.class);

  @Nonnull
  public abstract ASTNode getNode();

  @Nullable
  public Stub getStub() {
    return null;
  }

  @Nullable
  public Stub getGreenStub() {
    return getStub();
  }

  public abstract boolean isValid();

  @Nonnull
  public abstract PsiFile getContainingFile();

  @Nonnull
  public static SubstrateRef createInvalidRef(@Nonnull final StubBasedPsiElement<?> psi) {
    return new SubstrateRef() {
      @Nonnull
      @Override
      public ASTNode getNode() {
        throw new PsiInvalidElementAccessException(psi);
      }

      @Override
      public boolean isValid() {
        return false;
      }

      @Nonnull
      @Override
      public PsiFile getContainingFile() {
        throw new PsiInvalidElementAccessException(psi);
      }
    };
  }

  public static SubstrateRef createAstStrongRef(@Nonnull final ASTNode node) {
    return new SubstrateRef() {

      @Nonnull
      @Override
      public ASTNode getNode() {
        return node;
      }

      @Override
      public boolean isValid() {
        FileASTNode fileElement = SharedImplUtil.findFileElement(node);
        PsiElement file = fileElement == null ? null : fileElement.getPsi();
        return file != null && file.isValid();
      }

      @Nonnull
      @Override
      public PsiFile getContainingFile() {
        PsiFile file = SharedImplUtil.getContainingFile(node);
        if (file == null) throw PsiInvalidElementAccessException.createByNode(node, null);
        return file;
      }
    };
  }

  public static class StubRef extends SubstrateRef {
    private final StubElement myStub;

    public StubRef(@Nonnull StubElement stub) {
      myStub = stub;
    }

    @Nonnull
    @Override
    public ASTNode getNode() {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public Stub getStub() {
      return myStub;
    }

    @Override
    public boolean isValid() {
      PsiFileStub<?> fileStub = myStub.getContainingFileStub();
      if (fileStub == null) return false;
      PsiFile psi = fileStub.getPsi();
      return psi != null && psi.isValid();
    }

    @Nonnull
    @Override
    public PsiFile getContainingFile() {
      PsiFileStub<?> stub = myStub.getContainingFileStub();
      if (stub == null) {
        throw new PsiInvalidElementAccessException(myStub.getPsi(),
                                                   "stub hierarchy is invalid: " + this + " (" + getClass() + ")" +
                                                     " has null containing file stub", null);
      }
      PsiFile psi = stub.getPsi();
      if (psi != null) {
        return psi;
      }
      return reportFileInvalidError(stub);
    }

    @RequiredReadAction
    private PsiFile reportFileInvalidError(@Nonnull PsiFileStub<?> stub) {
      ApplicationManager.getApplication().assertReadAccessAllowed();

      String reason = stub.getInvalidationReason();
      PsiInvalidElementAccessException exception =
        new PsiInvalidElementAccessException(myStub.getPsi(),
                                             "no psi for file stub " + stub + " (" + stub.getClass() + "), invalidation reason=" + reason,
                                             null);
      if (PsiFileImpl.STUB_PSI_MISMATCH.equals(reason)) {
        // we're between finding stub-psi mismatch and the next EDT spot where the file is reparsed and stub rebuilt
        //    see com.intellij.psi.impl.source.PsiFileImpl.rebuildStub()
        // most likely it's just another highlighting thread accessing the same PSI concurrently and not yet canceled, so cancel it
        throw new ProcessCanceledException(exception);
      }
      throw exception;
    }
  }
}
