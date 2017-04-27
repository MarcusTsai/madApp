package ini_google.ad_hoc_building_sensor_devices.mad_hoc.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import ini_google.ad_hoc_building_sensor_devices.R;
import ini_google.ad_hoc_building_sensor_devices.mad_hoc.adapters.SensorListAdapter;

public class ListActivity extends AppCompatActivity implements SensorEventListener{
    // list of sensors & actuators
    SensorListAdapter listAdapter;
    ExpandableListView expListView;
    String DataHeader;
    HashMap<String, List<Parameter>> listDataChild;
    HashMap<String, HashMap<String, List<Parameter>>> record;
    private TextView AppName;

    private String targetSensor = null;
    private String instanceID = null;
    private String appID = null;
    private int phoneCount = 0;

    private Button deployButton, calibrateButton,backButton;
    private Spinner spinner;

    private SensorManager mSensorManager;
    private Sensor mSensor;
//    private HashMap<String, String> lookupTable;
    private String configData,deviceConfig;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private int calibrationCount;
    private float avgValue;
    private ProgressBar progressBar;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        AppName = (TextView) findViewById(R.id.appView);

        // calibration & deploy
        calibrateButton = (Button) findViewById(R.id.calibrateButton);
        spinner = (Spinner)findViewById(R.id.spinner);
        deployButton = (Button) findViewById(R.id.deployButton);
        backButton = (Button) findViewById(R.id.backButton);


        //lookupTable = new HashMap<String, String>();
        record = new HashMap<String, HashMap<String, List<Parameter>>>();
        deviceConfig = "";

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = null;
        calibrationCount = 0;
        avgValue=0;

        final Bundle bundle = getIntent().getExtras();
        configData = bundle.get("sensorConfig").toString();
        instanceID = bundle.get("instanceID").toString();
        appID = bundle.get("appID").toString();
        System.out.println("configData:" + configData);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        // get the listview, (ViewHolder)
        expListView = (ExpandableListView) findViewById(R.id.sensorList);

        setAppNamefromInstanceID();
        // set up spinner
        ArrayList<String> listItem = new ArrayList<>();

        // data transformation
        getHeader(listItem, configData);

        String[] items = listItem.toArray(new String[listItem.size()]);

        // recycle view
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // spinner is used to show the currently selected value
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            public void onItemSelected(AdapterView adapterView, View view, int position, long id){
                targetSensor = adapterView.getSelectedItem().toString();
                Toast.makeText(ListActivity.this, adapterView.getSelectedItem().toString() + " is chosen", Toast.LENGTH_LONG).show();

                // update listAdapter
                if (!record.containsKey(targetSensor)) {
                    prepareListData(configData);
                } else {
                    listDataChild = record.get(targetSensor);
                }

                System.out.println("target:" + targetSensor);
                listAdapter = new SensorListAdapter(ListActivity.this, targetSensor, listDataChild);
                expListView.setAdapter(listAdapter);
            }

            public void onNothingSelected(AdapterView arg0) {
                Toast.makeText(ListActivity.this, "No sensor is selected", Toast.LENGTH_LONG).show();
            }
        });

        progressBar.setVisibility(View.INVISIBLE);
        calibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //use firebase code to update json
                progressBar.setVisibility(View.VISIBLE);
                // check if the selected sensor is in firebase??

                startCalibration();

            }
        });

        deployButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkNodeInFirebase();
//                System.out.println("targetSensorExist:" + targetSensorExist);
//                if(!targetSensorExist && !deviceConfig.isEmpty()){
//                    System.out.println("Jump to activity");
//                    if(!targetSensor.isEmpty()) {
//                        Intent intent = new Intent(ListActivity.this, SensorActivity.class);
//                        intent.putExtra("sensorConfig", deviceConfig);
//                        intent.putExtra("avgValue", String.valueOf(avgValue));
//                        intent.putExtra("instanceID", bundle.get("instanceID").toString());
//                        startActivity(intent);
//                    }
//                } else if(deviceConfig.isEmpty()){
//                    Toast.makeText(ListActivity.this, "device configuration is empty", Toast.LENGTH_LONG).show();
//                } else {
//                    Toast.makeText(ListActivity.this, "target sensor/actuator has been chosen", Toast.LENGTH_LONG).show();
//                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ListActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void checkNodeInFirebase() {
        DatabaseReference device = database.getReference("/apps/").child(appID).child("default_config").child(targetSensor).child("num_devices");
        device.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                phoneCount = Integer.parseInt(dataSnapshot.getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference applicationInfo;
        //reference for the application
        if(targetSensor.equals("light") || targetSensor.equals("accelerometer")
                || targetSensor.equals("camera")) {
            applicationInfo = database.getReference("/install_sensors");
        } else {
            applicationInfo = database.getReference("/install_actuators");
        }

        //fetch reference of Instance
        final DatabaseReference instanceInfo = applicationInfo.child(instanceID);
        instanceInfo.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    System.out.println("targetSensor:" + targetSensor);
                    if (targetSensor != null) {
                        System.out.println("phoneCount:" + phoneCount);
                        boolean targetSensorExist = dataSnapshot.hasChild(targetSensor);
                        int curCount = 0;
                        if(targetSensorExist) {
                          curCount = (int)dataSnapshot.child(targetSensor).getChildrenCount();
                          System.out.println("curCount:" + curCount);
                        }

                        boolean registerenble = !targetSensorExist || (targetSensorExist && (curCount < phoneCount));
                        System.out.println("firebase:" + dataSnapshot.toString());

                        if(registerenble && !deviceConfig.isEmpty()){
                            System.out.println("Jump to activity");
                            if(!targetSensor.isEmpty()) {
                                Intent intent = new Intent(ListActivity.this, SensorActivity.class);
                                intent.putExtra("sensorConfig", deviceConfig);
                                intent.putExtra("avgValue", String.valueOf(avgValue));
                                intent.putExtra("instanceID", instanceID);
                                intent.putExtra("targetSensor", targetSensor);
                                startActivity(intent);
                            }
                        } else if(deviceConfig.isEmpty()){
                            Toast.makeText(ListActivity.this, "device configuration is empty", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ListActivity.this, "target sensor/actuator has been chosen", Toast.LENGTH_LONG).show();
                        }

                    }
                    //System.out.println("targetSensorExist1:" + targetSensorExist);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public void startCalibration(){
        if(targetSensor.equals("light") || targetSensor.equals("pd")){
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            mSensorManager.registerListener((SensorEventListener) this,mSensor,100000);
        }
        else {
            setCalibration();
        }
        mSensor = null;
    }

    // calibration
    public void onSensorChanged(SensorEvent event) {
        if(targetSensor.equals("light") || targetSensor.equals("PD")) {
            float sensor_value = event.values[0];
            if(calibrationCount == 0) {
                avgValue = sensor_value;
            }
            calibrationCount++;
            avgValue += sensor_value;
        }

        if(calibrationCount == 149) {
            calibrationCount = 0;
            avgValue = avgValue/150;
            mSensorManager.unregisterListener(this);
            setCalibration();
        }
    }


    public void setCalibration(){
        if(targetSensor.equals("light")){
            try {
                JSONObject lightconfig = (new JSONObject(configData)).getJSONObject("light");
                lightconfig.put("threshold",Math.round(avgValue));
                configData = ((new JSONObject(configData)).put("light",lightconfig)).toString();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // threshold for person identification
        if(targetSensor.equals("pd")){
            try {
                JSONObject pidconfig = (new JSONObject(configData)).getJSONObject("pd");
                pidconfig.put("value", 0);
                configData = ((new JSONObject(configData)).put("pd",pidconfig)).toString();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // update the threshold
        listAdapter.updateVal();
        // update new threshold to the JSON
        UpdateJson(configData);

        progressBar.setVisibility(View.INVISIBLE);
        Toast.makeText(ListActivity.this, "Sensors Calibrated Successfully", Toast.LENGTH_LONG).show();
        record.put(targetSensor, listDataChild);
        listAdapter = new SensorListAdapter(ListActivity.this, targetSensor, listDataChild);
        expListView.setAdapter(listAdapter);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // parse JSON into array list
    private void getHeader(ArrayList<String> listItem, String configData) {
        try {
            JSONObject config = new JSONObject(configData);
            Iterator<?> keys = config.keys();
            while(keys.hasNext()) {
                listItem.add((String) keys.next());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Set parameters for sensors
    private void prepareListData(String configData) {
        try {
            System.out.println("json:" + configData);
            JSONObject config = new JSONObject(configData);

            listDataChild = new HashMap<String, List<Parameter>>();

            Iterator<?> keys = config.keys();
            Parameter parameter = null;
            List<Parameter> parameters = null;

            parameters = new ArrayList<Parameter>();
            String key = targetSensor;
            JSONObject configSensor = (JSONObject) config.get(key);
            parameter = null;

            if (key.equals("accelerometer") && configSensor.has("threshold_lower")) {
                parameter = new Parameter("threshold", configSensor.get("threshold_lower").toString(), "Int");
                parameters.add(parameter);
            }

            if ((key.equals("light") || key.equals("pd")) && configSensor.has("sampling_rate")) {
                parameter = new Parameter("sampling_rate", configSensor.get("sampling_rate").toString(), "Int");
                parameters.add(parameter);
            }

            if (key.equals("camera")) {
                parameter = new Parameter("queue_number", "0", "Int");
                parameters.add(parameter);
            }
            listDataChild.put(targetSensor, parameters);

        } catch (Exception e) {
            Toast.makeText(this, "Configuration Error", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }


    // update Json after threshold values are changed by users
    private void UpdateJson(String configData) {
        try {
            JSONObject default_config = new JSONObject(configData);
            List<Parameter> list = listDataChild.get(targetSensor);
            JSONObject chosenConfig  = (JSONObject) default_config.get(targetSensor);

            for(Parameter parameter:list) {
                String item = parameter.getItem();
                String val = parameter.getVal();
                System.out.println("item:" + item + " val:" + val);
                chosenConfig.put(item, val);
            }

            default_config = new JSONObject();
            default_config.put(targetSensor,chosenConfig);
            deviceConfig = default_config.toString();
            System.out.println("deviceConfig:" + deviceConfig);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    // why do we need this?
    private void setAppNamefromInstanceID() {
        DatabaseReference applicaton = database.getReference("app_ids").child(instanceID);
        applicaton.addListenerForSingleValueEvent( new ValueEventListener() {

            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    String app_id = dataSnapshot.getValue().toString();
                    DatabaseReference applicationName = database.getReference("apps").child(app_id).child("app_name");
                    applicationName.addListenerForSingleValueEvent( new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            try{
                                String app_name = dataSnapshot.getValue().toString();
                                if(!app_name.isEmpty())AppName.setText(app_name);
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

    /**
     * New Parameters
     */
    public class Parameter {
        String item;
        String val;
        String type;

        Parameter(String item, String val, String type) {
            this.item = item;
            this.val = val;
            this.type = type;
        }

        public String getItem() {
            return this.item;
        }

        public void setItem(String item) {
            this.item = item;
        }

        public String getVal() {
            return this.val;
        }

        public void setVal(String val) {
            this.val = val;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String Type) {
            this.type = type;
        }

//        @Override
//        public String toString() {
//            System.out.println("sensor:" + );
//        }

    }



}
