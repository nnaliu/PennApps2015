package com.example.frederick.pennapps2015;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

public class MapsActivity extends FragmentActivity implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MapsActivity";
    private static final long INTERVAL = 1000 * 5; // 5 seconds
    private static final long FASTEST_INTERVAL = 1000 * 1; // 1 seconds
    private static final String URL_FORMAT = "http://dragonite.hcs.so/near/%1$s,%2$s";
    private static final Gson GSON = new Gson();
    private static final int THRESHOLD = 5;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;

    private boolean zoomedIn = false;

    private class FetchCrimeReportsTask extends AsyncTask<LatLng, Void, CrimeReport[]> {
        @Override
        protected CrimeReport[] doInBackground(LatLng... params) {
            LatLng latLng = params[0];

            try {
                URL url = new URL(String.format(URL_FORMAT, latLng.latitude, latLng.longitude));
                Log.d(TAG, "Fetching crime reports: " + url);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Accept-Encoding", "gzip");
                urlConnection.setAllowUserInteraction(false);
                urlConnection.connect();

                InputStream in = urlConnection.getInputStream();
                String contentEncoding = urlConnection.getContentEncoding();
                if (contentEncoding != null && contentEncoding.contains("gzip")) {
                    in = new GZIPInputStream(in);
                }
                Reader reader = new BufferedReader(new InputStreamReader(in));

                return GSON.fromJson(reader, CrimeReport[].class);
            } catch (IOException e) {
                Log.e(TAG, "Error fetching crime reports", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(CrimeReport[] crimeReports) {
            if (crimeReports != null) {
                mMap.clear();
                for (CrimeReport crimeReport : crimeReports) {
                    MarkerOptions marker = new MarkerOptions();
                    LatLng latLng = new LatLng(crimeReport.loc[1], crimeReport.loc[0]);
                    marker.position(latLng);
                    marker.title(crimeReport.type);
                    marker.snippet(crimeReport.address);
                    mMap.addMarker(marker);
                }

                if (crimeReports.length > THRESHOLD) {
                    
                }
            }
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        if (!isGooglePlayServicesAvailable()) {
            finish();
        }
        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        mGoogleApiClient.disconnect();
        Log.d(TAG, "isConnected: " + mGoogleApiClient.isConnected());
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected - isConnected: " + mGoogleApiClient.isConnected());
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        PendingResult<Status> pendingResults = LocationServices.FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        Log.d(TAG, "Location update started: " + pendingResults);
        zoomedIn = false;
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        Log.d(TAG, "Location updates stopped");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed: " + connectionResult);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: " + location);
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        zoomIn(latLng);

        FetchCrimeReportsTask task = new FetchCrimeReportsTask();
        task.execute(latLng);
    }

    private void zoomIn(LatLng latLng) {
        if (!zoomedIn) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
            Log.d(TAG, "Zoomed in");
            zoomedIn = true;
        }
    }
}
