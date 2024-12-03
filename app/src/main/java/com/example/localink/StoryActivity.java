package com.example.localink;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.Button;
import android.content.pm.PackageManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.util.concurrent.ExecutionException;
import androidx.camera.core.ImageCaptureException;

public class StoryActivity extends AppCompatActivity {

    private ImageView captureButton, flashButton, previewImageView;  // For capturing and preview
    private ImageButton profileButton;
    private Button sendButton, uploadButton;  // For sending to friend or uploading to story
    private androidx.camera.view.PreviewView cameraPreviewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private ImageCapture imageCapture;
    private Camera camera;
    private boolean isFlashOn = false; // Track flash state
    private File capturedPhotoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);

        // Initialize UI elements
        captureButton = findViewById(R.id.captureButton);
        profileButton = findViewById(R.id.profileButton);
        flashButton = findViewById(R.id.flashIndicator);
        sendButton = findViewById(R.id.sendButton);
        uploadButton = findViewById(R.id.uploadButton);
        cameraPreviewView = findViewById(R.id.cameraPreviewView);
        previewImageView = findViewById(R.id.previewImageView); // For displaying captured photo

        // Hide send/upload buttons initially
        sendButton.setVisibility(View.GONE);
        uploadButton.setVisibility(View.GONE);
        previewImageView.setVisibility(View.GONE);

        // Check for permissions and request if necessary
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    100);  // Request code for permissions
        } else {
            setupCamera();
        }

        // Set up listener for capture button
        captureButton.setOnClickListener(v -> capturePhoto());

        // Set up listener for profile button (navigate back to MainActivity)
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(StoryActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Set up listener for flash button (toggle flash)
        flashButton.setOnClickListener(v -> toggleFlash());

        // Set up listener for send button (send photo to friend)
        sendButton.setOnClickListener(v -> {
            sendToFriend();
        });

        // Set up listener for upload button (upload photo to story)
        uploadButton.setOnClickListener(v -> {
            uploadToStory();
        });
    }

    private void setupCamera() {
        // Set up CameraX preview
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, getMainExecutor());
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

        // ImageCapture use case (for taking photos)
        imageCapture = new ImageCapture.Builder().build();

        // Camera selector (choose front camera for selfie)
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        // Bind use cases to camera
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void capturePhoto() {
        // Create file to save the photo (you can use a file path of your choice)
        capturedPhotoFile = new File(getExternalFilesDir(null), "photo_" + System.currentTimeMillis() + ".jpg");

        // Set up the ImageCapture output options
        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(capturedPhotoFile).build();

        // Capture the photo
        imageCapture.takePicture(options, getMainExecutor(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                // File saved successfully, show the preview and options
                previewImageView.setImageURI(Uri.fromFile(capturedPhotoFile));
                previewImageView.setVisibility(View.VISIBLE);
                sendButton.setVisibility(View.VISIBLE);
                uploadButton.setVisibility(View.VISIBLE);
                captureButton.setVisibility(View.GONE);  // Hide capture button
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                // Handle error if photo capture fails
                Toast.makeText(StoryActivity.this, "Error capturing photo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleFlash() {
        if (camera != null) {
            isFlashOn = !isFlashOn;
            // Toggle the flash mode
            camera.getCameraControl().enableTorch(isFlashOn);

            // Change the flash button icon depending on the state
            if (isFlashOn) {
                flashButton.setImageResource(R.drawable.ic_flash_on);  // Change to "on" icon
            } else {
                flashButton.setImageResource(R.drawable.ic_flash_off);  // Change to "off" icon
            }
        }
    }

    private void sendToFriend() {
        // Implement sending the captured photo to a friend
        Toast.makeText(this, "Sending photo to friend...", Toast.LENGTH_SHORT).show();
        // You can use Firebase or another method to send the photo
    }

    private void uploadToStory() {
        // Implement uploading the captured photo to your story
        Toast.makeText(this, "Uploading photo to your story...", Toast.LENGTH_SHORT).show();
        // You can upload the photo to Firebase or your storage here
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
        } else {
            Toast.makeText(this, "Permissions denied, cannot access camera", Toast.LENGTH_SHORT).show();
        }
    }
}
