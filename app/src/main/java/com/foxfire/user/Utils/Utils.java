package com.foxfire.user.Utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class Utils<Data> {
    @NonNull
    public static Intent setIntent(@NonNull Context context, Class destination) {
        Intent intent = new Intent(context, destination);
        context.startActivity(intent);
        return intent;
    }

    @NonNull
    public static Intent setIntentFinish(@NonNull Context context, Class destination) {
        Intent intent = new Intent(context, destination);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        return intent;
    }

    @NonNull
    public static Intent setIntentExtra(@NonNull Context context, Class destination, String key, Bundle data) {
        Intent intent = new Intent(context, destination);
        intent.putExtra(key, data);
        context.startActivity(intent);
        return intent;
    }

    public static void showMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static void showLog(String TAG, String message, String exception) {
        Log.e(TAG, "showLog: " + message + " " + exception);
    }
}
