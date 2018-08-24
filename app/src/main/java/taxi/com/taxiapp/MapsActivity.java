package taxi.com.taxiapp;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static java.lang.Math.signum;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private ClusterManager<Taxi> mClusterManager;
    public LatLng myPosition;
    public String address;
    LocationTrack locationTrack;
    //Permission variables
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    private final static int ALL_PERMISSIONS_RESULT = 101;
//    protected Location mLastLocation;
//    private AddressResultReceiver mResultReceiver;
    private Random mRandom = new Random(1984);
    double latMin = 103.608656;
    double latMax = 104.095625;
    double longMin = 1.226108;
    double longMax = 1.470211;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Need to set PERMISSION
        getPermission();
//        startIntentService();

        //Get our current location
        locationTrack = new LocationTrack(MapsActivity.this);
        if(locationTrack.canGetLocation()){
            double lon = locationTrack.getLongitude();
            double lat = locationTrack.getLatitude();
            myPosition = new LatLng(lat,lon);
//            myPosition = new LatLng(locationTrack.getLongitude(), locationTrack.getLatitude());
//            mLastLocation = locationTrack.getLoc();
            address = locationTrack.getAddressStr();
            Toast.makeText(getApplicationContext(), myPosition.latitude + " " + myPosition.longitude, Toast.LENGTH_LONG).show();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //Call the web service and load taxi data
        new FetchTaxiInfo().execute();//Async
    }

    private void getPermission() {
        permissions.add(ACCESS_FINE_LOCATION);
        permissions.add(ACCESS_COARSE_LOCATION);

        permissionsToRequest = findUnAskedPermissions(permissions);
        //get the permissions we have asked for before but are not granted..
        //we will store this in a global list to access later.


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0)
                requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
        }
    }

    private ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList<String> result = new ArrayList<String>();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case ALL_PERMISSIONS_RESULT:
                for (String perms : permissionsToRequest) {
                    if (!hasPermission(perms)) {
                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }

                }

                break;
        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MapsActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
//        myPosition = new LatLng(1.309717700, 103.777356900);
        address = getAddress(myPosition);
        if(address == null || address.equals("")){
            address = "My Position";
        }
        mMap.addMarker(new MarkerOptions().position(myPosition).title(address)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        SharedPreferences sharedpreferences = getSharedPreferences(SettingsActivity.MYPREFERENCE, Context.MODE_PRIVATE);;
        int distance = 1000;
        if (sharedpreferences.contains(SettingsActivity.DISTANCE_KEY)) {
            distance = Integer.parseInt(sharedpreferences.getString(SettingsActivity.DISTANCE_KEY, "1")) * 1000;
        }
        Circle circle = mMap.addCircle(new CircleOptions()
                .center(myPosition)
                .radius(distance)
                .strokeColor(Color.RED));
        circle.setVisible(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPosition,15.0f));
        mClusterManager = new ClusterManager<Taxi>(this, mMap);
        mClusterManager.setAnimation(false);
        mMap.setOnCameraIdleListener(mClusterManager);
        boolean shareLocation = false;
        if (sharedpreferences.contains(SettingsActivity.SHARE_LOCATION_KEY)) {
            shareLocation = sharedpreferences.getBoolean(SettingsActivity.SHARE_LOCATION_KEY, false);
        }
        if(shareLocation){
            Log.i(TAG, "Share location");
            handleShareLocation();
        }
    }

    private void handleShareLocation() {
        int simulatedNumber = 1000;
        int distance = 1;
        SharedPreferences sharedpreferences = getSharedPreferences(SettingsActivity.MYPREFERENCE, Context.MODE_PRIVATE);;
        if (sharedpreferences.contains(SettingsActivity.DISTANCE_KEY)) {
            distance = Integer.parseInt(sharedpreferences.getString(SettingsActivity.DISTANCE_KEY, "1"));
        }
        List<Taxi> taxis = new ArrayList<Taxi>();
        ArrayList<LatLng> array = new ArrayList<LatLng>();
        for (int i = 0; i < simulatedNumber; i++) {
            LatLng generated = position();
            array.add(generated);
        }
        Iterator<LatLng> iterator;
        iterator = array.iterator();
        while (iterator.hasNext()) {
            LatLng l = iterator.next();
            if(DistanceCalculator.distance(l.latitude,l.longitude,
                    myPosition.latitude,myPosition.longitude,"K")<distance) {
                mMap.addMarker(new MarkerOptions()
                        .position(l)
                        .title("")
                        .snippet("")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.greeting_man_l)));
            }
        }
    }

    private String getAddress(LatLng position){
        String addressStr ="";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String errorMessage = "";

        List<Address> addresses = null;

        try {
            // In this sample, get just a single address.
            addresses = geocoder.getFromLocation(
                    position.latitude,
                    position.longitude,
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
//            errorMessage = getString(R.string.service_not_available);
            errorMessage = "service_not_available";
            Log.e(TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
//            errorMessage = getString(R.string.invalid_lat_long_used);
            errorMessage = "invalid_lat_long_used";
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + position.latitude +
                    ", Longitude = " +
                    position.longitude, illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
//                errorMessage = getString(R.string.no_address_found);
                errorMessage = "no_address_found";
                Log.e(TAG, errorMessage);
            }
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
//            Log.i(TAG, getString(R.string.address_found));
//            Log.i(TAG, "address_found");
            addressStr = TextUtils.join(System.getProperty("line.separator"), addressFragments);
        }
        return addressStr;
    }

    private LatLng position() {
        return new LatLng(random(longMin, longMax), random(latMin, latMax));
    }

    private double random(double min, double max) {
        return mRandom.nextDouble() * (max - min) + min;
    }

//    protected void startIntentService() {
//        Intent intent = new Intent(this, FetchAddressIntentService.class);
//        intent.putExtra(Constants.RECEIVER, mResultReceiver);
//        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
//        startService(intent);
//    }

    // Inner class FetchTaxiInfo
    public class FetchTaxiInfo extends AsyncTask<Void,Void,Void>{
        private static final String TAG = "FetchTaxiInfo";
        //        static final String URL_STRING =
//                "https://api.data.gov.sg/v1/transport/taxi-availability?date_time=2018-07-17T18%3A49%3A00";
        static final String URL_STRING =
                "https://api.data.gov.sg/v1/transport/taxi-availability";

        String response;
        @Override
        protected Void doInBackground(Void... voids) {
            response = creatingURLConnection(URL_STRING);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
/*            Toast.makeText(getApplicationContext(),
                    "Connection Successful",
                    Toast.LENGTH_LONG).show();
            Toast.makeText(getApplicationContext(),
                    response,
                    Toast.LENGTH_LONG).show();*/
            try{
                if(response!=null && response!=""){
                    //Create a JSON reader
                    JSONObject reader = new JSONObject(response);
                    JSONArray features = (JSONArray) reader.get("features");
                    JSONObject obj = features.getJSONObject(0);
                    JSONObject geo = obj.getJSONObject("geometry");
                    JSONArray array = geo.getJSONArray("coordinates");
                    //Toast.makeText(getApplicationContext(),
                    //        array.length()+"",
                    //        Toast.LENGTH_LONG).show();
                    List<Taxi> taxis = new ArrayList<Taxi>();
                    int distance = 1;
                    int maxTaxi = 10;
                    SharedPreferences sharedpreferences = getSharedPreferences(SettingsActivity.MYPREFERENCE, Context.MODE_PRIVATE);;
                    if (sharedpreferences.contains(SettingsActivity.MAX_TAXI_KEY)) {
                        maxTaxi = Integer.parseInt(sharedpreferences.getString(SettingsActivity.MAX_TAXI_KEY, ""));
                    }
                    if (sharedpreferences.contains(SettingsActivity.DISTANCE_KEY)) {
                        distance = Integer.parseInt(sharedpreferences.getString(SettingsActivity.DISTANCE_KEY, ""));
                    }
                    Log.i(TAG, "distance = " + distance + " maxTaxi = " + maxTaxi);
                    int taxiCount = 0;
                    if(array.length()>0){
                        Map<Double, Taxi> sortTaxi = null;
                        Map<Double, Taxi> unsortTaxi = new HashMap<>();
                        //Get all taxi within distance requested
                        for(int i=0;i<array.length();i++){
                            JSONArray coord = array.getJSONArray(i);
                            LatLng l = new LatLng(coord.getDouble(1),
                                    coord.getDouble(0));
                            double diffDistance = DistanceCalculator.distance(l.latitude,l.longitude,
                                    myPosition.latitude,myPosition.longitude,"K");
                            if(diffDistance<distance) {
                                String address = "";
                                address = getAddress(l);
                                Taxi taxi = new Taxi(l.latitude, l.longitude, address, "");
                                unsortTaxi.put(diffDistance, taxi);
                            }
                        }
                        //Sort taxis by distance from current position and set markers for the required number of taxis
                        sortTaxi = new TreeMap<Double, Taxi>(unsortTaxi);
                        if(sortTaxi != null) {
                            Log.i(TAG, "sortTaxi " + sortTaxi.size());
                            for (Map.Entry<Double, Taxi> entry : sortTaxi.entrySet()) {
                                Double dist = entry.getKey();
                                Log.i(TAG, "dist = " + dist);
                                Taxi taxi = entry.getValue();
                                taxis.add(taxi);
                                taxiCount++;
                                if(taxiCount == maxTaxi){
                                    Log.i(TAG, "Taxi count reach");
                                    break;
                                }
                            }
                        }
                        mClusterManager.addItems(taxis);
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "Error fetching data", Toast.LENGTH_LONG).show();
                    simulateTaxi();
                }
            }catch(Exception e){
                e.printStackTrace();
                simulateTaxi();

            }
        }

        private void simulateTaxi(){
            int simulatedNumber = 10000;
            Log.i(TAG, "simulateTaxi");
            int distance = 1;
            int maxTaxi = 10;
            SharedPreferences sharedpreferences = getSharedPreferences(SettingsActivity.MYPREFERENCE, Context.MODE_PRIVATE);;
            if (sharedpreferences.contains(SettingsActivity.MAX_TAXI_KEY)) {
                maxTaxi = Integer.parseInt(sharedpreferences.getString(SettingsActivity.MAX_TAXI_KEY, ""));
            }
            if (sharedpreferences.contains(SettingsActivity.DISTANCE_KEY)) {
                distance = Integer.parseInt(sharedpreferences.getString(SettingsActivity.DISTANCE_KEY, ""));
            }
            Log.i(TAG, "distance = " + distance + " maxTaxi = " + maxTaxi);
            int taxiCount = 0;
            List<Taxi> taxis = new ArrayList<Taxi>();
            ArrayList<LatLng> array = new ArrayList<LatLng>();
            for(int i = 0; i < simulatedNumber; i++){
                LatLng generated = position();
                array.add(generated);
            }
            Iterator<LatLng> iterator;
            iterator = array.iterator();
            while(iterator.hasNext()){
                LatLng l = iterator.next();
                if(DistanceCalculator.distance(l.latitude,l.longitude,
                        myPosition.latitude,myPosition.longitude,"K")<distance) {
                    taxis.add(new Taxi(l.latitude, l.longitude, "", ""));
                    taxiCount++;
                    if(taxiCount == maxTaxi){
                        Log.i(TAG, "Taxi count reach");
                        break;
                    }
                }
            }
            Log.i(TAG, "taxis size = " + taxis.size());
            mClusterManager.addItems(taxis);
        }

        private String creatingURLConnection(String urlString) {
            String response = "";
            HttpURLConnection conn;
            StringBuilder jsonResults = new StringBuilder();
            try{
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd");
                String strDate = simpleDateFormat.format(calendar.getTime());
                Log.i(TAG, "strDate = " + strDate);
                simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                String strTime = simpleDateFormat.format(calendar.getTime());
                strTime = strTime.replace(":", "%3A");
                Log.i(TAG, "strTime = " + strTime);
                //                ?date_time=2018-08-03T12%3A50%3A00
                String urlStringParameter = urlString + "?date_time=" + strDate +"T" + strTime;
                Log.i(TAG, "urlStringParameter = " + urlStringParameter);
                //Set the URL to connect to
                URL url = new URL(urlStringParameter);
                //create the connection
                conn = (HttpURLConnection)url.openConnection();
                //Pull the data from the web service
                InputStreamReader in = new InputStreamReader(
                        conn.getInputStream()
                );
                int read;
                char[] buff = new char[1024];
                while((read =  in.read(buff))!=-1){
                    jsonResults.append(buff,0,read);
                }
                response = jsonResults.toString();
            }catch(Exception e){
                e.printStackTrace();
            }
            return response;
        }
    }
    // Inner class FetchTaxiInfo End
}
