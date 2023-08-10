/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.explorer.pref.download;

import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import org.weasis.core.api.explorer.DataExplorerView;
import org.weasis.core.api.gui.util.AbstractItemDialogPage;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.core.api.media.data.Thumbnail;
import org.weasis.core.api.service.BundleTools;
import org.weasis.core.ui.docking.UIManager;
import org.weasis.core.ui.pref.PreferenceDialog;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.explorer.DicomExplorer;
import org.weasis.dicom.explorer.DicomSorter;
import org.weasis.dicom.explorer.DicomSorter.SortingTime;
import org.weasis.dicom.explorer.HangingProtocols.OpeningViewer;
import org.weasis.dicom.explorer.Messages;

public class DicomExplorerPrefView extends AbstractItemDialogPage {
  public static final String DOWNLOAD_IMMEDIATELY = "weasis.download.immediately";
  public static final String DOWNLOAD_OPEN_MODE = "weasis.download.open.view.mode";
  public static final String STUDY_DATE_SORTING = "weasis.sorting.study.date";
  private final JCheckBox downloadImmediatelyCheckbox =
      new JCheckBox(Messages.getString("SeriesDownloadPrefView.downloadImmediatelyCheckbox"));
  private final JSpinner spinner;

  private final JComboBox<OpeningViewer> openingViewerJComboBox =
      new JComboBox<>(OpeningViewer.values());

  private final JComboBox<SortingTime> studyDateSortingComboBox =
      new JComboBox<>(SortingTime.values());

  public DicomExplorerPrefView() {
    super(Messages.getString("DicomExplorer.title"), 607);

    int thumbnailSize =
        BundleTools.SYSTEM_PREFERENCES.getIntProperty(Thumbnail.KEY_SIZE, Thumbnail.DEFAULT_SIZE);
    JLabel thumbSize = new JLabel(Messages.getString("DicomExplorer.thmb_size"));
    SpinnerListModel model =
        new SpinnerListModel(
            List.of(
                Thumbnail.MIN_SIZE,
                Thumbnail.DEFAULT_SIZE,
                160,
                176,
                192,
                208,
                224,
                240,
                Thumbnail.MAX_SIZE));
    spinner = new JSpinner(model);
    model.setValue(thumbnailSize);
    add(GuiUtils.getFlowLayoutPanel(ITEM_SEPARATOR_SMALL, ITEM_SEPARATOR, thumbSize, spinner));

    JLabel labelStudyDate = new JLabel(Messages.getString("DicomExplorer.study_date_sort") + StringUtil.COLON);
    studyDateSortingComboBox.setSelectedItem(DicomSorter.getStudyDateSorting());
    add(
        GuiUtils.getFlowLayoutPanel(
            ITEM_SEPARATOR_SMALL, ITEM_SEPARATOR, labelStudyDate, studyDateSortingComboBox));

    JLabel labelOpenPatient =
        new JLabel(Messages.getString("DicomExplorer.open_win") + StringUtil.COLON);
    openingViewerJComboBox.setSelectedItem(getOpeningViewer());
    add(
        GuiUtils.getFlowLayoutPanel(
            ITEM_SEPARATOR_SMALL, ITEM_SEPARATOR, labelOpenPatient, openingViewerJComboBox));

    downloadImmediatelyCheckbox.setSelected(
        BundleTools.SYSTEM_PREFERENCES.getBooleanProperty(DOWNLOAD_IMMEDIATELY, true));
    add(
        GuiUtils.getFlowLayoutPanel(
            ITEM_SEPARATOR_SMALL, ITEM_SEPARATOR, downloadImmediatelyCheckbox));

    add(GuiUtils.boxYLastElement(LAST_FILLER_HEIGHT));

    getProperties().setProperty(PreferenceDialog.KEY_SHOW_APPLY, Boolean.TRUE.toString());
    getProperties().setProperty(PreferenceDialog.KEY_SHOW_RESTORE, Boolean.TRUE.toString());
    getProperties()
        .setProperty(PreferenceDialog.KEY_HELP, "dicom-explorer/#preferences"); // NON-NLS
  }

  private OpeningViewer getOpeningViewer() {
    String key = BundleTools.SYSTEM_PREFERENCES.getProperty(DOWNLOAD_OPEN_MODE);
    return OpeningViewer.getOpeningViewer(key, OpeningViewer.ALL_PATIENTS);
  }

  @Override
  public void resetToDefaultValues() {
    BundleTools.SYSTEM_PREFERENCES.resetProperty(DOWNLOAD_IMMEDIATELY, Boolean.TRUE.toString());
    downloadImmediatelyCheckbox.setSelected(
        BundleTools.SYSTEM_PREFERENCES.getBooleanProperty(DOWNLOAD_IMMEDIATELY, true));

    BundleTools.SYSTEM_PREFERENCES.resetProperty(
        STUDY_DATE_SORTING, String.valueOf(SortingTime.INVERSE_CHRONOLOGICAL.getId()));
    studyDateSortingComboBox.setSelectedItem(DicomSorter.getStudyDateSorting());

    BundleTools.SYSTEM_PREFERENCES.resetProperty(
        DOWNLOAD_OPEN_MODE, OpeningViewer.ALL_PATIENTS.name());
    openingViewerJComboBox.setSelectedItem(getOpeningViewer());

    spinner.setValue(Thumbnail.DEFAULT_SIZE);
  }

  @Override
  public void closeAdditionalWindow() {
    BundleTools.SYSTEM_PREFERENCES.putBooleanProperty(
        DOWNLOAD_IMMEDIATELY, downloadImmediatelyCheckbox.isSelected());

    SortingTime sortingTime = (SortingTime) studyDateSortingComboBox.getSelectedItem();
    if (sortingTime != null) {
      BundleTools.SYSTEM_PREFERENCES.putIntProperty(STUDY_DATE_SORTING, sortingTime.getId());
    }

    OpeningViewer openingViewer = (OpeningViewer) openingViewerJComboBox.getSelectedItem();
    if (openingViewer != null) {
      BundleTools.SYSTEM_PREFERENCES.put(DOWNLOAD_OPEN_MODE, openingViewer.name());
    }

    DataExplorerView dicomView = UIManager.getExplorerPlugin(DicomExplorer.NAME);
    if (dicomView instanceof DicomExplorer explorer) {
      int size = (int) spinner.getValue();
      BundleTools.SYSTEM_PREFERENCES.putIntProperty(Thumbnail.KEY_SIZE, size);
      explorer.updateThumbnailSize(size);
    }

    BundleTools.saveSystemPreferences();
  }
}
