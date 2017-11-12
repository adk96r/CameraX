
# Camera X
CameraX is a API Wrapper which simplifies Camera2 API in android. Even though the pipiined stucture in Camera2 is really efficient, the usability and simplicity of Camera API [Deprecated] are lost. This API aims to bring them back !    

This repo is divided into _2 sections_ - 
1. [How Camera2 API Works](#Cam2APIWorking)
2. [How CameraX simplifies the process](#CamXWorking)

### <a name="Cam2APIWorking">How does the Camera2 API Work</a>:
This picture sums up the whole working of Camera2 API really well.

[Camera2 API](http://cfile25.uf.tistory.com/image/2663323C58D2503E0C889F "Camera2 API")

Basically the steps followed for performing any camera related task are -  

__First things first__
We will be using the following objects for all the camera tasks.
1. _cameraManager_ - Helps to browse the availabe hardware to choose the appropriate camera
device.
2. _cameraCharacterstics_ - Stores the hardware properties of each camera device.
3. _cameraId_ - Stores the ID of the camera.
4. _cameraDevice_ - Stores the camera instance.

```java
{
    CameraManger cameraManger = null;
    CameraCharacterstics cameraCharacterstics = null;
    String cameraId = null;
    CameraDevice cameraDevice = null;
}
```    


__1. Obtain a CameraManager instance__
1. Get details of all the available camera devices.
2. Details are stored as _CameraCharacterstics_ objects.
3. Choose the most suitable camera device and store its _ID_.

```java
cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

for (String id : cameraId) {
    cameraCharacteristics = cameraManager.getCameraCharacteristics(camId);
    if ( ... suitable ... ) {
        cameraId = id;
        break;
    }
}    
```    


__2. Use the CameraManages instance to _Open the camera device___
1. Needs a _CameraDevice.StateCallback_ to handle what happends once a camera is opened or couldn't be opened.
2. Upon opening, store the reference to the _Camera Device_ given in the callback.
```java
cameraManager.openCamera(camId, new CameraDevice.StateCallback() {

    @Override
    public void onOpened(CameraDevice camera) {

        // We'll be using this camera device for all the tasks.
        cameraDevice = camera;
        .
        . // Use this cameraDevice to perform the required action.
        .

    }

}, null);
```
   
   
__3. Use the _obtained camera device_ to create a _CaptureRequest_<sup>[1](#CameraCaptureRequest)</sup>__

Capture request has details regarding the type of capture being made.
1. To create it, a _list of surfaces_ have to be given, ones which will show the output from the camera.
2. Use a CaptureRequest.Builder to build a request.
3. Add each surface as a target, to the builder.


```java
CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

// Add targets to the builder.
for (Surface surface : surfaces)
    builder.addTarget(surface);

// Build and return the request.
return builder.build();

```

__4. Use this request to create a capture session__

Capture session needs a capture request, a CameraCaptureSession.StateCallback and a handler.
1. Create a state callback to handle the session status.
2. Upon successfull configuration, use the session to set a repeating/burst request.
3. While doing it, another callback has to passed which is the one that deals with the final capture result.

```java
CameraCaptureSession.StateCallback callback = new CameraCaptureSession.StateCallback() {
    @Override
    public void onConfigured(CameraCaptureSession session) {
        captureSession = session;

        try {
            session.setRepeatingRequest(request, captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigureFailed(CameraCaptureSession session) {
        captureSession = null;
    }
};
 ```
Capture callback takes care of the data that comes from the camera device.
```java
CameraCaptureSession.CaptureCallback callback = new CameraCaptureSession.CaptureCallback() {
    @Override
    public void onCaptureCompleted(CameraCaptureSession ses, CaptureRequest request, TotalCaptureResult result) {
        super.onCaptureCompleted(session, request, result);
        // The result stores all the goodness that we want.
    }
};
```

Even thought this pipelines architecture is very clear and tweak-able, using camera for simple applications could be a overhead.Now lets have a look at CameraX!

### <a name="CamXWorking">Using CameraX</a>:


#### Footnotes
1. <a name="CameraCaptureRequest">Make sure to create the capture request only after opening the camera device, as opening may take time.</a>
License

----

MIT

