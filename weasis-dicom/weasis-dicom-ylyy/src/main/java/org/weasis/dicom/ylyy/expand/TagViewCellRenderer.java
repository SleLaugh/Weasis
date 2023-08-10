package org.weasis.dicom.ylyy.expand;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.function.Function;

/**
 * 四角信息 行样式渲染器 sle
 * 2023年5月26日10:52:59
 *
 * @param <T> 类型
 */
public class TagViewCellRenderer<T> extends DefaultTableCellRenderer {
    /**
     * format方法
     */
    private final Function<T, String> formatFunction;

    /**
     * 判断是否是系统默认tag值
     */
    private final Function<T, Boolean> isSystemDefaultFunction;

    /**
     * 对齐方式
     */
    private final int alignment;

    /**
     * 四角信息 行样式
     *
     * @param formatFunction          format方法
     * @param isSystemDefaultFunction 判断是否系统tag值方法
     * @param alignment               对齐方式
     */
    public TagViewCellRenderer(Function<T, String> formatFunction, Function<T, Boolean> isSystemDefaultFunction, int alignment) {
        this.formatFunction = formatFunction;
        this.isSystemDefaultFunction = isSystemDefaultFunction;
        this.alignment = alignment;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // 设置对齐方式
        setHorizontalAlignment(alignment);

        // 调用传入的 format 方法获取需要显示的内容
        String displayText = formatFunction.apply((T) value);
        setText(displayText);
        setBorder(BorderFactory.createEmptyBorder());

        boolean isSystem = isSystemDefaultFunction.apply((T) value);
        if (isSystem) {
            this.setForeground(Color.GRAY);
            this.setBackground(table.getBackground());
        } else {
            this.setForeground(Color.black);
            if (isSelected) {
                this.setBackground(table.getSelectionBackground());
            } else {
                this.setBackground(Color.white);
            }
        }
        return this;
    }
}