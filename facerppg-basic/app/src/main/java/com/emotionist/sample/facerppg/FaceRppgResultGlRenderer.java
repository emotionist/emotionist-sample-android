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

package com.emotionist.sample.facerppg;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.emotionist.sdk.formats.proto.BiomarkerProto.Biomarker;
import com.emotionist.sdk.formats.proto.ProgressProto.Progress;
import com.emotionist.sdk.solutioncore.ResultGlRenderer;
import com.emotionist.sdk.solutions.facerppg.FaceRppgResult;
import com.google.mediapipe.formats.proto.DetectionProto.Detection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/** A custom implementation of {@link ResultGlRenderer} to render {@link FaceRppgResult}. */
public class FaceRppgResultGlRenderer implements ResultGlRenderer<FaceRppgResult> {
    private static final String TAG = "FaceRppgResultGlRenderer";

    private static final float[] BBOX_COLOR = new float[] {0f, 1f, 0f, 1f};
    private static final int BBOX_THICKNESS = 8;
    private static final float[] PROGRESS_COLOR = new float[] {1f, 0f, 0f, 1f};
    private static final float PROGRESS_SIZE = 16f;
    private static final float[] BIOMARKER_COLOR = new float[] {1f, 0f, 0f, 1f};
    private static final float BIOMARKER_SIZE = 16f;
    private static final String VERTEX_SHADER =
            "uniform mat4 uProjectionMatrix;\n"
                    + "uniform float uPointSize;\n"
                    + "attribute vec4 vPosition;\n"
                    + "void main() {\n"
                    + "  gl_Position = uProjectionMatrix * vPosition;\n"
                    + "  gl_PointSize = uPointSize;"
                    + "}";
    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n"
                    + "uniform vec4 uColor;\n"
                    + "void main() {\n"
                    + "  gl_FragColor = uColor;\n"
                    + "}";
    private int program;
    private int positionHandle;
    private int projectionMatrixHandle;
    private int colorHandle;
    private int textUniformHandle;
    private int textHandle;

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    @Override
    public void setupRendering() {
        program = GLES20.glCreateProgram();
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        projectionMatrixHandle = GLES20.glGetUniformLocation(program, "uProjectionMatrix");
        colorHandle = GLES20.glGetUniformLocation(program, "uColor");
        textUniformHandle = GLES20.glGetUniformLocation(program, "u_Texture");
    }

    @Override
    public void renderResult(FaceRppgResult result, float[] projectionMatrix) {
        if (result == null) {
            return;
        }
        GLES20.glUseProgram(program);
        GLES20.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0);
        int numDetectedFaces = result.multiFaceDetections().size();
        for (int i = 0; i < numDetectedFaces; ++i) {
            drawDetection(result.multiFaceDetections().get(i));
//            drawBiomarker(result.multiProgress().get(i), result.multiBiomarker().get(i));
//            drawBiomarker();
        }
    }

    /**
     * Deletes the shader program.
     *
     * <p>This is only necessary if one wants to release the program while keeping the context around.
     */
    public void release() {
        GLES20.glDeleteProgram(program);
    }

    private void drawDetection(Detection detection) {
        if (!detection.hasLocationData()) {
            return;
        }
        // Draw bounding box.
        float left = detection.getLocationData().getRelativeBoundingBox().getXmin();
        float top = detection.getLocationData().getRelativeBoundingBox().getYmin();
        float right = left + detection.getLocationData().getRelativeBoundingBox().getWidth();
        float bottom = top + detection.getLocationData().getRelativeBoundingBox().getHeight();
        drawLine(top, left, top, right);
        drawLine(bottom, left, bottom, right);
        drawLine(top, left, bottom, left);
        drawLine(top, right, bottom, right);
    }

    private void drawLine(float y1, float x1, float y2, float x2) {
        GLES20.glUniform4fv(colorHandle, 1, BBOX_COLOR, 0);
        GLES20.glLineWidth(BBOX_THICKNESS);
        float[] vertex = {x1, y1, x2, y2};
        FloatBuffer vertexBuffer =
                ByteBuffer.allocateDirect(vertex.length * 4)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer()
                        .put(vertex);
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);
    }

//    private void drawBiomarker() {
////     private void drawBiomarker(Progress progress, Biomarker biomarker) {
////         if (!progress.hasValue()) {
////             return;
////         }
//        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_4444);
//        Canvas canvas = new Canvas(bitmap);
//        bitmap.eraseColor(0);
//
//        Paint paint = new Paint();
//        paint.setTextSize(18);
//        paint.setAntiAlias(true);
//        paint.setARGB(0xff, 0xff, 0x00, 0x00);
//        paint.setTextAlign(Paint.Align.LEFT);
//        paint.setTextScaleX(0.5f);
//        canvas.drawText("60bpm", 0.f, 15.f, paint);
//
//        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
//        bitmap.recycle();
//
//
////         final int[] textHandle = new int[1];
////         GLES20.glGenTextures(1, textHandle, 0);
////
////         Paint textPaint = new Paint();
////         textPaint.setTextSize(10);
////         textPaint.setFakeBoldText(false);
////         textPaint.setAntiAlias(true);
////         textPaint.setARGB(255, 255, 0, 0);
////         textPaint.setSubpixelText(true);
////         textPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
////         float realTextWidth = textPaint.measureText("60bpm");
////
////         // Creates a new mutable bitmap, with 128px of width and height
////         int bitmapWidth = (int)(realTextWidth + 2.0f);
////         int bitmapHeight = (int)10 + 2;
////
////         Bitmap textBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
////         textBitmap.eraseColor(Color.argb(0, 255, 255, 255));
////         // Creates a new canvas that will draw into a bitmap instead of rendering into the screen
////         Canvas bitmapCanvas = new Canvas(textBitmap);
////         // Set start drawing position to [1, base_line_position]
////         // The base_line_position may vary from one font to another but it usually is equal to 75% of font size (height)
////         bitmapCanvas.drawText("60bpm", 1, 1.0f + 10 * 0.75f, textPaint);
////
////         // Bind to the texture in OpenGL
////         GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textHandle[0]);
////
////         // Load the bitmap into the bound texture
////         GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textBitmap, 0);
////
////         // Recycle the bitmap, since its data has been loaded into OpenGL
////         textBitmap.recycle();
////
////         // After the image has been subloaded to texture, regenerate mipmaps
////         GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
//     }
}
