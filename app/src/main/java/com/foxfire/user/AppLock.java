package com.foxfire.user;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.foxfire.user.Utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AppLock extends AppCompatActivity {

    @BindView(R.id.twoTV)
    TextView twoTV;
    @BindView(R.id.threeTV)
    TextView threeTV;
    @BindView(R.id.oneTV)
    TextView oneTV;
    @BindView(R.id.fiveTV)
    TextView fiveTV;
    @BindView(R.id.fourTV)
    TextView fourTV;
    @BindView(R.id.sixTV)
    TextView sixTV;
    @BindView(R.id.eightTV)
    TextView eightTV;
    @BindView(R.id.sevenTV)
    TextView sevenTV;
    @BindView(R.id.nineTV)
    TextView nineTV;
    @BindView(R.id.zeroTV)
    TextView zeroTV;
    private final String TAG = "AppLock";
    @BindView(R.id.backBtn)
    Button backBtn;
    @BindView(R.id.changePassBtn)
    Button okBtn;
    @BindView(R.id.passCodeTV)
    TextView passCodeTV;

    private String passCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock);
        ButterKnife.bind(this);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        passCode = pref.getString("passcode", null);  // getting passCode
        Log.e(TAG, "onCreate: passCode in lockActivity " + passCode);

        passCodeTV.setEnabled(false);
    }

    private void setChar(String c) {
        String s = "";
        if (passCodeTV.getText().equals(getResources().getString(R.string.your_passcode)))
            passCodeTV.setText("");

        if (changePassTVMax()) {
            s += passCodeTV.getText();
            s += c;
            passCodeTV.setText(s);
        } else {

        }
    }

    private boolean changePassTVMax() {
        return passCodeTV.getText().length() <= 4 || passCodeTV.getText().equals(getResources().getString(R.string.your_passcode));
    }

    private boolean changePassTVCheck() {
        return !passCodeTV.getText().toString().isEmpty();
    }

    private void clearText() {
        String s = passCodeTV.getText().toString();
        if (s.length() > 0) {
            s = s.substring(0, (s.length() - 1));
        }
        passCodeTV.setText(s);
    }

    private void submit() {
        if (changePassTVCheck()) {
            if (passCodeTV.getText().toString().equals(passCode))
                Utils.setIntent(this, SettingsActivity.class);
            else
                Toast.makeText(this, "Pass Code not matched with server", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please fill your code first", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick({R.id.twoTV, R.id.threeTV, R.id.oneTV, R.id.fiveTV, R.id.fourTV, R.id.sixTV, R.id.eightTV, R.id.sevenTV, R.id.nineTV, R.id.zeroTV, R.id.changePassBtn, R.id.backBtn})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.twoTV:
                setChar("2");
                break;
            case R.id.threeTV:
                setChar("3");
                break;
            case R.id.oneTV:
                setChar("1");
                break;
            case R.id.fiveTV:
                setChar("5");
                break;
            case R.id.fourTV:
                setChar("4");
                break;
            case R.id.sixTV:
                setChar("6");
                break;
            case R.id.eightTV:
                setChar("8");
                break;
            case R.id.sevenTV:
                setChar("7");
                break;
            case R.id.nineTV:
                setChar("9");
                break;
            case R.id.zeroTV:
                setChar("0");
                break;
            case R.id.changePassBtn:
                submit();
                break;
            case R.id.backBtn:
                clearText();
                break;
        }
    }
}
