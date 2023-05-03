// Copyright 2023 The Emotionist Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.emotionist.sample.facerppg.main;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.emotionist.sample.facerppg.R;
import com.emotionist.sample.facerppg.view.FaceRppgResultGlRenderer;
import com.emotionist.sdk.formats.proto.BiomarkerProto.Biomarker;
import com.emotionist.sdk.formats.proto.BiosignalProto.Biosignal;
import com.emotionist.sdk.formats.proto.ProgressProto.Progress;
import com.emotionist.sdk.solutioncore.CameraInput;
import com.emotionist.sdk.solutioncore.SolutionGlSurfaceView;
import com.emotionist.sdk.solutions.facerppg.FaceRppg;
import com.emotionist.sdk.solutions.facerppg.FaceRppgOptions;
import com.emotionist.sdk.solutions.facerppg.FaceRppgResult;
import com.emotionist.sdk.solutions.facerppg.FaceKeypoint;
import com.google.mediapipe.formats.proto.LocationDataProto.LocationData.RelativeKeypoint;

/** Main activity of Emotionist FaceRppg app. */
public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";
  private static final String APP_ID = "99999999-9999-9999-9999-999999999999";

  private Activity activity;
  private FaceRppg faceRppg;
  private CameraInput cameraInput;
  private SolutionGlSurfaceView<FaceRppgResult> glSurfaceView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    activity = this;
  }

  @Override
  protected void onResume() {
    super.onResume();
    setupStreamingModePipeline();
  }

  @Override
  protected void onPause() {
    super.onPause();
    stopCurrentPipeline();
  }

  /** Sets up core workflow for streaming mode. */
  private void setupStreamingModePipeline() {
    // Initializes a new Emotionist FaceRppg solution instance in the static image mode.
    faceRppg =
            new FaceRppg(
                    this,
                    FaceRppgOptions.builder()
                            .setStaticImageMode(false)
                            .setRunOnGpu(false)
                            .setMinDetectionConfidence(0.5f)
                            .build());

    // Connects Emotionist FaceRppg solution to the user-defined FaceRppgResultImageView.
    faceRppg.setResultListener(
            faceRppgResult -> {
              logNoseTipKeypoint(faceRppgResult, /*faceIndex=*/ 0, /*showPixelValues=*/ true);
            });
    faceRppg.setErrorListener(
            (message, e) -> Log.e(TAG, "Emotionist FaceRppg error:" + message));

    // Initializes Emotionist FaceRPPG solution instance in the streaming mode.
    faceRppg.init(APP_ID, new FaceRppg.InitResultHandler() {

      @Override
      public void onInitSucceed() {
        // Initializes camera
        cameraInput = new CameraInput(activity);
        cameraInput.setNewFrameListener(textureFrame -> faceRppg.send(textureFrame));

        // Initializes a new Gl surface view with a user-defined
        // FaceRppgResultGlRenderer.
        glSurfaceView = new SolutionGlSurfaceView<>(
                getApplicationContext(), faceRppg.getGlContext(), faceRppg.getGlMajorVersion());
        glSurfaceView.setSolutionResultRenderer(new FaceRppgResultGlRenderer());
        glSurfaceView.setRenderInputImage(true);
        faceRppg.setResultListener(
                faceRppgResult -> {
                  logNoseTipKeypoint(faceRppgResult, /* faceIndex= */ 0, /* showPixelValues= */ false);
                  logProgress(faceRppgResult, /* faceIndex= */ 0);
                  logBiosignal(faceRppgResult, /* faceIndex= */ 0);
                  logBiomarker(faceRppgResult, /* faceIndex= */ 0);
                  glSurfaceView.setRenderData(faceRppgResult);
                  glSurfaceView.requestRender();
                });

        // The runnable to start camera after the gl surface view is attached.
        glSurfaceView.post(this::startCamera);

        // Updates the preview layout.
        FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
        frameLayout.removeAllViewsInLayout();
        frameLayout.addView(glSurfaceView);
        glSurfaceView.setVisibility(View.VISIBLE);
        frameLayout.requestLayout();
      }

      @Override
      public void onInitFailed() {
        Toast.makeText(getApplicationContext(), "Invalid license key.", Toast.LENGTH_SHORT).show();
      }

      private void startCamera() {
        cameraInput.start(
                activity,
                faceRppg.getGlContext(),
                CameraInput.CameraFacing.FRONT,
                glSurfaceView.getWidth(),
                glSurfaceView.getHeight());
      }
    });
  }

  private void stopCurrentPipeline() {
    if (cameraInput != null) {
      cameraInput.setNewFrameListener(null);
      cameraInput.close();
    }
    if (glSurfaceView != null) {
      glSurfaceView.setVisibility(View.GONE);
    }
    if (faceRppg != null) {
      faceRppg.close();
    }
  }

  private void logNoseTipKeypoint(
          FaceRppgResult result, int faceIndex, boolean showPixelValues) {
    if (result.multiFaceDetections().isEmpty()) {
      return;
    }
    RelativeKeypoint noseTip = result
            .multiFaceDetections()
            .get(faceIndex)
            .getLocationData()
            .getRelativeKeypoints(FaceKeypoint.NOSE_TIP);
    // For Bitmaps, show the pixel values. For texture inputs, show the normalized
    // coordinates.
    if (showPixelValues) {
      int width = result.inputBitmap().getWidth();
      int height = result.inputBitmap().getHeight();
      Log.i(
              TAG,
              String.format(
                      "Emotionist FaceRppg nose tip coordinates (pixel values): x=%f, y=%f",
                      noseTip.getX() * width, noseTip.getY() * height));
    } else {
      Log.i(
              TAG,
              String.format(
                      "Emotionist FaceRppg nose tip normalized coordinates (value range: [0, 1]):"
                              + " x=%f, y=%f",
                      noseTip.getX(), noseTip.getY()));
    }
  }

  private void logProgress(FaceRppgResult result, int faceIndex) {
    if (result.multiProgress().isEmpty()) {
      return;
    }
    Progress progress = result.multiProgress().get(faceIndex);

    Log.i(
            TAG,
            String.format(
                    "Emotionist FaceRppg progress: %f",
                    progress.getValue()));
  }

  private void logBiosignal(FaceRppgResult result, int faceIndex) {
    if (result.multiBiosignal().isEmpty()) {
      return;
    }
    Biosignal biosignal = result.multiBiosignal().get(faceIndex);

    Log.i(
            TAG,
            String.format(
                    "Emotionist FaceRppg biosignal"));
  }

  private void logBiomarker(FaceRppgResult result, int faceIndex) {
    if (result.multiBiomarker().isEmpty()) {
      return;
    }
    Biomarker biomarker = result.multiBiomarker().get(faceIndex);

    Log.i(
            TAG,
            String.format(
                    "Emotionist FaceRppg biomarker: %f",
                    biomarker.getHeartRate()));
  }
}
