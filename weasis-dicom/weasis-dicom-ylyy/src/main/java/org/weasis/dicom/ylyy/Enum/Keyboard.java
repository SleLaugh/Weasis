package org.weasis.dicom.ylyy.Enum;

import org.weasis.dicom.ylyy.Messages;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * 快捷键枚举 sle
 * 2023年5月19日18:06:28
 */
public enum Keyboard {
    F1(KeyEvent.VK_F1, "F1"),
    F2(KeyEvent.VK_F2, "F2"),
    F3(KeyEvent.VK_F3, "F3"),
    F4(KeyEvent.VK_F4, "F4"),
    F5(KeyEvent.VK_F5, "F5"),
    F6(KeyEvent.VK_F6, "F6"),
    F7(KeyEvent.VK_F7, "F7"),
    F8(KeyEvent.VK_F8, "F8"),
    F9(KeyEvent.VK_F9, "F9"),
    F10(KeyEvent.VK_F10, "F10"),
    F11(KeyEvent.VK_F11, "F11"),
    F12(KeyEvent.VK_F12, "F12"),
    NUMPAD_0(KeyEvent.VK_NUMPAD0, String.format(Messages.getString("Keyboard.Numpad"), "0")),
    NUMPAD_1(KeyEvent.VK_NUMPAD1, String.format(Messages.getString("Keyboard.Numpad"), "1")),
    NUMPAD_2(KeyEvent.VK_NUMPAD2, String.format(Messages.getString("Keyboard.Numpad"), "2")),
    NUMPAD_3(KeyEvent.VK_NUMPAD3, String.format(Messages.getString("Keyboard.Numpad"), "3")),
    NUMPAD_4(KeyEvent.VK_NUMPAD4, String.format(Messages.getString("Keyboard.Numpad"), "4")),
    NUMPAD_5(KeyEvent.VK_NUMPAD5, String.format(Messages.getString("Keyboard.Numpad"), "5")),
    NUMPAD_6(KeyEvent.VK_NUMPAD6, String.format(Messages.getString("Keyboard.Numpad"), "6")),
    NUMPAD_7(KeyEvent.VK_NUMPAD7, String.format(Messages.getString("Keyboard.Numpad"), "7")),
    NUMPAD_8(KeyEvent.VK_NUMPAD8, String.format(Messages.getString("Keyboard.Numpad"), "8")),
    NUMPAD_9(KeyEvent.VK_NUMPAD9, String.format(Messages.getString("Keyboard.Numpad"), "9")),
    DIVIDE(KeyEvent.VK_DIVIDE, String.format(Messages.getString("Keyboard.Numpad"), "/")),
    MULTIPLY(KeyEvent.VK_MULTIPLY, String.format(Messages.getString("Keyboard.Numpad"), "*")),
    SUBTRACT(KeyEvent.VK_SUBTRACT, String.format(Messages.getString("Keyboard.Numpad"), "-")),
    ADD(KeyEvent.VK_ADD, String.format(Messages.getString("Keyboard.Numpad"), "+")),
    DECIMAL(KeyEvent.VK_DECIMAL, String.format(Messages.getString("Keyboard.Numpad"), ".")),
    KEY_1(KeyEvent.VK_1, String.format(Messages.getString("Keyboard.main_keyboard"), "1")),
    KEY_2(KeyEvent.VK_2, String.format(Messages.getString("Keyboard.main_keyboard"), "2")),
    KEY_3(KeyEvent.VK_3, String.format(Messages.getString("Keyboard.main_keyboard"), "3")),
    KEY_4(KeyEvent.VK_4, String.format(Messages.getString("Keyboard.main_keyboard"), "4")),
    KEY_5(KeyEvent.VK_5, String.format(Messages.getString("Keyboard.main_keyboard"), "5")),
    KEY_6(KeyEvent.VK_6, String.format(Messages.getString("Keyboard.main_keyboard"), "6")),
    KEY_7(KeyEvent.VK_7, String.format(Messages.getString("Keyboard.main_keyboard"), "7")),
    KEY_8(KeyEvent.VK_8, String.format(Messages.getString("Keyboard.main_keyboard"), "8")),
    KEY_9(KeyEvent.VK_9, String.format(Messages.getString("Keyboard.main_keyboard"), "9")),
    KEY_0(KeyEvent.VK_0, String.format(Messages.getString("Keyboard.main_keyboard"), "0")),
    MINUS(KeyEvent.VK_MINUS, String.format(Messages.getString("Keyboard.main_keyboard"), "-")),
    EQUALS(KeyEvent.VK_EQUALS, String.format(Messages.getString("Keyboard.main_keyboard"), "="));

    private final int keyCode;
    private final String label;
    private static final Map<Integer, Keyboard> KEY_CODE_MAP = buildKeyCodeMap();

    Keyboard(int keyCode, String label) {
        this.keyCode = keyCode;
        this.label = label;
    }

    private static Map<Integer, Keyboard> buildKeyCodeMap() {
        Map<Integer, Keyboard> keyCodeMap = new HashMap<>();
        for (Keyboard keyboard : Keyboard.values()) {
            keyCodeMap.put(keyboard.getKeyCode(), keyboard);
        }
        return keyCodeMap;
    }

    /**
     * 获取枚举
     *
     * @param keyCode 快捷键的键值
     * @return
     */
    public static Keyboard getByKeyCode(int keyCode) {
        return KEY_CODE_MAP.get(keyCode);
    }

    public int getKeyCode() {
        return keyCode;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
