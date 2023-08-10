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

import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import net.miginfocom.swing.MigLayout;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.core.api.media.data.TagView;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.util.DicomResource;
import org.weasis.core.api.util.ResourceUtil;
import org.weasis.core.util.FileUtil;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.codec.display.CornerDisplay;
import org.weasis.dicom.codec.display.Modality;
import org.weasis.dicom.ylyy.expand.RestrictedTextField;
import org.weasis.dicom.ylyy.Messages;

/**
 * 新增、编辑四角信息项
 */
public class AttributeDialog extends JDialog {

    /**
     * 预设标签
     */
    private static final Map<String, String> preTagMap = readPreTag();

    /**
     * 原本选中的位置
     */
    private final CornerDisplay cornerDisplay;

    /**
     * 当前的检查类型
     */
    private final Modality modality;

    /**
     * 当前编辑行，如果为-1说明是新增
     */
    private final int row;

    /**
     * tableMap
     */
    private final Map<CornerDisplay, JTable> tableMap;

    /**
     * 当前选中检查类型的四角信息
     */
    private final Map<CornerDisplay, List<TagView>> tagViewMap;

    /**
     * 预设标签的下拉框
     */
    private final JComboBox<String> tagJComboBox = new JComboBox<>(preTagMap.keySet().toArray(new String[0]));

    /**
     * tag值的keyword
     */
    private final JLabel keywordValue = new JLabel();

    /**
     * 位置combox
     */
    private final JComboBox<CornerDisplay> cornerDisplayJComboBox = new JComboBox<>(CornerDisplay.values());

    /**
     * 标题
     */
    private RestrictedTextField titleTf;

    /**
     * $V占位符
     */
    private final String placeholderV = "$V";

    /**
     * 标题长度
     */
    private final Integer titleLength = 50;

    /**
     * 默认行高度
     */
    private final int DefaultLength;

    /**
     * 系统默认Tag值所在的方位
     */
    private final CornerDisplay DefaultCornerDisplay = CornerDisplay.BOTTOM_LEFT;

    /**
     * format拆除结果
     *
     * @param title  标题
     * @param length 长度
     */
    private record FormatResult(String title, int length) {
    }


    /**
     * 新增、编辑四角信息项 构造函数 sle
     * 2023年5月31日14:14:20
     *
     * @param parent        父节点
     * @param title         标题
     * @param cornerDisplay 方位
     * @param tagView       tag标签
     * @param modality      类型
     * @param tableMap      tableMap
     * @param selectTagView 当前选中类型的四角信息
     * @param row           当前编辑行
     * @param defaultLength 默认行高度
     */
    public AttributeDialog(Window parent, String title, CornerDisplay cornerDisplay, TagView tagView, Modality modality, Map<CornerDisplay, JTable> tableMap, Map<CornerDisplay, List<TagView>> selectTagView, int row, int defaultLength) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        this.cornerDisplay = cornerDisplay;
        this.tableMap = tableMap;
        this.tagViewMap = selectTagView;
        this.modality = modality;
        this.row = row;
        this.DefaultLength = defaultLength;

        initComponents();

        if (cornerDisplay != null) {
            this.cornerDisplayJComboBox.setSelectedItem(cornerDisplay);
        }
        if (tagView != null) {
            String keyword = tagView.getTag()[0].getKeyword();
            String selectItem = null;
            // 在预设的tag值中寻找，如果找到了则选中这个项
            for (Map.Entry<String, String> entry : preTagMap.entrySet()) {
                if (entry.getValue().equals(keyword)) {
                    selectItem = entry.getKey();
                    tagJComboBox.setSelectedItem(entry.getKey());
                    break;
                }
            }
            // 如果不在预设的tag值中，则把这一项加进去并选中它
            if (selectItem == null) {
                tagJComboBox.addItem(keyword);
                tagJComboBox.setSelectedItem(keyword);
                preTagMap.put(keyword, keyword);
            }
            keywordValue.setText(keyword);

            // 添加标题
            if (!tagView.getFormat().isEmpty()) {
                FormatResult format = parseFormatString(tagView.getFormat());
                titleTf.setText(format.title);
//                if (format.length > 0) {
//                    maxLengthTf.setText(String.valueOf(format.length));
//                }
            } else {
                titleTf.setText("");
            }
        } else {
            Object selectedItem = tagJComboBox.getSelectedItem();
            if (selectedItem != null) {
                selectTag((String) selectedItem);
            }
        }
        pack();
    }

    /**
     * 初始化 sle
     * 2023年6月1日15:58:04
     */
    private void initComponents() {
        tagJComboBox.addItemListener(e -> { // 选中检查类型时的方法
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectTag((String) e.getItem());
            }
        }); // 添加监听方法
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("insets 10lp 15lp 10lp 15lp", "[right]rel[grow,fill]")); // NON-NLS

        /// 检查类型
        JLabel modalityLabel = new JLabel(Messages.getString("PreView.modality") + StringUtil.COLON);
        JLabel modalityValue = new JLabel(modality.name());
        panel.add(modalityLabel, GuiUtils.NEWLINE);
        panel.add(modalityValue);

        // tag值
        JLabel tagLabel = new JLabel(Messages.getString("AttributePreView.tag") + StringUtil.COLON);
        panel.add(tagLabel, GuiUtils.NEWLINE);
        panel.add(tagJComboBox);

        // tag标签
        JLabel keywordLabel = new JLabel(Messages.getString("AttributePreView.keyword") + StringUtil.COLON);
        panel.add(keywordLabel, GuiUtils.NEWLINE);
        panel.add(keywordValue);

        // 位置
        JLabel positionLabel = new JLabel(Messages.getString("AttributePreView.position") + StringUtil.COLON);
        panel.add(positionLabel, GuiUtils.NEWLINE);
        panel.add(cornerDisplayJComboBox);

        // 标题
        JLabel titleLabel = new JLabel(Messages.getString("AttributePreView.title") + StringUtil.COLON);
        titleTf = new RestrictedTextField(titleLength, false);
        titleTf.setColumns(15);
        panel.add(titleLabel, GuiUtils.NEWLINE);
        panel.add(titleTf);
//
//        // 内容最大长度
//        JLabel maxLengthLabel = new JLabel(Messages.getString("AttributePreView.value_lenght") + StringUtil.COLON);
//        maxLengthTf = new RestrictedTextField(2, true);
//        maxLengthTf.setColumns(5);
//        GuiUtils.setPreferredWidth(maxLengthTf, 60);
//        panel.add(maxLengthLabel, GuiUtils.NEWLINE);
//        panel.add(maxLengthTf, "grow 0"); // NON-NLS

        // 确定
        JButton okButton = new JButton(Messages.getString("PreView.ok"));
        okButton.addActionListener(e -> okButtonActionPerformed());
        // 取消
        JButton cancelButton = new JButton(Messages.getString("PreView.cancel"));
        cancelButton.addActionListener(e -> dispose());

        panel.add(GuiUtils.getFlowLayoutPanel(FlowLayout.TRAILING, 0, 5, okButton, GuiUtils.boxHorizontalStrut(20), cancelButton), "newline, spanx, gaptop 10lp"); // NON-NLS
        setContentPane(panel);
        this.setResizable(false); // 设置边框不允许自定义拖拽大小
    }

    /**
     * 确定按钮 sle
     * 2023年6月1日10:43:50
     */
    private void okButtonActionPerformed() {
        CornerDisplay presentCornerDisplay = (CornerDisplay) cornerDisplayJComboBox.getSelectedItem(); // 保存的目标方位
        String keyword = preTagMap.get((String) tagJComboBox.getSelectedItem());
        TagW t = TagW.get(keyword);
        ArrayList<TagW> list = new ArrayList<>(1);
        list.add(t);
        TagView tagView = new TagView(formatJoint(), list.toArray(new TagW[0]));

        DefaultTableModel presentModel = (DefaultTableModel) tableMap.get(presentCornerDisplay).getModel(); // 保存的目标方位的model
        List<TagView> presentTagViewList = tagViewMap.get(presentCornerDisplay);
        if (presentTagViewList == null) {
            presentTagViewList = new ArrayList<>();
            tagViewMap.put(presentCornerDisplay, presentTagViewList);
        }

        // 如果是左下角，则增加项的行需要减去系统默认的行
        int presentRow = presentCornerDisplay == DefaultCornerDisplay ? presentModel.getRowCount() - DefaultLength : presentModel.getRowCount();
        // -1说明是新增,否则是编辑
        if (this.row == -1) {
            presentModel.insertRow(presentRow, new Object[]{tagView});
            presentTagViewList.add(tagView);
        }
        // 其他值说明是编辑
        else {
            // 如果没变位置，那么就更新内容
            if (Objects.equals(presentCornerDisplay, cornerDisplay)) {
                presentModel.setValueAt(tagView, row, 0);
                presentTagViewList.set(row, tagView);
            }
            // 如果变了位置,则要删除原先方位的，然后将这个添加到新方位
            else {
                JTable table = tableMap.get(cornerDisplay);
                DefaultTableModel model = (DefaultTableModel) table.getModel(); // 原方位的model
                model.removeRow(row); // 删除原先方位
                List<TagView> tagViewList = tagViewMap.get(cornerDisplay); // 原先方位的list
                tagViewList.remove(row); // 删除原先方位
                table.clearSelection();
                table.repaint();
                presentModel.insertRow(presentRow, new Object[]{tagView});
                presentTagViewList.add(tagView); // 添加到当前方位
            }
        }
        setVisible(false); // 将窗口设置为不可见
        dispose(); // 关闭窗口
    }

    /**
     * tag点击事件 sle
     * 2023年6月1日15:06:32
     *
     * @param item
     */
    private void selectTag(String item) {
        String title = item.length() > titleLength ? item.substring(titleLength) : item;
        titleTf.setText(title + "：");
        keywordValue.setText(preTagMap.get(item));
    }

    /**
     * format拆分 sle
     * 2023年6月1日10:31:51
     *
     * @param formatString format字符串
     * @return 拆分结果
     */
    private FormatResult parseFormatString(String formatString) {
        String title = "";
        int length = -1;

        // 查找$V的索引
        int vIndex = formatString.indexOf(placeholderV);
        if (vIndex != -1) {
            // 如果找到了$V
            title = formatString.substring(0, vIndex).trim();

            String placeholderL = ":l$";
            int lIndex = formatString.indexOf(placeholderL, vIndex);
            if (lIndex != -1) {
                // 如果找到了:l$
                int dollarIndex = formatString.indexOf("$", lIndex + placeholderL.length());
                if (dollarIndex != -1) {
                    // 如果找到了第二个$
                    String lengthString = formatString.substring(lIndex + placeholderL.length(), dollarIndex).trim();
                    try {
                        length = Integer.parseInt(lengthString);
                    } catch (NumberFormatException e) {
                        // 解析长度失败
                        e.printStackTrace();
                    }
                }
            }
        }

        return new FormatResult(title, length);
    }

    /**
     * format拼接 sle
     * 2023年6月1日10:45:15
     *
     * @return format
     */
    private String formatJoint() {
        String title = titleTf.getText();
        String formatString = title + placeholderV;
//        String length = maxLengthTf.getText();
//        if (!length.isEmpty() && Integer.parseInt(length) > 0) {
//            formatString += placeholderL + length + "$";
//        }
        return formatString;
    }

    /**
     * 读取预设标签 sle
     * 2023年6月1日14:53:42
     */
    private static Map<String, String> readPreTag() {
        Map<String, String> map = new LinkedHashMap<>();
        XMLStreamReader reader = null;

        try {
            File file = ResourceUtil.getResource(DicomResource.Pre_Tag);
            if (!file.canRead()) {
                return map;
            }

            //创建一个XMLInputFactory对象
            XMLInputFactory factory = XMLInputFactory.newInstance();
            //通过factory对象的createXMLStreamReader方法加载xml文件，获取XMLStreamReader对象
            reader = factory.createXMLStreamReader(new FileInputStream(file));
            //定义两个变量用于存储modality和id的值
            String keyword;
            String format;
            //遍历reader对象
            while (reader.hasNext()) {
                //获取当前事件类型
                int eventType = reader.next();
                //如果事件类型是开始元素
                if (eventType == XMLStreamConstants.START_ELEMENT) {
                    //获取当前元素的本地名称
                    String localName = reader.getLocalName();
                    //如果本地名称是tag
                    if ("tag".equals(localName)) {
                        //获取key属性值
                        keyword = reader.getAttributeValue(null, "keyword");
                        //获取format属性值
                        format = reader.getAttributeValue(null, "format");
                        TagW tag = TagW.get(keyword);
                        if (tag != null) {
                            map.put(format, keyword);
                        }
                    }
                }
            }
        } catch (Exception var10) {
            return map;
        } finally {
            FileUtil.safeClose(reader);
        }
        return map;
    }
}

