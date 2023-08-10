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

import org.weasis.core.api.gui.Insertable;
import org.weasis.core.api.gui.Insertable.Type;
import org.weasis.core.api.gui.PreferencesPageFactory;
import org.weasis.core.api.gui.util.AbstractItemDialogPage;

import java.util.Hashtable;

/**
 * 表示将该类注册为实现 PreferencesPageFactory 接口的组件
 * 通过实现 PreferencesPageFactory 接口，提供了创建和管理设置页面的功能
 */
@org.osgi.service.component.annotations.Component(service = PreferencesPageFactory.class)

/**
 * 注册为 PreferencesPageFactory 服务的组件
 * 创建 LayoutPreView 的实例，并将其作为偏好设置页面显示 sle
 * 2023年5月15日14:35:07
 */
public class LayoutPrefFactory implements PreferencesPageFactory {

  /**
   * 创建 LayoutPreView 的实例，并返回它作为 AbstractItemDialogPage，以便将其作为设置页面显示
   * @param properties
   * @return
   */
  @Override
  public AbstractItemDialogPage createInstance(Hashtable<String, Object> properties) {
    return new LayoutPreView();
  }

  @Override
  public void dispose(Insertable component) {}

  @Override
  public boolean isComponentCreatedByThisFactory(Insertable component) {
    return component instanceof LayoutPreView;
  }

  @Override
  public Type getType() {
    return Type.PREFERENCES;
  }
}
