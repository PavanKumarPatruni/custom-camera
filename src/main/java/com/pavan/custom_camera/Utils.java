package com.pavan.custom_camera;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by Pavan on 20/02/18.
 */

public class Utils {

    private static final String CAPTURED_IMAGES_ALBUM_NAME = "CUSTOM_CAMERA";
    private static final String CAPTURED_IMAGES_DIR = Environment.getExternalStoragePublicDirectory(CAPTURED_IMAGES_ALBUM_NAME).getAbsolutePath();

    public static File getCreatedFile() {
        File dir = new File(CAPTURED_IMAGES_DIR);
        final File captureTempFile = new File(CAPTURED_IMAGES_DIR + "/heartynote_" + System.currentTimeMillis() + ".png");
        try {
            if (!dir.exists()) {
                dir.mkdir();
            }
            try {
                captureTempFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("capture", e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return captureTempFile;
    }

}
