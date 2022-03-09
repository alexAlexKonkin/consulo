// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.IdeEventQueue;
import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.ShortcutSet;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.colorScheme.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl;
import com.intellij.openapi.keymap.KeymapUtil;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import com.intellij.openapi.util.DimensionService;
import consulo.util.lang.ref.Ref;
import com.intellij.openapi.util.text.StringUtil;
import consulo.application.ui.wm.IdeFocusManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.ui.CaptionPanel;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.ScrollingUtil;
import com.intellij.ui.WindowMoveListener;
import com.intellij.ui.components.JBCheckBox;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.JBUIScale;
import com.intellij.ui.speedSearch.ListWithFilter;
import com.intellij.ui.speedSearch.NameFilteringListModel;
import com.intellij.ui.speedSearch.SpeedSearch;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import static consulo.ui.ex.awt.speedSearch.SpeedSearchSupply.ENTERED_PREFIX_PROPERTY_NAME;

public class RecentLocationsAction extends DumbAwareAction {
  public static final String RECENT_LOCATIONS_ACTION_ID = "RecentLocations";
  private static final String LOCATION_SETTINGS_KEY = "recent.locations.popup";
  private static final int DEFAULT_WIDTH = JBUIScale.scale(700);
  private static final int DEFAULT_HEIGHT = JBUIScale.scale(530);
  private static final int MINIMUM_WIDTH = JBUIScale.scale(600);
  private static final int MINIMUM_HEIGHT = JBUIScale.scale(450);
  private static final Color SHORTCUT_FOREGROUND_COLOR = UIUtil.getContextHelpForeground();
  public static final String SHORTCUT_HEX_COLOR = String.format("#%02x%02x%02x", SHORTCUT_FOREGROUND_COLOR.getRed(), SHORTCUT_FOREGROUND_COLOR.getGreen(), SHORTCUT_FOREGROUND_COLOR.getBlue());

  static final String EMPTY_FILE_TEXT = IdeBundle.message("recent.locations.popup.empty.file.text");

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed(RECENT_LOCATIONS_ACTION_ID);
    Project project = e == null ? null : e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }

    showPopup(project, false);
  }

  public static void showPopup(@Nonnull Project project, boolean showChanged) {
    RecentLocationsDataModel model = new RecentLocationsDataModel(project, new ArrayList<>());
    JBList<RecentLocationItem> list = new JBList<>(JBList.createDefaultListModel(model.getPlaces(showChanged)));
    final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    scrollPane.setBorder(BorderFactory.createEmptyBorder());

    ShortcutSet showChangedOnlyShortcutSet = KeymapUtil.getActiveKeymapShortcuts(RECENT_LOCATIONS_ACTION_ID);
    JBCheckBox checkBox = createCheckbox(showChangedOnlyShortcutSet, showChanged);

    ListWithFilter<RecentLocationItem> listWithFilter = (ListWithFilter<RecentLocationItem>)ListWithFilter.wrap(list, scrollPane, getNamer(model, checkBox), true);
    listWithFilter.setAutoPackHeight(false);
    listWithFilter.setBorder(BorderFactory.createEmptyBorder());

    final SpeedSearch speedSearch = listWithFilter.getSpeedSearch();
    speedSearch.addChangeListener(evt -> {
      if (evt.getPropertyName().equals(ENTERED_PREFIX_PROPERTY_NAME)) {
        if (StringUtil.isEmpty(speedSearch.getFilter())) {
          model.getEditorsToRelease().forEach(editor -> clearSelectionInEditor(editor));
        }
      }
    });

    list.setCellRenderer(new RecentLocationsRenderer(project, speedSearch, model, checkBox));
    list.setEmptyText(IdeBundle.message("recent.locations.popup.empty.text"));
    list.setBackground(TargetAWT.to(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground()));
    ScrollingUtil.installActions(list);
    ScrollingUtil.ensureSelectionExists(list);

    JLabel title = createTitle(showChanged);

    JPanel topPanel = createHeaderPanel(title, checkBox);
    JPanel mainPanel = createMainPanel(listWithFilter, topPanel);

    Color borderColor = /*SystemInfo.isMac && LafManager.getInstance().getCurrentLookAndFeel() instanceof DarculaLookAndFeelInfo ? topPanel.getBackground() : */null;

    Ref<Boolean> navigationRef = Ref.create(false);
    JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(mainPanel, list).setProject(project).setCancelOnClickOutside(true).setRequestFocus(true).setCancelCallback(() -> {
      if (speedSearch.isHoldingFilter() && !navigationRef.get()) {
        speedSearch.reset();
        return false;
      }
      return true;
    }).setResizable(true).setMovable(true).setBorderColor(borderColor).setDimensionServiceKey(project, LOCATION_SETTINGS_KEY, true).setMinSize(new Dimension(MINIMUM_WIDTH, MINIMUM_HEIGHT))
            .setLocateWithinScreenBounds(false).createPopup();

    DumbAwareAction.create(event -> {
      checkBox.setSelected(!checkBox.isSelected());
      updateItems(model, listWithFilter, title, checkBox, popup);
    }).registerCustomShortcutSet(showChangedOnlyShortcutSet, list, popup);

    checkBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateItems(model, listWithFilter, title, checkBox, popup);
      }
    });

    if (DimensionService.getInstance().getSize(LOCATION_SETTINGS_KEY, project) == null) {
      popup.setSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
    }

    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        int clickCount = event.getClickCount();
        if (clickCount > 1 && clickCount % 2 == 0) {
          event.consume();
          final int i = list.locationToIndex(event.getPoint());
          if (i != -1) {
            list.setSelectedIndex(i);
            navigateToSelected(project, list, popup, navigationRef);
          }
        }
      }
    });

    popup.addListener(new JBPopupListener() {
      @Override
      public void onClosed(@Nonnull LightweightWindowEvent event) {
        model.getEditorsToRelease().forEach(editor -> EditorFactory.getInstance().releaseEditor(editor));
        model.getProjectConnection().disconnect();
      }
    });

    initSearchActions(project, model, listWithFilter, list, checkBox, popup, navigationRef);

    IdeEventQueue.getInstance().getPopupManager().closeAllPopups(false);

    list.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        if (!(e.getOppositeComponent() instanceof JCheckBox)) {
          popup.cancel();
        }
      }
    });

    showPopup(project, popup);
  }

  private static void updateItems(@Nonnull RecentLocationsDataModel data,
                                  @Nonnull ListWithFilter<RecentLocationItem> listWithFilter,
                                  @Nonnull JLabel title,
                                  @Nonnull JBCheckBox checkBox,
                                  @Nonnull JBPopup popup) {
    boolean state = checkBox.isSelected();
    updateModel(listWithFilter, data, state);
    updateTitleText(title, state);

    IdeFocusManager.getGlobalInstance().requestFocus(listWithFilter, false);

    popup.pack(false, false);
  }

  @Nonnull
  public static JBCheckBox createCheckbox(@Nonnull ShortcutSet checkboxShortcutSet, boolean showChanged) {
    String text = "<html>" +
                  IdeBundle.message("recent.locations.title.text") +
                  " <font color=\"" +
                  SHORTCUT_HEX_COLOR +
                  "\">" +
                  KeymapUtil.getShortcutsText(checkboxShortcutSet.getShortcuts()) +
                  "</font>" +
                  "</html>";
    JBCheckBox checkBox = new JBCheckBox(text);
    checkBox.setBorder(JBUI.Borders.empty());
    checkBox.setOpaque(false);
    checkBox.setSelected(showChanged);

    return checkBox;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled((e == null ? null : e.getData(CommonDataKeys.PROJECT)) != null);
  }

  static void clearSelectionInEditor(@Nonnull Editor editor) {
    editor.getSelectionModel().removeSelection(true);
  }

  private static void showPopup(@Nonnull Project project, @Nonnull JBPopup popup) {
    Point savedLocation = DimensionService.getInstance().getLocation(LOCATION_SETTINGS_KEY, project);
    Window recentFocusedWindow = TargetAWT.to(WindowManagerEx.getInstanceEx().getMostRecentFocusedWindow());
    if (savedLocation != null && recentFocusedWindow != null) {
      popup.showInScreenCoordinates(recentFocusedWindow, savedLocation);
    }
    else {
      popup.showCenteredInCurrentWindow(project);
    }
  }

  private static void updateModel(@Nonnull ListWithFilter<RecentLocationItem> listWithFilter, @Nonnull RecentLocationsDataModel data, boolean changed) {
    NameFilteringListModel<RecentLocationItem> model = (NameFilteringListModel<RecentLocationItem>)listWithFilter.getList().getModel();
    DefaultListModel<RecentLocationItem> originalModel = (DefaultListModel<RecentLocationItem>)model.getOriginalModel();

    originalModel.removeAllElements();
    data.getPlaces(changed).forEach(item -> originalModel.addElement(item));

    listWithFilter.getSpeedSearch().reset();
  }

  @Nonnull
  private static JPanel createMainPanel(@Nonnull ListWithFilter listWithFilter, @Nonnull JPanel topPanel) {
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.add(topPanel, BorderLayout.NORTH);
    mainPanel.add(listWithFilter, BorderLayout.CENTER);
    return mainPanel;
  }

  @Nonnull
  private static JPanel createHeaderPanel(@Nonnull JLabel title, @Nonnull JComponent checkbox) {
    JPanel topPanel = new CaptionPanel();
    topPanel.add(title, BorderLayout.WEST);
    topPanel.add(checkbox, BorderLayout.EAST);

    Dimension size = topPanel.getPreferredSize();
    size.height = JBUIScale.scale(29);
    topPanel.setPreferredSize(size);
    topPanel.setBorder(JBUI.Borders.empty(5, 8));

    WindowMoveListener moveListener = new WindowMoveListener(topPanel);
    topPanel.addMouseListener(moveListener);
    topPanel.addMouseMotionListener(moveListener);

    return topPanel;
  }

  @Nonnull
  private static JLabel createTitle(boolean showChanged) {
    JBLabel title = new JBLabel();
    title.setFont(title.getFont().deriveFont(Font.BOLD));
    updateTitleText(title, showChanged);
    return title;
  }

  private static void updateTitleText(@Nonnull JLabel title, boolean showChanged) {
    title.setText(showChanged ? IdeBundle.message("recent.locations.changed.locations") : IdeBundle.message("recent.locations.popup.title"));
  }

  @Nonnull
  private static Function<RecentLocationItem, String> getNamer(@Nonnull RecentLocationsDataModel data, @Nonnull JBCheckBox checkBox) {
    return value -> {
      String breadcrumb = data.getBreadcrumbsMap(checkBox.isSelected()).get(value.getInfo());
      EditorEx editor = value.getEditor();

      return breadcrumb + " " + value.getInfo().getFile().getName() + " " + editor.getDocument().getText();
    };
  }

  private static void initSearchActions(@Nonnull Project project,
                                        @Nonnull RecentLocationsDataModel data,
                                        @Nonnull ListWithFilter<RecentLocationItem> listWithFilter,
                                        @Nonnull JBList<RecentLocationItem> list,
                                        @Nonnull JBCheckBox checkBox,
                                        @Nonnull JBPopup popup,
                                        @Nonnull Ref<? super Boolean> navigationRef) {
    listWithFilter.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        int clickCount = event.getClickCount();
        if (clickCount > 1 && clickCount % 2 == 0) {
          event.consume();
          navigateToSelected(project, list, popup, navigationRef);
        }
      }
    });

    DumbAwareAction.create(e -> navigateToSelected(project, list, popup, navigationRef)).registerCustomShortcutSet(CustomShortcutSet.fromString("ENTER"), listWithFilter, popup);

    DumbAwareAction.create(e -> removePlaces(project, listWithFilter, list, data, checkBox.isSelected()))
            .registerCustomShortcutSet(CustomShortcutSet.fromString("DELETE", "BACK_SPACE"), listWithFilter, popup);
  }

  private static void removePlaces(@Nonnull Project project,
                                   @Nonnull ListWithFilter<RecentLocationItem> listWithFilter,
                                   @Nonnull JBList<RecentLocationItem> list,
                                   @Nonnull RecentLocationsDataModel data,
                                   boolean showChanged) {
    List<RecentLocationItem> selectedValue = list.getSelectedValuesList();
    if (selectedValue.isEmpty()) {
      return;
    }

    int index = list.getSelectedIndex();

    IdeDocumentHistory ideDocumentHistory = IdeDocumentHistory.getInstance(project);
    for (RecentLocationItem item : selectedValue) {
      if (showChanged) {
        ContainerUtil.filter(ideDocumentHistory.getChangePlaces(), info -> IdeDocumentHistoryImpl.isSame(info, item.getInfo())).forEach(info -> ideDocumentHistory.removeChangePlace(info));
      }
      else {
        ContainerUtil.filter(ideDocumentHistory.getBackPlaces(), info -> IdeDocumentHistoryImpl.isSame(info, item.getInfo())).forEach(info -> ideDocumentHistory.removeBackPlace(info));
      }
    }

    updateModel(listWithFilter, data, showChanged);

    if (list.getModel().getSize() > 0) ScrollingUtil.selectItem(list, index < list.getModel().getSize() ? index : index - 1);
  }

  private static void navigateToSelected(@Nonnull Project project, @Nonnull JBList<RecentLocationItem> list, @Nonnull JBPopup popup, @Nonnull Ref<? super Boolean> navigationRef) {
    ContainerUtil.reverse(list.getSelectedValuesList()).forEach(item -> IdeDocumentHistory.getInstance(project).gotoPlaceInfo(item.getInfo()));

    navigationRef.set(true);
    popup.closeOk(null);
  }
}
