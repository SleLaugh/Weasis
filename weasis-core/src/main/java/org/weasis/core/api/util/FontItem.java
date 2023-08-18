/*
 * Copyright (c) 2022 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.util;

import org.weasis.core.Messages;
import java.awt.Font;
import javax.swing.UIManager;

/**
 * 文字枚举 sle 添加注释
 * 2023年8月11日15:09:43
 * 添加中文描述 sle
 */
public enum FontItem {
  H1_SEMIBOLD("h1.font",Messages.getString("Font.h1_semibold")),
  H1("h1.regular.font", Messages.getString("Font.h1")),
  H2_SEMIBOLD("h2.font", Messages.getString("Font.h2_semibold")),
  H2("h2.regular.font", Messages.getString("Font.h2")),
  H3_SEMIBOLD("h3.font", Messages.getString("Font.h3_semibold")),
  H3("h3.regular.font", Messages.getString("Font.h3")),
  LARGE("large.font", Messages.getString("Font.large")),
  DEFAULT_BOLD("h4.font", Messages.getString("Font.default_bold")),
  DEFAULT_SEMIBOLD("semibold.font", Messages.getString("Font.default_semibold")),
  DEFAULT("defaultFont", Messages.getString("Font.default")),
  DEFAULT_LIGHT("light.font", Messages.getString("Font.default_light")),
  MONOSPACED("monospaced.font", Messages.getString("Font.monospaced")),
  MEDIUM_SEMIBOLD("medium.semibold.font", Messages.getString("Font.medium_semibold")),
  MEDIUM("medium.font", Messages.getString("Font.medium")),
  SMALL_SEMIBOLD("small.semibold.font", Messages.getString("Font.small_semibold")),
  SMALL("small.font", Messages.getString("Font.small")),
  MINI_SEMIBOLD("mini.semibold.font", Messages.getString("Font.mini_semibold")),
  MINI("mini.font", Messages.getString("Font.mini")),
  MICRO_SEMIBOLD("micro.semibold.font", Messages.getString("Font.micro_semibold")),
  MICRO("micro.font", Messages.getString("Font.micro"));


  private final String key;
  private final String name;

  FontItem(String key, String name) {
    this.key = key;
    this.name = name;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public Font getFont() {
    return UIManager.getFont(key);
  }

  @Override
  public String toString() {
    return name;
  }

  public static FontItem getFontItem(String key) {
    for (FontItem item : FontItem.values()) {
      if (item.key.equals(key)) {
        return item;
      }
    }
    return DEFAULT;
  }
}
