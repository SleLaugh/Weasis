/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.model.utils;

import org.weasis.core.Messages;
import org.weasis.core.ui.model.utils.bean.Measurement;

/**
 * 图像数据 （像素统计）sle
 * 2023年5月12日15:11:20
 */
public interface ImageStatistics {
  /**
   * 最小值
   */
  Measurement IMAGE_MIN = new Measurement(Messages.getString("measure.min"), 1, false, true, false);
  /**
   * 最大值
   */
  Measurement IMAGE_MAX = new Measurement(Messages.getString("measure.max"), 2, false, true, false);

  /**
   * 平均值
   */
  Measurement IMAGE_MEAN =
      new Measurement(Messages.getString("measure.mean"), 3, false, true, true);

  /**
   * 标准差
   */
  Measurement IMAGE_STD =
      new Measurement(Messages.getString("measure.stdev"), 4, false, true, false);

  /**
   * 倾斜度
   */
  Measurement IMAGE_SKEW =
      new Measurement(Messages.getString("measure.skew"), 5, false, true, false);

  /**
   * 峰度
   */
  Measurement IMAGE_KURTOSIS =
      new Measurement(Messages.getString("measure.kurtosis"), 6, false, true, false);

  /**
   * 像素
   */
  Measurement IMAGE_PIXELS =
      new Measurement(Messages.getString("ImageStatistics.pixels"), 7, false, true, false);
  /**
   * 中值
   */
  Measurement IMAGE_MEDIAN =
      new Measurement(Messages.getString("ImageStatistics.median"), 8, false, true, false);

  /**
   * 熵
   */
  Measurement IMAGE_ENTROPY =
      new Measurement(Messages.getString("ImageStatistics.entropy"), 9, false, true, false);

  /**
   * 图像数据集合（像素统计）
   */
  Measurement[] ALL_MEASUREMENTS = {
    IMAGE_PIXELS,
    IMAGE_MIN,
    IMAGE_MAX,
    IMAGE_MEDIAN,
    IMAGE_MEAN,
    IMAGE_STD,
    IMAGE_SKEW,
    IMAGE_KURTOSIS,
    IMAGE_ENTROPY
  };
}
