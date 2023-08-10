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

import org.dcm4che3.img.lut.PresetWindowLevel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.weasis.core.api.gui.util.AbstractItemDialogPage;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.core.api.util.DicomResource;
import org.weasis.core.api.util.ResourceUtil;
import org.weasis.core.ui.pref.PreferenceDialog;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.ylyy.Enum.Keyboard;
import org.weasis.dicom.codec.display.Modality;
import org.weasis.dicom.ylyy.expand.ComboBoxCellEditor;
import org.weasis.dicom.ylyy.expand.LabelCellEditor;
import org.weasis.dicom.ylyy.Messages;
import org.weasis.opencv.op.lut.LutShape;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

/**
 * 设置-窗宽窗位 sle
 * 2023年5月16日15:43:17
 */
public class PresetPreView extends AbstractItemDialogPage {

    /**
     * 是否有改动
     */
    private Boolean change = false;

    /**
     * 检查类型的下拉combox
     */
    private final JComboBox<Modality> modalityJComboBox = new JComboBox<>(Modality.values());

    /**
     * 新增项的名称
     */
    private final String newName = Messages.getString("PresetPreView.newItem");

    /**
     * 表列名
     */
    private final String[] columnNames = {Messages.getString("PresetPreView.name"), Messages.getString("PresetPreView.window"), Messages.getString("PresetPreView.level"), Messages.getString("PresetPreView.key")};

    /**
     * 表模型
     */
    private final DefaultTableModel tableModel = new DefaultTableModel(new Object[][]{}, columnNames);

    /**
     * 表
     */
    private final JTable table = new JTable(tableModel);

    /**
     * combox的编辑拓展
     */
    private final ComboBoxCellEditor<Keyboard> comboBoxCellEditor = new ComboBoxCellEditor<>(Keyboard.values(), false, this);

    /**
     * 当前选中类型的窗宽窗位列表
     */
    private List<PresetWindowLevel> selectPreset = new ArrayList<>();

    /**
     * 窗宽窗位配置文件
     */
    private static Map<String, List<PresetWindowLevel>> presetMap = null;

    /**
     * 设置-窗宽窗位 构造函数 sle
     * 2023年5月16日15:55:42
     */
    public PresetPreView() {
        super(Messages.getString("PresetPreView.preView"), 505);
//        this.modalityJComboBox.removeItemAt(0); // 删除第一个默认的

        // 如果为空，则深拷贝map
        if (presetMap == null) {
            DeepCopyMap();
        }

        jbInit();
        tableInit();
        initialize();
    }

    /**
     * 样式初始化 设置-图像上的标签 sle
     * 2023年5月16日16:46:14
     */
    private void jbInit() {
        JLabel jModality = new JLabel(Messages.getString("PreView.modality") + StringUtil.COLON); // 检查类型
        JPanel panelModality = GuiUtils.getFlowLayoutPanel(ITEM_SEPARATOR_SMALL, ITEM_SEPARATOR_LARGE, jModality, modalityJComboBox); // 检查类型的pane
        add(panelModality);
        modalityJComboBox.addItemListener(e -> {
            StopCellEditing(true);
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectModality((Modality) e.getItem());
            }
        }); // 添加选中检查类型时的监听方法

        JButton deleteButton = new JButton(Messages.getString("PreView.delete"));
        deleteButton.addActionListener(e -> {
            boolean stop = StopCellEditing(false);
            if (!stop) {
                return;
            }
            deletePresetButton();
        }); // 删除事件
        JButton addNodeButton = new JButton(Messages.getString("PreView.add_new"));
        addNodeButton.addActionListener(e -> {
            boolean stop = StopCellEditing(false);
            if (!stop) {
                return;
            }
            addPresetButton();
        }); // 新增事件
        JButton moveUpButton = new JButton(Messages.getString("PreView.move_up"));
        moveUpButton.addActionListener(e -> {
            boolean stop = StopCellEditing(false);
            if (!stop) {
                return;
            }
            moveUpPresetButton();
        }); // 上移事件
        JButton moveDownButton = new JButton(Messages.getString("PreView.move_down"));
        moveDownButton.addActionListener(e -> {
            boolean stop = StopCellEditing(false);
            if (!stop) {
                return;
            }
            moveDownPresetButton();
        }); // 下移事件

        add(GuiUtils.getFlowLayoutPanel(ITEM_SEPARATOR_SMALL, ITEM_SEPARATOR_LARGE, addNodeButton, // 添加按钮
                GuiUtils.boxHorizontalStrut(ITEM_SEPARATOR_LARGE), deleteButton,// 删除按钮
                GuiUtils.boxHorizontalStrut(BLOCK_SEPARATOR), moveUpButton, // 上移按钮
                GuiUtils.boxHorizontalStrut(ITEM_SEPARATOR_LARGE), moveDownButton// 上移按钮
        ));

        // 将表格放置在 JScrollPane 中，以支持滚动
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(400, 380));
        add(scrollPane);

        add(GuiUtils.boxYLastElement(LAST_FILLER_HEIGHT)); // 获取剩余的高度，并添加一个对应高度的空白处

        getProperties().setProperty(PreferenceDialog.KEY_SHOW_APPLY, Boolean.TRUE.toString()); // 恢复默认值按钮
        getProperties().setProperty(PreferenceDialog.KEY_SHOW_RESTORE, Boolean.TRUE.toString()); // 应用按钮

        // 点击事件，点击任何地方都调用关闭编辑table框
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                StopCellEditing(false);
            }
        });
    }

    /**
     * 初始化table sle
     * 2023年5月22日10:54:10
     */
    private void tableInit() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 只能选中一行

        // name控制
        table.getColumnModel().getColumn(0).setCellEditor(new LabelCellEditor(String.class, null, 10, false, Messages.getString("PresetPreView.name"),this));

        // windows控制
        table.getColumnModel().getColumn(1).setCellEditor(new LabelCellEditor(Double.class, new DecimalFormat("#0.00"), 10, false, Messages.getString("PresetPreView.window"),this));

        // level控制
        table.getColumnModel().getColumn(2).setCellEditor(new LabelCellEditor(Double.class, new DecimalFormat("#0.00"), 10, false, Messages.getString("PresetPreView.level"),this));

        // keyCode控制
        table.getColumnModel().getColumn(3).setCellEditor(comboBoxCellEditor);

        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() == e.getLastRow() && e.getColumn() != TableModelEvent.ALL_COLUMNS && table.isEditing()) {
                int editedRow = e.getFirstRow();
                int editedColumn = e.getColumn();
                Object updatedValue = table.getValueAt(editedRow, editedColumn);
                editPresetButton(editedRow, editedColumn, updatedValue);
            }
        }); // 编辑事件
    }

    /**
     * 根据选中项更新内容 sle
     * 2023年5月16日16:46:20
     */
    private void initialize() {
        Object selectedItem = modalityJComboBox.getSelectedItem();
        if (selectedItem != null) {
            selectModality((Modality) selectedItem);
        }
    }

    /**
     * 选中检查类型的方法 sle
     * 2023年5月16日16:46:29
     *
     * @param modality 选中的检查类型
     */
    private void selectModality(Modality modality) {
        int newItemIndex = GetAddItemIndex();
        // 删除新增项
        if (newItemIndex != -1) {
            selectPreset.remove(newItemIndex);
            tableModel.removeRow(newItemIndex);
        }
        List<PresetWindowLevel> list = presetMap.get(modality.name());
        comboBoxCellEditor.clearComboBoxMap(); // 清空当前所有行的快捷键combox
        selectPreset = list == null ? new ArrayList<>() : list; // 引用类型赋值，这样后面要删除或者添加的时候直接在这个变量里添加，就等于直接修改了map
        tableModel.setRowCount(0); // 清空table的数据
        for (int i = 0; i < selectPreset.size(); i++) {
            PresetWindowLevel preset = selectPreset.get(i);
            Object[] rowData = new Object[]{preset.getName(), preset.getWindow(), preset.getLevel()};
            tableModel.addRow(rowData); // 将每行数据添加到 tableModel
            Keyboard selectItem = Keyboard.getByKeyCode(preset.getKeyCode());
            table.setValueAt(selectItem, i, 3); // 这样设置，才能跟每次打开快捷键的下拉框时选中到这个值的位置
            comboBoxCellEditor.addComboBox(i, selectItem);
        }
    }

    /**
     * 添加 sle
     * 2023年5月18日11:15:03
     */
    private void addPresetButton() {
        if (selectPreset.size() == 0) {
            presetMap.put(((Modality) modalityJComboBox.getSelectedItem()).name(), selectPreset);
        }
        int newItemIndex = GetAddItemIndex(); // 获取新增项的ID，如果不为-1说明有新增项，就不允许往下走
        if (newItemIndex != -1) {
            table.setRowSelectionInterval(newItemIndex, newItemIndex); // 跳转到这个新增项的所在行
            JOptionPane.showMessageDialog(null, Messages.getString("PresetPreView.add_msg"));
            return;
        }
        tableModel.addRow(new Object[]{newName, 0.0, 0.0, null});
        PresetWindowLevel addOne = new PresetWindowLevel(newName, 0.0, 0.0, LutShape.LINEAR);

        selectPreset.add(addOne);
        table.setRowSelectionInterval(selectPreset.size() - 1, selectPreset.size() - 1); // 更新选中行
        change = true;

        JComboBox<Keyboard> comboBox = new JComboBox<>(Keyboard.values()); // 当前行的combox
        comboBox.setEditable(false); // 设置不允许手动编辑
        comboBoxCellEditor.addComboBox(selectPreset.size() - 1, null);
    }

    /**
     * 单元格更新 sle
     * 2023年5月18日15:11:08
     *
     * @param rowIndex    横轴index
     * @param columnIndex 竖轴index
     * @param value       值
     */
    private void editPresetButton(int rowIndex, int columnIndex, Object value) {
        PresetWindowLevel temp = selectPreset.get(rowIndex);
        PresetWindowLevel update;
        switch (columnIndex) {
            case 0 -> { // 更新name
                update = new PresetWindowLevel((String) value, temp.getWindow(), temp.getLevel(), temp.getLutShape());
                update.setKeyCode(temp.getKeyCode());
            }
            case 1 -> { // 更新windows
                update = new PresetWindowLevel(temp.getName(), Double.parseDouble(value.toString()), temp.getLevel(), temp.getLutShape());
                update.setKeyCode(temp.getKeyCode());
            }
            case 2 -> { // 更新level
                update = new PresetWindowLevel(temp.getName(), temp.getWindow(), Double.parseDouble(value.toString()), temp.getLutShape());
                update.setKeyCode(temp.getKeyCode());
            }
            case 3 -> { // 更新 keyCode
                update = new PresetWindowLevel(temp.getName(), temp.getWindow(), temp.getLevel(), temp.getLutShape());
                if (value != null) {
                    update.setKeyCode(((Keyboard) value).getKeyCode());
                }
            }
            default -> update = temp;
        }
        selectPreset.set(rowIndex, update);
        change = true;
    }

    /**
     * 删除 sle
     * 2023年5月18日10:06:52
     */
    private void deletePresetButton() {
        int index = table.getSelectedRow();
        // 小于0说明是空值
        if (index < 0) {
            return;
        }
        int response = 0;
        if (!selectPreset.get(index).getName().equals(newName)) {
            response = JOptionPane.showConfirmDialog(table,// 弹框的父节点
                    String.format(Messages.getString("PreView.delete_msg"), selectPreset.get(index).getName()), // 提示语句
                    Messages.getString("PreView.delete") + Messages.getString("PresetPreView.preView"), // 弹框名称
                    JOptionPane.YES_NO_OPTION); // 是和否
        }
        if (response == 0) {
            selectPreset.remove(index);
            tableModel.removeRow(index);
            change = true;
            comboBoxCellEditor.removeCombox(index);
        }
    }

    /**
     * 上移 sle
     * 2023年5月18日14:25:32
     */
    private void moveUpPresetButton() {
        int index = table.getSelectedRow();
        // 小于0说明是空值，等于0说明到最上面了
        if (index <= 0) {
            return;
        }
        // table移动部分
        tableModel.moveRow(index, index, index - 1); // 上移
        table.setRowSelectionInterval(index - 1, index - 1); // 更新选中行
        // selectPreset移动部分
        PresetWindowLevel temp = selectPreset.get(index);
        selectPreset.set(index, selectPreset.get(index - 1));
        selectPreset.set(index - 1, temp);
        change = true;
        comboBoxCellEditor.moveCombox(index, index - 1);
    }

    /**
     * 下移 sle
     * 2023年5月18日14:25:32
     */
    private void moveDownPresetButton() {
        int index = table.getSelectedRow();
        // 小于0说明是空值，且如果等于长度的最大值-1，说明就已经到最下面了
        if (index < 0 || index == selectPreset.size() - 1) {
            return;
        }
        // table移动部分
        tableModel.moveRow(index, index, index + 1); // 下移
        table.setRowSelectionInterval(index + 1, index + 1); // 更新选中行
        // selectPreset移动部分
        PresetWindowLevel temp = selectPreset.get(index);
        selectPreset.set(index, selectPreset.get(index + 1));
        selectPreset.set(index + 1, temp);
        change = true;
        comboBoxCellEditor.moveCombox(index, index + 1);
    }

    /**
     * 深拷贝Map sle
     * 2023年5月18日10:42:55
     */
    private void DeepCopyMap() {
        Map<String, List<PresetWindowLevel>> originalMap = DicomImageElement.presetListByModality;
        Map<String, List<PresetWindowLevel>> copiedMap = new LinkedHashMap<>();

        for (Map.Entry<String, List<PresetWindowLevel>> entry : originalMap.entrySet()) {
            String key = entry.getKey();
            List<PresetWindowLevel> originalList = entry.getValue();
            List<PresetWindowLevel> copiedList = new ArrayList<>(originalList); // 进行深拷贝
            copiedMap.put(key, copiedList);
        }

        presetMap = copiedMap;
    }

    /**
     * 获取新增项的索引 sle
     * 2023年5月19日11:45:47
     *
     * @return 如果不存在则返回-1
     */
    private int GetAddItemIndex() {
        for (int i = 0; i < selectPreset.size(); i++) {
            String name = selectPreset.get(i).getName();
            if (name.equals(newName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 关闭编辑 sle
     * 2023年5月19日16:44:18
     *
     * @param cancel 是否取消编辑
     * @return 是否成功关闭
     */
    private boolean StopCellEditing(boolean cancel) {
        if (table.isEditing()) {
            boolean result = table.getCellEditor().stopCellEditing();
            if (!result && cancel) {
                table.getCellEditor().cancelCellEditing();
            }
            return result;
        }
        return true;
    }

    /**
     * 关闭设置弹框/应用 sle
     * 写入xml文件
     */
    @Override
    public void closeAdditionalWindow() {
        boolean stop = StopCellEditing(false);
        if (!stop) {
            return;
        }
        // 如果没有改变，就不走后续逻辑，因为涉及到去读写xml文件，能快一点是一点
        if (!change) {
            return;
        }
        int newItemIndex = GetAddItemIndex();
        // 删除新增项
        if (newItemIndex != -1) {
            selectPreset.remove(newItemIndex);
            tableModel.removeRow(newItemIndex);
        }
        try {
            File file = ResourceUtil.getResource(DicomResource.PRESETS);
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // 读取xml文件
            Document doc = docBuilder.parse(file);
            Element mainElement = doc.getDocumentElement();

            // 删除原来的节点
            NodeList layoutList = doc.getElementsByTagName("preset");
            for (int i = layoutList.getLength() - 1; i >= 0; i--) {
                Element layout = (Element) layoutList.item(i);
                mainElement.removeChild(layout);
            }

            // 添加节点
            for (Map.Entry<String, List<PresetWindowLevel>> modality : presetMap.entrySet()) {
                String modalityID = modality.getKey();
                for (PresetWindowLevel preset : modality.getValue()) {
                    Element layout = doc.createElement("preset");
                    layout.setAttribute("modality", modalityID);
                    layout.setAttribute("name", preset.getName());
                    layout.setAttribute("window", String.valueOf(preset.getWindow()));
                    layout.setAttribute("level", String.valueOf(preset.getLevel()));
                    layout.setAttribute("key", String.valueOf(preset.getKeyCode()));
                    mainElement.appendChild(layout);
                }
            }
            saveDocumentXml(doc, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 恢复默认值 sle
     * 2023年5月16日16:46:36
     */
    @Override
    public void resetToDefaultValues() {
        if (table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
        }
        DeepCopyMap();
        initialize();
    }
}
