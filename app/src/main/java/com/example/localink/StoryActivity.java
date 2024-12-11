// File: StoryActivity.java
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
import android.widget.ToggleButton;
import android.widget.TextView;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
    private Uri capturedVideoUri; // Added to store video URI

    // Firebase Instances
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseFirestore firestore;

    // Constants
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);

        // Initialize Firebase Instances
        mAuth = FirebaseAuth.getInstance(); // Already initialized in LoginActivity
        storage = FirebaseStorage.getInstance(); // Initialize FirebaseStorage
        storageReference = storage.getReference();
        firestore = FirebaseFirestore.getInstance(); // Already initialized in LoginActivity
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Check if user is signed in (non-null)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Not signed in, redirect to LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

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
        ImageButton exitButton = findViewById(R.id.exitButton);
        uploadButton = findViewById(R.id.uploadButton);
        modeTextView = findViewById(R.id.modeTextView); // Ensure this exists in XML

        // Log for missing capture buttons
        if (capturePhotoButton == null) {
            Log.e("StoryActivity", "Capture Photo button (capturePhotoButton) not found.");
        }

        if (captureVideoButton == null) {
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
                switchToVideoMode();
            } else {
                switchToPhotoMode();
            }
        });

        exitButton.setOnClickListener(v -> onBackPressed());

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
        boolean readImagesGranted = true;
        boolean readVideosGranted = true;
        boolean readAudioGranted = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            readImagesGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
            readVideosGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
            readAudioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29-32
            readImagesGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
            readVideosGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else { // API < 29
            boolean readStorageGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean writeStorageGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            readImagesGranted = readStorageGranted;
            readVideosGranted = readStorageGranted;
            readAudioGranted = readStorageGranted;
        }

        Log.d("PermissionsStatus", "Camera Granted: " + cameraGranted +
                ", Audio Granted: " + audioGranted +
                ", Read Images Granted: " + readImagesGranted +
                ", Read Videos Granted: " + readVideosGranted +
                ", Read Audio Granted: " + readAudioGranted);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            return cameraGranted && audioGranted && readImagesGranted && readVideosGranted && readAudioGranted;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29-32
            return cameraGranted && audioGranted && readImagesGranted && readVideosGranted;
        } else { // API < 29
            return cameraGranted && audioGranted && readImagesGranted && readVideosGranted;
        }
    }

    private void requestNecessaryPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
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
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO
                    },
                    PERMISSION_REQUEST_CODE);
        } else { // API < 29
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
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Log.e("StoryActivity", "Error setting up camera.", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build();
        videoCapture = VideoCapture.withOutput(recorder);

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(isUsingFrontCamera ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK)
                .build();

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture);

            if (videoCapture != null && imageCapture != null && camera != null) {
                if (capturePhotoButton != null) {
                    capturePhotoButton.setEnabled(true);
                }
                if (captureVideoButton != null) {
                    captureVideoButton.setEnabled(true);
                }
                Log.d("StoryActivity", "Camera and VideoCapture initialized successfully.");
            } else {
                Log.e("StoryActivity", "VideoCapture is null after binding.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("StoryActivity", "Failed to bind camera use cases.", e);
        }
    }


    /**
     * Switches the UI and functionality to Photo Mode.
     */
    private void switchToPhotoMode() {
        if (capturePhotoButton != null && captureVideoButton != null && modeTextView != null) {
            capturePhotoButton.setVisibility(View.VISIBLE);
            captureVideoButton.setVisibility(View.GONE);
            modeTextView.setText("Photo"); // Update modeTextView
            Log.d("StoryActivity", "Switched to Photo Mode.");
        }
    }

    /**
     * Switches the UI and functionality to Video Mode.
     */
    private void switchToVideoMode() {
        if (capturePhotoButton != null && captureVideoButton != null && modeTextView != null) {
            capturePhotoButton.setVisibility(View.GONE);
            captureVideoButton.setVisibility(View.VISIBLE);
            modeTextView.setText("Video"); // Update modeTextView
            Log.d("StoryActivity", "Switched to Video Mode.");
        }
    }

    /**
     * Captures a photo using CameraX and displays a preview.
     */
    private void capturePhoto() {
        if (imageCapture == null) {
            Log.e("StoryActivity", "Attempted to capture photo, but ImageCapture is null.");
            return;
        }

        // Before capturing, check for required permissions
        if (!hasPermissions()) {
            // Permissions not granted, request them
            requestNecessaryPermissions();
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
                    Log.d("StoryActivity", "Photo captured and saved: " + capturedPhotoFile.getAbsolutePath());
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e("StoryActivity", "Error capturing photo.", exception);
                }
            });
        } catch (SecurityException e) {
            Log.e("StoryActivity", "Security exception capturing photo.", e);
        }
    }

    /**
     * Toggles the flash (torch) on/off.
     */
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

                Log.d("StoryActivity", "Flash toggled: " + (isFlashOn ? "ON" : "OFF"));
            } catch (SecurityException e) {
                Log.e("StoryActivity", "Security exception toggling flash.", e);
            }
        }
    }

    /**
     * Switches between front and back cameras.
     */
    private void switchCamera() {
        isUsingFrontCamera = !isUsingFrontCamera;  // Toggle the camera
        if (camera != null) {
            try {
                camera.getCameraControl().enableTorch(false);  // Turn off flash when switching
                isFlashOn = false;
                flashButton.setImageResource(R.drawable.ic_flash_off);  // Reset flash icon
                Log.d("StoryActivity", "Flash turned off during camera switch.");
            } catch (SecurityException e) {
                Log.e("StoryActivity", "Security exception toggling torch.", e);
            }
        }

        // Rebind camera use cases with the new camera selector
        try {
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            bindCameraUseCases(cameraProvider);
            Log.d("StoryActivity", "Camera use cases rebound with " + (isUsingFrontCamera ? "front" : "back") + " camera.");
        } catch (ExecutionException | InterruptedException e) {
            Log.e("StoryActivity", "Error switching camera.", e);
        }
    }

    /**
     * Sends the captured media to a friend (upload to Firebase).
     */
    private void sendToFriend() {
        // Implement sending the captured photo or video to a friend
        Log.d("StoryActivity", "sendToFriend() called.");
        // Add your sending logic here
        // For this implementation, we'll treat 'send' as uploading to Firestore
        uploadMedia("send");
    }

    /**
     * Uploads the captured media to the user's story (upload to Firebase).
     */
    private void uploadToStory() {
        // Implement uploading the captured photo or video to your story
        Log.d("StoryActivity", "uploadToStory() called.");
        // Add your uploading logic here
        // For this implementation, we'll treat 'upload' as uploading to Firestore
        uploadMedia("upload");
    }

    /**
     * Uploads the captured media to Firebase Storage and stores metadata in Firestore.
     *
     * @param mode The mode of upload ("send" or "upload").
     */
    private void uploadMedia(String mode) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        if (capturedPhotoFile != null && capturedPhotoFile.exists()) {
            uploadPhotoToFirebase(capturedPhotoFile, mode, currentUser.getUid());
        } else if (capturedVideoUri != null) { // Check if a video is captured
            uploadVideoToFirebase(capturedVideoUri, mode, currentUser.getUid());
        }
    }

    /**
     * Uploads a photo to Firebase Storage and stores its metadata in Firestore.
     *
     * @param photoFile The photo file to upload.
     * @param mode      The mode of upload ("send" or "upload").
     * @param userId    The ID of the current user.
     */
    private void uploadPhotoToFirebase(File photoFile, String mode, String userId) {
        // Initial validation
        if (photoFile == null || !photoFile.exists() || !photoFile.canRead() || photoFile.length() == 0) {
            return;
        }

        // Size validation
        long fileSizeInMB = photoFile.length() / (1024 * 1024);
        if (fileSizeInMB > 5) {
            return;
        }

        // Firebase Storage path construction
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileName = "photo_" + timestamp + ".jpg";
        String storagePath = String.format("photos/%s/%s", userId, fileName);

        // Create storage reference
        StorageReference photoRef = FirebaseStorage.getInstance().getReference().child(storagePath);

        // Create metadata
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("userId", userId)
                .setCustomMetadata("uploadMode", mode)
                .setCustomMetadata("timestamp", timestamp)
                .build();

        // Create upload task
        Uri fileUri = Uri.fromFile(photoFile);
        UploadTask uploadTask = photoRef.putFile(fileUri, metadata);

        // Monitor upload progress
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();

                Media media = new Media(
                        userId,
                        downloadUrl,
                        "photo",
                        System.currentTimeMillis(),
                        fileName,
                        System.currentTimeMillis() + (24 * 60 * 60 * 1000)
                );

                firestore.collection("users")
                        .document(userId)
                        .collection("stories")
                        .add(media)
                        .addOnSuccessListener(documentReference -> {
                            String docId = documentReference.getId();
                            media.setMediaId(docId);
                            documentReference.update("mediaId", docId);
                            if (photoFile.exists()) {
                                photoFile.delete();
                            }
                            resetUIAfterUpload();
                            capturedPhotoFile = null;
                        });
            });
        });
    }

    /**
     * Uploads a video to Firebase Storage and stores its metadata in Firestore.
     *
     * @param videoUri The URI of the video to upload.
     * @param mode     The mode of upload ("send" or "upload").
     * @param userId   The ID of the current user.
     */
    private void uploadVideoToFirebase(Uri videoUri, String mode, String userId) {
        if (videoUri == null) {
            return;
        }

        // Generate a unique file name for the video
        String fileName = "video_" + System.currentTimeMillis() + ".mp4";
        StorageReference videoRef = storageReference.child("videos/" + userId + "/" + fileName);

        // Upload the file to Firebase Storage
        UploadTask uploadTask = videoRef.putFile(videoUri);

        // Monitor upload success/failure
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Get the download URL after successful upload
            videoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();

                // Compute the current time and the expiration time (24 hours later)
                long currentTimeMillis = System.currentTimeMillis();
                long expiresAt = currentTimeMillis + (24 * 60 * 60 * 1000); // 24 hours in milliseconds

                // Create a Media object with all required fields
                Media media = new Media(
                        userId,
                        downloadUrl,
                        "video",
                        currentTimeMillis,
                        fileName,
                        expiresAt
                );

                // Save the media metadata under the user's 'stories' subcollection
                firestore.collection("users").document(userId)
                        .collection("stories")
                        .add(media)
                        .addOnSuccessListener(documentReference -> {
                            // Set the mediaId to the generated document ID
                            media.setMediaId(documentReference.getId());

                            Log.d("StoryActivity", "Video story metadata added to Firestore with ID: " + documentReference.getId());
                            resetUIAfterUpload();

                            // Clear the capturedVideoUri after upload
                            capturedVideoUri = null;
                        })
                        .addOnFailureListener(e -> {
                            Log.e("StoryActivity", "Error adding metadata to Firestore.", e);
                        });
            }).addOnFailureListener(e -> {
                Log.e("StoryActivity", "Error getting download URL.", e);
            });
        }).addOnFailureListener(e -> {
            Log.e("StoryActivity", "Error uploading video to Firebase Storage.", e);
        });
    }


    /**
     * Resets the UI elements after a successful upload.
     */
    private void resetUIAfterUpload() {
        previewImageView.setVisibility(View.GONE);
        sendButton.setVisibility(View.GONE);
        uploadButton.setVisibility(View.GONE);
        capturePhotoButton.setVisibility(View.VISIBLE);
        captureVideoButton.setVisibility(View.VISIBLE);
    }

    /**
     * Toggles video recording on/off.
     */
    private void toggleVideoRecording() {
        if (videoCapture == null) {
            Log.e("StoryActivity", "Attempted to record video, but VideoCapture is null.");
            return;
        }

        if (isRecordingVideo) {
            if (recording != null) {
                try {
                    recording.stop();
                    Log.d("StoryActivity", "Recording stopped.");
                } catch (SecurityException e) {
                    Log.e("StoryActivity", "Security exception stopping recording.", e);
                }
                recording = null;
            }
            isRecordingVideo = false;
            captureVideoButton.setImageResource(R.drawable.video_capture_button);
            modeSwitchButton.setEnabled(true);
            Log.d("StoryActivity", "Video recording stopped.");
        } else {
            if (!hasPermissions()) {
                requestNecessaryPermissions();
                Log.w("StoryActivity", "Permissions not granted. Recording cannot start.");
                return;
            }

            try {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "video_" + System.currentTimeMillis());
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
                contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/LocalInk");

                MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(
                        getContentResolver(),
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                        .setContentValues(contentValues)
                        .build();

                recording = videoCapture.getOutput()
                        .prepareRecording(this, options)
                        .withAudioEnabled()
                        .start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                                handleRecordingStart();
                            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                                handleRecordingFinalize((VideoRecordEvent.Finalize) videoRecordEvent);
                            }
                        });

                Log.d("StoryActivity", "Preparing to record video...");
            } catch (SecurityException e) {
                Log.e("StoryActivity", "Security exception starting recording.", e);
            }
        }
    }

    private void handleRecordingStart() {
        isRecordingVideo = true;
        runOnUiThread(() -> {
            captureVideoButton.setImageResource(R.drawable.ic_video_capture);
            modeSwitchButton.setEnabled(false);
        });
        Log.d("StoryActivity", "Video recording started.");
    }

    private void handleRecordingFinalize(VideoRecordEvent.Finalize finalizeEvent) {
        if (!finalizeEvent.hasError()) {
            Uri videoUri = finalizeEvent.getOutputResults().getOutputUri();
            capturedVideoUri = videoUri; // Store the video URI
            runOnUiThread(() -> {
                previewImageView.setImageURI(videoUri);
                previewImageView.setVisibility(View.VISIBLE);
                sendButton.setVisibility(View.VISIBLE);
                uploadButton.setVisibility(View.VISIBLE);
                capturePhotoButton.setVisibility(View.GONE);
                captureVideoButton.setVisibility(View.GONE);
            });
            Log.d("StoryActivity", "Video saved at: " + videoUri);
        } else {
            Log.e("StoryActivity", "Error recording video: " + finalizeEvent.getError());
        }

        isRecordingVideo = false;
        runOnUiThread(() -> {
            captureVideoButton.setImageResource(R.drawable.video_capture_button);
            modeSwitchButton.setEnabled(true);
        });
        Log.d("StoryActivity", "Video recording finalized.");
    }

    /**
     * Handles the cleanup of camera use cases when the activity is destroyed.
     */
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
