package com.jpz.go4lunch;

import android.content.Context;
import android.content.Intent;


import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;
import com.jpz.go4lunch.activities.DetailsRestaurantActivity;
import com.jpz.go4lunch.activities.MainActivity;
import com.jpz.go4lunch.activities.SettingsActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.fakes.RoboMenuItem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class NavigationDrawerTest {

    private MainActivity mainActivity;
    private Context context = ApplicationProvider.getApplicationContext();
    //private Context context = RuntimeEnvironment.systemContext;

    @Before
    public void setUp()
    {
        FirebaseApp.initializeApp(context);

        mainActivity = Robolectric.buildActivity(MainActivity.class)
                .create()
                .start()
                .resume()
                .get();
    }

    @Test
    public void checkMainActivityNotNull(){
        assertNotNull(mainActivity);
    }

    @Test
    public void clickYourLunchInNavDrawer_shouldStartDetailsRestaurantActivity() {
        // Check if the DetailsRestaurantActivity starts, when click on MyLunch in the NavigationDrawer
        mainActivity.onNavigationItemSelected(new RoboMenuItem(R.id.nav_drawer_lunch));
        Intent expectedIntent = new Intent(mainActivity, DetailsRestaurantActivity.class);
        Intent actual = shadowOf(mainActivity).getNextStartedActivity();
        assertEquals(expectedIntent.getComponent(), actual.getComponent());
    }

    @Test
    public void clickSettingsInNavDrawer_shouldStartSettingsActivity() {
        // Check if the NotificationsActivity starts, when click on Notifications in the NavigationDrawer
        mainActivity.onNavigationItemSelected(new RoboMenuItem(R.id.nav_drawer_settings));
        Intent expectedIntent = new Intent(mainActivity, SettingsActivity.class);
        Intent actual = shadowOf(mainActivity).getNextStartedActivity();
        assertEquals(expectedIntent.getComponent(), actual.getComponent());
    }
}