package taxi.com.taxiapp;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationTrack extends Service implements LocationListener {
    private static final String TAG = "LocationTrack";
    private final Context mContext;
    boolean checkGPS = false;
    boolean checkNetwork =false;
    boolean canGetLocation =false;
    Location loc;
    double latitude;
    double longitude;
    String addressStr;
    private static final long MIN_DISTANCE_CHANGE_FORM_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;
    protected LocationManager locationManager;

    public LocationTrack(Context mContext) {
        this.mContext = mContext;
        getLocation();
    }

    private Location getLocation() {
        try{
            //Get the Location service from Android
            locationManager = (LocationManager)mContext.getSystemService(LOCATION_SERVICE);
            //Check GPS availability
            checkGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            //Check Network availability
            checkNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if(!checkGPS && !checkNetwork){
                Toast.makeText(mContext,"No Service Provider", Toast.LENGTH_LONG).show();
            }else{
                canGetLocation = true;
            }
            if(checkGPS){
                if( ActivityCompat.checkSelfPermission(
                        mContext,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(mContext,
                                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        ){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FORM_UPDATES,this);
                    if(locationManager!=null){
                        loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if(loc!=null){
                            latitude = loc.getLatitude();
                            longitude = loc.getLongitude();
//                            getAddress(loc);
                        }
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return loc;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void getAddress(Location location){
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String errorMessage = "";
//        String addressStr = "";

        List<Address> addresses = null;

        try {
            // In this sample, get just a single address.
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
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
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
//                errorMessage = getString(R.string.no_address_found);
                errorMessage = "no_address_found";
                Log.e(TAG, errorMessage);
            }
//            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
//            Log.i(TAG, getString(R.string.address_found));
            Log.i(TAG, "address_found");
            addressStr = TextUtils.join(System.getProperty("line.separator"),addressFragments);
//                            addressFragments)
//            deliverResultToReceiver(Constants.SUCCESS_RESULT,
//                    TextUtils.join(System.getProperty("line.separator"),
//                            addressFragments));
//        }    }
        }
//        return addressStr;
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    public double getLongitude() {
        //longitude = loc.getLongitude();
        return longitude;
    }

    public double getLatitude() {
        //latitude = loc.getLatitude();
        return latitude;
    }

    public Location getLoc(){
        return loc;
    }

    public String getAddressStr(){
        return addressStr;
    }
}
