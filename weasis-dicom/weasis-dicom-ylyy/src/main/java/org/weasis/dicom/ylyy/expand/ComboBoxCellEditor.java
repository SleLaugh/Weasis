package org.weasis.dicom.ylyy.expand;

import org.weasis.dicom.ylyy.Enum.Keyboard;
import org.weasis.dicom.ylyy.Messages;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * combox在table单元格中的编辑器拓展 sle
 * 2023年5月22日11:02:00
 *
 * @param <T> 下拉框的类型
 */
public class ComboBoxCellEditor<T> extends AbstractCellEditor implements TableCellEditor {
    /**
     * 行对应的combox，每一行都有对应的combox控制
     */
    private final Map<Integer, JComboBox<T>> comboBoxMap;

    /**
     * 当前正在操作的combox
     */
    private JComboBox<T> editorComponent;

    /**
     * 上一次选中的值
     */
    private T previousValue;

    /**
     * 已选中的项集合
     */
    private final Set<T> selectedItems;

    /**
     * 是否允许重复项
     */
    private final Boolean allowDuplicates;

    /**
     * 父组件
     */
    final private Component parentComponent;

    /**
     * 下拉框的值
     */
    private final T[] items;

    /**
     * combox 编辑器
     * 通过外部直接传入comboBoxMap和selectedItems，由于是引用类型，所以可以直接在外部控制这两个值
     * 用于频繁操作comboBoxMap和selectedItems的场景
     *
     * @param items           下拉框的值
     * @param allowDuplicates 是否允许重复
     * @param parentComponent 父组件
     * @param comboBoxMap     表格的combox
     * @param selectedItems   combox的选中项
     */
    public ComboBoxCellEditor(T[] items, boolean allowDuplicates, Component parentComponent, Map<Integer, JComboBox<T>> comboBoxMap, Set<T> selectedItems) {
        this.items = items;
        this.allowDuplicates = allowDuplicates;
        this.comboBoxMap = comboBoxMap;
        this.selectedItems = selectedItems;
        this.parentComponent = parentComponent;
    }

    /**
     * combox 编辑器
     * 需配合addComboBox、removeCombox、clearComboBoxMap来控制comboBoxMap和selectedItems
     * 用于较少操作comboBoxMap和selectedItems的场景
     *
     * @param items           下拉框的值
     * @param allowDuplicates 是否允许重复
     * @param parentComponent 父组件
     */
    public ComboBoxCellEditor(T[] items, boolean allowDuplicates, Component parentComponent) {
        this.allowDuplicates = allowDuplicates;
        this.items = items;
        this.comboBoxMap = new HashMap<>();
        this.selectedItems = new HashSet<>();
        this.parentComponent = parentComponent;
    }

    /**
     * 获取当前单元格操作的值
     * 可以理解成编辑完成后事件 sle
     * 2023年5月22日17:57:02
     */
    @Override
    public Object getCellEditorValue() {
        return editorComponent.getSelectedItem();
    }

    /**
     * 编辑单元格时获取单元格的组件
     * 可以理解成点击时事件 sle
     * 2023年5月22日17:54:41
     */
    @Override
    public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        editorComponent = comboBoxMap.get(row);
        previousValue = value == null ? null : (T) value; // 保存之前的值
        editorComponent.setSelectedItem(value);
        return editorComponent;
    }

    /**
     * 新增comboBox sle
     * 2023年5月22日17:52:15
     *
     * @param row        行索引
     * @param selectItem 当前下拉框的选中项
     */
    public void addComboBox(int row, T selectItem) {
        JComboBox<T> comboBox = new JComboBox<>(items);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!allowDuplicates && checkDuplicateItem(comboBox)) {
                    comboBox.setSelectedItem(previousValue); // 恢复之前的值
                    JOptionPane.showMessageDialog(parentComponent, Messages.getString("comboBoxCellEditor.duplicates"));
                } else {
                    stopCellEditing();
                }
            }
        }); // 选中后直接关闭当前编辑状态

        comboBox.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                for (Keyboard keyboard : Keyboard.values()) {
                    if (keyboard.getKeyCode() == keyCode) {
                        comboBox.setSelectedItem(keyboard);
                        break;
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // 不执行编辑完成事件
            }
        }); // 按下键位后，如果指定的键位存在并且没有被选中，则直接选中
        comboBox.setEditable(false);
        comboBoxMap.put(row, comboBox);
        previousValue = selectItem;
        if (selectItem != null) {
            selectedItems.add(selectItem);
            comboBox.setSelectedItem(selectItem);
        }
    }

    /**
     * 删除combox sle
     * 2023年5月23日17:53:47
     *
     * @param row 行索引
     */
    public void removeCombox(int row) {
        JComboBox<T> comboBox = comboBoxMap.get(row);
        if (comboBox != null) {
            comboBoxMap.remove(row);
            T selectedItem = (T) comboBox.getSelectedItem();
            if (selectedItem != null) {
                selectedItems.remove(selectedItem);
            }

            // 更新剩余行的索引
            for (int i = row + 1; i <= comboBoxMap.size(); i++) {
                JComboBox<T> existingComboBox = comboBoxMap.get(i);
                if (existingComboBox != null) {
                    comboBoxMap.put(i - 1, existingComboBox);
                    comboBoxMap.remove(i);
                }
            }
        }
    }

    /**
     * 移动当前行到指定行
     * 并且互换二者的索引
     * @param currentRow 当期行索引
     * @param targetRow 目标行索引
     */
    public void moveCombox(int currentRow, int targetRow) {
        if (currentRow >= 0 && currentRow < comboBoxMap.size() && targetRow >= 0 && targetRow < comboBoxMap.size() && currentRow != targetRow) {
            JComboBox<T> currentComboBox = comboBoxMap.get(currentRow);
            JComboBox<T> targetComboBox = comboBoxMap.get(targetRow);

            comboBoxMap.put(currentRow, targetComboBox);
            comboBoxMap.put(targetRow, currentComboBox);

            // 更新选中项集合的顺序
            T currentSelectedItem = (T) currentComboBox.getSelectedItem();
            T targetSelectedItem = (T) targetComboBox.getSelectedItem();
            if (currentSelectedItem != null) {
                selectedItems.remove(currentSelectedItem);
                selectedItems.add(targetSelectedItem);
            }
            if (targetSelectedItem != null) {
                selectedItems.remove(targetSelectedItem);
                selectedItems.add(currentSelectedItem);
            }
        }
    }

    /**
     * 清空所有的combox sle
     * 2023年5月22日16:06:50
     */
    public void clearComboBoxMap() {
        comboBoxMap.clear();
        selectedItems.clear();
        previousValue = null;
    }

    /**
     * 检查选项是否重复 sle
     * 2023年5月22日18:44:54
     *
     * @param comboBox 下拉框
     * @return 是否存在重复选项
     */
    private boolean checkDuplicateItem(JComboBox<T> comboBox) {
        T selectedItem = (T) comboBox.getSelectedItem();
        if (selectedItem == null || selectedItem.equals(previousValue)) {
            return false; // 选项为null或与上一次选中的值相同，不重复
        }

        if (selectedItems.contains(selectedItem)) {
            return true; // 选项重复
        }

        selectedItems.remove(previousValue); // 清除上一次选中的值
        selectedItems.add(selectedItem); // 添加本次的值
        return false; // 选项不重复
    }
}