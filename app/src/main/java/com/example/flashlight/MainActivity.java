package com.example.flashlight;

import static android.hardware.Camera.Parameters.FLASH_MODE_AUTO;
import static android.hardware.Camera.Parameters.FLASH_MODE_ON;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener {

    SwitchCompat toggle;
    EditText input;
    Context context;
    boolean hasFlash;
    Camera camera;
    boolean startUp;
    CameraManager cameraManager;
    GestureDetectorCompat gestureDetectorCompat;
    final int swipeThreshold = 100;
    final int swipeVelocityThreshold = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toggle = findViewById(R.id.toggle);
        input = findViewById(R.id.input);
        context = getApplicationContext();
        startUp = true;
        gestureDetectorCompat = new GestureDetectorCompat(this, this);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        hasFlash = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (hasFlash) {
            Handler handler = new Handler(getMainLooper());
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
                    @Override
                    public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
                        super.onTorchModeChanged(cameraId, enabled);
                        toggle.setChecked(enabled);
                        startUp = false;
                    }
                };
                cameraManager.registerTorchCallback(torchCallback, handler);
            }
        }

        toggle.setOnCheckedChangeListener((compoundButton, b) -> {
            if (hasFlash) {
                if (b && !startUp) {
                    turnOn();
                } else if (!b && !startUp) {
                    turnOff();
                }
            } else {
                Toast.makeText(this, "No Flashlight for this device", Toast.LENGTH_LONG).show();
            }
        });

        input.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_ENTER) {
                if (input.getText().toString().equalsIgnoreCase("ON")) {
                    toggle.setChecked(true);
                } else if (input.getText().toString().equalsIgnoreCase("OFF")) {
                    toggle.setChecked(false);
                }
            }
            return true;
        });
    }

    private String getFlashOnParameter() {
        List<String> flashModes = camera.getParameters().getSupportedFlashModes();
        if (flashModes.contains(FLASH_MODE_TORCH)) {
            return FLASH_MODE_TORCH;
        } else if (flashModes.contains(FLASH_MODE_ON)) {
            return FLASH_MODE_ON;
        } else if (flashModes.contains(FLASH_MODE_AUTO)) {
            return FLASH_MODE_AUTO;
        }
        throw new RuntimeException();
    }

    private void turnOn() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                String cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, true);
            } catch (Exception ignored) {
            }
        } else {
            camera = Camera.open();
            Camera.Parameters p = camera.getParameters();
            p.setFlashMode(getFlashOnParameter());
            camera.setParameters(p);
            SurfaceTexture preview = new SurfaceTexture(0);
            try {
                camera.setPreviewTexture(preview);
                camera.startPreview();
            } catch (IOException ignored) {

            }
        }
    }

    private void turnOff() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                String cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, false);
            } catch (Exception ignored) {
            }
        } else {
            camera = Camera.open();
            camera.stopPreview();
            camera.release();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.gestureDetectorCompat.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        try {
            float diffY = motionEvent1.getY() - motionEvent.getY();
            float diffX = motionEvent1.getX() - motionEvent.getX();
            if (Math.abs(diffY) > swipeThreshold && Math.abs(diffX) > swipeVelocityThreshold) {
                toggle.setChecked(!(diffY > 0));
            }
        } catch (Exception ignored) {

        }
        return true;
    }
}