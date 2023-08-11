/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.gui.util;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.weasis.core.api.gui.Insertable;

public abstract class AbstractItemDialogPage extends JPanel implements PageItem, Insertable {

  private final String title;
  private final List<PageItem> subPageList = new ArrayList<>();
  private int pagePosition;

  private final Properties properties = new Properties();

  protected AbstractItemDialogPage(String title) {
    this(title, 1000);
  }

  protected AbstractItemDialogPage(String title, int pagePosition) {
    this.title = title == null ? "item" : title; // NON-NLS
    this.pagePosition = pagePosition;
    setBorder(
        GuiUtils.getEmptyBorder(BLOCK_SEPARATOR, ITEM_SEPARATOR_LARGE, 0, ITEM_SEPARATOR_LARGE));
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
  }

  public void deselectPageAction() {}

  public void selectPageAction() {}

  @Override
  public String getTitle() {
    return title;
  }

  public void addSubPage(PageItem subPage) {
    subPageList.add(subPage);
  }

  public void addSubPage(PageItem subPage, ActionListener actionListener, JComponent menuPanel) {
    // 设置页签添加循环，判断添加到哪一行，因为现在直接add进去顺序是有问题的，所以使用position的大小来控制位置 sle 2023年8月11日10:31:13
    int insertIndex = subPageList.size();
    int position = ((AbstractItemDialogPage) subPage).getComponentPosition();
    for (int i = 0; i < subPageList.size(); i++) {
      AbstractItemDialogPage item = (AbstractItemDialogPage) subPageList.get(i);
      if (item.getComponentPosition() > position) {
        insertIndex = i;
        break;
      }
    }
    subPageList.add(insertIndex, subPage);
    if (actionListener != null && menuPanel != null) {
      JButton button = new JButton();
      button.putClientProperty("JButton.buttonType", "roundRect");
      button.setText(subPage.getTitle());
      button.addActionListener(actionListener);
      menuPanel.add(button);
    }
  }

  public void removeSubPage(PageItem subPage) {
    subPageList.remove(subPage);
  }

  @Override
  public List<PageItem> getSubPages() {
    return new ArrayList<>(subPageList);
  }

  public void sortSubPages() {
    subPageList.sort(
        (o1, o2) -> {
          int val1 =
              o1 instanceof AbstractItemDialogPage page
                  ? page.getComponentPosition()
                  : Integer.MAX_VALUE;
          int val2 =
              o2 instanceof AbstractItemDialogPage page
                  ? page.getComponentPosition()
                  : Integer.MAX_VALUE;
          return Integer.compare(val1, val2);
        });
  }

  public void resetAllSubPagesToDefaultValues() {
    for (PageItem subPage : subPageList) {
      subPage.resetToDefaultValues();
    }
  }

  public Properties getProperties() {
    return properties;
  }

  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  public String getProperty(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }

  @Override
  public String toString() {
    return title;
  }

  @Override
  public Type getType() {
    return Insertable.Type.PREFERENCES;
  }

  @Override
  public String getComponentName() {
    return title;
  }

  @Override
  public boolean isComponentEnabled() {
    return isEnabled();
  }

  @Override
  public void setComponentEnabled(boolean enabled) {
    if (enabled != isComponentEnabled()) {
      setEnabled(enabled);
    }
  }

  @Override
  public int getComponentPosition() {
    return pagePosition;
  }

  @Override
  public void setComponentPosition(int position) {
    this.pagePosition = position;
  }

  public JComponent getMenuPanel() {
    return null;
  }

  /**
   * 写入xml文件 sle
   * 2023年6月2日14:49:05
   * @param doc  xml内容
   * @param file xml文件
   */
  protected void saveDocumentXml(Document doc, File file) {
    try {
      // 写入xml
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); // 启用安全处理特性
      Transformer transformer = transformerFactory.newTransformer();

      // 设置输出格式
      transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // 自动添加缩进
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); // 设置缩进的空格数

      // 创建StringWriter来保存XML内容
      StringWriter writer = new StringWriter();
      StreamResult result = new StreamResult(writer);
      DOMSource source = new DOMSource(doc);
      transformer.transform(source, result);

      String xmlContent = writer.toString().trim().replaceAll("\n\\s*\n", "\n"); // 获取XML内容并去除多余的空白行
      xmlContent = xmlContent.replaceAll("(<!--[^-]*-->)(\\S)", "$1\n$2"); // 将注释的元素单独一行显示

      // 将XML内容写入文件
      try (PrintWriter printWriter = new PrintWriter(file.getPath())) {
        printWriter.print(xmlContent);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
