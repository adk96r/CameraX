package adk.giteye;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Adu on 11/8/2017.
 */

public class CameraX {

    // Camera Modes
    public static final int CAMERA_BACK = CameraCharacteristics.LENS_FACING_BACK;
    public static final int CAMERA_FRONT = CameraCharacteristics.LENS_FACING_FRONT;
    private String debugTag;
    private String cameraId;
    private boolean debug = false;
    private Activity activity;
    private Context context;

    private CameraDevice cameraDevice;
    private CameraManager cameraManager;
    private CameraCaptureSession captureSession;
    private List<Object> outputSurfaces;
    private List<Surface> surfaces;
    private String instanceName;
    private int cameraMode;
    private Map<CaptureRequest.Key, Integer> captureRequestOptions;

    private Drawable back;

    private CameraCaptureSession.CaptureCallback livePreviewCaptureCallback;

    public CameraX(Activity activity, String instanceName, int cameraMode) {

        this.activity = activity;
        this.context = activity.getApplicationContext();

        this.instanceName = instanceName;
        this.debugTag = "CameraX : " + instanceName;

        try {
            cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            cameraDevice = null;
            this.captureRequestOptions = null;
            this.cameraMode = cameraMode;
            this.cameraId = getCameraDeviceId(cameraMode);
        } catch (Exception e) {
            // Couldnt' get the camera service.
        }

    }

    /**************
     * FUNDAMENTALS
     *******************/

    private String getCameraDeviceId(int cameraMode) throws CameraAccessException {

        String camId = null;

        for (String id : cameraManager.getCameraIdList()) {
            camId = id;
            break;
        }

        if (debug)
            Log.d(debugTag, "Got the camera ID = " + camId);

        return camId;

    }

    private void openCamera(CameraDevice.StateCallback callback, Handler handler) throws CameraAccessException {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(debugTag, "No permissions granted to use the camera.");
            return;
        }

        // Open the camera device.
        try {
            cameraManager.openCamera(cameraId, callback, handler);
        } catch (CameraAccessException e) {
            if (debug)
                Log.d(debugTag, "Exception while opening the camera : " + e.getMessage());
        }

        if (debug)
            Log.d(debugTag, "Opened the camera successfully.");

    }

    private void closeCamera() throws CameraAccessException {

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }


        if (debug) {
            Log.d(debugTag, "Closed the camera successfully.");
        }
    }

    // Set the destination for the feed.
    public void setOutputSurfaces(List<Object> surfaces) {

        // Stop any camera capture sessions being used.
        if (captureSession != null) {
            try {
                stopLivePreview();
            } catch (Exception e) {
                if (debug) {
                    Log.d(debugTag, "Couldn't stop the capture session while setting the output surfaces.");
                }
            }
        }

        // Convert the outputs into surfaces for later use.
        this.outputSurfaces = surfaces;
        this.surfaces = new ArrayList<>(surfaces.size());

        for (Object surface : surfaces) {
            Log.d(debugTag, String.valueOf(surface.getClass()));
            if (surface instanceof SurfaceView) {
                this.surfaces.add(((SurfaceView) surface).getHolder().getSurface());
            } else if (surface instanceof TextureView) {
                this.surfaces.add(new Surface(((TextureView) surface).getSurfaceTexture()));
            }
        }

        if (debug)
            Log.d("CameraX_" + instanceName, "Output surfaces set.");

    }

    private CaptureRequest getCaptureRequest(CameraDevice cameraDevice, int requestTemplate) throws CameraAccessException {

        // Create a builder for the given template.
        CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

        // Set any options if provided.
        for (CaptureRequest.Key key : captureRequestOptions.keySet())
            builder.set(key, captureRequestOptions.get(key));

        // Add targets to the builder.
        for (Surface surface : surfaces)
            builder.addTarget(surface);

        // Build and return the request.
        return builder.build();

    }

    private CameraDevice.StateCallback getCameraDeviceStateCallback(@Nullable final CameraCaptureSession.CaptureCallback captureCallback) {

        // Create the callback.
        final CameraDevice.StateCallback callback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {

                cameraDevice = camera;
                CaptureRequest captureRequest = null;
                CameraCaptureSession.StateCallback stateCallback = null;

                try {
                    captureRequest = getCaptureRequest(camera, CameraDevice.TEMPLATE_PREVIEW);
                } catch (Exception e) {
                    if (debug)
                        Log.d(debugTag, "Exception while getting capture request!");
                }

                try {
                    stateCallback = getCaptureSessionCallback(captureRequest, captureCallback);
                } catch (Exception e) {
                    if (debug)
                        Log.d(debugTag, "Exception while getting capture session!");
                }

                try {
                    cameraDevice.createCaptureSession(surfaces, stateCallback, null);
                } catch (Exception e) {
                    if (debug)
                        Log.d(debugTag, "Exception while creating capture session!");
                }
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                if (debug)
                    Log.d(debugTag, "Camera disconnected.");
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                if (debug)
                    Log.d(debugTag, "Error while opening the camera.");
            }
        };

        return callback;
    }

    private CameraCaptureSession.StateCallback getCaptureSessionCallback(final CaptureRequest request, @Nullable CameraCaptureSession.CaptureCallback captureCallback) {

        if (captureCallback == null) {
            captureCallback = getCaptureCallback();
        }

        final CameraCaptureSession.CaptureCallback finalCaptureCallback = captureCallback;

        CameraCaptureSession.StateCallback callback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                captureSession = session;

                try {
                    session.setRepeatingRequest(request, finalCaptureCallback, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                captureSession = null;
            }
        };

        return callback;
    }

    private CameraCaptureSession.CaptureCallback getCaptureCallback() {

        CameraCaptureSession.CaptureCallback callback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
            }

            @Override
            public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                super.onCaptureStarted(session, request, timestamp, frameNumber);
            }

            @Override
            public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
                super.onCaptureProgressed(session, request, partialResult);
            }

            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
            }
        };

        return callback;
    }


    /**************
     * PERSONALISATION
     ****************/

    // Enable/Disable Debug
    public void debugOn(boolean debug) {
        this.debug = debug;
    }


    // Set Capture request options ( eg- enable face detection )
    public void setCaptureRequestOptions(Map<CaptureRequest.Key, Integer> captureRequestOptions) {
        this.captureRequestOptions = captureRequestOptions;
    }

    // Return maximum output size for JPEG
    public Size getMaxOutputSize(@Nullable Integer imageFormat) {

        if (cameraManager == null) {
            return new Size(0, 0);
        }

        if (imageFormat == null) {
            imageFormat = ImageFormat.JPEG;
        }

        try {
            StreamConfigurationMap map = cameraManager.getCameraCharacteristics(this.cameraId)
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = map.getOutputSizes(imageFormat);
            return sizes[0];    // Max
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return new Size(0, 0);
        }

    }


    /***************
     * OPERATIONS
     *****************/

    public void startLivePreview(@Nullable final CameraCaptureSession.CaptureCallback captureCallback) throws CameraAccessException {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        // Save the capture callback for future use.
        this.livePreviewCaptureCallback = captureCallback;

        cameraManager.openCamera(getCameraDeviceId(cameraMode), getCameraDeviceStateCallback(captureCallback), null);

    }

    public void pauseLivePreview() throws CameraAccessException {
        captureSession.stopRepeating();
    }

    public void resumeLivePreview() throws CameraAccessException {
        //captureSession.setRepeatingRequest()
        captureSession.setRepeatingRequest(getCaptureRequest(cameraDevice, CameraDevice.TEMPLATE_PREVIEW), livePreviewCaptureCallback, null);
    }

    public void stopLivePreview() throws CameraAccessException {

        // Stop any existing repeating requests in the live preview.
        if (captureSession != null) {
            captureSession.stopRepeating();
            captureSession = null;
            livePreviewCaptureCallback = null;
        }

        // Clear the surfaces
        SurfaceView surfaceView;
        TextureView textureView;
        int[] colors = {Color.BLACK};
        Canvas canvas = new Canvas();

        // Make the surface blank and black again.
        for (Object surface : outputSurfaces) {
            if (surface instanceof SurfaceView) {
                surfaceView = ((SurfaceView) surface);

            } else if (surface instanceof TextureView) {
                textureView = (TextureView) surface;

            }
        }

        // Close the camera device.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    closeCamera();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
