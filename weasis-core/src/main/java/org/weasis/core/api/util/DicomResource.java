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

import org.weasis.core.api.util.ResourceUtil.ResourcePath;

/**
 * xml配置文件 sle
 * 2023年5月11日09:09:34
 */
public enum DicomResource implements ResourcePath {
  /**
   * 四角信息
   */
  ATTRIBUTES_VIEW("attributes-view.xml"),

  /**
   *
   */
  CALLING_NODES("dicomCallingNodes.xml"),

  /**
   * 检查类型的布局
   */
  MODALITY_LAYOUT("modalityLayout.xml"),

  /**
   * LUT选择
   */
  LUTS("luts"),

  /**
   * 窗宽窗位（预设）
   */
  PRESETS("presets.xml"),

  /**
   *
   */
  SERIES_SPITTING_RULES("series-splitting-rules.xml"), // NON-NLS

  /**
   *
   */
  CGET_SOP_UID("store-tcs.properties"),

  /**
   *
   */
  BODY_PART_EXAMINED("bodyPartExamined.csv"),

  /**
   * 预设标签
   */
  Pre_Tag("preTag.xml"); // NON-NLS

  private final String path;

  DicomResource(String path) {
    this.path = path;
  }

  public String getPath() {
    return path;
  }
}
