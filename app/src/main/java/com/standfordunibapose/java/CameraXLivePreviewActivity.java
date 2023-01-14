/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.standfordunibapose.java;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory;

import com.google.android.gms.common.annotation.KeepName;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.mlkit.common.MlKitException;
import com.standfordunibapose.CameraXViewModel;
import com.standfordunibapose.GraphicOverlay;
import com.standfordunibapose.R;
import com.standfordunibapose.VisionImageProcessor;
import com.standfordunibapose.java.facedetector.FaceDetectorProcessor;
import com.standfordunibapose.java.posedetector.PoseDetectorProcessor;
import com.standfordunibapose.preference.PreferenceUtils;
import com.standfordunibapose.preference.SettingsActivity;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Live preview demo app for ML Kit APIs using CameraX.
 */
@KeepName
public final class CameraXLivePreviewActivity extends AppCompatActivity
        implements OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "CameraXLivePreview";

    private static final String OBJECT_DETECTION = "Object Detection";
    private static final String OBJECT_DETECTION_CUSTOM = "Custom Object Detection";
    private static final String CUSTOM_AUTOML_OBJECT_DETECTION =
            "Custom AutoML Object Detection (Flower)";
    private static final String FACE_DETECTION = "Face Detection";
    private static final String POSE_DETECTION = "Pose Detection";
    private static final String STATE_SELECTED_MODEL = "selected_model";
    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;


    @Nullable
    private ProcessCameraProvider cameraProvider;
    @Nullable
    private Preview previewUseCase;
    @Nullable
    private ImageAnalysis analysisUseCase;
    @Nullable
    private VisionImageProcessor imageProcessor;
    private boolean needUpdateGraphicOverlayImageSourceInfo;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String selectedModel = FACE_DETECTION;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private CameraSelector cameraSelector;
    Handler handler = null;
    ImageButton recordbutton;

    enum status {started, recording, stopped}

    ;
    status curreentstatus = status.started;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        if (savedInstanceState != null) {
            selectedModel = savedInstanceState.getString(STATE_SELECTED_MODEL, FACE_DETECTION);
        }
        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        setContentView(R.layout.activity_vision_camerax_live_preview);
        previewView = findViewById(R.id.preview_view);
        if (previewView == null) {
            Log.d(TAG, "previewView is null");
        }
        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }
        recordbutton = findViewById(R.id.imageButton);
        recordbutton.setEnabled(false);
        recordbutton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (curreentstatus == status.started) {
                    curreentstatus = status.recording;
                    recordbutton.setEnabled(true);
                    recordbutton.setImageResource(R.drawable.stopbutton);
                    selectedModel = POSE_DETECTION;
                    bindAnalysisUseCase();
                } else {
                    if (curreentstatus == status.recording) {

                        List<PoseTime> poses = new ArrayList<>();
                        for (int i = 0; i < PoseDetectorProcessor.poseList.size(); i++) {
                            List<PoseCoords> coordinates = new ArrayList<>();
                            List<PoseLandmark> land = PoseDetectorProcessor.poseList.get(i);
                            Long timestamp = PoseDetectorProcessor.timestamps.get(i);
                            for (int j = 0; j < land.size(); j++) {
                                float x = land.get(j).getPosition3D().getX();
                                float y = land.get(j).getPosition3D().getY();
                                float z = land.get(j).getPosition3D().getZ();
                                int landmark = land.get(j).getLandmarkType();
                                coordinates.add(new PoseCoords(x, y, z, landmark));

                            }
                            PoseTime curr = new PoseTime();
                            curr.data = coordinates;
                            curr.timestamp = timestamp;
                            poses.add(curr);
                        }

                        Gson gson = new GsonBuilder().create();

                        String str = gson.toJson(poses);
                        Log.e("JSON GENERATED", str);

                        Map<String, Object> data = new HashMap<>();
                        data.put("poses", str);
                        data.put("dateTime", FieldValue.serverTimestamp());

                        new AlertDialog.Builder(CameraXLivePreviewActivity.this).setTitle("Do you want to upload that data?")
                                .setMessage("")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        db.collection("pose_data")
                                                .add(data)
                                                .addOnCompleteListener(CameraXLivePreviewActivity.this, new OnCompleteListener<DocumentReference>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentReference> task) {
                                                        if (task.isSuccessful()) {
                                                            Toast.makeText(CameraXLivePreviewActivity.this,
                                                                    "Successfully uploaded data",
                                                                    Toast.LENGTH_SHORT).show();
                                                            Intent intent = new Intent(CameraXLivePreviewActivity.this, SurveyWebViewActivity.class);
                                                            intent.putExtra("id", task.getResult().getId());
                                                            startActivity(intent);
                                                        } else {
                                                            Toast.makeText(CameraXLivePreviewActivity.this,
                                                                    "Something went wrong while uploading data",
                                                                    Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
//                                                .
//                                        addOnSuccessListener(new OnSuccessListener<Void>() {
//                                            @Override
//                                            public void onSuccess(Void aVoid) {
//                                                Toast.makeText(CameraXLivePreviewActivity.this,
//                                                        "Successfully uploaded data",
//                                                        Toast.LENGTH_SHORT).show();
//                                            }
//                                        })
//                                                .addOnFailureListener(new OnFailureListener() {
//                                                    @Override
//                                                    public void onFailure(@NonNull Exception e) {
//                                                        Log.d(TAG, "Error writing document", e);
//                                                        Toast.makeText(CameraXLivePreviewActivity.this,
//                                                                "Something went wrong while uploading data",
//                                                                Toast.LENGTH_SHORT).show();
//                                                    }
//                                                });
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create().show();


                        SharedPreferences sharedPref = getSharedPreferences("appData", Context.MODE_MULTI_PROCESS);
                        SharedPreferences.Editor prefEditor = getSharedPreferences("appData", Context.MODE_MULTI_PROCESS).edit();
                        prefEditor.putString("json", str);
                        prefEditor.apply();


                        PoseDetectorProcessor.poseList = new ArrayList();
                        PoseDetectorProcessor.timestamps = new ArrayList<>();

                        recordbutton.setEnabled(false);
                        recordbutton.setImageResource(R.drawable.recordbutton_disabled);
                        curreentstatus = status.started;
                        selectedModel = FACE_DETECTION;
                        bindAnalysisUseCase();
                    }
                }


                //to do record joints into a list with timestamp

                //Toast.makeText(getApplicationContext(),"Coordinates Generated",Toast.LENGTH_SHORT).show();
            }
        });
        //Spinner spinner = findViewById(R.id.spinner);
        List<String> options = new ArrayList<>();
        options.add(FACE_DETECTION);
        options.add(OBJECT_DETECTION_CUSTOM);
        options.add(CUSTOM_AUTOML_OBJECT_DETECTION);
        options.add(FACE_DETECTION);

        options.add(POSE_DETECTION);
        /**

         // Creating adapter for spinner
         ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
         // Drop down layout style - list view with radio button
         dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
         // attaching data adapter to spinner
         spinner.setAdapter(dataAdapter);
         spinner.setOnItemSelectedListener(this);

         ToggleButton facingSwitch = findViewById(R.id.facing_switch);
         facingSwitch.setOnCheckedChangeListener(this);
         **/

        if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            new ViewModelProvider(this, AndroidViewModelFactory.getInstance(getApplication()))
                    .get(CameraXViewModel.class)
                    .getProcessCameraProvider()
                    .observe(
                            this,
                            provider -> {
                                cameraProvider = provider;
                                bindAllCameraUseCases();
                            });
        }

        ImageView settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(
                v -> {
                    Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                    intent.putExtra(
                            SettingsActivity.EXTRA_LAUNCH_SOURCE,
                            SettingsActivity.LaunchSource.CAMERAX_LIVE_PREVIEW);
                    startActivity(intent);
                });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(STATE_SELECTED_MODEL, selectedModel);
    }

    @Override
    public synchronized void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        selectedModel = parent.getItemAtPosition(pos).toString();
        Log.d(TAG, "Selected model: " + selectedModel);
        bindAnalysisUseCase();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing.
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (cameraProvider == null) {
            return;
        }
        int newLensFacing =
                lensFacing == CameraSelector.LENS_FACING_FRONT
                        ? CameraSelector.LENS_FACING_BACK
                        : CameraSelector.LENS_FACING_FRONT;
        CameraSelector newCameraSelector =
                new CameraSelector.Builder().requireLensFacing(newLensFacing).build();
        try {
            if (cameraProvider.hasCamera(newCameraSelector)) {
                Log.d(TAG, "Set facing to " + newLensFacing);
                lensFacing = newLensFacing;
                cameraSelector = newCameraSelector;
                bindAllCameraUseCases();
                return;
            }
        } catch (CameraInfoUnavailableException e) {
            // Falls through
        }
        Toast.makeText(
                        getApplicationContext(),
                        "This device does not have lens with facing: " + newLensFacing,
                        Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindAllCameraUseCases();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() {
        if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
            return;
        }
        if (cameraProvider == null) {
            return;
        }
        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        Preview.Builder builder = new Preview.Builder();
        Size targetResolution = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing);
        }
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution);
        }
        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, previewUseCase);
    }

    private void bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }
        if (imageProcessor != null) {
            imageProcessor.stop();
        }

        try {
            switch (selectedModel) {


                case FACE_DETECTION:
                    Log.i(TAG, "Using Face Detector Processor");
                    imageProcessor = new FaceDetectorProcessor(this);
                    handler = new Handler();

                    final Runnable r = new Runnable() {
                        public void run() {
                            if (curreentstatus == status.started) {
                                if (FaceDetectorProcessor.numfaces == 1) {
                                    recordbutton.setEnabled(true);
                                    recordbutton.setImageResource(R.drawable.recordbutton);
                                } else {
                                    recordbutton.setEnabled(false);
                                    recordbutton.setImageResource(R.drawable.recordbutton_disabled);
                                }
                            }

                            handler.postDelayed(this, 500);
                        }
                    };

                    handler.postDelayed(r, 500);
                    break;

                case POSE_DETECTION:
                    PoseDetectorOptionsBase poseDetectorOptions =
                            PreferenceUtils.getPoseDetectorOptionsForLivePreview(this);
                    boolean shouldShowInFrameLikelihood =
                            PreferenceUtils.shouldShowPoseDetectionInFrameLikelihoodLivePreview(this);
                    boolean visualizeZ = PreferenceUtils.shouldPoseDetectionVisualizeZ(this);
                    boolean rescaleZ = PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this);
                    boolean runClassification = PreferenceUtils.shouldPoseDetectionRunClassification(this);
                    imageProcessor =
                            new PoseDetectorProcessor(
                                    this,
                                    poseDetectorOptions,
                                    shouldShowInFrameLikelihood,
                                    visualizeZ,
                                    rescaleZ,
                                    runClassification,
                                    /* isStreamMode = */ true);

                    break;

                default:
                    throw new IllegalStateException("Invalid model name");
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: " + selectedModel, e);
            Toast.makeText(
                            getApplicationContext(),
                            "Can not create image processor: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG)
                    .show();
            return;
        }


        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        Size targetResolution = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing);
        }
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution);
        }
        analysisUseCase = builder.build();

        needUpdateGraphicOverlayImageSourceInfo = true;
        analysisUseCase.setAnalyzer(
                // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                // thus we can just runs the analyzer itself on main thread.
                ContextCompat.getMainExecutor(this),
                imageProxy -> {
                    if (needUpdateGraphicOverlayImageSourceInfo) {
                        boolean isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT;
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        if (rotationDegrees == 0 || rotationDegrees == 180) {
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getWidth(), imageProxy.getHeight(), isImageFlipped);
                        } else {
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getHeight(), imageProxy.getWidth(), isImageFlipped);
                        }
                        needUpdateGraphicOverlayImageSourceInfo = false;
                    }
                    try {
                        imageProcessor.processImageProxy(imageProxy, graphicOverlay);
                    } catch (MlKitException e) {
                        Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });

        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, analysisUseCase);
    }

}
