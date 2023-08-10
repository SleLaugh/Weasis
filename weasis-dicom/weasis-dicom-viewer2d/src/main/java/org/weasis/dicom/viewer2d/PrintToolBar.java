/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.viewer2d;

import org.weasis.core.api.util.ResourceUtil;
import org.weasis.core.api.util.ResourceUtil.ActionIcon;
import org.weasis.core.ui.util.WtoolBar;

import javax.swing.*;

/**
 * 上方打印栏
 * sle 2023年8月10日17:02:19
 */
public class PrintToolBar extends WtoolBar {

  public PrintToolBar(int index) {
    super(Messages.getString("PrintToolBar.name"), index);

    final JButton printButton = new JButton();
    printButton.setIcon(ResourceUtil.getToolBarIcon(ActionIcon.PRINT));
    printButton.setToolTipText(Messages.getString("PrintToolBar.print"));
    printButton.addActionListener(e -> PrintToolManager.PrintStart());
    add(printButton);
  }
}
