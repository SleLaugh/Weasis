/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.ylyy.pref;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.weasis.core.api.gui.util.AbstractItemDialogPage;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.core.api.image.GridBagLayoutModel;
import org.weasis.core.api.util.ResourceUtil;
import org.weasis.core.api.util.DicomResource;
import org.weasis.dicom.ylyy.Messages;
import org.weasis.core.ui.editor.image.ImageViewerPlugin;
import org.weasis.core.ui.pref.PreferenceDialog;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.codec.display.Modality;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.List;
import java.util.*;

/**
 * 设置-挂片协议 sle
 * 2023年5月12日14:52:58
 */
public class LayoutPreView extends AbstractItemDialogPage {

    /**
     * 是否有改动
     */
    private Boolean change = false;

    /**
     * 检查类型的下拉combox
     */
    private final JComboBox<Modality> modalityJComboBox = new JComboBox<>(Modality.values());

    /**
     * 布局的下拉combox
     */
    private final JComboBox<GridBagLayoutModel> layoutJComboBox = new JComboBox<>((new ArrayList<>(default_layout_list)).toArray(GridBagLayoutModel[]::new));

    /**
     * 布局列表
     */
    private static List<GridBagLayoutModel> default_layout_list = ImageViewerPlugin.DEFAULT_LAYOUT_LIST;

    /**
     * 检查类型对应布局的配置文件
     */
    private static Map<String, String> modalityLayoutMap = null;

    /**
     * 设置-挂片协议 构造函数 sle
     * 2023年5月12日15:54:51
     */
    public LayoutPreView() {
        super(Messages.getString("LayoutPreView.preView"), 504);
//        this.modalityJComboBox.removeItemAt(0); // 删除第一个默认的

        this.modalityLayoutMap = this.modalityLayoutMap == null ? new LinkedHashMap<>(ImageViewerPlugin.getModalityLayoutList()) : this.modalityLayoutMap; // 获取文件中的布局列表
        jbInit();
        initialize();
    }

    /**
     * 样式初始化 设置-图像上的标签 sle
     * 2023年5月12日14:50:35
     */
    private void jbInit() {
        JLabel jModality = new JLabel(Messages.getString("PreView.modality") + StringUtil.COLON); // 检查类型
        JPanel panelModality = GuiUtils.getFlowLayoutPanel(ITEM_SEPARATOR_SMALL, ITEM_SEPARATOR_LARGE, jModality, modalityJComboBox); // 检查类型的pane
        add(panelModality);
        modalityJComboBox.addItemListener(e -> { // 选中检查类型时的方法
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectModality((Modality) e.getItem());
            }
        }); // 添加监听方法

        JLabel jLayout = new JLabel(Messages.getString("LayoutPreView.layout") + StringUtil.COLON); // 布局
        jLayout.setPreferredSize(new Dimension(jModality.getPreferredSize().width, jModality.getPreferredSize().height)); // 设置长度和检查类型相等
        JPanel panelLayout = GuiUtils.getFlowLayoutPanel(ITEM_SEPARATOR_SMALL, ITEM_SEPARATOR_LARGE, jLayout, layoutJComboBox); // 布局的pane
        add(panelLayout);
        layoutJComboBox.addItemListener(e -> { // 选中布局时的方法
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectLayout((GridBagLayoutModel) e.getItem());
            }
        }); // 添加监听方法

        add(GuiUtils.boxYLastElement(LAST_FILLER_HEIGHT)); // 获取剩余的高度，并添加一个对应高度的空白处

        getProperties().setProperty(PreferenceDialog.KEY_SHOW_APPLY, Boolean.TRUE.toString()); // 恢复默认值按钮
        getProperties().setProperty(PreferenceDialog.KEY_SHOW_RESTORE, Boolean.TRUE.toString()); // 应用按钮
    }

    /**
     * 根据选中项更新内容 sle
     * 2023年5月12日14:52:58
     */
    private void initialize() {
        Object selectedItem = modalityJComboBox.getSelectedItem();
        if (selectedItem != null) {
            selectModality((Modality) selectedItem);
        }
    }

    /**
     * 选中检查类型的方法 sle
     * 2023年5月15日16:17:46
     *
     * @param modality 选中的检查类型
     */
    private void selectModality(Modality modality) {
        String modalitID = modality.name();
        String modelID = modalityLayoutMap.get(modalitID);
        if (modelID == null || modelID.isEmpty()) {
            layoutJComboBox.setSelectedIndex(-1);
        }
        GridBagLayoutModel layout = getViewLayout(modelID);
        if (layout != null) {
            layoutJComboBox.setSelectedItem(layout);
        } else {
            layoutJComboBox.setSelectedIndex(-1);
        }
    }

    /**
     * 选中布局的方法 sle
     * 2023年5月15日16:20:06
     *
     * @param layout 选中的布局
     */
    private void selectLayout(GridBagLayoutModel layout) {
        if (layout != null) {
            String modelID = layout.getId(); // 本次选中的布局ID

            Modality modalitySelectedItem = (Modality) modalityJComboBox.getSelectedItem();
            String modalityID = modalitySelectedItem.name(); // 检查类型ID
            String mapModelID = modalityLayoutMap.get(modalityID); // map中当前检查类型ID对应的布局ID
            // 如果本次选中的和map中的不一样，则更新，并且更新change变量为true
            if (!modelID.equals(mapModelID)) {
                modalityLayoutMap.put(modalityID, modelID);
                change = true;
            }
        }
    }

    /**
     * 根据指定id获取指定布局 sle
     * 2023年5月15日17:33:13
     *
     * @param id
     * @return
     */
    public GridBagLayoutModel getViewLayout(String id) {
        if (id != null) {
            for (GridBagLayoutModel m : default_layout_list) {
                if (id.equals(m.getId())) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * 关闭设置弹框/应用 sle
     * 写入xml文件
     * 2023年5月12日14:52:58
     */
    @Override
    public void closeAdditionalWindow() {
        // 如果没有改变，就不走后续逻辑，因为涉及到去读写xml文件，能快一点是一点
        if (!change) {
            return;
        }
        try {
            File file = ResourceUtil.getResource(DicomResource.MODALITY_LAYOUT);
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // 读取xml文件
            Document doc = docBuilder.parse(file);
            Element mainElement = doc.getDocumentElement();

            // 删除原来的节点
            NodeList layoutList = doc.getElementsByTagName("layout");
            for (int i = layoutList.getLength() - 1; i >= 0; i--) {
                Element layout = (Element) layoutList.item(i);
                mainElement.removeChild(layout);
            }

            // 添加节点
            for (Map.Entry<String, String> entry : modalityLayoutMap.entrySet()) {
                String modality = entry.getKey();
                String id = entry.getValue();
                Element layout = doc.createElement("layout");
                layout.setAttribute("modality", modality);
                layout.setAttribute("id", id);

                mainElement.appendChild(layout);
            }

            saveDocumentXml(doc, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 恢复默认值 sle
     * 2023年5月12日14:52:58
     */
    @Override
    public void resetToDefaultValues() {
        this.modalityLayoutMap = new LinkedHashMap<>(ImageViewerPlugin.getModalityLayoutList());
        initialize();
    }
}
