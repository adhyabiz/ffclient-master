package com.foxfire.user;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.foxfire.user.APICALL.APIInterface;
import com.foxfire.user.APICALL.APINotiClient;
import com.foxfire.user.Notification.Data;
import com.foxfire.user.Notification.MyResponse;
import com.foxfire.user.Notification.Notification;
import com.foxfire.user.Notification.Sender;
import com.foxfire.user.Utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    @BindView(R.id.map_speedTV)
    TextView mapSpeedTV;
    @BindView(R.id.searchImageView)
    ImageView searchImageView;
    @BindView(R.id.searchTV)
    TextView searchTV;
    @BindView(R.id.searchET)
    EditText searchET;

    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private Marker mCurrLocationMarker;
    private MediaRecorder myAudioRecorder;

    private static final String TAG = "MainActivity";
    private static final int REQUEST_LOCATION_PERMISSION_CODE = 101;
    private MarkerOptions markerOptions;
    private PendingIntent pendingIntent;
    private FirebaseFirestore firestore;
    private String user_id, master_id;
    private HashMap<String, Object> locations = new HashMap<>();
    private boolean updatedLocation = false;
    private boolean notificationSent = false;
    private int moveSpeed = 0;
    private Location oldLocation;
    private String msgFromGeo;
    private boolean openingNotification = true;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("Status");
            Log.e(TAG, "onReceive: msg from GeoService " + message);
            Log.e(TAG, "onReceive: notificationSent " + notificationSent);

            if (message.contains("outside")) {
                Log.e(TAG, "onReceive: outside if");
                Log.e(TAG, "onReceive: user id " + user_id);
                msgFromGeo = message;
                //outside the fencing
                HashMap<String, Object> map = new HashMap<>();
                map.put("time", FieldValue.serverTimestamp());
                map.put("msg", "user " + user_id + " is outside the fencing area at |");
                map.put("master_id", master_id);
                map.put("title", "Alert!! User Outside Fencing");
                Log.e(TAG, "onReceive: after map");
                if (!notificationSent) {
                    firestoreFencingNotification(map, "outside");
                    notificationSent = true;
                }
                Log.e(TAG, "onReceive: after firebase");
            } else if (message.contains("inside")) {
                if (!notificationSent) {
                    msgFromGeo = message;
                    Log.e(TAG, "onReceive: inside if");
                    Log.e(TAG, "onReceive: user id " + user_id);
                    //outside the fencing
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("time", FieldValue.serverTimestamp());
                    map.put("msg", "user " + user_id + " is inside the fencing area at |");
                    map.put("master_id", master_id);
                    map.put("title", "Alert!! User Outside Fencing");
                    Log.e(TAG, "onReceive: after map");
                    if (!notificationSent) {
                        firestoreFencingNotification(map, "inside");
                    }
                }
            }
        }
    };

    public static double getSpeed(Location currentLocation, Location oldLocation) {
        //  Click Speed of maps
        //TODO: 16/5/19 do Notification

        double newLat = currentLocation.getLatitude();
        double newLon = currentLocation.getLongitude();

        double oldLat = oldLocation.getLatitude();
        double oldLon = oldLocation.getLongitude();

        if (currentLocation.hasSpeed()) {
            return currentLocation.getSpeed();
        } else {
            double radius = 6371000;
            double dLat = Math.toRadians(newLat - oldLat);
            double dLon = Math.toRadians(newLon - oldLon);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(newLat)) * Math.cos(Math.toRadians(oldLat)) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.asin(Math.sqrt(a));
            double distance = Math.round(radius * c);

            double timeDifferent = currentLocation.getTime() - oldLocation.getTime();
            return (distance / timeDifferent) * 1.6;
        }
    }

    private void firestoreFencingNotification(HashMap<String, Object> map, String geo) {
        Log.e(TAG, "firestoreFencingNotification: inside notification");
        try {
            Log.e(TAG, "firestoreFencingNotification: inside notification try");
            firestore.collection("Users").document(user_id).collection("Notification").document().set(map)
                    .addOnSuccessListener(aVoid -> {
                        Log.e(TAG, "firebase: notification data send");
                        HashMap<String, Object> map1 = new HashMap<>();
                        map1.put("geoFencing", geo);
                        firestore.collection("Users").document(user_id).update(map1)
                                .addOnSuccessListener(aVoid1 -> Log.e(TAG, "firestoreFencingNotification: notification geo updated user document"))
                                .addOnFailureListener(e -> Log.e(TAG, "firestoreFencingNotification: notification failed " + e.getMessage()));
                        firestore.collection("Master").document(master_id).get()
                                .addOnCompleteListener(task -> {
                                    if (Objects.requireNonNull(task.getResult()).exists()) {
                                        String token = task.getResult().getString("token");
                                        String notiMsg = "User " + user_id + " is " + geo.toUpperCase();
                                        sendNotification(token, notiMsg);
                                    } else {
                                        Log.e(TAG, "addUserOutSide: exception " + Objects.requireNonNull(task.getException()).getMessage());
                                    }
                                });
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "onFailure: notification failed " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "onReceive: notification exception " + e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.main_map_frag);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        firestore = FirebaseFirestore.getInstance();
        checkFirebase();

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        user_id = pref.getString("user", "null");  // getting user_id
        master_id = pref.getString("master", "111");  // getting master_id

        Log.e(TAG, "onCreate: master_id " + master_id);

        addUserData();
        //addNotification();
        //github.com/iRahulGaur

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE);
        }

        searchET.setVisibility(View.GONE);

        searchET.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus)
                searchET.setVisibility(View.GONE);
        });
    }

    private void sendNotification(String token, String msg) {
        Log.e(TAG, "sendNotification: inside sendNotification");
        Data data = new Data(user_id, "no", "no");
        Notification notification = new Notification("" + msg, "Alert!!", "android.intent.action.MAIN");
        Sender sender = new Sender(notification, token, data);
        Log.e(TAG, "sendNotification: sender token " + token);
        APIInterface apiInterface = APINotiClient.getNotiClient().create(APIInterface.class);
        apiInterface.sendNotification(sender)
                .enqueue(new Callback<MyResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MyResponse> call, @NonNull Response<MyResponse> response) {
                        try {
                            if (Objects.requireNonNull(response.body()).success == 1)
                                Log.e(TAG, "onResponse: notification send " + msg);
                            else
                                Log.e(TAG, "onResponse: notification failed " + response.body().failure);
                        } catch (Exception e) {
                            Log.e(TAG, "onResponse: notification exception " + e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MyResponse> call, @NonNull Throwable t) {
                        try {
                            Log.e(TAG, "onFailure: notification failed " + t.getMessage());
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "onFailure: notification exception " + e.getMessage());
                        }
                    }
                });
    }

    private void addUserOutSide(String msg) {
        FirebaseFirestore firebase = FirebaseFirestore.getInstance();
        HashMap<String, Object> map = new HashMap<>();
        map.put("geoFencing", msg);
        firebase.collection("Users").document(user_id).update(map)
                .addOnSuccessListener(aVoid -> Log.e(TAG, "addUserOutSide: user is outside "))
                .addOnFailureListener(e -> Log.e(TAG, "addUserOutSide: failed outside"));

        firebase.collection("Master").document(master_id).get()
                .addOnCompleteListener(task -> {
                    if (Objects.requireNonNull(task.getResult()).exists()) {
                        String token = task.getResult().getString("token");
                        String notiMsg = "User " + user_id + " is " + msg.toUpperCase() + " Fencing area";
                        sendNotification(token, notiMsg);
                    } else {
                        Log.e(TAG, "addUserOutSide: exception " + Objects.requireNonNull(task.getException()).getMessage());
                    }
                });
    }

    private void checkFirebase() {
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = Objects.requireNonNull(snapshot.getValue(Boolean.class));
                if (connected) {
                    Log.d(TAG, "connected");
                } else {
                    Log.d(TAG, "not connected");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Listener was cancelled");
            }
        });
    }

    private void search() {
        //24/05/2019 Search Use

        String location = searchET.getText().toString();
        if (location.equals("")) {
            searchET.setError("please Enter Address");
        } else {
            List<Address> addressList = null;

            if (location != null || !location.equals("")) {
                Geocoder geocoder = new Geocoder(this);
                try {
                    addressList = geocoder.getFromLocationName(location, 1);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                Address address = Objects.requireNonNull(addressList).get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                map.addMarker(new MarkerOptions().position(latLng).title("Marker"));
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }

            searchET.setText("");

        }
    }


    private void addNotification() {
        //click Notification on Maps
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Notification Example")
                .setContentText("This is a best Notification");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(contentPendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }

    private void screenShot() {
        //click screenshot of the app
        Bitmap bitmap = takeScreenshot();
        saveBitmap(bitmap);
    }

    public Bitmap takeScreenshot() {
        View rootView = findViewById(android.R.id.content).getRootView();
        rootView.setDrawingCacheEnabled(true);
        return rootView.getDrawingCache();
    }

    public void saveBitmap(Bitmap bitmap) {
        File imagePath = new File(Environment.getExternalStorageDirectory() + "/screenshot.png");
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(imagePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e("GREC", e.getMessage(), e);
        } catch (IOException e) {
            Log.e("GREC", e.getMessage(), e);
        }
    }

    private void addUserData() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("user_id", user_id);
        map.put("master_id", master_id);
        map.put("start", "on");
        firestore.collection("Users").document(user_id)
                .get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.e(TAG, "addUserData: data exists");
                firestore.collection("Users").document(user_id).update(map)
                        .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful())
                                Log.e(TAG, "addUserData: user data saved ");
                            else
                                Log.e(TAG, "addUserData: user data error " + Objects.requireNonNull(task1.getException()).getMessage());
                        });
            } else {
                firestore.collection("Users").document(user_id).update(map)
                        .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful())
                                Log.e(TAG, "addUserData: user data saved ");
                            else
                                Log.e(TAG, "addUserData: user data error " + Objects.requireNonNull(task1.getException()).getMessage());
                        });
            }
        });

        HashMap<String, Object> map1 = new HashMap<>();
        map1.put("start", "on");
        updateUserData(map1);

    }

    private void updateUserData(HashMap<String, Object> map1) {
        firestore.collection("Users").document(user_id).update(map1)
                .addOnSuccessListener(aVoid -> Log.e(TAG, "updateUserData: success engine updated "))
                .addOnFailureListener(e -> Log.e(TAG, "updateUserData: failed user update " + e.getMessage()));
    }

    private PendingIntent getGeofencePendingIntent() {
        if (pendingIntent != null) {
            return pendingIntent;
        }
        Intent intent = new Intent(this, GeofenceRegistrationService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    private Geofence getGeofence() {
        // get Geofencing click..

        LatLng latLng = Constant.AREA_LANDMARKS.get(Constant.GEOFENCE_ID_STAN_UNI);
        return new Geofence.Builder()
                .setRequestId(Constant.GEOFENCE_ID_STAN_UNI)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setCircularRegion(Objects.requireNonNull(latLng).latitude, latLng.longitude, Constant.GEOFENCE_RADIUS_IN_METERS)
                .setNotificationResponsiveness(1000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    private void stopGeoFencing() {
        pendingIntent = getGeofencePendingIntent();
        LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, pendingIntent)
                .setResultCallback(status -> {
                    if (status.isSuccess())
                        Log.d(TAG, "Stop geofencing");
                    else
                        Log.d(TAG, "Not stop geofencing");
                });
        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.disconnect();
        int response = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);
        if (response != ConnectionResult.SUCCESS) {
            Log.d(TAG, "Google Play Service Not Available");
            GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, response, 1).show();
        } else {
            Log.d(TAG, "Google play service available");
        }
        HashMap<String, Object> map1 = new HashMap<>();
        map1.put("start", "on");
        updateUserData(map1);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.reconnect();
        HashMap<String, Object> map1 = new HashMap<>();
        map1.put("start", "on");
        updateUserData(map1);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
        HashMap<String, Object> map1 = new HashMap<>();
        map1.put("start", "off");
        updateUserData(map1);
    }


    private void startGeofencing() {
        Log.d(TAG, "Start geofencing monitoring call");
        pendingIntent = getGeofencePendingIntent();
        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
                .addGeofence(getGeofence())
                .build();

        if (!mGoogleApiClient.isConnected()) {
            Log.d(TAG, "Google API client not connected");
        } else {
            try {
                LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, geofencingRequest,
                        pendingIntent).setResultCallback(status -> {
                    if (status.isSuccess()) {
                        Log.d(TAG, "Successfully Geofencing Connected");
                    } else {
                        Log.d(TAG, "Failed to add Geofencing " + status.getStatus());
                    }
                });
            } catch (SecurityException e) {
                Log.d(TAG, e.getMessage());
            }
        }
        invalidateOptionsMenu();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HashMap<String, Object> map1 = new HashMap<>();
        map1.put("start", "on");
        updateUserData(map1);
    }

    private void audioRecoding() {
        String outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.3gp";
        myAudioRecorder = new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        myAudioRecorder.setOutputFile(outputFile);
    }

    private void stopRecording() {
        Log.e("my audio recorder", " enter try  stop ");
        myAudioRecorder.stop();
        myAudioRecorder.release();
        myAudioRecorder = null;

        uploadRecording();
    }

    private void uploadRecording() {
    }

    private void recordAudio() {
        try {
            myAudioRecorder.prepare();
            myAudioRecorder.start();
        } catch (IllegalStateException ise) {
            // make something ...
        } catch (IOException ioe) {
            // make something
        }
    }

    private void settings() {
        Utils.setIntent(this, AppLock.class);
    }

    private void showSearchET() {
        searchET.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();
        HashMap<String, Object> map1 = new HashMap<>();
        map1.put("start", "on");
        updateUserData(map1);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        locationGeofencing();
        map.setMyLocationEnabled(true);

        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //Initialize Google Play Services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
//                Location Permission already granted
                buildGoogleApiClient();
                map.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {
            buildGoogleApiClient();
            map.setMyLocationEnabled(true);
        }

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private void locationGeofencing() {
        LatLng latLng = Constant.AREA_LANDMARKS.get(Constant.GEOFENCE_ID_STAN_UNI);
        map.addMarker(new MarkerOptions().position(Objects.requireNonNull(latLng)).title("New Delhi"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));

        map.addCircle(new CircleOptions()
                .center(new LatLng(latLng.latitude, latLng.longitude))
                .radius(Constant.GEOFENCE_RADIUS_IN_METERS)
                .strokeColor(Color.RED)
                .strokeWidth(4f));
    }

    private void addLocationData() {
        Log.e(TAG, "addLocationData: called");
        HashMap<String, Object> map = new HashMap<>();
        map.put("location", locations);
        map.put("time", FieldValue.serverTimestamp());
        map.put("master_id", master_id);
        firestore.collection("Users").document(user_id).update(map)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful())
                        Log.e(TAG, "addUserData: user data saved ");
                    else
                        Log.e(TAG, "addUserData: user data error " + Objects.requireNonNull(task.getException()).getMessage());
                });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Edit.........
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(1000);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                startLocationMonitor();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "onConnected: ecveption in start " + e.getMessage());
            }
            startGeofencing();
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    mMessageReceiver, new IntentFilter("Data"));
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        //when location changes Edit.........
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        Log.e(TAG, "onLocationChanged: on location change called ");
        //place current location market
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");

        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        mCurrLocationMarker = map.addMarker(markerOptions);

        //move map camera
        if (!updatedLocation)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));

        startLocationMonitor();
    }

    @SuppressLint("SetTextI18n")
    private void startLocationMonitor() {
        //Edit.........

        Log.d(TAG, "start location monitor");
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, location -> {

                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                if (oldLocation != null) {
                    Log.e(TAG, "startLocationMonitor: notification sent " + notificationSent);
                    Log.e(TAG, "startLocationMonitor: opening notification " + openingNotification);
                    if (openingNotification) {
                        if (msgFromGeo == null) {
                            Log.e(TAG, "onCreate: user outside fencing");
                            openingNotification = false;
                            addUserOutSide("outside");
                        } else {
                            openingNotification = false;
                            addUserOutSide("inside");
                        }
                    }
                    Log.e(TAG, "startLocationMonitor: inside oldLocation!=null ");
                    moveSpeed = 0;
                    moveSpeed = (int) getSpeed(location, oldLocation);
                    oldLocation = location;
                    if (moveSpeed <= 1) {
                        mapSpeedTV.setText("Not Moving");
                        addSpeedToFirebase("Not Moving");
                    } else {
                        mapSpeedTV.setText("Speed: " + moveSpeed + " km/h");
                        addSpeedToFirebase(moveSpeed + " KM/H");
                    }

                } else {
                    Log.e(TAG, "startLocationMonitor: location is null");
                    oldLocation = location;
                    if (moveSpeed <= 1) {
                        mapSpeedTV.setText("Not Moving");
                        addSpeedToFirebase("Not Moving");
                    }
                }

                Log.e(TAG, "startLocationMonitor: speed " + moveSpeed);

                markerOptions = new MarkerOptions();
                markerOptions.position(new LatLng(location.getLatitude(), location.getLongitude()));
                markerOptions.title("Current Location");
                mCurrLocationMarker = map.addMarker(markerOptions);
                Log.d(TAG, "Location Change Lat Lng " + location.getLatitude() + " " + location.getLongitude());

                try {
                    locations.remove(locations);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "startLocationMonitor: removing previous data");
                }
                locations.put("lat", location.getLatitude());
                locations.put("long", location.getLongitude());

                if (!updatedLocation) {
                    Log.e(TAG, "onLocationChanged: inside updatedLocation");
                    addLocationData();
                    updatedLocation = true;
                } else {
                    Log.e(TAG, "onLocationChanged: else of updatedLocation");
                }
            });
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
        }


    }

    private void addSpeedToFirebase(String moveSpeed) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("speed", moveSpeed);
        firestore.collection("Users").document(user_id).update(map)
                .addOnSuccessListener(aVoid -> Log.e(TAG, "addSpeedToFirebase: speed updated"))
                .addOnFailureListener(e -> Log.e(TAG, "addSpeedToFirebase: speed upload error"));
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Edit.........
        Log.d(TAG, "Google Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Edit.........
        Log.e(TAG, "Connection Failed:" + connectionResult.getErrorMessage());

    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", (dialogInterface, i) -> {
                            //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    MY_PERMISSIONS_REQUEST_LOCATION);
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // If request is cancelled, the result arrays are empty.
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay! Do the
                // location-related task you need to do.
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    if (mGoogleApiClient == null) {
                        buildGoogleApiClient();
                    }
                    map.setMyLocationEnabled(true);
                }

            } else {

                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @OnClick({R.id.searchImageView, R.id.searchTV, R.id.searchET, R.id.settingImageView, R.id.settingTV})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.searchImageView:
                showSearchET();
                break;
            case R.id.searchTV:
                showSearchET();
                search();
                break;
            case R.id.searchET:
                search();
                break;
            case R.id.settingImageView:
                settings();
                break;
            case R.id.settingTV:
                settings();
                break;
        }
    }
}
