package com.pavan.custom_camera;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "custom_camera";

    private static final int REQUEST_CODE_ALL_PERMISSIONS = 1;

    private static List<String> listPermissions;

    private Camera camera;
    private Camera.CameraInfo cameraInfo;
    private int cameraType;
    private int degrees = 0;
    private String flashType;

    private DisplayMetrics displayMetrics;

    private FrameLayout frameLayoutCameraPreview;

    private ImageView imageViewCapture;
    private ImageView imageViewChangeCamera;
    private ImageView imageViewFlash;

    private int permissionsAllowCount;

    private int width, height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;

        cameraType = Camera.CameraInfo.CAMERA_FACING_FRONT;
        flashType = Camera.Parameters.FLASH_MODE_AUTO;

        initUI();
        initListeners();
    }

    private void initUI() {
        frameLayoutCameraPreview = (FrameLayout) findViewById(R.id.frameLayoutCameraPreview);

        imageViewCapture = (ImageView) findViewById(R.id.imageViewCapture);
        imageViewChangeCamera = (ImageView) findViewById(R.id.imageViewChangeCamera);
        imageViewFlash = (ImageView) findViewById(R.id.imageViewFlash);
    }

    private void initListeners() {
        imageViewCapture.setOnClickListener(this);
        imageViewChangeCamera.setOnClickListener(this);
        imageViewFlash.setOnClickListener(this);
    }

    private void checkPermissions() {
        if (listPermissions == null) {
            listPermissions = new ArrayList<>();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            listPermissions.add(Manifest.permission.CAMERA);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            listPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            listPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            listPermissions.add(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void requestPermissions() {
        checkPermissions();

        if (listPermissions.size() > 0) {
            String[] arrayPermissions = listPermissions.toArray(new String[0]);
            ActivityCompat.requestPermissions(this, arrayPermissions, REQUEST_CODE_ALL_PERMISSIONS);
        } else {
            initCamera();
        }
    }

    public static boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public void setCameraDisplayOrientation() {
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int orientation = 0;
        if (cameraType == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            orientation = (cameraInfo.orientation + degrees) % 360;
            orientation = (360 - orientation) % 360;  // compensate the mirror
        } else {  // back-facing
            orientation = (cameraInfo.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(orientation);
    }

    private int getCameraId() {
        for (int index = 0; index < Camera.getNumberOfCameras(); index++) {
            if (cameraInfo == null) {
                cameraInfo = new Camera.CameraInfo();
            }

            Camera.getCameraInfo(index, cameraInfo);
            if (cameraInfo.facing == cameraType) {
                return index;
            }
        }
        return -1;
    }

    public Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open(getCameraId()); // attempt to get a Camera instance
        } catch (Exception e) {
            e.printStackTrace();
            // Camera is not available (in use or does not exist)
        }
        return camera; // returns null if camera is unavailable
    }

    private void initCamera() {
        try {
            if (checkCameraHardware(this)) {
                if (camera == null) {
                    camera = getCameraInstance();
                }

                setCameraDisplayOrientation();

                if (camera != null) {
                    // Create our Preview view and set it as the content of our activity.
                    CameraPreview cameraPreview = new CameraPreview(this, camera);
                    frameLayoutCameraPreview.addView(cameraPreview);

                    Camera.Parameters cameraParameters = camera.getParameters();
                    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                        imageViewFlash.setVisibility(View.GONE);
                    } else {
                        if (flashType.equals(Camera.Parameters.FLASH_MODE_AUTO)) {
                            cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                        } else if (flashType.equals(Camera.Parameters.FLASH_MODE_ON)) {
                            cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                        } else {
                            cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        }
                    }
                    List<Camera.Size> supportedSizes = cameraParameters.getSupportedPictureSizes();
                    Camera.Size bestSize = supportedSizes.get(0);
                    for(int index = 1; index < supportedSizes.size(); index++){
                        if((supportedSizes.get(index).width * supportedSizes.get(index).height) > (bestSize.width * bestSize.height)){
                            bestSize = supportedSizes.get(index);
                        }
                    }
                    cameraParameters.setPictureSize(bestSize.width, bestSize.height);
                    cameraParameters.setJpegQuality(100);
                    cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    camera.setParameters(cameraParameters);

                    camera.startPreview();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseCamera() {
        try {
            if (camera != null) {
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlertMarshmallowPermissions() {
        View bottomSheetView = this.getLayoutInflater().inflate(R.layout.layout_confirmation_alert, null);

        final Dialog mBottomSheetDialog = new Dialog(this, R.style.MaterialDialogSheet);
        mBottomSheetDialog.setContentView(bottomSheetView);
        mBottomSheetDialog.setCancelable(true);
        mBottomSheetDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mBottomSheetDialog.getWindow().setGravity(Gravity.BOTTOM);
        mBottomSheetDialog.show();

        Button buttonYes = (Button) bottomSheetView.findViewById(R.id.buttonYes);
        buttonYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomSheetDialog.dismiss();
                checkPermissions();
            }
        });

        Button buttonNo = (Button) bottomSheetView.findViewById(R.id.buttonNo);
        buttonNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomSheetDialog.dismiss();
            }
        });
    }

    private Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            //Log.d(TAG, "onShutter'd");
        }
    };

    private Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            //Log.d(TAG, "onPictureTaken - raw. Raw is null: " + (data == null));
        }
    };

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = Utils.getCreatedFile();
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file");
                return;
            }

            try {
                Bitmap picture = BitmapFactory.decodeByteArray(data, 0, data.length);

                Matrix matrix = new Matrix();
                if (cameraType == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    matrix.setRotate(degrees - 90);
                } else {
                    matrix.setRotate(degrees - 270);
                }

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(picture, 2 * height, 2 * width, true);
                Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

                FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
                rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                fileOutputStream.close();

                imageViewCapture.setEnabled(true);

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.imageViewCapture) {
            try {
                imageViewCapture.setEnabled(false);
                camera.takePicture(shutterCallback, rawCallback, pictureCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (id == R.id.imageViewChangeCamera) {
            if (cameraType == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraType = Camera.CameraInfo.CAMERA_FACING_BACK;
                imageViewChangeCamera.setImageResource(R.drawable.ic_camera_rear);
            } else if (cameraType == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraType = Camera.CameraInfo.CAMERA_FACING_FRONT;
                imageViewChangeCamera.setImageResource(R.drawable.ic_camera_front);
            }

            releaseCamera();

            initCamera();
        } else if (id == R.id.imageViewFlash) {
            if (flashType.equals(Camera.Parameters.FLASH_MODE_AUTO)) {
                flashType = Camera.Parameters.FLASH_MODE_ON;
                imageViewFlash.setImageResource(R.drawable.ic_flash_on);
            } else if (flashType.equals(Camera.Parameters.FLASH_MODE_ON)) {
                flashType = Camera.Parameters.FLASH_MODE_OFF;
                imageViewFlash.setImageResource(R.drawable.ic_flash_off);
            } else if (flashType.equals(Camera.Parameters.FLASH_MODE_OFF)) {
                flashType = Camera.Parameters.FLASH_MODE_AUTO;
                imageViewFlash.setImageResource(R.drawable.ic_flash_auto);
            }

            if (cameraType == Camera.CameraInfo.CAMERA_FACING_BACK) {
                initCamera();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ALL_PERMISSIONS: {
                for (int index = 0; index < permissions.length; index++) {
                    if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                        permissionsAllowCount++;
                    }
                }
                if (permissionsAllowCount != 0 && permissionsAllowCount != permissions.length) {
                    showAlertMarshmallowPermissions();
                    permissionsAllowCount = 0;
                }
                if (permissionsAllowCount == permissions.length) {
                    initCamera();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        camera.lock();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        releaseCamera();

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        requestPermissions();
    }
}
