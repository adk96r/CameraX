package adk.camerax;

import android.hardware.camera2.CameraAccessException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import adk.camerax.CameraX.CameraX;

public class Home extends AppCompatActivity {

    private boolean on = true;
    private boolean pause = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.SurfaceView);
        List<Object> surfaces = new ArrayList<>(1);
        surfaces.add(surfaceView);

        final CameraX cameraX = new CameraX(this, "0", CameraX.CAMERA_BACK);
        cameraX.debugOn(true);
        cameraX.setOutputSurfaces(surfaces);

        final Button pauseBtn = ((Button) findViewById(R.id.PausePreviewBtn));

        ((Button) findViewById(R.id.StartPreviewBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    pause = false;
                    if (on) {
                        cameraX.startLivePreview(null);
                        pauseBtn.setText("Pause");
                        pauseBtn.setEnabled(true);
                    }
                    else {
                        cameraX.stopLivePreview();
                        pauseBtn.setEnabled(false);

                    }
                    ((Button) v).setText((on ? "Stop " : "Start ") + "Preview");


                    on = !on;

                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });

        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseBtn.setText(pause ? "Resume" : "Pause");
                pause = !pause;

                try {
                    if (pause) {
                        cameraX.pauseLivePreview();
                    } else {
                        cameraX.resumeLivePreview();
                    }
                } catch (Exception e) {

                }
            }
        });


    }
}
