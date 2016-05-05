package aeq.com.gmap.controller;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GmapActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {


    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;

    private LocationRequest mLocationRequest;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters

    // UI elements
    private EditText lblLocation;
    private Button btnShowLocation, btnStartLocationUpdates;
    Fragment frg;
    LatLng latLng;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    LatLng mLatLng;
    //Vivek
    GoogleMap googleMap;
    MarkerOptions markerOptions = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gmap);
        //GoogleMap googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map));
        lblLocation = (EditText) findViewById(R.id.srch_txt);
        Button btnSrch = (Button) findViewById(R.id.btn_srch);

        btnSrch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String srch_box_txt = lblLocation.getText().toString().trim();
                mLatLng = getLocationFromAddress(GmapActivity.this, srch_box_txt);
                Toast.makeText(GmapActivity.this, "" + mLatLng, Toast.LENGTH_SHORT).show();
                try {
                    if(mLatLng != null){
                    markPosition(mLatLng);}
                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
        });

        try {
            // Loading map
            initilizeMap();

        } catch (Exception e) {
            e.printStackTrace();
        }


        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        } else {
            if (checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
            }
        }
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            displayLocation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


    /**
     * function to load map. If map is not created it will create it for you
     */
    private void initilizeMap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                    R.id.map)).getMap();

            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initilizeMap();
        client.connect();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initilizeMap();
        checkPlayServices();
        if (mGoogleApiClient != null)
            if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
                startLocationUpdates(this);
                // displayLocation();
            }

    }

    @Override
    protected void onStop() {
        super.onStop();
        client.disconnect();
    }

    /**
     * Method to verify google play services on the device
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
    }

    /**
     * Starting the location updates
     */
    protected void startLocationUpdates(Context context) {

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

    }

    // Get the address from the latitude and longitude
    public String getCurrentAddress(double latitude, double longitude) throws IOException {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> listAdd = geocoder.getFromLocation(latitude, longitude, 1);
        String adr = "";
        if (listAdd.size() > 0) {
            // Toast.makeText(GmapActivity.this, "" + listAdd, Toast.LENGTH_LONG).show();
            adr = listAdd.get(0).getLocality();
            // listAdd.get(0).getSubLocality()  /* Venkateshwara Nagar */
            //listAdd.get(0).getLocality()    /* Chennai */
            //listAdd.get(0).getAdminArea() /* Tamil Nadu */
            //listAdd.get(0).getAddressLine(1); /*Venkatashwara Nagar velachery*/
        }
        return adr;
    }

    //DISPLAY THE LOCATION
    private void displayLocation() throws IOException {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            if (mLatLng == null) {
                double latitude = mLastLocation.getLatitude();
                double longitude = mLastLocation.getLongitude();
                mLatLng = new LatLng(latitude,longitude);
            }
            if (mLastLocation != null) {
                markPosition(mLatLng);
            }

            // latLng = new LatLng(latitude, longitude);
        } else {
            lblLocation
                    .setText("(Couldn't get the location. Make sure location is enabled on the device)");
        }
    }

    public void markPosition(LatLng latLng) throws IOException {
        String currentAddress = getCurrentAddress(latLng.latitude, latLng.longitude);
        Log.d("displayLocation: ", currentAddress);
        markerOptions = new MarkerOptions().position(latLng).title(currentAddress);
// adding marker
        googleMap.addMarker(markerOptions);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(latLng.latitude, latLng.longitude), 13));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latLng.latitude,latLng.longitude))      // Sets the center of the map to location user
                .zoom(18)                   // Sets the zoom
                .bearing(90)                // Sets the orientation of the camera to east
                .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    /**
     * Creating location request object
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        if (checkPlayServices()) {
                            buildGoogleApiClient();
                            createLocationRequest();
                        }
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    /*
    * get Latitude and longitude based on address
    * */


    public LatLng getLocationFromAddress(Context context, String address) {
        Geocoder geoCoder = new Geocoder(context);
        double lat = 0.0;
        double lng = 0.0;
        if (address != null && !address.isEmpty()) {
            try {
                List<Address> addressList = geoCoder.getFromLocationName(address, 1);
                if (addressList != null && addressList.size() > 0) {
                    lat = addressList.get(0).getLatitude();
                    lng = addressList.get(0).getLongitude();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } // end catch
        } //
        return new LatLng(lat, lng);
    }
}