package org.weasis.dicom.ylyy.expand;

import javax.swing.*;
import javax.swing.text.*;

/**
 * 自定义输入框，控制是否为int输入框和内容最大长度
 * sle
 * 2023年6月1日11:09:00
 */
public class RestrictedTextField extends JTextField {
    /**
     * 最大长度
     */
    private final int maxLength;

    /**
     * 是否为int输入框
     */
    private final boolean allowOnlyIntegers;

    /**
     * 输入类型和长度自定义输入框
     * @param maxLength 最大长度
     * @param allowOnlyIntegers 是否int输入框
     */
    public RestrictedTextField(int maxLength, boolean allowOnlyIntegers) {
        this.maxLength = maxLength;
        this.allowOnlyIntegers = allowOnlyIntegers;
        setDocument(new RestrictedDocument());
    }

    private class RestrictedDocument extends PlainDocument {
        @Override
        public void insertString(int offset, String text, AttributeSet attr) throws BadLocationException {
            if (text == null) {
                return;
            }

            String currentText = getText(0, getLength());
            String newText = currentText.substring(0, offset) + text + currentText.substring(offset);

            if ((!allowOnlyIntegers || isValidInteger(newText)) && newText.length() <= maxLength) {
                super.insertString(offset, text, attr);
            }
        }

        private boolean isValidInteger(String text) {
            try {
                Integer.parseInt(text);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
}