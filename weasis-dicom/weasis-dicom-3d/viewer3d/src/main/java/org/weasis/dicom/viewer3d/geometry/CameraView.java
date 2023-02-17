/*
 * Copyright (c) 2022 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.viewer3d.geometry;

import static org.weasis.dicom.viewer3d.geometry.Camera.getQuaternion;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.weasis.dicom.codec.geometry.ImageOrientation.Plan;
import org.weasis.dicom.codec.geometry.PatientOrientation.Biped;

public enum CameraView implements View {
  INITIAL(
      "Default", getQuaternion(-90, 0, 15), getQuaternion(15, 0, 90), getQuaternion(-90, 15, 0)),
  FRONT(
      Biped.A.getFullName(),
      getQuaternion(-90, 0, 0),
      getQuaternion(0, 0, 90),
      getQuaternion(-90, 0, 0)),
  BACK(
      Biped.P.getFullName(),
      getQuaternion(-90, 0, 180),
      getQuaternion(180, 0, 90),
      getQuaternion(-90, 180, 0)),
  TOP(
      Biped.H.getFullName(),
      getQuaternion(0, 0, 180),
      getQuaternion(0, 180, 0),
      getQuaternion(0, 180, 0)),
  BOTTOM(
      Biped.F.getFullName(),
      getQuaternion(180, 0, 0),
      getQuaternion(0, 0, 180),
      getQuaternion(180, 0, 0)),
  LEFT(
      Biped.L.getFullName(),
      getQuaternion(-90, 0, -90),
      getQuaternion(-90, 0, 90),
      getQuaternion(-90, 90, 0)),
  RIGHT(
      Biped.R.getFullName(),
      getQuaternion(-90, 0, 90),
      getQuaternion(90, 0, 90),
      getQuaternion(-90, -90, 0));

  private Quaterniond rotation;
  private Quaterniond sagittalRotation;
  private Quaterniond coronalRotation;

  private String title;

  CameraView(
      String title,
      Quaterniond rotation,
      Quaterniond sagittalRotation,
      Quaterniond coronalRotation) {
    this.title = title;
    this.rotation = rotation;
    this.sagittalRotation = sagittalRotation;
    this.coronalRotation = coronalRotation;
  }

  public String title() {
    return title;
  }

  @Override
  public Vector3d position() {
    return Camera.POSITION_ZERO;
  }

  @Override
  public double zoom() {
    return Camera.DEFAULT_ZOOM;
  }

  @Override
  public Quaterniond rotation() {
    return rotation;
  }

  @Override
  public Quaterniond rotation(Plan plan) {
    if (plan == Plan.CORONAL) {
      return coronalRotation;
    } else if (plan == Plan.SAGITTAL) {
      return sagittalRotation;
    }
    return rotation;
  }
}
