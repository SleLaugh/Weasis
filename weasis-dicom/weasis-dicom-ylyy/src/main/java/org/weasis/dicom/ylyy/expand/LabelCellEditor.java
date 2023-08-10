package org.weasis.dicom.ylyy.expand;

import org.weasis.dicom.ylyy.Messages;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.text.Format;

/**
 * label在table中的编辑器拓展 sle
 * 2023年5月18日16:03:36
 * 单元格编辑器
 * 在表格中编辑单元格时的交互方式和规则
 */
public class LabelCellEditor extends DefaultCellEditor {
    /**
     * 类型
     */
    final private Class<?> valueType;

    /**
     * 格式
     */
    final private Format format;

    /**
     * 是否可空
     */
    final private boolean isNull;

    /**
     * 列名
     */
    final private String columnName;

    /**
     * 父组件
     */
    final private Component parentComponent;

    private final JTextField textField;

    /**
     * label 编辑器
     *
     * @param valueType       值类型
     * @param format          格式化
     * @param maxLength       最大长度
     * @param isNull          可否为空
     * @param columnName      列名
     * @param parentComponent 父组件
     */
    public LabelCellEditor(Class<?> valueType, Format format, int maxLength, boolean isNull, String columnName, Component parentComponent) {
        super(new JTextField());
        this.valueType = valueType;
        textField = (JTextField) getComponent();
        textField.setBorder(null);
        if (maxLength > 0) {
            ((AbstractDocument) textField.getDocument()).setDocumentFilter(new LengthDocumentFilter(maxLength));
        }

        this.format = format;
        this.isNull = isNull;
        this.columnName = columnName;
        this.parentComponent = parentComponent;
    }

    /**
     * 停止编辑方法 sle
     * 2023年5月19日14:49:18
     *
     * @return 是否停止编辑
     */
    @Override
    public boolean stopCellEditing() {
        // 非空验证
        String editedValue = textField.getText();
        if (!isNull && editedValue.isEmpty()) {
            JOptionPane.showMessageDialog(parentComponent, String.format(Messages.getString("CellEditor.notNull"), columnName));
            return false;
        }
        if (valueType == Double.class) {
            try {
                Double.parseDouble(editedValue);
            } catch (NumberFormatException e) {
//                textField.setBorder(BorderFactory.createLineBorder(Color.RED)); // 可选，用红色边框标记输入错误的单元格
                JOptionPane.showMessageDialog(parentComponent, String.format(Messages.getString("CellEditor.valueTypeError"), valueType.getSimpleName()));
                return false;
            }
        }

        return super.stopCellEditing();
    }

    /**
     * 长度控制 sle
     * 2023年5月19日14:49:45
     */
    private static class LengthDocumentFilter extends DocumentFilter {
        /**
         * 最大长度
         */
        private final int maxLength;

        public LengthDocumentFilter(int maxLength) {
            this.maxLength = maxLength;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (fb.getDocument().getLength() + string.length() <= maxLength) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            int newLength = fb.getDocument().getLength() - length + text.length();
            if (newLength <= maxLength) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }
}