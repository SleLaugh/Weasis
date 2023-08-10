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
import org.weasis.core.api.media.data.TagView;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.util.DicomResource;
import org.weasis.core.api.util.ResourceUtil;
import org.weasis.core.ui.pref.PreferenceDialog;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.codec.display.CornerDisplay;
import org.weasis.dicom.codec.display.Modality;
import org.weasis.dicom.ylyy.expand.TagViewCellRenderer;
import org.weasis.dicom.ylyy.Messages;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.function.Function;

import static org.weasis.dicom.codec.display.ModalityView.ATTRIBUTE_VIEW_MAP;

/**
 * 设置-四角信息 sle
 * 2023年5月23日19:18:38
 */
public class AttributePreView extends AbstractItemDialogPage {

    /**
     * 是否有改动
     */
    private Boolean change = false;

    /**
     * 检查类型的下拉combox
     */
    private final JComboBox<Modality> modalityJComboBox = new JComboBox<>(Modality.values());

    /**
     * modelMap
     */
    private final Map<CornerDisplay, DefaultTableModel> modelMap = new LinkedHashMap<>();

    /**
     * 表Map
     */
    private final Map<CornerDisplay, JTable> tableMap = new LinkedHashMap<>();

    /**
     * 当前选中检查类型的四角信息
     */
    private Map<CornerDisplay, List<TagView>> selectTagViewListMap;

    /**
     * 当前选中方位
     */
    private CornerDisplay selectCornerDisplay;

    /**
     * 当前选中model
     */
    private DefaultTableModel selectModel;

    /**
     * 当前选中表
     */
    private JTable selectTable;

    /**
     * 当前选中的list
     */
    private List<TagView> selectTagViewList;

    /**
     * 四角信息配置文件
     */
    private static Map<Modality, Map<CornerDisplay, List<TagView>>> ModalityViewMap;

    /**
     * 字符串拼接方法
     */
    private final Function<TagView, String> formatFunction = tagView -> {
        TagW[] tagArray = tagView.getTag();
        if (tagArray.length > 0) {
            TagW firstTag = tagArray[0];
            // 调用 getFormattedTagValue 方法获取需要显示的内容
            return firstTag.getFormattedTagValue(firstTag, tagView.getFormat());
        }
        return "";
    };

    /**
     * 判断是否是系统默认tag值
     */
    private final Function<TagView, Boolean> isSystemDefaultFunction = tagView -> isSystemTagView(tagView);

    /**
     * 方位枚举
     */
    private enum Direction {
        UP, DOWN, LEFT, RIGHT, TRANSLATION;
    }

    /**
     * 系统默认Tag值
     */
    private final List<TagView> SystemDefaultTagViewList = getSystemDefaultTagViewList();

    /**
     * 系统默认Tag值所在的方位
     */
    private final CornerDisplay SystemDefaultCornerDisplay = CornerDisplay.BOTTOM_LEFT;


    /**
     * 设置-四角信息 构造函数 sle
     * 2023年5月23日19:21:00
     */
    public AttributePreView() {
        super(Messages.getString("AttributePreView.preView"), 506);
//        this.modalityJComboBox.removeItemAt(0); // 删除第一个默认的

        // 如果为空，则深拷贝map
        if (ModalityViewMap == null) {
            DeepCopyMap();
        }

        jbInit();
        initialize();
    }

    /**
     * 样式初始化 设置-图像上的标签 sle
     * 2023年5月23日19:20:56
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

        JButton deleteButton = new JButton(Messages.getString("PreView.delete"));
        JButton addNodeButton = new JButton(Messages.getString("PreView.add_new"));
        JButton moveUpButton = new JButton(Messages.getString("PreView.move_up"));
        JButton moveDownButton = new JButton(Messages.getString("PreView.move_down"));
        JButton moveTranslationButton = new JButton(Messages.getString("PreView.move_translation"));
        deleteButton.addActionListener(e -> {
            deleteAttributeButton();
        }); // 删除事件
        addNodeButton.addActionListener(e -> {
            addAttributeButton();
        }); // 新增事件
        moveUpButton.addActionListener(e -> {
            moveUpAttributeButton();
        }); // 上移事件
        moveDownButton.addActionListener(e -> {
            moveDownAttributeButton();
        }); // 下移事件
        moveTranslationButton.addActionListener(e -> {
            moveAttribute(Direction.TRANSLATION);
        }); // 平移事件

        add(GuiUtils.getFlowLayoutPanel(ITEM_SEPARATOR_SMALL, ITEM_SEPARATOR_LARGE, addNodeButton, // 添加按钮
                GuiUtils.boxHorizontalStrut(ITEM_SEPARATOR_LARGE), deleteButton,// 删除按钮
                GuiUtils.boxHorizontalStrut(BLOCK_SEPARATOR), moveUpButton, // 上移按钮
                GuiUtils.boxHorizontalStrut(ITEM_SEPARATOR_LARGE), moveDownButton,// 上移按钮
                GuiUtils.boxHorizontalStrut(ITEM_SEPARATOR_LARGE), moveTranslationButton// 平移按钮
        ));

        tableInit();

        add(GuiUtils.boxYLastElement(LAST_FILLER_HEIGHT)); // 获取剩余的高度，并添加一个对应高度的空白处

        getProperties().setProperty(PreferenceDialog.KEY_SHOW_APPLY, Boolean.TRUE.toString()); // 恢复默认值按钮
        getProperties().setProperty(PreferenceDialog.KEY_SHOW_RESTORE, Boolean.TRUE.toString()); // 应用按钮
    }

    /**
     * 表初始化 sle
     * 2023年5月25日15:09:01
     */
    private void tableInit() {
        for (CornerDisplay cornerDisplay : CornerDisplay.values()) {
            String[] columnNames = {cornerDisplay.toString()};
            DefaultTableModel model = new DefaultTableModel(new Object[][]{}, columnNames);
            modelMap.put(cornerDisplay, model);

            // 创建对应的 JTable 对象
            JTable table = new JTable(model);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 只能选中一行

            JTableHeader emptyHeader = new JTableHeader(table.getColumnModel()) {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(0, 0);
                }
            }; // 自定义表头，当前表头为不显示的表头
            table.setTableHeader(emptyHeader);
            table.setDefaultEditor(Object.class, null); // 关闭编辑
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectTable = table;
                    selectModel = model;
                    selectCornerDisplay = cornerDisplay;
                    List<TagView> temp = selectTagViewListMap.get(cornerDisplay);
                    selectTagViewList = temp == null ? new ArrayList<>() : temp;

                    // 清空其他table选中
                    for (Map.Entry<CornerDisplay, JTable> entry : tableMap.entrySet()) {
                        if (selectTable != entry.getValue()) {
                            entry.getValue().clearSelection();
                        }
                    }
                    // 双击进入编辑事件
                    if (e.getClickCount() == 2) {
                        editAttributeButton();
                    }
                }
            }); // 添加点击事件

            int alignment = SwingConstants.LEFT; // 对齐方式，默认靠左
            // 右侧的两个table文本靠右显示
            if (cornerDisplay == CornerDisplay.TOP_RIGHT || cornerDisplay == CornerDisplay.BOTTOM_RIGHT) {
                alignment = SwingConstants.RIGHT;
            }

            table.getColumnModel().getColumn(0).setCellRenderer(new TagViewCellRenderer<>(formatFunction, isSystemDefaultFunction, alignment));

            // 将 JTable 对象添加到 Map 中
            tableMap.put(cornerDisplay, table);

        }
        Dimension dimension = new Dimension(280, 190);
        GridLayout gridLayout = new GridLayout(1, 2);
        // 创建一个主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        // 创建上方面板，包含左上和右上的表格
        JPanel topPanel = new JPanel();
        topPanel.setLayout(gridLayout);

        JScrollPane topLeftScrollPane = new JScrollPane(tableMap.get(CornerDisplay.TOP_LEFT));
        topLeftScrollPane.setPreferredSize(dimension);
        topPanel.add(topLeftScrollPane);

        JScrollPane topRightScrollPane = new JScrollPane(tableMap.get(CornerDisplay.TOP_RIGHT));
        topRightScrollPane.setPreferredSize(dimension);
        topPanel.add(topRightScrollPane);

        // 创建下方面板，包含左下和右下的表格
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(gridLayout);

        JScrollPane bottomLeftScrollPane = new JScrollPane(tableMap.get(CornerDisplay.BOTTOM_LEFT));
        bottomLeftScrollPane.setPreferredSize(dimension);
        bottomPanel.add(bottomLeftScrollPane);

        JScrollPane bottomRightScrollPane = new JScrollPane(tableMap.get(CornerDisplay.BOTTOM_RIGHT));
        bottomRightScrollPane.setPreferredSize(dimension);
        bottomPanel.add(bottomRightScrollPane);

        // 将上方面板和下方面板添加到主面板
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // 添加主面板到容器中
        add(mainPanel);
    }

    /**
     * 根据选中项更新内容 sle
     */
    private void initialize() {
        Object selectedItem = modalityJComboBox.getSelectedItem();
        if (selectedItem != null) {
            selectModality((Modality) selectedItem);
        }
    }

    /**
     * 选中检查类型的方法 sle
     *
     * @param modality 选中的检查类型
     */
    private void selectModality(Modality modality) {
        Map<CornerDisplay, List<TagView>> selectItem = ModalityViewMap.get(modality); // 根据当前选中的类型获取对应的四角信息
        selectTagViewListMap = selectItem == null ? new LinkedHashMap<>() : selectItem;

        // 循环给model添加数据
        for (Map.Entry<CornerDisplay, DefaultTableModel> entry : modelMap.entrySet()) {
            CornerDisplay cornerDisplay = entry.getKey();
            DefaultTableModel model = entry.getValue();
            model.setRowCount(0); // 清空行
            List<TagView> tagViews = selectTagViewListMap.get(cornerDisplay); // 获取当前方位的tag值
            if (tagViews != null) {
                for (TagView tag : tagViews) {
                    if (tag != null) {
                        Object[] rowData = new Object[]{tag};
                        model.addRow(rowData);
                    }
                }
            }
            // 如果是左下角，那么添加系统默认的Tag值
            if (cornerDisplay == SystemDefaultCornerDisplay) {
                for (TagView tag : SystemDefaultTagViewList) {
                    Object[] rowData = new Object[]{tag};
                    model.addRow(rowData);
                }
            }
        }
    }

    /**
     * 新增方法 sle
     * 2023年5月25日17:10:54
     */
    private void addAttributeButton() {
        if (selectTagViewListMap.size() == 0) {
            for (CornerDisplay cornerDisplay : CornerDisplay.values()) {
                selectTagViewListMap.put(cornerDisplay, new ArrayList<>());
            }
            ModalityViewMap.put((Modality) modalityJComboBox.getSelectedItem(), selectTagViewListMap);
        }
        JDialog dialog = new AttributeDialog(SwingUtilities.getWindowAncestor(this), Messages.getString("PreView.add_new"), selectCornerDisplay, null, (Modality) modalityJComboBox.getSelectedItem(), tableMap, selectTagViewListMap, -1, SystemDefaultTagViewList.size());
        GuiUtils.showCenterScreen(dialog, this);
        change = true;
    }

    /**
     * 编辑方法 sle
     * 2023年5月25日17:11:04
     */
    private void editAttributeButton() {
        // 如果操作项是系统默认的，则不操作，并返回
        TagView selectTagView = getSelectTagView();
        if (isSystemTagView(selectTagView)) {
            return;
        }
        JDialog dialog = new AttributeDialog(SwingUtilities.getWindowAncestor(this), Messages.getString("PreView.edit"), selectCornerDisplay, selectTagView, (Modality) modalityJComboBox.getSelectedItem(), tableMap, selectTagViewListMap, selectTable.getSelectedRow(), SystemDefaultTagViewList.size());
        GuiUtils.showCenterScreen(dialog, this);
        change = true;
    }

    /**
     * 获取当前选中项 sle
     * 2023年6月1日17:28:27
     *
     * @return 当前选中项
     */
    private TagView getSelectTagView() {
        int row = selectTable.getSelectedRow(); // 获取选中行的索引
        int column = selectTable.getSelectedColumn(); // 获取选中列的索引
        return (TagView) selectTable.getValueAt(row, column);
    }

    /**
     * 删除方法 sle
     * 2023年5月25日17:11:08
     */
    private void deleteAttributeButton() {
        if (selectTable == null) {
            return;
        }
        int index = selectTable.getSelectedRow();
        // 小于0说明是空值
        if (index < 0) {
            return;
        }
        // 如果操作项是系统默认的，则不操作，并返回
        if (isSystemTagView(getSelectTagView())) {
            return;
        }
        int response = 0;
        response = JOptionPane.showConfirmDialog(this,// 弹框的父节点
                String.format(Messages.getString("PreView.delete_msg"), formatFunction.apply(selectTagViewList.get(index))), // 提示语句
                selectCornerDisplay.toString(), // 弹框名称
                JOptionPane.YES_NO_OPTION); // 是和否
        if (response == 0) {
            selectTagViewList.remove(index);
            selectModel.removeRow(index);
            change = true;
        }
    }

    /**
     * 上移方法 sle
     * 2023年5月25日17:11:12
     */
    private void moveUpAttributeButton() {
        if (selectTable == null) {
            return;
        }
        int index = selectTable.getSelectedRow();
        // 小于0说明是空值
        if (index < 0) {
            return;
        }
        // 如果操作项是系统默认的，则不操作，并返回
        if (isSystemTagView(getSelectTagView())) {
            return;
        }
        // 等于0说明到最上面了，那么就要移动到上面的表格
        if (index == 0) {
            moveAttribute(Direction.UP);
            return;
        }

        // 表格内移动
        // table移动部分
        selectModel.moveRow(index, index, index - 1); // 上移
        selectTable.setRowSelectionInterval(index - 1, index - 1); // 更新选中行

        TagView temp = selectTagViewList.get(index);
        selectTagViewList.set(index, selectTagViewList.get(index - 1));
        selectTagViewList.set(index - 1, temp);
        change = true;
    }

    /**
     * 下移方法 sle
     * 2023年5月25日17:11:16
     */
    private void moveDownAttributeButton() {
        if (selectTable == null) {
            return;
        }
        int index = selectTable.getSelectedRow();
        // 小于0说明是空值
        if (index < 0) {
            return;
        }
        // 如果操作项是系统默认的，则不操作，并返回
        if (isSystemTagView(getSelectTagView())) {
            return;
        }
        // 如果当前方位是系统默认tag值方位，并且当前选中行的索引已经等于了整个model的长度-系统默认tag值的长度，说明再往下就要到系统默认行了，那么就返回
        if (selectCornerDisplay == SystemDefaultCornerDisplay && index == selectModel.getRowCount() - SystemDefaultTagViewList.size()) {
            return;
        }
        // 如果等于长度的最大值-1，说明就已经到最下面了,那么就要移动到下面的表格里去
        if (index == selectTagViewList.size() - 1) {
            moveAttribute(Direction.DOWN);
            return;
        }

        // 表格内移动
        // table移动部分
        selectModel.moveRow(index, index, index + 1); // 下移
        selectTable.setRowSelectionInterval(index + 1, index + 1); // 更新选中行
        // selectPreset移动部分
        TagView temp = selectTagViewList.get(index);
        selectTagViewList.set(index, selectTagViewList.get(index + 1));
        selectTagViewList.set(index + 1, temp);
        change = true;
    }

    /**
     * 跨表格移动 sle
     * 2023年6月2日09:31:07
     *
     * @param direction 方位
     */
    private void moveAttribute(Direction direction) {
        if (selectTable == null) {
            return;
        }

        int index = selectTable.getSelectedRow();
        // 获取选中行的索引
        if (index < 0) {
            return;
        }
        // 如果操作项是系统默认的，则不操作，并返回
        if (isSystemTagView(getSelectTagView())) {
            return;
        }
        CornerDisplay moveCornerDisplay = getNextCorner(direction);
        // 为空说明到头了，无法移动了
        if (moveCornerDisplay == null) {
            return;
        }

        DefaultTableModel moveModel = modelMap.get(moveCornerDisplay);
        List<TagView> moveTagView = selectTagViewListMap.get(moveCornerDisplay);
        JTable moveTable = tableMap.get(moveCornerDisplay);
        if (moveTagView == null) {
            moveTagView = new ArrayList<>();
            selectTagViewListMap.put(moveCornerDisplay, moveTagView);
        }

        TagView tagView = getSelectTagView();
        int targetRow = -1;

        if (direction == Direction.UP) {
            targetRow = moveTagView.size();
            // 怎么写
        } else if (direction == Direction.DOWN) {
            targetRow = 0;
        } else if (direction == Direction.LEFT || direction == Direction.RIGHT || direction == Direction.TRANSLATION) {
            targetRow = Math.min(index, moveTagView.size());
        }

        // 处理当前表格
        selectModel.removeRow(index);
        selectTagViewList.remove(index);
        selectTable.clearSelection();

        // 添加到目标表格
        moveModel.insertRow(targetRow, new Object[]{tagView});
        moveTagView.add(targetRow, tagView);
        moveTable.setRowSelectionInterval(targetRow, targetRow);
        moveTable.setColumnSelectionInterval(0, 0); // 设置选中第一列

        // 替换当前选中项
        selectModel = moveModel;
        selectTagViewList = moveTagView;
        selectTable = moveTable;
        selectCornerDisplay = moveCornerDisplay;
    }

    /**
     * 获取移动的目标方位 sle
     * 2023年6月1日17:07:53     *
     *
     * @param direction 方位
     * @return 目标方位
     */
    public CornerDisplay getNextCorner(Direction direction) {
        switch (selectCornerDisplay) {
            case TOP_LEFT:
                if (direction == Direction.UP) {
                    return null;
                } else if (direction == Direction.RIGHT || direction == Direction.TRANSLATION) {
                    return CornerDisplay.TOP_RIGHT;
                } else if (direction == Direction.DOWN) {
                    return CornerDisplay.BOTTOM_LEFT;
                }
                break;
            case TOP_RIGHT:
                if (direction == Direction.UP) {
                    return null;
                } else if (direction == Direction.LEFT || direction == Direction.TRANSLATION) {
                    return CornerDisplay.TOP_LEFT;
                } else if (direction == Direction.DOWN) {
                    return CornerDisplay.BOTTOM_RIGHT;
                }
                break;
            case BOTTOM_LEFT:
                if (direction == Direction.DOWN) {
                    return null;
                } else if (direction == Direction.UP) {
                    return CornerDisplay.TOP_LEFT;
                } else if (direction == Direction.RIGHT || direction == Direction.TRANSLATION) {
                    return CornerDisplay.BOTTOM_RIGHT;
                }
                break;
            case BOTTOM_RIGHT:
                if (direction == Direction.DOWN) {
                    return null;
                } else if (direction == Direction.UP) {
                    return CornerDisplay.TOP_RIGHT;
                } else if (direction == Direction.LEFT || direction == Direction.TRANSLATION) {
                    return CornerDisplay.BOTTOM_LEFT;
                }
                break;
        }
        return null;
    }

    /**
     * 深拷贝Map sle
     * 2023年5月25日14:23:05
     * 如果使用浅拷贝
     * 那么虽然map的引用不一样了，但是map中的list他们的引用还是一样的，会导致在修改当前页面map的list时，影响到了静态变量map中的list
     */
    private void DeepCopyMap() {
        // 假设 originalMap 已经包含了数据

        // 执行深拷贝
        Map<Modality, Map<CornerDisplay, List<TagView>>> clonedMap = new LinkedHashMap<>();

        for (Map.Entry<Modality, Map<CornerDisplay, TagView[]>> entry : ATTRIBUTE_VIEW_MAP.entrySet()) {
            Modality modality = entry.getKey();
            Map<CornerDisplay, TagView[]> cornerDisplayMap = entry.getValue();

            Map<CornerDisplay, List<TagView>> clonedCornerDisplayMap = new LinkedHashMap<>();
            for (Map.Entry<CornerDisplay, TagView[]> cornerDisplayEntry : cornerDisplayMap.entrySet()) {
                CornerDisplay cornerDisplay = cornerDisplayEntry.getKey();
                TagView[] tagViews = cornerDisplayEntry.getValue();

                // 进行 TagView[] 的深拷贝
                List<TagView> clonedTagViews = new ArrayList<>();
                for (TagView item : tagViews) {
                    if (item != null) {
                        TagView clonedTagView = new TagView(item.getFormat(), item.getTag().clone());
                        clonedTagViews.add(clonedTagView);
                    }
                }

                // 将深拷贝后的数据放入 clonedCornerDisplayMap
                clonedCornerDisplayMap.put(cornerDisplay, clonedTagViews);
            }

            // 将深拷贝后的数据放入 clonedMap
            clonedMap.put(modality, clonedCornerDisplayMap);
        }

        ModalityViewMap = clonedMap;
    }

    /**
     * 获取系统的Tag值
     * 左下角的系统Tag值，不允许编辑和移动
     *
     * @return 系统Tag值
     */
    private List<TagView> getSystemDefaultTagViewList() {
        String systemDefaultStr = " (" + Messages.getString("System") + Messages.getString("Default") + ")";
        List<TagView> list = new ArrayList<>();
        list.add(new TagView(new TagW("FRAME", Messages.getString("AttributePreView.SystemFRAME") + systemDefaultStr, TagW.TagType.STRING)));
        list.add(new TagView(new TagW("ROTATION", Messages.getString("AttributePreView.SystemROTATION") + systemDefaultStr, TagW.TagType.STRING)));
        list.add(new TagView(new TagW("ZOOM", Messages.getString("AttributePreView.SystemZOOM") + systemDefaultStr, TagW.TagType.STRING)));
        list.add(new TagView(new TagW("WINDOW_LEVEL", Messages.getString("AttributePreView.SystemWINDOW_LEVEL") + systemDefaultStr, TagW.TagType.STRING)));
        list.add(new TagView(new TagW("PIXEL", Messages.getString("AttributePreView.SystemPIXEL") + systemDefaultStr, TagW.TagType.STRING)));
        list.add(new TagView(new TagW("MODALITY", Messages.getString("AttributePreView.SystemMODALITY") + systemDefaultStr, TagW.TagType.STRING)));
        return list;
    }

    /**
     * 是否是系统默认项
     *
     * @param tagView 判断项
     * @return 是和否
     */
    private boolean isSystemTagView(TagView tagView) {
        return SystemDefaultTagViewList.contains(tagView);
    }

    /**
     * 关闭设置弹框/应用 sle
     * 写入xml文件
     */
    @Override
    public void closeAdditionalWindow() {
        // 如果没有改变，就不走后续逻辑，因为涉及到去读写xml文件，能快一点是一点
        if (!change) {
            return;
        }
        try {
            File file = ResourceUtil.getResource(DicomResource.ATTRIBUTES_VIEW);
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // 读取xml文件
            Document doc = docBuilder.parse(file);
            Element mainElement = doc.getDocumentElement();

            // 删除原来的节点
            NodeList layoutList = doc.getElementsByTagName("modality");
            for (int i = layoutList.getLength() - 1; i >= 0; i--) {
                Element layout = (Element) layoutList.item(i);
                mainElement.removeChild(layout);
            }

            // 添加节点
            for (Map.Entry<Modality, Map<CornerDisplay, List<TagView>>> modalityEntry : ModalityViewMap.entrySet()) {
                Map<CornerDisplay, List<TagView>> cornerDisplayMap = modalityEntry.getValue();
                String modalityID = modalityEntry.getKey().name();

                boolean add = false;
                // 创建 <modality> 元素，并设置 name 属性
                Element modalityElement = doc.createElement("modality");
                modalityElement.setAttribute("name", modalityID);
                for (Map.Entry<CornerDisplay, List<TagView>> cornerEntry : cornerDisplayMap.entrySet()) {
                    CornerDisplay cornerDisplay = cornerEntry.getKey();
                    List<TagView> tagViews = cornerEntry.getValue();
                    if (tagViews.size() == 0) {
                        continue;
                    }
                    add = true;
                    Element cornerElement = doc.createElement("corner");
                    cornerElement.setAttribute("name", cornerDisplay.name());

                    for (int i = 0; i < tagViews.size(); i++) {
                        TagView tagView = tagViews.get(i);
                        Element pElement = doc.createElement("p");
                        pElement.setAttribute("index", String.valueOf(i + 1));
                        pElement.setAttribute("format", tagView.getFormat());
                        TagW tagW = tagView.getTag()[0];
                        pElement.setTextContent(tagW.getKeyword());

                        // 将 <p> 元素添加到 <corner> 元素中
                        cornerElement.appendChild(pElement);
                    }

                    // 将 <corner> 元素添加到 <modality> 元素中
                    modalityElement.appendChild(cornerElement);
                }

                // 将 <modality> 元素添加到 <modalities> 元素中
                if (add) {
                    mainElement.appendChild(modalityElement);
                }
            }

            saveDocumentXml(doc, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 恢复默认值 sle
     * 2023年5月23日19:20:31
     */
    @Override
    public void resetToDefaultValues() {
        DeepCopyMap();
        initialize();
    }
}
