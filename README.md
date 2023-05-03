# Emotionist for Android

[![Platform](https://img.shields.io/badge/platform-android-orange.svg)](https://github.com/emotionist/emotionist-sample-android)
[![Languages](https://img.shields.io/badge/language-java-orange.svg)](https://github.com/emotionist/emotionist-sample-android)
[![Commercial License](https://img.shields.io/badge/license-Commercial-brightgreen.svg)](https://github.com/emotionist/emotionist-sample-android/blob/master/LICENSE.md)

<br />

## Introduction

[Emotionist](http://emotionist.ai) provides the vision API and SDK for your mobile app, enabling real-time recognizing facial expression, heart response and emotion using a camera.

<br />

## Installation

To use our Android samples, you should first install [Emotionist SDK for Android](https://github.com/emotionist/emotionist-sdk-android) 1.-0.0 or higher on your system and should be received License Key by requesting by our email: **support@emotionist.ai** <br /> 

<br />

## Before getting started

### Requirements

The minimum requirements for Emotionist SDK for Android are:

- Android 4.2.0 (API level 17) or later
- Java 8 or later
- Gradle 4.0.0 or later

```groovy
// build.gradle(app)
android {
    defaultConfig {
        minSdkVersion 17
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

<br />

## Getting started

if you would like to try the sample app specifically fit to your usage, you can do so by following the steps below.

### Step 1: Initialize the Emotionist SDK

Initialization binds the Emotionist SDK to Android’s context, thereby allowing it to use a camera in your mobile. To the `init()` method, pass the **App ID** of your application to initialize the SDK and the **InitResultHandler** to received callback for validation of the App ID.

```java
FaceRppg faceRppg = new FaceRppg(this,
                        FaceRppgOptions.builder()
                            .setStaticImageMode(false)
                            .setRunOnGpu(true)
                            .setMinDetectionConfidence(0.5f)
                            .build());
// Connects Emotionist FaceRppg solution to the user-defined FaceRppgResultImageView.
faceRppg.setResultListener(
        faceRppgResult -> {
          // TODO
        });
faceRppg.setErrorListener(
        (message, e) -> Log.e(TAG, "Emotionist FaceRppg error:" + message));

// Initializes Emotionist FaceRPPG solution instance in the streaming mode.
faceRppg.init(APP_ID, new FaceRppg.InitResultHandler() {
    @Override
    public void onInitSucceed() {
        // TODO
    }
    
    @Override
    public void onInitFailed() {
        // TODO
    }
}
```

> Note: The `init()` method must be called once across your Android app. It is recommended to initialize the Emotionist SDK in the `onCreate()` method of the Application instance.

### Step 2: Initialize camera nad surfaceveiw

If you want to access the camera and draw the measurements on the view easily, you can use the Emotionist solutioncore. Include the **container** to bind the Emotionist Fragment in your layout `.xml` file.

```xml
<FrameLayout
        android:id="@+id/preview_display_layout"
        android:layout_width="fill_parent"
        android:layout_height="fill_parent" />
```

> Note: FrameLayout is just one of examples. You can change to other layout type to purpose your app.

Bind the emotionist to display the image taken with the camera on the screen. FaceRppgResultGlRenderer automatically display the image to fit the size of your custom layout.

```java
// Initializes camera.
CameraInput cameraInput = new CameraInput(activity);
cameraInput.setNewFrameListener(textureFrame -> faceRppg.send(textureFrame));

// Initializes a new Gl surface view with a user-defined
// FaceRppgResultGlRenderer.
SolutionGlSurfaceView<FaceRppgResult> glSurfaceView = 
    new SolutionGlSurfaceView<>(
        getApplicationContext(), faceRppg.getGlContext(), faceRppg.getGlMajorVersion());
glSurfaceView.setSolutionResultRenderer(new FaceRppgResultGlRenderer());
glSurfaceView.setRenderInputImage(true);
faceRppg.setResultListener(
        faceRppgResult -> {
          // TODO
          
          // Render view
          glSurfaceView.setRenderData(faceRppgResult);
          glSurfaceView.requestRender();
        });
```

> Note: The code for initlaization of camera and view must be called once across your Android app. It is recommended to call the 'onInitSucceed()' method of the callback of 'init()' method.

### Step 3: Start the Emotionist SDK

Start the Emotionist SDK to recognize your facial expression, heart response and emotion. Please refer to **[sample app](https://github.com/emotionist/emotionist-sample-android)**.

```java
// The runnable to start camera after the gl surface view is attached.
glSurfaceView.post(this::startCamera);

// Updates the preview layout.
FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
frameLayout.removeAllViewsInLayout();
frameLayout.addView(glSurfaceView);
glSurfaceView.setVisibility(View.VISIBLE);
frameLayout.requestLayout();

// Define a function to start camera.
private void startCamera() {
    cameraInput.start(activity,
        faceRppg.getGlContext(),
        CameraInput.CameraFacing.FRONT,
        glSurfaceView.getWidth(),
        glSurfaceView.getHeight());
}
```

> Note: The code to start Emotionist SDK must be called once across your Android app. It is recommended to call the 'onInitSucceed()' method of the callback of 'init()' method.

### Step 4: Stop the Emotionist SDK

When your app is not use the camera or destroyed, stop the Emotionist SDK.

```java
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
```


<br />

## Android sample

You can **clone** the project from the [Sample repository](https://github.com/emotionist/emotionist-sample-android).

```
// Clone this repository
git clone git@github.com:emotionist/emotionist-sample-android.git

// Move to the sample
cd emotionist-sample-android
```

<br />

## Reference

For further detail on Emotionist SDK for Android, reter to [Emotionist SDK for Android README](https://github.com/emotionist/emotionist-sdk-android/blob/master/README.md).
