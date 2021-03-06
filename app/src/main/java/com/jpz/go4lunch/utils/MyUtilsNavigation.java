package com.jpz.go4lunch.utils;

import android.content.Context;
import android.content.Intent;

import com.jpz.go4lunch.activities.ConnectionActivity;
import com.jpz.go4lunch.activities.DetailsRestaurantActivity;
import com.jpz.go4lunch.activities.MainActivity;
import com.jpz.go4lunch.activities.SettingsActivity;

public class MyUtilsNavigation {
    // Class to navigate between activities

    // Key for Intent
    public static final String KEY_ID = "key_id";

    // Start DetailsRestaurantActivity when click the user click on a restaurant
    // (from the map or list), a workmate or "your lunch" and transfer the Place id.
    public void startDetailsRestaurantActivity(Context context, String id) {
        Intent intent = new Intent(context, DetailsRestaurantActivity.class);
        intent.putExtra(KEY_ID, id);
        context.startActivity(intent);
    }

    public void startConnectionActivity(Context context) {
        Intent intent = new Intent(context, ConnectionActivity.class);
        context.startActivity(intent);
    }

    public void startSettingsActivity(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }

    public void startMainActivity(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }
}