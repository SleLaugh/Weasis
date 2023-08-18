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

import java.awt.GridLayout;
import java.util.List;
import javax.swing.JPanel;
import org.weasis.core.Messages;
import org.weasis.core.api.gui.util.AbstractItemDialogPage;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.core.api.gui.util.PageItem;

/**
 * 设置-查看器 sle 添加注释
 * 2023年5月12日15:58:05
 */
public class ViewerPrefView extends AbstractItemDialogPage {

  /**
   * 设置-查看器 页签
   */
  private final JPanel menuPanel = new JPanel();

  /**
   * 设置-查看器 构造函数 sle 添加注释
   * 2023年5月12日15:57:52
   * @param dialog
   */
  public ViewerPrefView(PreferenceDialog dialog) {
    super(Messages.getString("viewer"), 500);

    menuPanel.setLayout(new GridLayout(0, 2)); // 自动增加行数、每行包含两列的网格布局
    add(menuPanel);
    add(GuiUtils.boxVerticalStrut(BLOCK_SEPARATOR)); // 添加一个垂直的空白间隔

    add(GuiUtils.boxYLastElement(LAST_FILLER_HEIGHT));// 添加一个垂直的空白间隔，以填充布局的最后一行

    List<AbstractItemDialogPage> childPages = List.of(new LabelsPrefView());
    childPages.forEach(p -> addSubPage(p, a -> dialog.showPage(p.getTitle()), menuPanel)); // 添加为当前节点的页签，同时定义一个动作（action）来显示该页签
  }

  @Override
  public JPanel getMenuPanel() {
    return menuPanel;
  }

  @Override
  public void closeAdditionalWindow() {
    for (PageItem subpage : getSubPages()) {
      subpage.closeAdditionalWindow();
    }
  }

  @Override
  public void resetToDefaultValues() {
    // Do nothing
  }
}
