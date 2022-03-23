/**
 * @author VISTALL
 * @since 16-Feb-22
 */
module consulo.language.impl {
  requires consulo.project.api;
  requires consulo.language.api;
  requires consulo.document.impl;
  requires consulo.undo.redo.api;
  requires consulo.util.interner;

  exports consulo.language.impl;
  exports consulo.language.impl.ast;
  exports consulo.language.impl.file;
  exports consulo.language.impl.psi;
  exports consulo.language.impl.psi.reference;

  // internal implementation
  exports consulo.language.impl.plain to consulo.ide.impl, consulo.injecting.pico.impl, consulo.test.impl;
  exports consulo.language.impl.ast.internal to consulo.ide.impl, consulo.injecting.pico.impl, consulo.test.impl, consulo.language.code.style.api;
  exports consulo.language.impl.file.internal to consulo.ide.impl, consulo.language.inject.impl;
  exports consulo.language.impl.parser.internal to consulo.ide.impl, consulo.injecting.pico.impl, consulo.test.impl;
  exports consulo.language.impl.psi.internal to
          consulo.ide.impl,
          consulo.injecting.pico.impl,
          consulo.test.impl,
          consulo.component.impl,
          consulo.language.code.style.api,
          consulo.language.editor.impl,
          consulo.language.inject.impl;
  exports consulo.language.impl.psi.internal.diff to consulo.ide.impl, consulo.language.inject.impl;
  exports consulo.language.impl.psi.internal.pointer to consulo.ide.impl, consulo.injecting.pico.impl, consulo.test.impl, consulo.language.inject.impl;
  exports consulo.language.impl.psi.internal.stub to consulo.ide.impl;
  exports consulo.language.impl.pom.internal to consulo.ide.impl;
}