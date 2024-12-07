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

    /**
     * Checks if the app has all the necessary permissions.
     *
     * @return true if all permissions are granted, false otherwise.
     */
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

    /**
     * Requests all necessary permissions based on the Android version.
     */
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

    /**
     * Shows a dialog explaining why the app needs certain permissions.
     */
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

    /**
     * Shows a dialog directing the user to app settings to manually enable permissions.
     */
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

    /**
     * Opens the application's settings page.
     */
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

    /**
     * Sets up the camera using CameraX.
     */
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

    /**
     * Binds the camera use cases (Preview, ImageCapture, VideoCapture).
     *
     * @param cameraProvider The camera provider.
     */
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

    /**
     * Switches the UI and functionality to Photo Mode.
     */
    private void switchToPhotoMode() {
        if (capturePhotoButton != null && captureVideoButton != null && modeTextView != null) {
            capturePhotoButton.setVisibility(View.VISIBLE);
            captureVideoButton.setVisibility(View.GONE);
            modeTextView.setText("Photo"); // Update modeTextView
            Toast.makeText(this, "Switched to Photo Mode", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Switched to Video Mode", Toast.LENGTH_SHORT).show();
            Log.d("StoryActivity", "Switched to Video Mode.");
        }
    }

    /**
     * Captures a photo using CameraX and displays a preview.
     */
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

                Toast.makeText(this, isFlashOn ? "Flash turned ON" : "Flash turned OFF", Toast.LENGTH_SHORT).show();
                Log.d("StoryActivity", "Flash toggled: " + (isFlashOn ? "ON" : "OFF"));
            } catch (SecurityException e) {
                Toast.makeText(this, "Security exception toggling flash: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
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

    /**
     * Sends the captured media to a friend (upload to Firebase).
     */
    private void sendToFriend() {
        // Implement sending the captured photo or video to a friend
        Toast.makeText(this, "Sending media to friend...", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "Uploading media to your story...", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        if (capturedPhotoFile != null && capturedPhotoFile.exists()) {
            uploadPhotoToFirebase(capturedPhotoFile, mode, currentUser.getUid());
        } else if (capturedVideoUri != null) { // Check if a video is captured
            uploadVideoToFirebase(capturedVideoUri, mode, currentUser.getUid());
        } else {
            Toast.makeText(this, "No media to upload", Toast.LENGTH_SHORT).show();
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
        if (photoFile == null) {
            Log.e("StoryActivity", "Upload failed: Photo file is null");
            Toast.makeText(this, "Error: No photo file found", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("StoryActivity", "Checking file before upload...");
        Log.d("StoryActivity", "- Path: " + photoFile.getAbsolutePath());
        Log.d("StoryActivity", "- Exists: " + photoFile.exists());
        Log.d("StoryActivity", "- Can read: " + photoFile.canRead());
        Log.d("StoryActivity", "- Size (bytes): " + photoFile.length());

        if (!photoFile.exists()) {
            Log.e("StoryActivity", "Upload failed: Photo file does not exist at path: " + photoFile.getAbsolutePath());
            Toast.makeText(this, "Error: Photo file doesn't exist", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!photoFile.canRead()) {
            Log.e("StoryActivity", "Upload failed: Cannot read photo file at path: " + photoFile.getAbsolutePath());
            Toast.makeText(this, "Error: Cannot read photo file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile.length() == 0) {
            Log.e("StoryActivity", "Upload failed: Photo file is empty");
            Toast.makeText(this, "Error: Photo file is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Size validation
        long fileSizeInBytes = photoFile.length();
        long fileSizeInMB = fileSizeInBytes / (1024 * 1024);
        Log.d("StoryActivity", "File size: " + fileSizeInMB + "MB (" + fileSizeInBytes + " bytes)");

        if (fileSizeInMB > 5) {
            Log.e("StoryActivity", "Upload failed: File size exceeds 5MB limit");
            Toast.makeText(this, "Error: Photo exceeds 5MB size limit", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase Storage path construction
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileName = "photo_" + timestamp + ".jpg";
        String storagePath = String.format("photos/%s/%s", userId, fileName);

        Log.d("StoryActivity", "Storage details:");
        Log.d("StoryActivity", "- File name: " + fileName);
        Log.d("StoryActivity", "- Storage path: " + storagePath);

        // Create storage reference
        StorageReference photoRef = FirebaseStorage.getInstance().getReference().child(storagePath);
        Log.d("StoryActivity", "- Full storage reference: " + photoRef.toString());

        // Create metadata
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("userId", userId)
                .setCustomMetadata("uploadMode", mode)
                .setCustomMetadata("timestamp", timestamp)
                .build();
        Log.d("StoryActivity", "Metadata created: " + metadata.toString());

        // Create upload task
        Uri fileUri = Uri.fromFile(photoFile);
        Log.d("StoryActivity", "File URI created: " + fileUri.toString());

        UploadTask uploadTask = photoRef.putFile(fileUri, metadata);
        Log.d("StoryActivity", "Upload task created and starting...");

        // Monitor upload progress
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            Log.d("StoryActivity", String.format("Upload progress: %.1f%%", progress));
        }).addOnSuccessListener(taskSnapshot -> {
            Log.d("StoryActivity", "Upload successful. Getting download URL...");

            photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d("StoryActivity", "Download URL obtained: " + downloadUrl);

                // Create media object
                long currentTimeMillis = System.currentTimeMillis();
                long expiresAt = currentTimeMillis + (24 * 60 * 60 * 1000);

                Media media = new Media(
                        userId,
                        downloadUrl,
                        "photo",
                        currentTimeMillis,
                        fileName,
                        expiresAt
                );

                // Save to Firestore
                Log.d("StoryActivity", "Saving to Firestore path: users/" + userId + "/stories");
                firestore.collection("users")
                        .document(userId)
                        .collection("stories")
                        .add(media)
                        .addOnSuccessListener(documentReference -> {
                            String docId = documentReference.getId();
                            media.setMediaId(docId);
                            Log.d("StoryActivity", "Firestore document created with ID: " + docId);

                            // Clean up and reset
                            if (photoFile.exists() && photoFile.delete()) {
                                Log.d("StoryActivity", "Local photo file deleted");
                            }

                            resetUIAfterUpload();
                            capturedPhotoFile = null;

                            Toast.makeText(StoryActivity.this, "Photo uploaded successfully!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("StoryActivity", "Firestore save failed", e);
                            Toast.makeText(StoryActivity.this, "Failed to save photo metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }).addOnFailureListener(e -> {
                Log.e("StoryActivity", "Failed to get download URL", e);
                Toast.makeText(this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Log.e("StoryActivity", "Upload failed for path: " + storagePath, e);
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }).addOnCanceledListener(() -> {
            Log.d("StoryActivity", "Upload canceled for path: " + storagePath);
            Toast.makeText(this, "Upload canceled", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "No video to upload", Toast.LENGTH_SHORT).show();
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

                            Toast.makeText(this, "Video story uploaded successfully!", Toast.LENGTH_SHORT).show();
                            Log.d("StoryActivity", "Video story metadata added to Firestore with ID: " + documentReference.getId());
                            resetUIAfterUpload();

                            // Clear the capturedVideoUri after upload
                            capturedVideoUri = null;
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to add metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("StoryActivity", "Error adding metadata to Firestore.", e);
                        });
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to retrieve download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("StoryActivity", "Error getting download URL.", e);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to upload video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Video capture not initialized", Toast.LENGTH_SHORT).show();
            Log.e("StoryActivity", "Attempted to record video, but VideoCapture is null.");
            return;
        }

        if (isRecordingVideo) {
            if (recording != null) {
                try {
                    recording.stop();
                    Log.d("StoryActivity", "Recording stopped.");
                } catch (SecurityException e) {
                    Toast.makeText(this, "Security exception stopping recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("StoryActivity", "Security exception stopping recording.", e);
                }
                recording = null;
            }
            isRecordingVideo = false;
            captureVideoButton.setImageResource(R.drawable.video_capture_button);
            modeSwitchButton.setEnabled(true);
            Toast.makeText(this, "Video recording stopped", Toast.LENGTH_SHORT).show();
            Log.d("StoryActivity", "Video recording stopped.");
        } else {
            if (!hasPermissions()) {
                requestNecessaryPermissions();
                Toast.makeText(this, "Permissions are required to record video", Toast.LENGTH_SHORT).show();
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

                Toast.makeText(this, "Preparing to record video...", Toast.LENGTH_SHORT).show();
                Log.d("StoryActivity", "Preparing to record video...");
            } catch (SecurityException e) {
                Toast.makeText(this, "Security exception starting recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("StoryActivity", "Security exception starting recording.", e);
            }
        }
    }

    private void handleRecordingStart() {
        isRecordingVideo = true;
        runOnUiThread(() -> {
            captureVideoButton.setImageResource(R.drawable.ic_video_capture);
            modeSwitchButton.setEnabled(false);
            Toast.makeText(this, "Video recording started", Toast.LENGTH_SHORT).show();
        });
        Log.d("StoryActivity", "Video recording started.");
    }

    private void handleRecordingFinalize(VideoRecordEvent.Finalize finalizeEvent) {
        if (!finalizeEvent.hasError()) {
            Uri videoUri = finalizeEvent.getOutputResults().getOutputUri();
            capturedVideoUri = videoUri; // Store the video URI
            runOnUiThread(() -> {
                Toast.makeText(this, "Video saved successfully", Toast.LENGTH_SHORT).show();
                previewImageView.setImageURI(videoUri);
                previewImageView.setVisibility(View.VISIBLE);
                sendButton.setVisibility(View.VISIBLE);
                uploadButton.setVisibility(View.VISIBLE);
                capturePhotoButton.setVisibility(View.GONE);
                captureVideoButton.setVisibility(View.GONE);
            });
            Log.d("StoryActivity", "Video saved at: " + videoUri);
        } else {
            runOnUiThread(() -> {
                Toast.makeText(this, "Error recording video: " + finalizeEvent.getError(), Toast.LENGTH_SHORT).show();
            });
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
}
