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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Rectangle;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.UIManager;
import org.dcm4che3.data.Tag;
import org.dcm4che3.img.data.PrDicomObject;
import org.joml.Vector3d;
import org.weasis.core.api.explorer.model.TreeModelNode;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.gui.util.DecFormatter;
import org.weasis.core.api.gui.util.Filter;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.core.api.gui.util.GuiUtils.IconColor;
import org.weasis.core.api.image.OpManager;
import org.weasis.core.api.image.WindowOp;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.core.api.media.data.Series;
import org.weasis.core.api.media.data.TagReadable;
import org.weasis.core.api.media.data.TagView;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.util.FontTools;
import org.weasis.core.ui.editor.image.SynchData;
import org.weasis.core.ui.editor.image.ViewButton;
import org.weasis.core.ui.editor.image.ViewCanvas;
import org.weasis.core.ui.model.layer.AbstractInfoLayer;
import org.weasis.core.ui.model.layer.LayerAnnotation;
import org.weasis.core.ui.model.layer.LayerItem;
import org.weasis.core.util.LangUtil;
import org.weasis.core.util.StringUtil;
import org.weasis.core.util.StringUtil.Suffix;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.codec.DicomSeries;
import org.weasis.dicom.codec.RejectedKOSpecialElement;
import org.weasis.dicom.codec.TagD;
import org.weasis.dicom.codec.display.CornerDisplay;
import org.weasis.dicom.codec.display.CornerInfoData;
import org.weasis.dicom.codec.display.Modality;
import org.weasis.dicom.codec.display.ModalityInfoData;
import org.weasis.dicom.codec.display.ModalityView;
import org.weasis.dicom.codec.geometry.ImageOrientation;
import org.weasis.dicom.codec.geometry.ImageOrientation.Plan;
import org.weasis.dicom.codec.geometry.PatientOrientation.Biped;
import org.weasis.dicom.codec.geometry.VectorUtils;
import org.weasis.dicom.explorer.DicomModel;
import org.weasis.dicom.viewer2d.mpr.MprView;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.lut.DefaultWlPresentation;

/**
 * The Class InfoLayer.
 *
 * @author Nicolas Roduit
 */
public class InfoLayer extends AbstractInfoLayer<DicomImageElement> {

  public InfoLayer(ViewCanvas<DicomImageElement> view2DPane) {
    this(view2DPane, true);
  }

  public InfoLayer(ViewCanvas<DicomImageElement> view2DPane, boolean useGlobalPreferences) {
    super(view2DPane, useGlobalPreferences);
    displayPreferences.put(LayerItem.ANNOTATIONS, true);
    displayPreferences.put(LayerItem.MIN_ANNOTATIONS, false);
    displayPreferences.put(LayerItem.ANONYM_ANNOTATIONS, false);
    displayPreferences.put(LayerItem.SCALE, true);
    displayPreferences.put(LayerItem.LUT, false);
    displayPreferences.put(LayerItem.IMAGE_ORIENTATION, true);
    displayPreferences.put(LayerItem.WINDOW_LEVEL, true);
    displayPreferences.put(LayerItem.ZOOM, true);
    displayPreferences.put(LayerItem.ROTATION, false);
    displayPreferences.put(LayerItem.FRAME, true);
    displayPreferences.put(LayerItem.PIXEL, true);
    displayPreferences.put(LayerItem.PRELOADING_BAR, true);
  }

  @Override
  public LayerAnnotation getLayerCopy(ViewCanvas view2DPane, boolean useGlobalPreferences) {
    InfoLayer layer = new InfoLayer(view2DPane, useGlobalPreferences);
    Map<LayerItem, Boolean> prefMap = layer.displayPreferences;
    setLayerValue(prefMap, LayerItem.ANNOTATIONS);
    setLayerValue(prefMap, LayerItem.ANONYM_ANNOTATIONS);
    setLayerValue(prefMap, LayerItem.IMAGE_ORIENTATION);
    setLayerValue(prefMap, LayerItem.SCALE);
    setLayerValue(prefMap, LayerItem.LUT);
    setLayerValue(prefMap, LayerItem.PIXEL);
    setLayerValue(prefMap, LayerItem.WINDOW_LEVEL);
    setLayerValue(prefMap, LayerItem.ZOOM);
    setLayerValue(prefMap, LayerItem.ROTATION);
    setLayerValue(prefMap, LayerItem.FRAME);
    setLayerValue(prefMap, LayerItem.PRELOADING_BAR);
    setLayerValue(prefMap, LayerItem.MIN_ANNOTATIONS);
    return layer;
  }

  /**
   * 画板渲染 sle
   * 2023年5月5日18:27:23
   * @param g2
   */
  @Override
  public void paint(Graphics2D g2) {
    DicomImageElement image = view2DPane.getImage(); // 影像图片
    FontMetrics fontMetrics = g2.getFontMetrics(); // 字体
    final Rectangle bound = view2DPane.getJComponent().getBounds(); // 当前块大小
    int minSize = fontMetrics.stringWidth(Messages.getString("InfoLayer.msg_outside_levels")) * 2;
    if (!visible || image == null || minSize > bound.width || minSize > bound.height) {
      return;
    }

    Object[] oldRenderingHints =
        GuiUtils.setRenderingHints(g2, true, false, view2DPane.requiredTextAntialiasing());

    OpManager disOp = view2DPane.getDisplayOpManager(); // 当前操作名称
    Modality mod =
        Modality.getModality(TagD.getTagValue(view2DPane.getSeries(), Tag.Modality, String.class)); // 检查类型
    ModalityInfoData modality = ModalityView.getModlatityInfos(mod); // 获取检查类型的四角信息

    final float minY = border; // 最上方Y轴
    final float midY = bound.height / 2f;// 竖轴的中间位置
    final float maxY = bound.height - border; // 最下方Y轴
    final float minX = border; // 起始宽度
    final float midX = bound.width / 2f;// 横轴的中间位置
    final float maxX = bound.width - border; // 结束宽度
    final Color errorColor = IconColor.ACTIONS_RED.getColor(); // 错误字体颜色
    final int fontHeight = fontMetrics.getHeight();// 字体高度
    final int midFontHeight = fontHeight - fontMetrics.getDescent();// 最小字体高度
    thickLength = Math.max(fontHeight, GuiUtils.getScaleLength(5.0));// 比例尺的宽度

    float drawY ; // -1.5 for outline 接下来要控制的元素高度
    float drawX ;// 截下来要控制的元素宽度

    g2.setPaint(Color.BLACK);

    boolean fullAnnotations = !getDisplayPreferences(LayerItem.MIN_ANNOTATIONS);// 完整批注(未勾选精简批注)

    // 读取影像失败时的显示
    if (!image.isReadable()) {
      paintNotReadable(g2, image, midX, midY, fontHeight);
    }

    // 读取影像成功时，并且源影像不为空时的渲染内容
    if (image.isReadable() && view2DPane.getSourceImage() != null) {
      // 比例
      if (getDisplayPreferences(LayerItem.SCALE)) {
        PlanarImage source = image.getImage(); // 源影像
        if (source != null) {
          ImageProperties props =
              new ImageProperties(
                  source.width(),// 源影像宽
                  source.height(),// 源影像高
                  image.getPixelSize(),// 像素大小
                  image.getRescaleX(),// x轴缩放比例
                  image.getRescaleY(),// y轴缩放比例
                  image.getPixelSpacingUnit(),// 长度单位
                  image.getPixelSizeCalibrationDescription());// 像素尺寸校准描述
          drawScale(g2, bound, fontHeight, props);// 设置当前块的比例
        }
      }
      // LUT
      if (getDisplayPreferences(LayerItem.LUT) && fullAnnotations) {
        drawLUT(g2, bound, midFontHeight);// 设置当前块的LUT
      }
    }

    // 左下角
    drawY = maxY - GuiUtils.getScaleLength(1.5f) - fontHeight;// 左下角的高度为底部高度，并且减去一行文本高度

    // 有损压缩的情况
    drawY = checkAndPaintLossyImage(g2, image, drawY, fontHeight, border);

    // 暂时不知道是干什么的
    Integer frame = TagD.getTagValue(image, Tag.InstanceNumber, Integer.class);
    RejectedKOSpecialElement koElement =
        DicomModel.getRejectionKoSpecialElement(
            view2DPane.getSeries(),
            TagD.getTagValue(image, Tag.SOPInstanceUID, String.class),
            frame);

    if (koElement != null) {
      String message = "Not a valid image: " + koElement.getDocumentTitle(); // NON-NLS
      drawX = midX - g2.getFontMetrics().stringWidth(message) / 2F;
      FontTools.paintColorFontOutline(g2, message, drawX, midY, errorColor); // 块中央
    }

    // 像素
    if (getDisplayPreferences(LayerItem.PIXEL) && fullAnnotations) {
      StringBuilder sb = new StringBuilder(Messages.getString("InfoLayer.pixel"));
      sb.append(StringUtil.COLON_AND_SPACE);
      if (pixelInfo != null) {
        sb.append(pixelInfo.getPixelValueText());
        sb.append(" - ");
        sb.append(pixelInfo.getPixelPositionText());
      }
      String str = sb.toString();
      FontTools.paintFontOutline(g2, str, minX, drawY); // 块左下方
      drawX = fontMetrics.stringWidth(str) + GuiUtils.getScaleLength(2);
      drawY -= fontHeight;
      pixelInfoBound.setBounds((int) minX, (int) drawY + fontMetrics.getDescent(), (int) drawX , fontHeight);
    }
    // 窗宽窗位（预设）
    if (getDisplayPreferences(LayerItem.WINDOW_LEVEL) && fullAnnotations) {
      StringBuilder sb = new StringBuilder();
      Number window = (Number) disOp.getParamValue(WindowOp.OP_NAME, ActionW.WINDOW.cmd()); // 窗宽
      Number level = (Number) disOp.getParamValue(WindowOp.OP_NAME, ActionW.LEVEL.cmd()); // 窗位
      boolean outside = false; // 是否是图像光谱之外的值
      if (window != null && level != null) {
        sb.append(ActionW.WINLEVEL.getTitle());
        sb.append(StringUtil.COLON_AND_SPACE);
        sb.append(DecFormatter.allNumber(window));
        sb.append("/");
        sb.append(DecFormatter.allNumber(level));

        PrDicomObject prDicomObject =
            PRManager.getPrDicomObject(view2DPane.getActionValue(ActionW.PR_STATE.cmd()));
        boolean pixelPadding =
            (Boolean) disOp.getParamValue(WindowOp.OP_NAME, ActionW.IMAGE_PIX_PADDING.cmd());
        DefaultWlPresentation wlp = new DefaultWlPresentation(prDicomObject, pixelPadding);
        double minModLUT = image.getMinValue(wlp);
        double maxModLUT = image.getMaxValue(wlp);
        double minp = level.doubleValue() - window.doubleValue() / 2.0;
        double maxp = level.doubleValue() + window.doubleValue() / 2.0;
        // 判断是否超出了光谱
        if (minp > maxModLUT || maxp < minModLUT) {
          outside = true;
          sb.append(" - ");
          sb.append(Messages.getString("InfoLayer.msg_outside_levels"));
        }
      }
      // 如果超出图像光谱，则渲染为error红色语句
      if (outside) {
        FontTools.paintColorFontOutline(g2, sb.toString(), minX, drawY, errorColor);// 块左下方
      } else {
        FontTools.paintFontOutline(g2, sb.toString(), minX, drawY);// 块左下方
      }
      drawY -= fontHeight;
    }
    // 缩放
    if (getDisplayPreferences(LayerItem.ZOOM) && fullAnnotations) {
      FontTools.paintFontOutline(
          g2,
          Messages.getString("InfoLayer.zoom")
              + StringUtil.COLON_AND_SPACE
              + DecFormatter.percentTwoDecimal(view2DPane.getViewModel().getViewScale()),
              minX,
          drawY);// 块左下方
      drawY -= fontHeight;
    }
    // 角度
    if (getDisplayPreferences(LayerItem.ROTATION) && fullAnnotations) {
      FontTools.paintFontOutline(
          g2,
          Messages.getString("InfoLayer.angle")
              + StringUtil.COLON_AND_SPACE
              + view2DPane.getActionValue(ActionW.ROTATION.cmd())
              + " °",
              minX,
          drawY);// 块左下方
      drawY -= fontHeight;
    }
    // 帧号
    if (getDisplayPreferences(LayerItem.FRAME) && fullAnnotations) {
      StringBuilder buf = new StringBuilder(Messages.getString("InfoLayer.frame"));
      buf.append(StringUtil.COLON_AND_SPACE);
      Integer inst = TagD.getTagValue(image, Tag.InstanceNumber, Integer.class); // 当前帧号
      if (inst != null) {
        buf.append("[");
        buf.append(inst);
        buf.append("] ");
      }
      buf.append(view2DPane.getFrameIndex() + 1);
      buf.append(" / ");
      buf.append(
          view2DPane
              .getSeries()
              .size(
                  (Filter<DicomImageElement>)
                      view2DPane.getActionValue(ActionW.FILTERED_SERIES.cmd()))); // 总帧数
      FontTools.paintFontOutline(g2, buf.toString(), minX, drawY); // 块左下方
      drawY -= fontHeight;

      Double imgProgression = (Double) view2DPane.getActionValue(ActionW.PROGRESSION.cmd()); // 暂时不知道是干什么的
      if (imgProgression != null) {
        int inset = GuiUtils.getScaleLength(13);
        drawY -= inset;
        int pColor = (int) (510 * imgProgression);
        g2.setPaint(new Color(Math.min(510 - pColor, 255), Math.min(pColor, 255), 0));
        g2.fillOval((int) minX, (int) drawY, inset, inset);
      }
    }
    
    Point2D.Float[] positions = new Point2D.Float[4]; // 遵循左上[0]，右上[1]，右下[2]，左下[3]的原则
    Series series = (Series) view2DPane.getSeries();
    MediaSeriesGroup study = getParent(series, DicomModel.study);
    MediaSeriesGroup patient = getParent(series, DicomModel.patient);
    CornerInfoData corner;
    TagView[] infos;

    corner = modality.getCornerInfo(CornerDisplay.BOTTOM_LEFT);// 获取左下角方位
    infos = corner.getInfos();// 获取需要渲染的四角信息
    Boolean leftAnnotations = infos.length > 0 && fullAnnotations; // 是否渲染左下角批注(存在左下角的自定义批注并且渲染完整批注)
    // 如果不渲染左下角批注的话，则需要添加position
    if (!leftAnnotations){
      positions[3] = new Point2D.Float(minX, drawY - GuiUtils.getScaleLength(5));
    }

    // 批注
    if (getDisplayPreferences(LayerItem.ANNOTATIONS)) {
      boolean anonymize = getDisplayPreferences(LayerItem.ANONYM_ANNOTATIONS);
      
      // 如果左下角存在自定义批注，则渲染左下角的
      if (leftAnnotations) {
        drawY -= fontHeight;
        TagView[] orderDescInfos = OrderDesc(infos);
        for (TagView info : orderDescInfos) {
          if (info != null) {
            for (TagW tag : info.getTag()) {
              if (!anonymize || tag.getAnonymizationType() != 1) {
                Object value = getTagValue(tag, patient, study, series, image);
                if (value != null) {
                  String str = tag.getFormattedTagValue(value, info.getFormat());
                  if (StringUtil.hasText(str)) {
                    FontTools.paintFontOutline(g2, str, minX, drawY);// 块左下方
                    drawY -= fontHeight;
                  }
                  break;
                }
              }
            }
          }
        }
        //并且渲染完后需要添加到position中
        positions[3] = new Point2D.Float(minX, drawY - GuiUtils.getScaleLength(5));
      }

      // 左上角
      drawY = fontHeight; // 左上角的Y轴为最高处开始，也就是从0开始，默认加一行文本高度，然后每渲染一行加一行文本高度
      corner = modality.getCornerInfo(CornerDisplay.TOP_LEFT);// 获取方位
      infos = corner.getInfos(); // 获取需要渲染的四角信息
      for (TagView tagView : infos) {
        if (tagView != null && (fullAnnotations || tagView.containsTag(TagD.get(Tag.PatientName)))) {
          for (TagW tag : tagView.getTag()) {
            if (!anonymize || tag.getAnonymizationType() != 1) {
              Object value = getTagValue(tag, patient, study, series, image);
              if (value != null) {
                String str = tag.getFormattedTagValue(value, tagView.getFormat());
                if (StringUtil.hasText(str)) {
                  FontTools.paintFontOutline(g2, str, minX, drawY); // 块左上方
                  drawY += fontHeight;
                }
                break;
              }
            }
          }
        }
      }
      positions[0] = new Point2D.Float(minX, drawY - fontHeight + GuiUtils.getScaleLength(5));

      // 右上角
      drawY = fontHeight;// 右上角的Y轴为最高处开始，也就是从0开始，默认加一行文本高度，然后每渲染一行加一行文本高度
      corner = modality.getCornerInfo(CornerDisplay.TOP_RIGHT);// 获取方位
      infos = corner.getInfos();// 获取需要渲染的四角信息
      for (TagView info : infos) {
        if (info != null) {
          if (fullAnnotations || info.containsTag(TagD.get(Tag.SeriesDate))) {
            Object value;
            for (TagW tag : info.getTag()) {
              if (!anonymize || tag.getAnonymizationType() != 1) {
                value = getTagValue(tag, patient, study, series, image);
                if (value != null) {
                  String str = tag.getFormattedTagValue(value, info.getFormat());
                  if (StringUtil.hasText(str)) {
                    drawX = maxX - g2.getFontMetrics().stringWidth(str);
                    FontTools.paintFontOutline( g2, str,  drawX, drawY); // 块右上方
                    drawY += fontHeight;
                  }
                  break;
                }
              }
            }
          }
        }
      }
      positions[1] = new Point2D.Float(maxX, drawY - fontHeight + GuiUtils.getScaleLength(5));

      // 右下角
      drawY = maxY  - GuiUtils.getScaleLength(1.5f); // 右下角高度为底部高度
      if (fullAnnotations) {
        corner = modality.getCornerInfo(CornerDisplay.BOTTOM_RIGHT);// 获取方位
        infos = corner.getInfos();// 获取需要渲染的四角信息
        TagView[] orderDescInfos = OrderDesc(infos);
        for (TagView info : orderDescInfos) {
          if (info != null) {
            for (TagW tag : info.getTag()) {
              // 匿名化判断
              if (!anonymize || tag.getAnonymizationType() != 1) {
                Object value = getTagValue(tag, patient, study, series, image);
                if (value != null) {
                  String str = tag.getFormattedTagValue(value, info.getFormat());
                  if (StringUtil.hasText(str)) {
                    drawX = maxX - g2.getFontMetrics().stringWidth(str);
                    FontTools.paintFontOutline(g2, str, drawX, drawY);// 块右下方
                    drawY -= fontHeight;
                  }
                  break;
                }
              }
            }
          }
        }
        drawY -= 5;
        drawSeriesInMemoryState(g2, view2DPane.getSeries(), (int) maxX, (int) drawY);
      }
      positions[2] = new Point2D.Float(maxX, drawY - GuiUtils.getScaleLength(5));

      // Boolean synchLink = (Boolean) view2DPane.getActionValue(ActionW.SYNCH_LINK);
      // String str = synchLink != null && synchLink ? "linked" : "unlinked"; // NON-NLS
      // paintFontOutline(g2, str, bound.width - g2.getFontMetrics().stringWidth(str) - BORDER,
      // drawY);

      Integer columns = TagD.getTagValue(image, Tag.Columns, Integer.class);// 竖向像素尺寸
      Integer rows = TagD.getTagValue(image, Tag.Rows, Integer.class);// 横向像素尺寸
      StringBuilder orientation = new StringBuilder(mod.name());// 检查类型
      Plan plan = null;//图像处理方式（影像是呈现x-y还是x-z平面方式展现）
      // 图像尺寸的文本
      if (rows != null && columns != null) {
        orientation.append(" (");
        orientation.append(columns);
        orientation.append("x"); // NON-NLS
        orientation.append(rows);
        orientation.append(")");
      }

      // 定向
      String colLeft = null;// 左边显示的方位
      String rowTop = null; // 上方显示的方位
      boolean quadruped =
          "QUADRUPED"
              .equalsIgnoreCase(
                  TagD.getTagValue(series, Tag.AnatomicalOrientationType, String.class));
      Vector3d vr = ImageOrientation.getRowImagePosition(image); // 获取行方向的位置信息
      Vector3d vc = ImageOrientation.getColumnImagePosition(image); // 获取列方向的位置信息
      Integer rotationAngle = (Integer) view2DPane.getActionValue(ActionW.ROTATION.cmd()); // 旋转的角度
      if (getDisplayPreferences(LayerItem.IMAGE_ORIENTATION) && vr != null && vc != null) {
        orientation.append(" - ");
        plan = ImageOrientation.getPlan(vr, vc);
        orientation.append(plan);
        orientation.append(StringUtil.SPACE);

        // Set the opposite vector direction (otherwise label should be placed in mid-right and
        // mid-bottom

        // 当前影像旋转后的逻辑
        if (rotationAngle != null && rotationAngle != 0) {
          double rad = Math.toRadians(rotationAngle); // 将角度转化为弧度
          Vector3d normal = VectorUtils.computeNormalOfSurface(vr, vc);
          vr.negate();
          vr.rotateAxis(-rad, normal.x, normal.y, normal.z);
          vc.negate();
          vc.rotateAxis(-rad, normal.x, normal.y, normal.z);
        }
        // 当前影像未旋转
        else {
          vr.negate();
          vc.negate();
        }
        // 是否水平翻转
        if (LangUtil.getNULLtoFalse((Boolean) view2DPane.getActionValue((ActionW.FLIP.cmd())))) {
          vr.negate();
        }

        colLeft = ImageOrientation.getOrientation(vr, quadruped);
        rowTop = ImageOrientation.getOrientation(vc, quadruped);

      } else {
        String[] po = TagD.getTagValue(image, Tag.PatientOrientation, String[].class);// 用于描述图像第一行和第一列相对于病人的方向。在DICOM坐标系中，X轴正向指向病人的左侧，Y轴正向指向病人的背部，Z轴正向指向病人的头部
        // 影像中如果有方向，并且有旋转角度
        if (po != null && po.length == 2 && (rotationAngle == null || rotationAngle == 0)) {
          // Do not display if there is a transformation
          // 是否水平翻转
          if (LangUtil.getNULLtoFalse((Boolean) view2DPane.getActionValue((ActionW.FLIP.cmd())))) {
            colLeft = po[0];
          } else {
            StringBuilder buf = new StringBuilder();
            for (String s : po[0].split("(?=\\p{Upper})")) { // NON-NLS
              buf.append(ImageOrientation.getImageOrientationOpposite(s, quadruped));
            }
            colLeft = buf.toString();
          }
          StringBuilder buf = new StringBuilder();
          for (String s : po[1].split("(?=\\p{Upper})")) { // NON-NLS
            buf.append(ImageOrientation.getImageOrientationOpposite(s, quadruped));
          }
          rowTop = buf.toString();
        }
      }
      if (rowTop != null && colLeft != null) {
        String[] left = colLeft.split(StringUtil.SPACE);// 当有旋转或者其他定向时，会有小角标，表示当前旋转到哪个方向
        String[] top = rowTop.split(StringUtil.SPACE);// 当有旋转或者其他定向时，会有小角标，表示当前旋转到哪个方向

        Font oldFont = g2.getFont();// 原先的显示字体
        Font bigFont = oldFont.deriveFont(oldFont.getSize() + 5.0f);// 定向的显示字体
        g2.setFont(bigFont);// 设置定向的显示字体
        Map<TextAttribute, Object> map = new HashMap<>(1);
        map.put(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB);
        Font subscriptFont = bigFont.deriveFont(map);

        // 上方定向标识
        String bigLetter = top.length > 0 && top[0].length() > 0 ? top[0] : StringUtil.SPACE;// 上方的定向
        int shiftX = g2.getFontMetrics().stringWidth(bigLetter);// 文本在X轴的尺寸
        int shiftY = fontHeight + GuiUtils.getScaleLength(5);// 文本在Y轴的尺寸
        FontTools.paintColorFontOutline(g2, bigLetter, midX - shiftX, shiftY, highlight);// 块中上方
        // 如果有小角标
        if (top.length > 1) {
          g2.setFont(subscriptFont); // 设置重定向小角标的显示字体
          FontTools.paintColorFontOutline(
              g2,
              String.join("-", Arrays.copyOfRange(top, 1, top.length)),
              midX,
              shiftY,
              highlight); // 块中上方，根据字体大小和x周的位置，最终会显示在定向字体的右下方
          g2.setFont(bigFont);// 设置回定向显示字体
        }

        // 左侧定向标识
        bigLetter = left.length > 0 && left[0].length() > 0 ? left[0] : StringUtil.SPACE; // 左侧的定向
        FontTools.paintColorFontOutline(g2, bigLetter, (float) (minX + thickLength), midY + fontHeight / 2.0f, highlight); // 块中左方，并且排在比例尺之后所以需要添加比例尺的宽度
        // 如果有小角标
        if (left.length > 1) {
          shiftX = g2.getFontMetrics().stringWidth(bigLetter); // 文本在X轴的尺寸
          g2.setFont(subscriptFont); // 设置重定向小角标的显示字体
          FontTools.paintColorFontOutline(
              g2,
              String.join("-", Arrays.copyOfRange(left, 1, left.length)),
              (float) (minX + thickLength + shiftX),
              midY + fontHeight / 2.0f,
              highlight);// 块中左方，并且排在比例尺之后所以需要添加比例尺的宽度，根据字体大小和x周的位置，最终会显示在定向字体的右下方
        }
        g2.setFont(oldFont);// 设置回原本的字体大小
      }

      float offsetY = bound.height - border - GuiUtils.getScaleLength(1.5f); // -1.5 for outline
      FontTools.paintFontOutline(g2, orientation.toString(), minX, offsetY); // 块左下方
      // 如果有图像处理方式
      if (plan != null) {
        // 如果正在进行mpr，在定向后面增加对应颜色的小方块
        if (view2DPane instanceof MprView) {
          Color planColor = null;
          if (Plan.AXIAL.equals(plan)) {
            planColor = Biped.F.getColor();
          } else if (Plan.CORONAL.equals(plan)) {
            planColor = Biped.A.getColor();
          } else if (Plan.SAGITTAL.equals(plan)) {
            planColor = Biped.L.getColor();
          }

          int shiftX = g2.getFontMetrics().stringWidth(orientation.toString()); // 获取定向文本的宽度，也就是小方块的x轴位置
          g2.setColor(planColor); // 设置接下来要设置的颜色
          int size = midFontHeight - fontMetrics.getDescent(); // 获取定向文本后面小方块的尺寸
          int shiftY =(int) maxY - size; // 显示在最下面，并且减去小方块的高度
          g2.fillRect((int) minX + shiftX, shiftY, size - 1, size - 1); // 添加在左下方
        }
      }
    } else {
      positions[0] = new Point2D.Float(minX, minY);
      positions[1] = new Point2D.Float(maxX, minY);
      positions[2] = new Point2D.Float(maxX, maxY);
    }

    drawExtendedActions(g2, positions);
    GuiUtils.resetRenderingHints(g2, oldRenderingHints);
  }

  public static MediaSeriesGroup getParent(
      MediaSeries<DicomImageElement> series, TreeModelNode node) {
    if (series != null) {
      Object tagValue = series.getTagValue(TagW.ExplorerModel);
      if (tagValue instanceof DicomModel model) {
        return model.getParent(series, node);
      }
    }
    return null;
  }

  /**
   * 创建一个倒叙的数组，左下角和右下角用，因为左下角和右下角是从下往上渲染的
   * sle 2023年7月7日16:22:17
   * @param arr
   * @return
   */
  private static TagView[] OrderDesc(TagView[] arr){
    // 计算有效值的个数
    int n = 0;
    for (TagView tagView : arr) {
      if (tagView != null) {
        n++;
      }
    }

    // 创建一个新数组来存储反转后的结果
    TagView[] newArr = new TagView[n];
    for (int i = 0; i < n; i++) {
      newArr[i] = arr[n - i - 1];
    }
    return newArr;
  }

  /**
   * 读取影像文件失败的情况 sle 添加注释
   * 2023年8月11日14:01:58
   * @param g2
   * @param image
   * @param midX
   * @param midY
   * @param fontHeight
   */
  public static void paintNotReadable(Graphics2D g2, DicomImageElement image, float midX, float midY, float fontHeight) {
    String message = Messages.getString("InfoLayer.msg_not_read");
    final Color errorColor = IconColor.ACTIONS_RED.getColor(); // 错误字体颜色
    float drawX = midX - g2.getFontMetrics().stringWidth(message) / 2.0F;
    float drawY = midY;// 起始位置在中间
    FontTools.paintColorFontOutline(g2, message, drawX, drawY, errorColor);

    if (image != null) {
      String tsuid = TagD.getTagValue(image, Tag.TransferSyntaxUID, String.class); // 传输语法UID
      // 如果存在传输语法UID，则显示出来
      if (StringUtil.hasText(tsuid)) {
        tsuid = Messages.getString("InfoLayer.tsuid") + StringUtil.COLON_AND_SPACE + tsuid;
        drawX = midX - g2.getFontMetrics().stringWidth(tsuid) / 2.0F;
        drawY += fontHeight; // 重启一行
        FontTools.paintColorFontOutline(g2, tsuid, drawX, drawY, errorColor);
      }

      String[] desc = image.getMediaReader().getReaderDescription(); // 应该是影像的一些描述信息
      // 如果存在的就依次往下一行显示
      if (desc != null) {
        for (String str : desc) {
          if (StringUtil.hasText(str)) {
            drawX = midX - g2.getFontMetrics().stringWidth(str) / 2F;
            drawY += fontHeight;// 重启一行
            FontTools.paintColorFontOutline(g2, str, drawX, drawY, errorColor); // 块中央、上一行文本的下一行
          }
        }
      }
    }
  }

  /**
   * 有损压缩的情况 sle
   * 2023年8月11日14:12:54
   * @param g2d
   * @param taggable
   * @param drawY
   * @param fontHeight
   * @param border
   * @return
   */
  public static float checkAndPaintLossyImage(Graphics2D g2d, TagReadable taggable, float drawY, float fontHeight, int border) {
    /*
     * IHE BIR RAD TF-­‐2: 4.16.4.2.2.5.8
     *
     * Whether lossy compression has been applied, derived from Lossy Image 990 Compression (0028,2110),
     * and if so, the value of Lossy Image Compression Ratio (0028,2112) and Lossy Image Compression Method
     * (0028,2114), if present (as per FDA Guidance for the Submission Of Premarket Notifications for Medical
     * Image Management Devices, July 27, 2000).
     */
    if ("01".equals(TagD.getTagValue(taggable, Tag.LossyImageCompression))) {
      double[] rates = TagD.getTagValue(taggable, Tag.LossyImageCompressionRatio, double[].class);// 获取有损压缩后的压缩比
      StringBuilder buf = new StringBuilder(Messages.getString("InfoLayer.lossy"));
      buf.append(StringUtil.COLON_AND_SPACE);
      if (rates != null && rates.length > 0) {
        for (int i = 0; i < rates.length; i++) {
          if (i > 0) {
            buf.append(",");
          }
          buf.append(" [");
          buf.append(Math.round(rates[i]));
          buf.append(":1");
          buf.append(']');
        }
      } else {
        String val = TagD.getTagValue(taggable, Tag.DerivationDescription, String.class);
        if (val != null) {
          buf.append(StringUtil.getTruncatedString(val, 25, Suffix.THREE_PTS));
        }
      }

      FontTools.paintColorFontOutline(g2d, buf.toString(), border, drawY, IconColor.ACTIONS_RED.getColor());
      drawY -= fontHeight; // 高度再减一行，因为这这里已经渲染了一行了
    }
    return drawY;
  }

  private void drawSeriesInMemoryState(
      Graphics2D g2d, MediaSeries<DicomImageElement> series, int x, int y) {
    if (getDisplayPreferences(LayerItem.PRELOADING_BAR)
        && series instanceof DicomSeries dicomSeries) {
      boolean[] list = dicomSeries.getImageInMemoryList();
      int maxLength = GuiUtils.getScaleLength(120);
      int height = GuiUtils.getScaleLength(5);
      int length = Math.min(list.length, maxLength);
      x -= length;
      preloadingProgressBound.setBounds(x - 1, y - 1, length + 1, height + 1);
      g2d.fillRect(x, y, length, height);
      g2d.setPaint(Color.BLACK);
      g2d.draw(preloadingProgressBound);
      double factorResize = list.length > maxLength ? (double) maxLength / list.length : 1;
      for (int i = 0; i < list.length; i++) {
        if (!list[i]) {
          int val = x + (int) (i * factorResize);
          g2d.fillRect(x, y, val, height);
        }
      }
    }
  }

  private Object getTagValue(
      TagW tag,
      MediaSeriesGroup patient,
      MediaSeriesGroup study,
      MediaSeries<DicomImageElement> series,
      ImageElement image) {
    if (image.containTagKey(tag)) {
      return image.getTagValue(tag);
    }
    if (series.containTagKey(tag)) {
      return series.getTagValue(tag);
    }
    if (study != null && study.containTagKey(tag)) {
      return study.getTagValue(tag);
    }
    if (patient != null && patient.containTagKey(tag)) {
      return patient.getTagValue(tag);
    }
    return null;
  }

  protected void drawExtendedActions(Graphics2D g2d, Point2D.Float[] positions) {
    if (!view2DPane.getViewButtons().isEmpty()) {
      int space = GuiUtils.getScaleLength(5);
      int height = 0;
      for (ViewButton b : view2DPane.getViewButtons()) {
        if (b.isVisible() && b.getPosition() == GridBagConstraints.EAST) {
          height += b.getIcon().getIconHeight() + space;
        }
      }

      Point2D.Float midy =
          new Point2D.Float(
              positions[1].x,
              (float) (view2DPane.getJComponent().getHeight() * 0.5 - (height - space) * 0.5));
      SynchData synchData = (SynchData) view2DPane.getActionValue(ActionW.SYNCH_LINK.cmd());
      boolean tile = synchData != null && SynchData.Mode.TILE.equals(synchData.getMode());
      for (ViewButton b : view2DPane.getViewButtons()) {
        if (b.isVisible() && !(tile && ActionW.KO_SELECTION.getTitle().equals(b.getName()))) {
          Icon icon = b.getIcon();
          int p = b.getPosition();

          if (p == GridBagConstraints.EAST) {
            b.x = midy.x - icon.getIconWidth();
            b.y = midy.y;
            midy.y += icon.getIconHeight() + space;
          } else if (p == GridBagConstraints.NORTHEAST) {
            b.x = positions[1].x - icon.getIconWidth();
            b.y = positions[1].y;
            positions[1].x -= icon.getIconWidth() + space;
          } else if (p == GridBagConstraints.SOUTHEAST) {
            b.x = positions[2].x - icon.getIconWidth();
            b.y = positions[2].y - icon.getIconHeight();
            positions[2].x -= icon.getIconWidth() + space;
          } else if (p == GridBagConstraints.NORTHWEST) {
            b.x = positions[0].x;
            b.y = positions[0].y;
            positions[0].x += icon.getIconWidth() + space;
          } else if (p == GridBagConstraints.SOUTHWEST) {
            b.x = positions[3].x;
            b.y = positions[3].y - icon.getIconHeight();
            positions[3].x += icon.getIconWidth() + space;
          }

          Color oldColor = g2d.getColor();
          Color bck;
          if (b.isHover()) {
            bck = UIManager.getColor("Button.hoverBackground");
          } else {
            bck = UIManager.getColor("Button.background");
          }
          g2d.setColor(bck);
          g2d.fillRoundRect(
              (int) b.x - 3,
              (int) b.y - 3,
              icon.getIconWidth() + 7,
              icon.getIconHeight() + 7,
              7,
              7);
          icon.paintIcon(view2DPane.getJComponent(), g2d, (int) b.x, (int) b.y);
          g2d.setColor(oldColor);
        }
      }
    }
  }
}
