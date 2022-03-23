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
package com.intellij.xdebugger.impl.frame;

import consulo.dataContext.DataManager;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataProvider;
import consulo.application.ApplicationManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ObjectLongHashMap;
import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.debugger.XDebugProcess;
import consulo.debugger.XDebugSession;
import consulo.debugger.XSourcePosition;
import consulo.debugger.frame.XStackFrame;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil;
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueContainerNode;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import consulo.util.dataholder.Key;
import gnu.trove.TObjectLongHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.*;

/**
 * @author nik
 */
public class XVariablesView extends XVariablesViewBase implements DataProvider {
  public static final Key<InlineVariablesInfo> DEBUG_VARIABLES = Key.create("debug.variables");
  public static final Key<ObjectLongHashMap<VirtualFile>> DEBUG_VARIABLES_TIMESTAMPS = Key.create("debug.variables.timestamps");
  private final JPanel myComponent;

  public XVariablesView(@Nonnull XDebugSessionImpl session) {
    super(session.getProject(), session.getDebugProcess().getEditorsProvider(), session.getValueMarkers());
    myComponent = new BorderLayoutPanel();
    myComponent.add(super.getPanel());
    DataManager.registerDataProvider(myComponent, this);
  }

  @Override
  public JPanel getPanel() {
    return myComponent;
  }

  @Override
  public void processSessionEvent(@Nonnull SessionEvent event, @Nonnull XDebugSession session) {
    if (ApplicationManager.getApplication().isDispatchThread()) { // mark nodes obsolete asap
      getTree().markNodesObsolete();
    }

    XStackFrame stackFrame = session.getCurrentStackFrame();
    DebuggerUIUtil.invokeLater(() -> {
      XDebuggerTree tree = getTree();

      if (event == SessionEvent.BEFORE_RESUME || event == SessionEvent.SETTINGS_CHANGED) {
        saveCurrentTreeState(stackFrame);
        if (event == SessionEvent.BEFORE_RESUME) {
          return;
        }
      }

      tree.markNodesObsolete();
      if (stackFrame != null) {
        cancelClear();
        buildTreeAndRestoreState(stackFrame);
      }
      else {
        requestClear();
      }
    });
  }

  @Override
  public void dispose() {
    clearInlineData(getTree());
    super.dispose();
  }

  private static void clearInlineData(XDebuggerTree tree) {
    tree.getProject().putUserData(DEBUG_VARIABLES, null);
    tree.getProject().putUserData(DEBUG_VARIABLES_TIMESTAMPS, null);
    tree.updateEditor();
    clearInlays(tree);
  }

  protected void addEmptyMessage(XValueContainerNode root) {
    XDebugSession session = getSession(getPanel());
    if (session != null) {
      if (!session.isStopped() && session.isPaused()) {
        root.setInfoMessage("Frame is not available", null);
      }
      else {
        XDebugProcess debugProcess = session.getDebugProcess();
        root.setInfoMessage(debugProcess.getCurrentStateMessage(), debugProcess.getCurrentStateHyperlinkListener());
      }
    }
  }

  @Override
  protected void clear() {
    XDebuggerTree tree = getTree();
    tree.setSourcePosition(null);
    clearInlineData(tree);

    XValueContainerNode root = createNewRootNode(null);
    addEmptyMessage(root);
    super.clear();
  }

  @Nullable
  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    if (CommonDataKeys.VIRTUAL_FILE == dataId) {
      return getCurrentFile(getTree());
    }
    return null;
  }

  public static class InlineVariablesInfo {
    private final Map<Pair<VirtualFile, Integer>, Set<Entry>> myData = new HashMap<>();
    private final TObjectLongHashMap<VirtualFile> myTimestamps = new ObjectLongHashMap<>();

    @Nullable
    public synchronized List<XValueNodeImpl> get(@Nonnull VirtualFile file, int line, long currentTimestamp) {
      long timestamp = myTimestamps.get(file);
      if (timestamp == -1 || timestamp < currentTimestamp) {
        return null;
      }
      Set<Entry> entries = myData.get(Pair.create(file, line));
      if (entries == null) return null;
      return ContainerUtil.map(entries, entry -> entry.myNode);
    }

    public synchronized void put(@Nonnull VirtualFile file, @Nonnull XSourcePosition position, @Nonnull XValueNodeImpl node, long timestamp) {
      myTimestamps.put(file, timestamp);
      Pair<VirtualFile, Integer> key = Pair.create(file, position.getLine());
      myData.computeIfAbsent(key, k -> new TreeSet<>()).add(new Entry(position.getOffset(), node));
    }

    private static class Entry implements Comparable<Entry> {
      private final long myOffset;
      private final XValueNodeImpl myNode;

      public Entry(long offset, @Nonnull XValueNodeImpl node) {
        myOffset = offset;
        myNode = node;
      }

      @Override
      public int compareTo(Entry o) {
        if (myNode == o.myNode) return 0;
        int res = Comparing.compare(myOffset, o.myOffset);
        if (res == 0) {
          return XValueNodeImpl.COMPARATOR.compare(myNode, o.myNode);
        }
        return res;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Entry entry = (Entry)o;

        if (!myNode.equals(entry.myNode)) return false;

        return true;
      }

      @Override
      public int hashCode() {
        return myNode.hashCode();
      }
    }
  }
}
