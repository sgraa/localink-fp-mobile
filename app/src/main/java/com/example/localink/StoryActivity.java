package com.example.localink;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.util.concurrent.ExecutionException;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Quality;
import androidx.camera.video.Recording;

public class StoryActivity extends AppCompatActivity {

    // UI Elements
    private ToggleButton modeSwitchButton;
    private ImageButton capturePhotoButton, captureVideoButton;
    private ImageView flashButton, previewImageView;
    private ImageButton profileButton, switchCameraButton;
    private Button sendButton, uploadButton;
    private PreviewView cameraPreviewView;
    private TextView modeTextView;

    // CameraX Variables
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recorder recorder;
    private Recording recording;
    private CameraSelector cameraSelector;
    private Camera camera;

    // State Variables
    private boolean isFlashOn = false;
    private boolean isUsingFrontCamera = true;
    private boolean isRecordingVideo = false;

    private File capturedPhotoFile;

    // Constants
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);

        // Initialize UI elements
        cameraPreviewView = findViewById(R.id.cameraPreviewView);
        modeSwitchButton = findViewById(R.id.modeSwitchButton); // ToggleButton in XML
        capturePhotoButton = findViewById(R.id.captureButton); // ImageButton in XML
        captureVideoButton = findViewById(R.id.videoCaptureButton); // ImageButton in XML
        flashButton = findViewById(R.id.flashIndicator);
        previewImageView = findViewById(R.id.previewImageView);
        profileButton = findViewById(R.id.profileButton);
        switchCameraButton = findViewById(R.id.switchCameraButton);
        sendButton = findViewById(R.id.sendButton);
        uploadButton = findViewById(R.id.uploadButton);
        modeTextView = findViewById(R.id.modeTextView); // Ensure this exists in XML

        // Check if capture buttons are correctly linked
        if (capturePhotoButton == null) {
            Toast.makeText(this, "Capture Photo button not found in layout", Toast.LENGTH_SHORT).show();
            Log.e("StoryActivity", "Capture Photo button (capturePhotoButton) not found.");
        }

        if (captureVideoButton == null) {
            Toast.makeText(this, "Capture Video button not found in layout", Toast.LENGTH_SHORT).show();
            Log.e("StoryActivity", "Capture Video button (captureVideoButton) not found.");
        }

        // Hide send/upload buttons and preview initially
        sendButton.setVisibility(View.GONE);
        uploadButton.setVisibility(View.GONE);
        previewImageView.setVisibility(View.GONE);

        // Disable capture buttons until camera is set up
        if (capturePhotoButton != null) {
            capturePhotoButton.setEnabled(false);
        }
        if (captureVideoButton != null) {
            captureVideoButton.setEnabled(false);
        }

        // Set up mode switch listener
        modeSwitchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Switch to Video Mode
                switchToVideoMode();
            } else {
                // Switch to Photo Mode
                switchToPhotoMode();
            }
        });

        // Set up capture buttons
        if (capturePhotoButton != null) {
            capturePhotoButton.setOnClickListener(v -> capturePhoto());
        }
        if (captureVideoButton != null) {
            captureVideoButton.setOnClickListener(v -> toggleVideoRecording());
        }

        // Set up other buttons
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(StoryActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        flashButton.setOnClickListener(v -> toggleFlash());
        switchCameraButton.setOnClickListener(v -> switchCamera());
        sendButton.setOnClickListener(v -> sendToFriend());
        uploadButton.setOnClickListener(v -> uploadToStory());

        // Check for permissions and request if necessary
        if (!hasPermissions()) {
            requestNecessaryPermissions();
        } else {
            setupCamera();
        }
    }

    private boolean hasPermissions() {
        boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean readImagesGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        boolean readVideosGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        boolean readAudioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;

        boolean readStorageGranted = true;
        boolean writeStorageGranted = true;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            readStorageGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            writeStorageGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        Log.d("PermissionsStatus", "Camera Granted: " + cameraGranted +
                ", Audio Granted: " + audioGranted +
                ", Read Images Granted: " + readImagesGranted +
                ", Read Videos Granted: " + readVideosGranted +
                ", Read Audio Granted: " + readAudioGranted +
                ", Read Storage Granted: " + readStorageGranted +
                ", Write Storage Granted: " + writeStorageGranted);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            return cameraGranted && audioGranted && readImagesGranted && readVideosGranted && readAudioGranted;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29-32
            return cameraGranted && audioGranted && readImagesGranted && readVideosGranted;
        } else { // API < 29
            return cameraGranted && audioGranted && readStorageGranted && writeStorageGranted;
        }
    }

    private void requestNecessaryPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            // For Android 13 and above
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO
                    },
                    PERMISSION_REQUEST_CODE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29-32
            // For Android 10-12
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO
                    },
                    PERMISSION_REQUEST_CODE);
        } else { // API < 29
            // For Android 9 and below
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void showPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app requires access to your camera and microphone to capture photos and videos.")
                .setPositiveButton("Grant", (dialog, which) -> requestNecessaryPermissions())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Permissions denied. App cannot function without required permissions.", Toast.LENGTH_LONG).show();
                    // Optionally, navigate the user back or disable certain features
                })
                .create()
                .show();
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app requires camera and audio permissions to function. Please enable them in app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> openAppSettings())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Permissions denied. App cannot function without required permissions.", Toast.LENGTH_LONG).show();
                    // Optionally, navigate the user back or disable certain features
                })
                .create()
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasPermissions()) {
            Log.d("PermissionsStatus", "Permissions are already granted.");
            setupCamera();
        } else {
            Log.d("PermissionsStatus", "Permissions are not granted yet.");
        }
    }

    private void setupCamera() {
        Log.d("StoryActivity", "Setting up camera...");
        // Set up CameraX preview
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error setting up camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("StoryActivity", "Error setting up camera.", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

        // ImageCapture use case (for taking photos)
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // VideoCapture use case (for recording videos)
        recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build();
        videoCapture = VideoCapture.withOutput(recorder);

        // Camera selector (front or back)
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(isUsingFrontCamera ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK)
                .build();

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture);

            // Check if VideoCapture is initialized
            if (videoCapture != null && imageCapture != null && camera != null) {
                if (capturePhotoButton != null) {
                    capturePhotoButton.setEnabled(true);
                }
                if (captureVideoButton != null) {
                    captureVideoButton.setEnabled(true);
                }
                Toast.makeText(this, "Camera and VideoCapture initialized successfully.", Toast.LENGTH_SHORT).show();
                Log.d("StoryActivity", "Camera and VideoCapture initialized successfully.");
            } else {
                Toast.makeText(this, "VideoCapture is null after binding.", Toast.LENGTH_SHORT).show();
                Log.e("StoryActivity", "VideoCapture is null after binding.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to bind camera use cases: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("StoryActivity", "Failed to bind camera use cases.", e);
        }
    }

    private void switchToPhotoMode() {
        if (capturePhotoButton != null && captureVideoButton != null && modeTextView != null) {
            capturePhotoButton.setVisibility(View.VISIBLE);
            captureVideoButton.setVisibility(View.GONE);
            modeTextView.setText("Photo"); // Update modeTextView
            Toast.makeText(this, "Switched to Photo Mode", Toast.LENGTH_SHORT).show();
            Log.d("StoryActivity", "Switched to Photo Mode.");
        }
    }

    private void switchToVideoMode() {
        if (capturePhotoButton != null && captureVideoButton != null && modeTextView != null) {
            capturePhotoButton.setVisibility(View.GONE);
            captureVideoButton.setVisibility(View.VISIBLE);
            modeTextView.setText("Video"); // Update modeTextView
            Toast.makeText(this, "Switched to Video Mode", Toast.LENGTH_SHORT).show();
            Log.d("StoryActivity", "Switched to Video Mode.");
        }
    }

    private void capturePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "Image capture not initialized", Toast.LENGTH_SHORT).show();
            Log.e("StoryActivity", "Attempted to capture photo, but ImageCapture is null.");
            return;
        }

        // Before capturing, check for required permissions
        if (!hasPermissions()) {
            // Permissions not granted, request them
            requestNecessaryPermissions();
            Toast.makeText(this, "Permissions are required to capture photo", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Proceed with capturing the photo
            capturedPhotoFile = new File(getExternalFilesDir(null), "photo_" + System.currentTimeMillis() + ".jpg");
            ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(capturedPhotoFile).build();

            imageCapture.takePicture(options, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    previewImageView.setImageURI(Uri.fromFile(capturedPhotoFile));
                    previewImageView.setVisibility(View.VISIBLE);
                    sendButton.setVisibility(View.VISIBLE);
                    uploadButton.setVisibility(View.VISIBLE);
                    capturePhotoButton.setVisibility(View.GONE);  // Hide capture photo button
                    captureVideoButton.setVisibility(View.GONE);   // Hide capture video button
                    Toast.makeText(StoryActivity.this, "Photo captured successfully!", Toast.LENGTH_SHORT).show();
                    Log.d("StoryActivity", "Photo captured and saved: " + capturedPhotoFile.getAbsolutePath());
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Toast.makeText(StoryActivity.this, "Error capturing photo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("StoryActivity", "Error capturing photo.", exception);
                }
            });
        } catch (SecurityException e) {
            Toast.makeText(this, "Security exception capturing photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            Log.e("StoryActivity", "Security exception capturing photo.", e);
        }
    }

    private void toggleFlash() {
        if (camera != null) {
            isFlashOn = !isFlashOn;
            // Toggle the flash mode
            try {
                camera.getCameraControl().enableTorch(isFlashOn);

                // Change the flash button icon depending on the state
                if (isFlashOn) {
                    flashButton.setImageResource(R.drawable.ic_flash_on);  // Change to "on" icon
                } else {
                    flashButton.setImageResource(R.drawable.ic_flash_off);  // Change to "off" icon
                }

                Toast.makeText(this, isFlashOn ? "Flash turned ON" : "Flash turned OFF", Toast.LENGTH_SHORT).show();
                Log.d("StoryActivity", "Flash toggled: " + (isFlashOn ? "ON" : "OFF"));
            } catch (SecurityException e) {
                Toast.makeText(this, "Security exception toggling flash: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                Log.e("StoryActivity", "Security exception toggling flash.", e);
            }
        }
    }

    private void switchCamera() {
        isUsingFrontCamera = !isUsingFrontCamera;  // Toggle the camera
        if (camera != null) {
            try {
                camera.getCameraControl().enableTorch(false);  // Turn off flash when switching
                isFlashOn = false;
                flashButton.setImageResource(R.drawable.ic_flash_off);  // Reset flash icon
                Log.d("StoryActivity", "Flash turned off during camera switch.");
            } catch (SecurityException e) {
                Toast.makeText(this, "Security exception toggling torch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                Log.e("StoryActivity", "Security exception toggling torch.", e);
            }
        }

        // Rebind camera use cases with the new camera selector
        try {
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            bindCameraUseCases(cameraProvider);
            Log.d("StoryActivity", "Camera use cases rebound with " + (isUsingFrontCamera ? "front" : "back") + " camera.");
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error switching camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("StoryActivity", "Error switching camera.", e);
        }
    }

    private void sendToFriend() {
        // Implement sending the captured photo or video to a friend
        Toast.makeText(this, "Sending media to friend...", Toast.LENGTH_SHORT).show();
        Log.d("StoryActivity", "sendToFriend() called.");
        // Add your sending logic here
    }

    private void uploadToStory() {
        // Implement uploading the captured photo or video to your story
        Toast.makeText(this, "Uploading media to your story...", Toast.LENGTH_SHORT).show();
        Log.d("StoryActivity", "uploadToStory() called.");
        // Add your uploading logic here
    }

    private void toggleVideoRecording() {
        if (videoCapture == null) {
            Toast.makeText(this, "Video capture not initialized", Toast.LENGTH_SHORT).show();
            Log.e("StoryActivity", "Attempted to record video, but VideoCapture is null.");
            return;
        }

        if (isRecordingVideo) {
            // Stop recording
            if (recording != null) {
                try {
                    recording.stop(); // Stop the recording
                    Log.d("StoryActivity", "Recording stopped.");
                } catch (SecurityException e) {
                    Toast.makeText(this, "Security exception stopping recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    Log.e("StoryActivity", "Security exception stopping recording.", e);
                }
                recording = null;
            }
            isRecordingVideo = false;
            captureVideoButton.setImageResource(R.drawable.video_capture_button); // Change to record icon
            modeSwitchButton.setEnabled(true); // Re-enable mode switch
            Toast.makeText(this, "Video recording stopped", Toast.LENGTH_SHORT).show();
            Log.d("StoryActivity", "Video recording stopped.");
        } else {
            // Before starting recording, check for required permissions
            if (!hasPermissions()) {
                // Permissions not granted, request them
                requestNecessaryPermissions();
                Toast.makeText(this, "Permissions are required to record video", Toast.LENGTH_SHORT).show();
                Log.w("StoryActivity", "Permissions not granted. Recording cannot start.");
                return;
            }

            try {
                // Start recording
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, "video_" + System.currentTimeMillis() + ".mp4");
                contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/LocalInk"); // Save to Movies/LocalInk directory

                MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(
                        getContentResolver(),
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                ).setContentValues(contentValues)
                        .build();

                PendingRecording pendingRecording = videoCapture.getOutput().prepareRecording(this, options)
                        .withAudioEnabled(); // Enable audio if desired

                recording = pendingRecording.start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                    if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                        isRecordingVideo = true;
                        runOnUiThread(() -> {
                            captureVideoButton.setImageResource(R.drawable.ic_video_capture); // Change to stop icon
                            modeSwitchButton.setEnabled(false); // Disable mode switch
                            Toast.makeText(this, "Video recording started", Toast.LENGTH_SHORT).show();
                        });
                        Log.d("StoryActivity", "Video recording started.");
                    } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalize = (VideoRecordEvent.Finalize) videoRecordEvent;
                        if (!finalize.hasError()) {
                            Uri videoUri = finalize.getOutputResults().getOutputUri();
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Video saved: " + videoUri.toString(), Toast.LENGTH_SHORT).show();
                                previewImageView.setImageURI(videoUri);
                                previewImageView.setVisibility(View.VISIBLE);
                                sendButton.setVisibility(View.VISIBLE);
                                uploadButton.setVisibility(View.VISIBLE);
                                capturePhotoButton.setVisibility(View.GONE);  // Hide capture photo button
                                captureVideoButton.setVisibility(View.GONE);   // Hide capture video button
                            });
                            Log.d("StoryActivity", "Video saved at: " + videoUri.toString());
                        } else {
                            runOnUiThread(() -> Toast.makeText(this, "Error recording video: " + finalize.getError(), Toast.LENGTH_SHORT).show());
                            Log.e("StoryActivity", "Error recording video: " + finalize.getError());
                        }
                        isRecordingVideo = false;
                        runOnUiThread(() -> {
                            captureVideoButton.setImageResource(R.drawable.video_capture_button); // Reset to record icon
                            modeSwitchButton.setEnabled(true); // Re-enable mode switch
                        });
                        Log.d("StoryActivity", "Video recording finalized.");
                    }
                });
                Toast.makeText(this, "Preparing to record video...", Toast.LENGTH_SHORT).show();
                Log.d("StoryActivity", "Preparing to record video...");
            } catch (SecurityException e) {
                Toast.makeText(this, "Security exception starting recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                Log.e("StoryActivity", "Security exception starting recording.", e);
            }
        }
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            boolean someDeniedForever = false;

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                        // User denied with "Don't ask again"
                        someDeniedForever = true;
                    }
                }
            }

            if (allGranted) {
                setupCamera();
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
                Log.d("PermissionsStatus", "All permissions granted.");
            } else {
                if (someDeniedForever) {
                    // User denied with "Don't ask again"
                    showSettingsDialog();
                } else {
                    // User denied without "Don't ask again"
                    showPermissionRationale();
                }
                // Disable capture functionalities
                if (capturePhotoButton != null) {
                    capturePhotoButton.setEnabled(false);
                }
                if (captureVideoButton != null) {
                    captureVideoButton.setEnabled(false);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProviderFuture != null) {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                Log.d("StoryActivity", "Camera use cases unbound.");
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Log.e("StoryActivity", "Error unbinding camera use cases.", e);
            }
        }
    }
}
