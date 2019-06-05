package com.foxfire.user;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.foxfire.user.APICALL.APIClient;
import com.foxfire.user.APICALL.APIInterface;
import com.foxfire.user.APICALL.UserData.UserData;
import com.foxfire.user.Utils.Utils;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0;
    private final String TAG = "LoginActivity";
    @BindView(R.id.userIdLayout)
    TextInputLayout userIdLayout;
    @BindView(R.id.userPassLayout)
    TextInputLayout userPassLayout;
    @BindView(R.id.changePassBtn)
    Button button;
    private String uid, pass;
    private String deviceIMEI = "";
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
        try {
            deviceIMEI = telephonyManager.getDeviceId();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "onCreate: exception with telephone permission " + e.getMessage());
        }

        checkLogin();
    }

    private void checkLogin() {
        boolean userFirstLogin = pref.getBoolean("login", false);  // getting boolean
        if (userFirstLogin) {
            Log.e(TAG, "checkLogin: please Wait!!");
            userPassLayout.setEnabled(false);
            userIdLayout.setEnabled(false);
            //String user = pref.getString("user", "null");
            //String master = pref.getString("master", "null");
            //uploadData(user, master);
            Utils.setIntent(LoginActivity.this, MainActivity.class);
        }
        Log.e(TAG, "checkLogin: login found in pref " + userFirstLogin);
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    private void apiCall() {
        APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);
        Call<UserData> call = apiInterface.getUserData(Integer.parseInt(uid));
        call.enqueue(new Callback<UserData>() {
            @Override
            public void onResponse(@NonNull Call<UserData> call, @NonNull Response<UserData> response) {
                UserData data = response.body();
                assert data != null;
                String password = data.user_password;
                String imei = data.imei;
                String passCode = data.lock_password + "";
                String master_id = data.master_id + "";

                Utils.showLog(TAG, "pass, imei from API ", password + " " + imei);
                Utils.showLog(TAG, "device imei ", deviceIMEI);
                Utils.showLog(TAG, "device lock pass ", passCode);

                if (pass.equals(password)) {
                    if (imei.equals(deviceIMEI)) {
                        editor = pref.edit();
                        editor.putBoolean("login", true);
                        editor.putString("passcode", passCode);
                        editor.putString("master", master_id);
                        editor.putString("user", Objects.requireNonNull(userIdLayout.getEditText()).getText().toString());
                        editor.apply();
                        uploadData(userIdLayout.getEditText().getText().toString(), master_id);
                    } else
                        Utils.showMessage(LoginActivity.this, "Device does not match with user");
                } else
                    Utils.showMessage(LoginActivity.this, "Password not correct");
            }

            @Override
            public void onFailure(@NonNull Call<UserData> call, @NonNull Throwable t) {
                Utils.showLog(TAG, "exception ", t.getMessage());
                Utils.showMessage(LoginActivity.this, "Details did not match with server");
            }
        });
    }

    private void uploadData(String user_id, String master_id) {
        Log.e(TAG, "uploadData: login uploadData ");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        HashMap<String, Object> map = new HashMap<>();
        map.put("user_id", user_id);
        map.put("master_id", master_id);
        map.put("start", "on");
        firestore.collection("Users").document(user_id)
                .get().addOnCompleteListener(task -> {
            if (task.getResult().exists()) {
                Log.e(TAG, "loginUser: data exists");
                firestore.collection("Users").document(user_id).update(map)
                        .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                Log.e(TAG, "loginUser: user data saved ");
                                Utils.setIntent(LoginActivity.this, MainActivity.class);
                            } else
                                Log.e(TAG, "loginUser: user data error " + Objects.requireNonNull(task1.getException()).getMessage());
                        });
            } else {
                Log.e(TAG, "uploadData: data do not exists");
                firestore.collection("Users").document(user_id).set(map)
                        .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                Log.e(TAG, "loginUser: user data saved ");
                                Utils.setIntent(LoginActivity.this, MainActivity.class);
                            } else
                                Log.e(TAG, "loginUser: user data error " + Objects.requireNonNull(task1.getException()).getMessage());
                        });
            }
            Utils.setIntent(LoginActivity.this, MainActivity.class);
        }).addOnFailureListener(e -> Log.e(TAG, "uploadData: failed " + e.getMessage()));
        Utils.setIntent(LoginActivity.this, MainActivity.class);
    }

    @OnClick(R.id.changePassBtn)
    public void onViewClicked() {
        Utils.showMessage(LoginActivity.this, "Please Wait....");
        if (checkFields()) {
            apiCall();
        } else {
            Toast.makeText(this, "Please fill both fields", Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkFields() {
        uid = Objects.requireNonNull(userIdLayout.getEditText()).getText().toString();
        pass = Objects.requireNonNull(userPassLayout.getEditText()).getText().toString();
        return !uid.isEmpty() || !pass.isEmpty();
    }
}
