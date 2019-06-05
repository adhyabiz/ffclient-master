package com.foxfire.user;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.foxfire.user.Utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsActivity extends AppCompatActivity {
    @BindView(R.id.passCodeTV)
    EditText changePassET;
    @BindView(R.id.confirmPassET)
    EditText confirmPassET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

    }

    @OnClick({R.id.changePassBtn, R.id.logOutBtn})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.changePassBtn:
                changePass();
                break;
            case R.id.logOutBtn:
                logOut();
                break;
        }
    }

    private void changePass() {
        if (passSame()) {
            // TODO change pass on server
        } else {
            Toast.makeText(this, "Password not same", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean passSame() {
        return changePassET.getText().toString().equals(confirmPassET.getText().toString());
    }

    private void logOut() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.apply();
        Log.e("SettingsActivity", "logOut: ");
        Utils.setIntentFinish(this, LoginActivity.class);
    }
}
