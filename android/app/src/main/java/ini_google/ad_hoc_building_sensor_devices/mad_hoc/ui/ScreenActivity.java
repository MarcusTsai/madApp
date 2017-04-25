package ini_google.ad_hoc_building_sensor_devices.mad_hoc.ui;

/**
 * Created by chilli on 11/23/16.
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import ini_google.ad_hoc_building_sensor_devices.R;

/**
 * Screen
 */
public class ScreenActivity extends AppCompatActivity {
    private Button cancelButton;
    private Activity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen);
        cancelButton = (Button) findViewById(R.id.cancelButton);

        cancelButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                //mSensorManager.unregisterListener();
                Intent intent = new Intent();
                intent.setClass(activity, MainActivity.class);
                startActivity(intent);
            }
        });
    }

}
