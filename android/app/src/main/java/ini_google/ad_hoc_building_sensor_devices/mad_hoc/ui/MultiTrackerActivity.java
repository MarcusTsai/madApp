package ini_google.ad_hoc_building_sensor_devices.mad_hoc.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiDetector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ini_google.ad_hoc_building_sensor_devices.R;
import ini_google.ad_hoc_building_sensor_devices.mad_hoc.facetracker.CameraSourcePreview;
import ini_google.ad_hoc_building_sensor_devices.mad_hoc.facetracker.FaceTrackerFactory;
import ini_google.ad_hoc_building_sensor_devices.mad_hoc.facetracker.GraphicOverlay;

public class MultiTrackerActivity extends AppCompatActivity{
    private static final String TAG = "MultiTracker";

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private String instanceID = null;
    private String deviceID = null;
    public static int OffsetThread = 0;
    private Button backButton, confirmButton;
    private static TextView faceCount;
    public static List<Integer> idList;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference sensors;

    public static Handler handler;

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_tracker);
            backButton = (Button) findViewById(R.id.cancelButton);
            confirmButton = (Button) findViewById(R.id.confirmButton);
            faceCount = (TextView) findViewById(R.id.faceCount);
            mPreview = (CameraSourcePreview) findViewById(R.id.preview);
            mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
            idList = new ArrayList<Integer>();
            final Bundle bundle = getIntent().getExtras();
            //String sensor_url = bundle.get("sensor_url").toString();
            //System.out.println("sensor_url:" + sensor_url);
            instanceID = bundle.get("instanceID").toString();
            deviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            sensors = database.getReference("install_sensors/" + instanceID + "/" + "camera/" + deviceID);
            sensors.child("value").setValue("0");
            sensors.child("last_modified").setValue(String.valueOf(new Date().getTime()));

            // Check for the camera permission before accessing the camera.  If the
            // permission is not granted yet, request permission.
            int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (rc == PackageManager.PERMISSION_GRANTED) {
                createCameraSource();
            } else {
                requestCameraPermission();
            }


            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // close handler
                    if(handler != null) {
                      handler.removeCallbacksAndMessages(null);
                    }
                    deleteNodeInFirebase();

                    Intent intent = new Intent(MultiTrackerActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            });

            confirmButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    sensors.child("value").setValue(String.valueOf(idList.size()));
                    sensors.child("last_modified").setValue(String.valueOf(new Date().getTime()));
                }
            });

            // confirm the face count and sent it to firebase

            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    faceCount.setText(String.valueOf(idList.size()));
                    System.out.println("value:" + idList.size());
                    checkNodeInFirebase();

                    //sensors.child("value").setValue(idList.size());
                    //sensors.child("last_modified").setValue(new Date().getTime());
                    //System.out.println("test");
                    //deleteNodeInFirebase();
                }
            };

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void deleteNodeInFirebase() {
        //DatabaseReference applicationInfo;
        //reference for the application
        database.getReference("/install_sensors").child(instanceID).child("camera").removeValue();
    }

    private void checkNodeInFirebase() {
        confirmButton.setEnabled(false);
        DatabaseReference applicationInfo = database.getReference("/install_sensors");
        final DatabaseReference instanceInfo = applicationInfo.child(instanceID);
        instanceInfo.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    boolean enable = (dataSnapshot.child("camera").child(deviceID).child("value").getValue().equals("0"));
                    System.out.println("camera enable:" + enable);
                    System.out.println("cvalue:" + dataSnapshot.child("camera").child(deviceID).child("value"));
                    confirmButton.setEnabled(enable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }



    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {


        Context context = getApplicationContext();

        // A face detector is created to track faces.  An associated multi-processor instance
        // is set to receive the face detection results, track the faces, and maintain graphics for
        // each face on screen.  The factory is used by the multi-processor to create a separate
        // activity_tracker instance for each face.
        FaceDetector faceDetector = new FaceDetector.Builder(context).build();
        FaceTrackerFactory faceFactory = new FaceTrackerFactory(mGraphicOverlay);
        faceDetector.setProcessor(
                new MultiProcessor.Builder<>(faceFactory).build());

//        // A barcode detector is created to track barcodes.  An associated multi-processor instance
//        // is set to receive the barcode detection results, track the barcodes, and maintain
//        // graphics for each barcode on screen.  The factory is used by the multi-processor to
//        // create a separate activity_tracker instance for each barcode.
//        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
//        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay);
//        barcodeDetector.setProcessor(
//                new MultiProcessor.Builder<>(barcodeFactory).build());

        // A multi-detector groups the two detectors together as one detector.  All images received
        // by this detector from the camera will be sent to each of the underlying detectors, which
        // will each do face and barcode detection, respectively.  The detection results from each
        // are then sent to associated activity_tracker instances which maintain per-item graphics on the
        // screen.
        OffsetThread = Thread.activeCount();
        MultiDetector multiDetector = new MultiDetector.Builder()
                .add(faceDetector)
                .build();
//                .add(barcodeDetector)
//                .build();
//        System.out.println("Face Count:" + multiDetector. );
        if (!multiDetector.isOperational()) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        mCameraSource = new CameraSource.Builder(getApplicationContext(), multiDetector)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(60.0f)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }


    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }
    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

}
