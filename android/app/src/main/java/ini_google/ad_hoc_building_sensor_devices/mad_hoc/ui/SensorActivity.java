package ini_google.ad_hoc_building_sensor_devices.mad_hoc.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.FloatBuffer;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

import ini_google.ad_hoc_building_sensor_devices.R;

/**
 * Sensor
 * 1. light sensor
 * 2. face detection
 * 3. accelerometer
 * 4. gryoscope
 * 5. Person identification using light sensors
 *
 * Actuators
 * 1. flash
 * 2. screen
 * 3. speaker
 */
public class SensorActivity extends AppCompatActivity implements SensorEventListener{
    private static final String TAG = "SensorActivity";
    private final int RINGBUFSIZE = 5;
    private String androidID;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference devices;
    private DatabaseReference sensors,actuators;

    private JSONObject deviceConfig;
    private CameraManager mCameraManager;

    private static String device_id;
    private TextView configView,sensorType ,sensorValue,taskView;
    private Button cancelButton;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    //private int avgReading;
    // get the value from firebase?
    private float lightThreshold;
    private LinkedList<Float> ringbuffer;
    private float sum = 0.0f;
    private String instanceID ;
    private String type;
    private String targetSensor = null;

    private Sensor accelerometer;
    private float mAccel;
    private float mAccelCurrent, mAccelLast;
    private float accThreshold;

    private TextView textView;
    private TextToSpeech speaker;

    public ValueEventListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        taskView = (TextView) findViewById(R.id.taskView);
        cancelButton = (Button) findViewById(R.id.cancelbutton);
        sensorValue = (TextView) findViewById(R.id.valueView);
        sensorType = (TextView) findViewById(R.id.sensorType);
        configView = (TextView) findViewById(R.id.configuration);
        Bundle bundle = getIntent().getExtras();
        targetSensor = bundle.get("targetSensor").toString();

        // device ID
        androidID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // actuators: flash
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        // sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = null;
        ringbuffer = new LinkedList<Float>();

        // textView = (TextView) findViewById(R.id.textView);
        mAccel = 0.00f;
        // GRAVITY_EARTH -> const. 9.8 m/s^2
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        try {
            this.deviceConfig = new JSONObject(bundle.get("sensorConfig").toString());
            this.lightThreshold = Float.parseFloat(bundle.get("avgValue").toString());
            System.out.println("json:" + deviceConfig);
            this.instanceID = bundle.get("instanceID").toString();
            this.device_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

            devices = database.getReference("/devices");
            actuators = database.getReference("install_actuators");
            sensors = database.getReference("install_sensors");

            deployDevicewithConfig(this.instanceID,this.deviceConfig);
            Iterator<String> configParameters = this.deviceConfig.keys();

            while (configParameters.hasNext()) {
                String parameter = configParameters.next();
                String parameterType = ((JSONObject) this.deviceConfig.get(parameter)).get("type").toString();
                int role = getResources().getIdentifier(parameterType, "String", getPackageName());
                this.type = parameterType;

                if (parameterType.equals("LIGHT") || parameterType.equals("PD") || parameterType.equals("ACCELEROMETER")) {
                    configureSensors(parameter,parameterType);

                }
                if (parameterType.equals("FLASH") || parameterType.equals("SPEAKER")) {
                    configureActuators(parameter,parameterType);
                }

                if (parameterType.equals("SCREEN")) {
                    configureActuators(parameter,parameterType);
                }

                if(parameterType.equals("CAMERA")){
                    Intent intent = new Intent(SensorActivity.this, MultiTrackerActivity.class);
                    System.out.println("parameter:" + parameter);
                    sensors.child(instanceID).child(parameter).child(device_id).child("value").setValue("0");
                    sensors.child(instanceID).child(parameter).child(device_id).child(((JSONObject)this.deviceConfig.get(parameter)).toString()).setValue(true);
                    intent.putExtra("sensor_url","install_sensors".concat("/").concat(this.instanceID).concat("/").concat(parameter).concat("/").concat(device_id).concat("/").concat(((JSONObject)this.deviceConfig.get(parameter)).toString()));
                    intent.putExtra("instanceID", instanceID);
                    startActivity(intent);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        cancelButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                //mSensorManager.unregisterListener();
                // delete node
                if (targetSensor.equals("flash") || targetSensor.equals("screen") || targetSensor.equals("speaker")) {
                    actuators.removeEventListener(listener);
                }
                deleteNodeInFirebase();

                Intent intent = new Intent();
                intent.setClass(SensorActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }


    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    private void deleteNodeInFirebase() {
        DatabaseReference applicationInfo;
        //reference for the application
        if(targetSensor.equals("light") || targetSensor.equals("accelerometer")) {
            applicationInfo = database.getReference("/install_sensors");
        } else {
            applicationInfo = database.getReference("/install_actuators");
        }
        applicationInfo.child(instanceID).child(targetSensor).removeValue();
    }


    /**
     *  Actuators
     */

    /**
     *
     * @param parameter
     * @param parameterType
     */
    private void configureActuators(String parameter, String parameterType){
        actuators = database.getReference("install_actuators").child(instanceID).child(parameter).child(device_id);
        configView.setText(deviceConfig.toString());
        setAppNamefromInstanceID(instanceID);

        // flash
        if(this.type.equals("FLASH")) {
            actuators.setValue(false);
            actuators.addValueEventListener(listener = new ValueEventListener() {
                //listener = this;
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String flashON = dataSnapshot.getValue().toString();
                    if (flashON.equals("true")) {
                        turnOnFlashLight();
                    } else {
                        turnOffFlashLight();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.out.println(databaseError.toString());
                }
            });
        }

        // screen
        if(this.type.equals("SCREEN")) {
            Intent intent = new Intent(SensorActivity.this, QueueDisplayActivity.class);

            actuators.setValue(true);
            intent.putExtra("actuator_url","install_actuators".concat("/").concat(this.instanceID).concat("/").concat(parameter).concat("/").concat(device_id));
            intent.putExtra("instanceID", instanceID);
            startActivity(intent);
        }

        // speaker
        if(this.type.equals("SPEAKER")){
            speaker = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status != TextToSpeech.ERROR) {
                        speaker.setLanguage(Locale.US);
                    }

                }
            });
            // default value
            actuators.setValue("");
            actuators.addValueEventListener(listener = new ValueEventListener() {

                public void onDataChange(DataSnapshot dataSnapshot) {
                    String speakerText = dataSnapshot.getValue().toString();
                    //System.out.println("speaker text:" + speakerText);
                    //System.out.println("speaker length:" + speakerText.length());
                    if(speakerText.length() != 0) {
                        speakMessage(speakerText);
                        database.getReference("/install_actuators").child(instanceID).child("speaker").child(device_id).setValue("");
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.out.println(databaseError.toString());
                }
            });
        }
    }


    // flash
    private void turnOnFlashLight() {
        //here to judge if flash is available
        try{
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics("0");
            boolean flashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (flashAvailable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mCameraManager.setTorchMode("0",true);
                    sensorValue.setText("Flash On");
                }
                else{
                    sensorValue.setText("Flash not supported in this device");
                }

            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void turnOffFlashLight() {
        try{
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics("0");
            boolean flashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (flashAvailable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mCameraManager.setTorchMode("0",false);
                    sensorValue.setText("Flash Off");
                }
                else{
                    sensorValue.setText("Flash not supported in this device");
                }

            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    // speaker
    private void speakMessage(String speakerText){
        // source from firebase
        Toast.makeText(SensorActivity.this, "speaker invoked", Toast.LENGTH_SHORT).show();
        speaker.speak(speakerText, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    // update sensor value to the firebase
    private void deployDevicewithConfig(String instanceID, JSONObject deviceConfig) {
        Gson gson = new Gson();
        Map<String, Object> configuration = gson.fromJson(deviceConfig.toString(), Map.class );
        configView.setText(deviceConfig.toString());
        devices.child(instanceID).child(androidID).child("config").updateChildren(configuration);
        Toast.makeText(SensorActivity.this, "Configuration Deployed Successfully", Toast.LENGTH_LONG).show();
    }

    /**
     * Sensors
     */

    /**
     *
     * @param parameter
     * @param parameterType
     */
    private void configureSensors(String parameter, String parameterType){
        try {

            sensors = database.getReference("install_sensors").child(instanceID).child(parameter).child(device_id);

            setAppNamefromInstanceID(this.instanceID);
            sensorType.setText(parameterType);

            sensors.setValue(true);
            JSONObject config = (JSONObject)(this.deviceConfig.get(parameter));

            // light sensor
            if(parameterType.equals("LIGHT") || parameterType.equals("PD")) {
                mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
                // ?
                //avgReading = Integer.parseInt(config.get("threshold").toString());
                //upperThreshold = Integer.parseInt(config.get("threshold_upper").toString());

            }
            // accelerometer
            if(parameterType.equals("ACCELEROMETER")){
                mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                accThreshold = Float.parseFloat(config.get("threshold").toString());
            }
            else{
                sensorValue.setText("Sensor not supported");
            }

//            // person identification
//            /**
//             * Detect the varation of light inorder to know how many people passed by
//             */
//            if(parameterType.equals("PID")) {
//                mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
//
//
//            } else {
//                sensorValue.setText("Sensor not supported");
//            }

            //?
            if(mSensor == null){
                taskView.setText("Sensor Not Available");
            } else {
                //taskView.setText(sensorConfig.get("application_name").toString());
                System.out.println("Job Is Running");
                int interval = 1000000;
                if (config.has("sampling_rate")) {
                    interval = Integer.parseInt(config.get("sampling_rate").toString()) * 1000000;
                }
                mSensorManager.registerListener(this,mSensor,interval);
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    // why do we need this?
    private void setAppNamefromInstanceID(String instanceID) {
        DatabaseReference applicaton = database.getReference("app_ids").child(instanceID);
        applicaton.addListenerForSingleValueEvent( new ValueEventListener() {

            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    JSONObject app_info = new JSONObject((HashMap) dataSnapshot.getValue());
                    String app_id = app_info.get("app_id").toString();

                    DatabaseReference applicationName = database.getReference("apps").child(app_id).child("app_name");
                    applicationName.addListenerForSingleValueEvent( new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            try{
                                String app_name = dataSnapshot.getValue().toString();
                                if(!app_name.isEmpty())taskView.setText(app_name);
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // light sensors
        if(this.type.equals("LIGHT")){
            float sensor_value = event.values[0];
            sensorValue.setText(Float.toString(sensor_value));

            if(sensor_value != 0)
            {
                sensors.child("value").setValue(Float.toString(sensor_value));
                sensors.child("last_modified").setValue(Long.toString(new Date().getTime()));
            }
        }

        // person identification
        if(this.type.equals("PD")) {
            float sensor_value = event.values[0];
            sensorValue.setText(Float.toString(sensor_value));
            sensors.child("value").setValue(0);
            // how to get threshold?
            float avg_sensor_value = 0.0f;
            if(ringbuffer.size() == RINGBUFSIZE) {
               sum -= ringbuffer.poll();
            }

            ringbuffer.offer(sensor_value);
            sum += sensor_value;
            avg_sensor_value = sum / ringbuffer.size();


            if(avg_sensor_value < lightThreshold) {
                sensors.child("value").setValue(1);
                sensors.child("last_modified").setValue(Long.toString(new Date().getTime()));
            }
        }

        // accelerometer
        if(this.type.equals("ACCELEROMETER")){
            float[] mAcc = event.values.clone();

            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(mAcc[0] * mAcc[0] + mAcc[1] * mAcc[1] + mAcc[2] * mAcc[2]);
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;
            System.out.println("mAccel:" + mAccel);
            System.out.println("mAccel threshold:" + accThreshold);
            sensors.child("value").setValue(false);
            if(Math.abs(mAccel) > accThreshold) {
                Toast.makeText(SensorActivity.this, "Motion detected!", Toast.LENGTH_SHORT).show();
                sensors.child("value").setValue(true);
                sensors.child("last_modified").setValue(Long.toString(new Date().getTime()));
                sensorValue.setText(Float.toString(mAccel));
            }

        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
