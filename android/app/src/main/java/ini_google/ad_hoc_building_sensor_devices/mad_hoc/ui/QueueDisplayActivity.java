package ini_google.ad_hoc_building_sensor_devices.mad_hoc.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ini_google.ad_hoc_building_sensor_devices.R;
import ini_google.ad_hoc_building_sensor_devices.mad_hoc.adapters.QueueListAdapter;

public class QueueDisplayActivity extends AppCompatActivity{
    QueueListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<Queue>> listDataChild;
    private Button backButton;
    private Activity activity = this;
    private String configData = null;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference actuator;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);
        expListView = (ExpandableListView) findViewById(R.id.queueList);
        backButton = (Button) findViewById(R.id.cancelButton);

        final Bundle bundle = getIntent().getExtras();
        //configData =  bundle.get("sensorConfig").toString();
        String actuator_url = bundle.get("actuator_url").toString();

        actuator = database.getReference(actuator_url);
        actuator.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()) {
                    JSONObject actuator_display = new JSONObject((HashMap) dataSnapshot.getValue());
                    Iterator display_keys = actuator_display.keys();
                    while(display_keys.hasNext()){
                        String display = display_keys.next().toString();
                        if(!display.startsWith("display")){
                            actuator_display.remove(display);
                        }
                    }
                    prepareListData(actuator_display);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, MainActivity.class);
                startActivity(intent);
            }
        });

    }

    private void prepareListData(JSONObject displayData) {
        try {
            listDataHeader = new ArrayList<String>();
            listDataChild = new HashMap<String, List<Queue>>();

            listDataHeader.add("Queue List");
            listDataChild = new HashMap<String, List<Queue>>();

            Iterator<?> keys = displayData.keys();
            List<Queue> queues = new ArrayList<Queue>();
            while(keys.hasNext()){
                String display = keys.next().toString();
                JSONObject display_data = (JSONObject) displayData.get(display);
                String text,value,color;
                if(display_data.has("display_text")){
                    text = display_data.get("display_text").toString();
                }
                else{
                    text= "";
                }
                if(display_data.has("display_value")){
                    value = display_data.get("display_value").toString();
                }
                else{
                    value= "";
                }
                if(display_data.has("display_color")){
                    color = display_data.get("display_color").toString();
                }
                else{
                    color= "teal";
                }
                queues.add(new Queue(text, value, Color.parseColor(color)));


            }

            //queues.add(new Queue("queue1", "10", Color.BLUE));
            //queues.add(new Queue("queue2", "12", Color.YELLOW));
            //queues.add(new Queue("queue3", "15", Color.RED));
            listDataChild.put(listDataHeader.get(0), queues);

            listAdapter = new QueueListAdapter(activity, listDataHeader, listDataChild);
            expListView.setAdapter(listAdapter);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public class Queue {
        String Qname;
        String Qcount;
        int Qcolor;

        Queue(String name, String count, int color) {
            this.Qname = name;
            this.Qcount = count;
            this.Qcolor = color;
        }

        public String getQname() {
            return Qname;
        }

        public void setQname(String qname) {
            Qname = qname;
        }

        public String getQcount() {
            return Qcount;
        }

        public void setQcount(String qcount) {
            Qcount = qcount;
        }

        public int getQcolor() {
            return Qcolor;
        }

        public void setQcolor(int qcolor) {
            Qcolor = qcolor;
        }
    }
}
