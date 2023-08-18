/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.explorer.pref.node;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import org.weasis.core.api.gui.util.AbstractItemDialogPage;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.dicom.explorer.Messages;

/**
 * 设置-DICOM节点列表 sle 添加注释
 * 2023年5月15日14:22:22
 */
public class DicomNodeListView extends AbstractItemDialogPage {

  /**
   * 设置-DICOM节点列表 构造函数 sle 添加注释
   * 2023年5月15日14:22:22
   */
  public DicomNodeListView() {
    super(Messages.getString("DicomNodeListView.node_list"), 605);
    initGUI();
  }

  private void initGUI() {
    buildPanel(AbstractDicomNode.Type.DICOM_CALLING);
    buildPanel(AbstractDicomNode.Type.DICOM);
    buildPanel(AbstractDicomNode.Type.WEB);
    // buildPanel(AbstractDicomNode.Type.WEB_QIDO);

    add(GuiUtils.boxYLastElement(LAST_FILLER_HEIGHT));
  }

  private void buildPanel(AbstractDicomNode.Type nodeType) {
    final JComboBox<AbstractDicomNode> nodeComboBox = new JComboBox<>();
    AbstractDicomNode.loadDicomNodes(nodeComboBox, nodeType);
    AbstractDicomNode.addTooltipToComboList(nodeComboBox);
    GuiUtils.setPreferredWidth(nodeComboBox, 200, 150); // 宽由270改为200 sle 2023年8月11日10:29:59
    JButton editButton = new JButton(Messages.getString("DicomNodeListView.edit"));
    JButton deleteButton = new JButton(Messages.getString("DicomNodeListView.delete"));
    JButton addNodeButton = new JButton(Messages.getString("DicomNodeListView.add_new"));
    deleteButton.addActionListener(e -> AbstractDicomNode.deleteNodeActionPerformed(nodeComboBox));
    editButton.addActionListener(e -> AbstractDicomNode.editNodeActionPerformed(nodeComboBox));
    addNodeButton.addActionListener(
        e -> AbstractDicomNode.addNodeActionPerformed(nodeComboBox, nodeType));

    JPanel panel = GuiUtils.getVerticalBoxLayoutPanel();
    panel.setBorder(GuiUtils.getTitledBorder(nodeType.toString()));
    panel.add(
        GuiUtils.getFlowLayoutPanel(
            ITEM_SEPARATOR,
            ITEM_SEPARATOR,
            nodeComboBox,
            GuiUtils.boxHorizontalStrut(BLOCK_SEPARATOR),
            editButton,
            GuiUtils.boxHorizontalStrut(ITEM_SEPARATOR_LARGE),
            deleteButton));
    panel.add(GuiUtils.getFlowLayoutPanel(ITEM_SEPARATOR, ITEM_SEPARATOR, addNodeButton));
    add(panel);

    add(GuiUtils.boxVerticalStrut(BLOCK_SEPARATOR));
  }

  @Override
  public void closeAdditionalWindow() {
    // Do nothing
  }

  @Override
  public void resetToDefaultValues() {
    // Do nothing
  }
}
