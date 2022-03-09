/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.desktop.awt.action;

import consulo.application.AllIcons;
import com.intellij.ide.dnd.DnDActionInfo;
import com.intellij.ide.dnd.DnDDragStartBean;
import com.intellij.ide.dnd.DnDImage;
import com.intellij.ide.dnd.DnDSupport;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBList;
import com.intellij.util.Function;
import com.intellij.util.IconUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.annotation.Nullable;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
public class TestDnd extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    new DialogWrapper(e == null ? null : e.getData(CommonDataKeys.PROJECT)) {
      {
        setTitle("DnD Test");
        setScalableSize(600, 500);
        init();
      }
      @Nullable
      @Override
      protected JComponent createCenterPanel() {
        JBList list = new JBList(new String[]{"1111111", "222222", "333333", "44444", "555555555555555555555555"});
        DnDSupport.createBuilder(list)
          .setBeanProvider(new Function<DnDActionInfo, DnDDragStartBean>() {
            @Override
            public DnDDragStartBean fun(DnDActionInfo info) {
              return new DnDDragStartBean("something");
            }
          })
          .setImageProvider(new Function<DnDActionInfo, DnDImage>() {
            @Override
            public DnDImage fun(DnDActionInfo info) {
              return new DnDImage(IconUtil.toImage(TargetAWT.to(AllIcons.Icon32)));
            }
          })
          .install();

        return list;
      }
    }.show();
  }
}