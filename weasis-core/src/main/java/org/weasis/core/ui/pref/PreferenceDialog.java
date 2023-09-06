/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.pref;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.Messages;
import org.weasis.core.api.gui.InsertableUtil;
import org.weasis.core.api.gui.PreferencesPageFactory;
import org.weasis.core.api.gui.util.AbstractItemDialogPage;
import org.weasis.core.api.gui.util.AbstractWizardDialog;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.core.util.StringUtil;

/**
 * 设置 sle 添加注释
 * 2023年5月12日16:10:02
 */
public class PreferenceDialog extends AbstractWizardDialog {
  private static final Logger LOGGER = LoggerFactory.getLogger(PreferenceDialog.class);

  public static final String KEY_SHOW_APPLY = "show.apply";
  public static final String KEY_SHOW_RESTORE = "show.restore";
  public static final String KEY_HELP = "help.item";

  protected final JButton jButtonHelp = new JButton();
  protected final JButton restoreButton = new JButton(Messages.getString("restore.values"));
  protected final JButton applyButton = new JButton(Messages.getString("LabelPrefView.apply"));
  protected final JPanel bottomPrefPanel =
      GuiUtils.getFlowLayoutPanel(
          FlowLayout.TRAILING, 10, 7, jButtonHelp, restoreButton, applyButton);

  /**
   * 设置 构造函数 sle 添加注释
   * 2023年5月12日16:18:43
   * @param parentWin
   */
  public PreferenceDialog(Window parentWin) {
    super(
        parentWin,
        Messages.getString("OpenPreferencesAction.title"),
        ModalityType.APPLICATION_MODAL,
        new Dimension(600, 500)); // 高度由450改为500 sle 2023年8月11日10:44:11

    jPanelBottom.add(bottomPrefPanel, 0);

    jButtonHelp.putClientProperty("JButton.buttonType", "help");
    applyButton.addActionListener(
        e -> {
          if (currentPage != null) currentPage.closeAdditionalWindow();
        });
    restoreButton.addActionListener(
        e -> {
          if (currentPage != null) {
            currentPage.resetToDefaultValues();
          }
        });

    initializePages();
    pack();
    showFirstPage();
    this.setResizable(false); // 设置边框不允许自定义拖拽大小 sle
  }

  @Override
  protected void initializePages() {
    Hashtable<String, Object> properties = new Hashtable<>();
    properties.put("weasis.user.prefs", System.getProperty("weasis.user.prefs", "user")); // NON-NLS

    ArrayList<AbstractItemDialogPage> list = new ArrayList<>();
    GeneralSetting generalSetting = new GeneralSetting(this); // 一般
    list.add(generalSetting);
    ViewerPrefView viewerSetting = new ViewerPrefView(this); // 查看器
    list.add(viewerSetting);
    DicomPrefView dicomPrefView = new DicomPrefView(this); // DICOM
    list.add(dicomPrefView);

    BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext(); // BundleContext 是用于与 OSGi 容器进行交互的接口
    try {
      // 获取所有实现了 PreferencesPageFactory 接口的服务引用并循环
      for (ServiceReference<PreferencesPageFactory> service :
          context.getServiceReferences(PreferencesPageFactory.class, null)) {
        PreferencesPageFactory factory = context.getService(service); // 获取服务实例
        if (factory != null) {
          AbstractItemDialogPage page = factory.createInstance(properties); // 创建三级设置菜单
          if (page != null) {
            // 根据position将三级菜单添加到对应的二级菜单下
            int position = page.getComponentPosition();
            if (position < 1000) {
              AbstractItemDialogPage mainPage;
              if (position > 500 && position < 600) {
                mainPage = viewerSetting;
              } else if (position > 600 && position < 700) {
                mainPage = dicomPrefView;
              } else {
                mainPage = generalSetting;
              }
              JComponent menuPanel = mainPage.getMenuPanel();
              mainPage.addSubPage(page, a -> showPage(page.getTitle()), menuPanel);
              if (menuPanel != null) {
                menuPanel.revalidate(); // 重新布局
                menuPanel.repaint(); // 重新绘制菜单面板
              }
            } else {
              list.add(page);
            }
          }
        }
      }
    } catch (InvalidSyntaxException e) {
      LOGGER.error("Get Preference pages from service", e);
    }

    InsertableUtil.sortInsertable(list);
    for (AbstractItemDialogPage page : list) {
      page.sortSubPages();
      pagesRoot.add(new DefaultMutableTreeNode(page));
    }
    iniTree();
  }

  @Override
  protected void selectPage(AbstractItemDialogPage page) {
    if (page != null) {
      super.selectPage(page);
      applyButton.setVisible(Boolean.TRUE.toString().equals(page.getProperty(KEY_SHOW_APPLY)));
      restoreButton.setVisible(Boolean.TRUE.toString().equals(page.getProperty(KEY_SHOW_RESTORE)));

      String helpKey = page.getProperty(KEY_HELP);
      for (ActionListener al : jButtonHelp.getActionListeners()) {
        jButtonHelp.removeActionListener(al);
      }
      jButtonHelp.setVisible(StringUtil.hasText(helpKey));
      if (jButtonHelp.isVisible()) {
        jButtonHelp.addActionListener(GuiUtils.createHelpActionListener(jButtonHelp, helpKey));
      }
    }
  }

  @Override
  public void cancel() {
    dispose();
  }

  @Override
  public void dispose() {
    closeAllPages();
    super.dispose();
  }
}
