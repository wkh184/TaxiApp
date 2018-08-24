package taxi.com.taxiapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {
    SharedPreferences sharedpreferences;
    TextView maxTaxi;
    TextView distance;
    Switch shareLocation;
    public static final String MYPREFERENCE = "mypref";
    public static final String MAX_TAXI_KEY = "maxTaxiKey";
    public static final String DISTANCE_KEY = "distanceKey";
    public static final String SHARE_LOCATION_KEY = "shareLocationKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        maxTaxi = (TextView) findViewById(R.id.maxTaxi);
        distance = (TextView) findViewById(R.id.distance);
        shareLocation = (Switch) findViewById(R.id.shareLocation);
        sharedpreferences = getSharedPreferences(MYPREFERENCE, Context.MODE_PRIVATE);
        if (sharedpreferences.contains(MAX_TAXI_KEY)) {
            maxTaxi.setText(sharedpreferences.getString(MAX_TAXI_KEY, ""));
        }
        if (sharedpreferences.contains(DISTANCE_KEY)) {
            distance.setText(sharedpreferences.getString(DISTANCE_KEY, ""));
        }
        if (sharedpreferences.contains(SHARE_LOCATION_KEY)) {
            shareLocation.setChecked(sharedpreferences.getBoolean(SHARE_LOCATION_KEY, false));
        }
    }

    public void Save(View view) {
        Intent intent = new Intent(getApplicationContext(),TaxiMain.class);
        String maxTaxiVal = this.maxTaxi.getText().toString();
        String distanceVal = this.distance.getText().toString();
        boolean shareLocationVal = this.shareLocation.isChecked();
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(MAX_TAXI_KEY, maxTaxiVal);
        editor.putString(DISTANCE_KEY, distanceVal);
        editor.putBoolean(SHARE_LOCATION_KEY, shareLocationVal);
        editor.apply();
        finish();
        startActivity(intent);
    }

    public void clear(View view) {
        maxTaxi = (TextView) findViewById(R.id.maxTaxi);
        distance = (TextView) findViewById(R.id.distance);
        shareLocation = (Switch) findViewById(R.id.shareLocation);
        maxTaxi.setText("");
        distance.setText("");
        shareLocation.setChecked(false);
    }

    public void Get(View view) {
        maxTaxi = (TextView) findViewById(R.id.maxTaxi);
        distance = (TextView) findViewById(R.id.distance);
        sharedpreferences = getSharedPreferences(MYPREFERENCE,
                Context.MODE_PRIVATE);

        if (sharedpreferences.contains(MAX_TAXI_KEY)) {
            maxTaxi.setText(sharedpreferences.getString(MAX_TAXI_KEY, ""));
        }
        if (sharedpreferences.contains(DISTANCE_KEY)) {
            distance.setText(sharedpreferences.getString(DISTANCE_KEY, ""));
        }
        if (sharedpreferences.contains(SHARE_LOCATION_KEY)) {
            shareLocation.setChecked(sharedpreferences.getBoolean(SHARE_LOCATION_KEY, false));
        }
    }
}
