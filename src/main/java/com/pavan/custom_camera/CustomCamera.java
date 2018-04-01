package com.pavan.custom_camera;

import android.content.Context;
import android.content.Intent;

/**
 * Created by Pavan on 20/02/18.
 */

public class CustomCamera {

    public static void startCamera(Context context) {
        context.startActivity(new Intent(context, CameraActivity.class));
    }

}
