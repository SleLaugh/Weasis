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

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.DataWriterAdapter;
import org.dcm4che3.net.DimseRSP;
import org.dcm4che3.net.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.service.BundleTools;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.explorer.print.DicomPrint;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 右键菜单添加打印序列相关 sle
 * 2023年4月15日09:23:01
 */
public class PrintToolManager {
    private static String ProgramName;
    private static String ProgramPath;
    private static String targetHost;
    private static int targetPort;
    private static String targetAE;
    private static String localAE;
    private static boolean Initialize = false;
    private static String TransferSyntaxUID = UID.ImplicitVRLittleEndian;
    private static final Logger LOGGER = LoggerFactory.getLogger(DicomPrint.class);

    /**
     * 打开打印工具 sle
     * 2023年4月15日09:23:01
     */
    public static void PrintStart() {
        if (IsProcessRunning()) {
//            BringProgramToFront();
        } else {
            OpenProgram();
        }
    }

    /**
     * 判断程序是否已启动
     * @return true or false
     */
    private static boolean IsProcessRunning() {
        try {
            if (!Initialize) {
                GetConfig();
            }
            ProcessBuilder processBuilder = new ProcessBuilder("tasklist.exe");
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(ProgramName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 打开指定程序 sle
     * 2023年4月15日09:23:01
     */
    private static void OpenProgram() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(ProgramPath);
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 呼起到最前端 sle
     * 2023年4月15日09:23:01
     */
    private static void BringProgramToFront() {
        try {
            Process p = Runtime.getRuntime().exec("wmic process where \"name='" + ProgramName + "'\" call setforeground");
            p.waitFor();
            Thread.sleep(100); // 等待100毫秒
            Robot robot = new Robot();
            robot.keyPress(KeyEvent.VK_ALT);
            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_ALT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加指定序列到打印序列 sle
     * 2023年4月15日09:23:01
     * @param series 当前选中的序列
     */
    public static void PrintAddSelected(DicomImageElement series) {
        if (!IsProcessRunning()) {
            OpenProgram();
            return;
        }

        try {
            File dicomFile = new File(series.getMediaURI());

            DicomInputStream ds = new DicomInputStream(dicomFile);
            Attributes attribute = ds.readDataset();
            String sopInstanceUID = attribute.getString(Tag.SOPInstanceUID);
            String sopClassUID = attribute.getString(Tag.SOPClassUID);
            DataWriterAdapter data = new DataWriterAdapter(attribute);

            Association as = DicomPrint.ConnectAssociation(localAE, targetAE, targetHost, targetPort, sopClassUID);

            try {
                dimseRSPHandler(as.cstore(sopClassUID, sopInstanceUID, 1, data, TransferSyntaxUID));
                as.waitForOutstandingRSP();
            } finally {
                if (as != null && as.isReadyForDataTransfer()) {
                    as.release();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 添加关键图像组到打印序列 sle
     * 2023年4月15日09:23:01
     * @param attributesList 图像组中的影像信息
     */
    public static void PrintAddKOList(List<Attributes> attributesList) {
        if (!IsProcessRunning()) {
            OpenProgram();
            return;
        }

        Map<String, List<Attributes>> map = new HashMap<>();
        for (Attributes item : attributesList) {
            String sopClassUID = item.getString(Tag.ReferencedSOPClassUID);
            if (!map.containsKey(sopClassUID)) {
                map.put(sopClassUID, new ArrayList<>());
            }
            map.get(sopClassUID).add(item);
        }

        try {
            for (Map.Entry<String, List<Attributes>> entry : map.entrySet()) {
                String sopClassUID = entry.getKey();
                List<Attributes> list = entry.getValue();
                Association as = DicomPrint.ConnectAssociation(localAE, targetAE, targetHost, targetPort, sopClassUID);
                try {
                    for (Attributes item : list) {
                        String Uri = item.getString(Tag.RetrieveURI);
                        File dicomFile = new File(new URI(Uri));
                        DicomInputStream ds = new DicomInputStream(dicomFile);
                        Attributes attribute = ds.readDataset();
                        String sopInstanceUID = attribute.getString(Tag.SOPInstanceUID);
                        DataWriterAdapter data = new DataWriterAdapter(attribute);

                        dimseRSPHandler(as.cstore(sopClassUID, sopInstanceUID, 1, data, TransferSyntaxUID));
                        as.waitForOutstandingRSP();
                    }
                } finally {
                    if (as != null && as.isReadyForDataTransfer()) {
                        as.release();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取配置信息 sle
     * 2023年4月15日09:23:01
     */
    private static void GetConfig() {
        try {
            localAE = BundleTools.SYSTEM_PREFERENCES.getProperty("weasis.aet", "WEASIS_AE");
            targetPort = Integer.parseInt(BundleTools.SYSTEM_PREFERENCES.getProperty("weasis.printAEport", "9999"));
            targetAE = BundleTools.SYSTEM_PREFERENCES.getProperty("weasis.printAEtitle", "EL_PACS_AE");
            targetHost = InetAddress.getLocalHost().getHostAddress();
            ProgramName = BundleTools.SYSTEM_PREFERENCES.getProperty("weasis.printProgramName", "WeasisOpen.exe");
            // 先去读配置项，如果配置项里没有写路径，那么就获取当前运行目录的上一级目录。
            // 但是配置项默认没有值，也就是一般情况下是读取当前运行目录的上一级目录的
            ProgramPath = BundleTools.SYSTEM_PREFERENCES.getProperty("weasis.printProgramPath", GetsUpperPath()) + "\\" + ProgramName;

            Initialize = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取当前目录 sle
     * 2023年4月15日09:23:01
     * @return 上一级目录
     */
    public static String GetsUpperPath() {
        String currentDir = System.getProperty("user.dir");
//        File currentFile = new File(currentDir);
//        String parentDir = currentFile.getParentFile().getAbsolutePath();
        return currentDir;
    }

    private static void dimseRSPHandler(DimseRSP response) throws IOException, InterruptedException {
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
            LOGGER.warn("DICOM send warning status: {}", Integer.toHexString(status));
        } else if (status != Status.Success) {
            throw new IOException(
                    "Unable to send the Dicom. DICOM response status: " + Integer.toHexString(status));
        }
    }
}
