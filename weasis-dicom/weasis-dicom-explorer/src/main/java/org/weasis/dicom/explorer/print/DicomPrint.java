/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.explorer.print;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DimseRSP;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.util.UIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.image.AffineTransformOp;
import org.weasis.core.api.image.LayoutConstraints;
import org.weasis.core.api.image.ZoomOp.Interpolation;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.api.service.BundleTools;
import org.weasis.core.ui.editor.image.ExportImage;
import org.weasis.core.ui.util.ExportLayout;
import org.weasis.core.ui.util.ImagePrint;
import org.weasis.core.ui.util.PrintOptions;
import org.weasis.core.util.MathUtil;
import org.weasis.dicom.explorer.pref.node.DicomPrintNode;
import org.weasis.dicom.explorer.print.DicomPrintDialog.FilmSize;
import org.weasis.opencv.data.PlanarImage;

public class DicomPrint {
  private static final Logger LOGGER = LoggerFactory.getLogger(DicomPrint.class);

  private final DicomPrintNode dcmNode;
  private final DicomPrintOptions printOptions;
  private Interpolation interpolation;
  private double placeholderX;
  private double placeholderY;

  private int lastx;
  private double lastwx;
  private double[] lastwy;
  private double wx;

  public DicomPrint(DicomPrintNode dicomPrintNode, DicomPrintOptions printOptions) {
    if (dicomPrintNode == null) {
      throw new IllegalArgumentException();
    }
    this.dcmNode = dicomPrintNode;
    this.printOptions = printOptions == null ? dicomPrintNode.getPrintOptions() : printOptions;
  }

  public BufferedImage printImage(ExportLayout<? extends ImageElement> layout) {
    if (layout == null) {
      return null;
    }

    BufferedImage bufferedImage = initialize(layout);
    Graphics2D g2d = (Graphics2D) bufferedImage.getGraphics();

    if (g2d != null) {
      Color borderColor =
              "WHITE".equals(printOptions.getBorderDensity(true)) ? Color.WHITE : Color.BLACK;
      Color background = "WHITE".equals(printOptions.getEmptyDensity(true)) ? Color.WHITE : Color.BLACK;
      g2d.setBackground(background);
      if (!Color.BLACK.equals(background)) {
        // Change background color
        g2d.clearRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
      }
      final Map<LayoutConstraints, Component> elements = layout.getLayoutModel().getConstraints();
      for (Entry<LayoutConstraints, Component> e : elements.entrySet()) {
        LayoutConstraints key = e.getKey();
        Component value = e.getValue();

        ExportImage<? extends ImageElement> image = null;
        Point2D.Double pad = new Point2D.Double(0.0, 0.0);

        if (value instanceof ExportImage<?> exportImage) {
          image = exportImage;
          formatImage(image, key, pad);
        }

        if (key.gridx == 0) {
          wx = 0.0;
        } else if (lastx < key.gridx) {
          wx += lastwx;
        }
        double wy = lastwy[key.gridx];

        double x =
                5 + (placeholderX * wx) + (MathUtil.isEqualToZero(wx) ? 0 : key.gridx * 5) + pad.x;
        double y =
                5 + (placeholderY * wy) + (MathUtil.isEqualToZero(wy) ? 0 : key.gridy * 5) + pad.y;
        lastx = key.gridx;
        lastwx = key.weightx;
        for (int i = key.gridx; i < key.gridx + key.gridwidth; i++) {
          lastwy[i] += key.weighty;
        }

        if (image != null) {
          boolean wasBuffered = ImagePrint.disableDoubleBuffering(image);

          // Set us to the upper left corner
          g2d.translate(x, y);
          g2d.setClip(image.getBounds());
          image.draw(g2d);
          ImagePrint.restoreDoubleBuffering(image, wasBuffered);
          g2d.translate(-x, -y);

          if (!borderColor.equals(background)) {
            // Change background color
            g2d.setClip(null);
            g2d.setColor(borderColor);
            g2d.setStroke(new BasicStroke(2));
            Dimension viewSize = image.getSize();
            g2d.drawRect((int) x - 1, (int) y - 1, viewSize.width + 1, viewSize.height + 1);
          }
        }
      }
    }

    return bufferedImage;
  }

  private BufferedImage initialize(ExportLayout<? extends ImageElement> layout) {
    Dimension dimGrid = layout.getLayoutModel().getGridSize();
    FilmSize filmSize = printOptions.getFilmSizeId();
    PrintOptions.DotPerInches dpi = printOptions.getDpi();

    int width = filmSize.getWidth(dpi);
    int height = filmSize.getHeight(dpi);

    if ("LANDSCAPE".equals(printOptions.getFilmOrientation(true))) {
      int tmp = width;
      width = height;
      height = tmp;
    }

    String mType = printOptions.getMagnificationType(true);
    interpolation = Interpolation.BILINEAR;

    if ("REPLICATE".equals(mType)) {
      interpolation = Interpolation.NEAREST_NEIGHBOUR;
    } else if ("CUBIC".equals(mType)) {
      interpolation = Interpolation.BICUBIC;
    }

    // Printable size
    placeholderX = width - (dimGrid.width + 1) * 5.0;
    placeholderY = height - (dimGrid.height + 1) * 5.0;

    lastx = 0;
    lastwx = 0.0;
    lastwy = new double[dimGrid.width];
    wx = 0.0;

    if (printOptions.isColorPrint()) {
      return createRGBBufferedImage(width, height);
    } else {
      return createGrayBufferedImage(width, height);
    }
  }

  private void formatImage(
          ExportImage<? extends ImageElement> image, LayoutConstraints key, Point2D.Double pad) {
    if (!printOptions.isShowingAnnotations() && image.getInfoLayer().getVisible()) {
      image.getInfoLayer().setVisible(false);
    }

    Rectangle2D originSize = (Rectangle2D) image.getActionValue("origin.image.bound");
    Point2D originCenterOffset = (Point2D) image.getActionValue("origin.center.offset");
    Double originZoom = (Double) image.getActionValue("origin.zoom");
    PlanarImage img = image.getSourceImage();
    if (img != null && originCenterOffset != null && originZoom != null) {
      boolean bestfit = originZoom <= 0.0;
      double canvasWidth;
      double canvasHeight;
      if (bestfit || originSize == null) {
        canvasWidth = img.width() * image.getImage().getRescaleX();
        canvasHeight = img.height() * image.getImage().getRescaleY();
      } else {
        canvasWidth = originSize.getWidth() / originZoom;
        canvasHeight = originSize.getHeight() / originZoom;
      }
      double scaleCanvas =
              Math.min(
                      placeholderX * key.weightx / canvasWidth, placeholderY * key.weighty / canvasHeight);

      // Set the print area in pixel
      double cw = canvasWidth * scaleCanvas;
      double ch = canvasHeight * scaleCanvas;
      image.setSize((int) (cw + 0.5), (int) (ch + 0.5));

      if (printOptions.isCenter()) {
        pad.x = (placeholderX * key.weightx - cw) * 0.5;
        pad.y = (placeholderY * key.weighty - ch) * 0.5;
      } else {
        pad.x = 0.0;
        pad.y = 0.0;
      }

      image
              .getDisplayOpManager()
              .setParamValue(
                      AffineTransformOp.OP_NAME, AffineTransformOp.P_INTERPOLATION, interpolation);
      double scaleFactor = Math.min(cw / canvasWidth, ch / canvasHeight);
      // Resize in best fit window
      image.zoom(scaleFactor);
      if (bestfit) {
        image.center();
      } else {
        image.setCenter(originCenterOffset.getX(), originCenterOffset.getY());
      }
    }
  }

  /**
   * Creates a BufferedImage with a custom color model that can be used to store 3 channel RGB data
   * in a byte array data buffer
   */
  public static BufferedImage createRGBBufferedImage(int destWidth, int destHeight) {
    ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    ColorModel cm =
            new ComponentColorModel(cs, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
    WritableRaster r = cm.createCompatibleWritableRaster(destWidth, destHeight);
    return new BufferedImage(cm, r, false, null);
  }

  public static BufferedImage createGrayBufferedImage(int destWidth, int destHeight) {
    ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
    ColorModel cm =
            new ComponentColorModel(cs, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
    WritableRaster r = cm.createCompatibleWritableRaster(destWidth, destHeight);
    return new BufferedImage(cm, r, false, null);
  }

  public void printImage(BufferedImage image) throws Exception {
    Attributes filmSessionAttrs = new Attributes();
    Attributes filmBoxAttrs = new Attributes();
    Attributes imageBoxAttrs = new Attributes();
    Attributes dicomImage = new Attributes();

    storeRasterInDicom(image, dicomImage, printOptions.isColorPrint());

    // writeDICOM(new File("/tmp/print.dcm"), dicomImage);
    // 获取Weasis AE的配置信息，如果没有配置则默认为"WEASIS_AE"
    String weasisAet = BundleTools.SYSTEM_PREFERENCES.getProperty("weasis.aet", "WEASIS_AE"); // NON-NLS


    filmSessionAttrs.setInt(Tag.NumberOfCopies, VR.IS, printOptions.getNumOfCopies());
    filmSessionAttrs.setString(Tag.PrintPriority, VR.CS, printOptions.getPriority(true));
    filmSessionAttrs.setString(Tag.MediumType, VR.CS, printOptions.getMediumType(true));
    filmSessionAttrs.setString(Tag.FilmDestination, VR.CS, printOptions.getFilmDestination(true));
    filmBoxAttrs.setString(Tag.FilmSizeID, VR.CS, printOptions.getFilmSizeId().toString());
    filmBoxAttrs.setString(Tag.FilmOrientation, VR.CS, printOptions.getFilmOrientation(true));
    filmBoxAttrs.setString(Tag.MagnificationType, VR.CS, printOptions.getMagnificationType(true));
    filmBoxAttrs.setString(Tag.SmoothingType, VR.CS, printOptions.getSmoothingType(true));
    filmBoxAttrs.setString(Tag.Trim, VR.CS, printOptions.getTrim(true));
    filmBoxAttrs.setString(Tag.BorderDensity, VR.CS, printOptions.getBorderDensity(true));
    filmBoxAttrs.setInt(Tag.MinDensity, VR.US, printOptions.getMinDensity());
    filmBoxAttrs.setInt(Tag.MaxDensity, VR.US, printOptions.getMaxDensity());
    filmBoxAttrs.setString(Tag.ImageDisplayFormat, VR.ST, printOptions.getImageDisplayFormat());
    imageBoxAttrs.setInt(Tag.ImageBoxPosition, VR.US, 1);

    //创建一个新的Sequence，用于保存DICOM图像
    Sequence seq =
            imageBoxAttrs.ensureSequence(
                    printOptions.isColorPrint()
                            ? Tag.BasicColorImageSequence
                            : Tag.BasicGrayscaleImageSequence,
                    1);

    // 将DICOM图像添加到Sequence中
    seq.add(dicomImage);

    // 创建一个新的FilmSessionUID和FilmBoxUID
    final String filmSessionUID = UIDUtils.createUID();
    final String filmBoxUID = UIDUtils.createUID();

    // 创建一个Attributes对象，用于保存FilmSession的参考信息
    Attributes filmSessionSequenceObject = new Attributes();

    // 将ReferencedSOPClassUID设置为BasicFilmSession SOP Class UID
    filmSessionSequenceObject.setString(Tag.ReferencedSOPClassUID, VR.UI, UID.BasicFilmSession);
    // 将ReferencedSOPInstanceUID设置为新创建的FilmSessionUID
    filmSessionSequenceObject.setString(Tag.ReferencedSOPInstanceUID, VR.UI, filmSessionUID);
    // 创建一个新的Sequence，用于保存ReferencedFilmSessionSequence
    seq = filmBoxAttrs.ensureSequence(Tag.ReferencedFilmSessionSequence, 1);
    // 将保存FilmSession的Attributes对象添加到Sequence中
    seq.add(filmSessionSequenceObject);

    // 根据打印选项设置打印管理SOP类的UID
    // 如果是彩色打印，使用BasicColorPrintManagementMeta，否则使用BasicGrayscalePrintManagementMeta
    final String printManagementSOPClass =
            printOptions.isColorPrint()
                    ? UID.BasicColorPrintManagementMeta
                    : UID.BasicGrayscalePrintManagementMeta;
    final String imageBoxSOPClass =
            printOptions.isColorPrint() ? UID.BasicColorImageBox : UID.BasicGrayscaleImageBox;

    Association as = ConnectAssociation(weasisAet,dcmNode.getAeTitle(),dcmNode.getHostname(),dcmNode.getPort(),printManagementSOPClass);

    try {
      // See http://dicom.nema.org/medical/dicom/current/output/chtml/part02/sect_E.4.2.html
      // Create a Basic Film Session
      //创建一个 DICOM 打印会话，并使用给定的属性（filmSessionAttrs）作为参数
      dimseRSPHandler(
              as.ncreate(
                      printManagementSOPClass,
                      UID.BasicFilmSession,
                      filmSessionUID,
                      filmSessionAttrs,
                      UID.ImplicitVRLittleEndian));
      // Create a Basic Film Box. We need to get the Image Box UID from the response
      // 创建基本打印对象（Basic Film Box）。我们需要从响应中获取图像框UID
      DimseRSP ncreateFilmBoxRSP =
              as.ncreate(
                      printManagementSOPClass,
                      UID.BasicFilmBox,
                      filmBoxUID,
                      filmBoxAttrs,
                      UID.ImplicitVRLittleEndian);
      //创建Basic Film Box的响应消息。将创建Basic Film Box的响应消息传递给 dimseRSPHandler() 方法进行处理
      dimseRSPHandler(ncreateFilmBoxRSP);
      // 将指针移动到 Basic Film Box 中的 Referenced Image Box Sequence。
      // 因为 Basic Film Box 包含一个或多个 Referenced Image Box，我们需要将指针移动到这个序列中才能获取其中的数据
      ncreateFilmBoxRSP.next();
      //取响应消息的数据集,该序列存储以便后续使用
      Attributes imageBoxSequence = ncreateFilmBoxRSP.getDataset().getNestedDataset(Tag.ReferencedImageBoxSequence);
      // Send N-SET message with the Image Box
      // 使用图像框发送N-SET消息
      dimseRSPHandler(
              as.nset(
                      printManagementSOPClass,
                      imageBoxSOPClass,
                      imageBoxSequence.getString(Tag.ReferencedSOPInstanceUID),
                      imageBoxAttrs,
                      UID.ImplicitVRLittleEndian));
      // Send N-ACTION message with the print action
      // 发送N-ACTION消息进行打印操作
      dimseRSPHandler(
              as.naction(
                      printManagementSOPClass,
                      UID.BasicFilmBox,
                      filmBoxUID,
                      1,
                      null,
                      UID.ImplicitVRLittleEndian));
      // The print action ends here. This will only delete the Film Box and Film Session
      // 打印完成后，删除Film Box和Film Session
      as.ndelete(printManagementSOPClass, UID.BasicFilmBox, filmBoxUID);
      as.ndelete(printManagementSOPClass, UID.BasicFilmSession, filmSessionUID);
    } finally {
      // 确保释放资源
      if (as != null && as.isReadyForDataTransfer()) {
        as.waitForOutstandingRSP();
        as.release();
      }
    }
  }

  public static Association ConnectAssociation(String localAE, String targetAE, String targetHost, int targetPort, String SOPClass) {
    try {
      // 创建DICOM设备
      Device device = new Device(localAE);

      //创建本地DICOM连接。
      Connection lConn = new Connection();
      //创建本地应用实体
      ApplicationEntity lAE = new ApplicationEntity(localAE);
      //添加连接到应用实体和设备。
      lAE.addConnection(lConn);
      //设置本地应用实体的信息，包括AETitle和是否是协会发起方。
      lAE.setAssociationInitiator(true);
      lAE.setAETitle(localAE);

      //创建远程连接
      Connection tConn = new Connection();
      tConn.setPort(targetPort);
      tConn.setHostname(targetHost);
      tConn.setSocketCloseDelay(90);
      //创建远程应用实体。
      ApplicationEntity tAE = new ApplicationEntity(targetAE);
      //设置远程应用实体的信息，包括是否是协会接受方。
      tAE.setAssociationAcceptor(true);
      //添加远程连接到远程应用实体。
      tAE.addConnection(tConn);

      //将本地应用实体和设备关联起来。
      device.addConnection(lConn);
      //添加连接到应用实体和设备。
      device.addApplicationEntity(lAE);
      lAE.addConnection(lConn);
      //设置设备的执行器和计划执行器
      device.setExecutor(Executors.newSingleThreadExecutor());
      device.setScheduledExecutor(Executors.newSingleThreadScheduledExecutor());

      // 创建一个A-ASSOCIATE请求
      AAssociateRQ request = new AAssociateRQ();
      // 将PresentationContext与A-ASSOCIATE请求相关联
      request.addPresentationContext(new PresentationContext(1, SOPClass, UID.ImplicitVRLittleEndian));
      // 设置调用方的AE Title和被调用方的AE Title
      request.setCallingAET(lAE.getAETitle());
      request.setCalledAET(tAE.getAETitle());
      // 使用A-ASSOCIATE请求连接本地的AE和远程的AE
      Association as = lAE.connect(tConn, request);
      return as;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void dimseRSPHandler(DimseRSP response) throws IOException, InterruptedException {
    response.next();
    Attributes command = response.getCommand();
    int status = command.getInt(Tag.Status, 0);
    if (status == Status.AttributeValueOutOfRange
            || status == Status.AttributeListError
            || status == 0xB600
            || status == 0xB602
            || status == 0xB604
            || status == 0xB609
            || status == 0xB60A) {
      LOGGER.warn("DICOM Print warning status: {}", Integer.toHexString(status));
    } else if (status != Status.Success) {
      throw new IOException(
              "Unable to print the image. DICOM response status: " + Integer.toHexString(status));
    }
  }

  public static void storeRasterInDicom(
          BufferedImage image, Attributes dcmObj, Boolean printInColor) {
    byte[] bytesOut = null;
    if (dcmObj != null && image != null) {
      dcmObj.setInt(Tag.Columns, VR.US, image.getWidth());
      dcmObj.setInt(Tag.Rows, VR.US, image.getHeight());
      dcmObj.setInt(Tag.PixelRepresentation, VR.US, 0);
      dcmObj.setString(
              Tag.PhotometricInterpretation, VR.CS, printInColor ? "RGB" : "MONOCHROME2"); // NON-NLS
      dcmObj.setInt(Tag.SamplesPerPixel, VR.US, printInColor ? 3 : 1);
      dcmObj.setInt(Tag.BitsAllocated, VR.US, 8);
      dcmObj.setInt(Tag.BitsStored, VR.US, 8);
      dcmObj.setInt(Tag.HighBit, VR.US, 7);
      // Assumed that the displayed image has always an 1/1 aspect ratio.
      dcmObj.setInt(Tag.PixelAspectRatio, VR.IS, 1, 1);
      // Issue with some PrintSCP servers
      // dcmObj.putString(Tag.TransferSyntaxUID, VR.UI, UID.ImplicitVRLittleEndian);

      DataBuffer dataBuffer;
      if (printInColor) {
        // Must be PixelInterleavedSampleModel
        dcmObj.setInt(Tag.PlanarConfiguration, VR.US, 0);
        dataBuffer = image.getRaster().getDataBuffer();
      } else {
        dataBuffer = convertRGBImageToMonochrome(image).getRaster().getDataBuffer();
      }

      if (dataBuffer instanceof DataBufferByte dataBufferByte) {
        bytesOut = dataBufferByte.getData();
      } else if (dataBuffer instanceof DataBufferShort dataBufferShort) {
        bytesOut = fillShortArray(dataBufferShort.getData());
      } else if (dataBuffer instanceof DataBufferUShort dataBufferUShort) {
        bytesOut = fillShortArray(dataBufferUShort.getData());
      }
      dcmObj.setBytes(Tag.PixelData, VR.OW, bytesOut);
    }
  }

  private static byte[] fillShortArray(short[] data) {
    byte[] bytesOut = new byte[data.length * 2];
    for (int i = 0; i < data.length; i++) {
      bytesOut[i * 2] = (byte) (data[i] & 0xFF);
      bytesOut[i * 2 + 1] = (byte) ((data[i] >>> 8) & 0xFF);
    }
    return bytesOut;
  }

  private static BufferedImage convertRGBImageToMonochrome(BufferedImage colorImage) {
    if (colorImage.getType() == BufferedImage.TYPE_BYTE_GRAY) {
      return colorImage;
    }
    BufferedImage image =
            new BufferedImage(
                    colorImage.getWidth(), colorImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
    Graphics g = image.getGraphics();
    g.drawImage(colorImage, 0, 0, null);
    g.dispose();
    return image;
  }
}
