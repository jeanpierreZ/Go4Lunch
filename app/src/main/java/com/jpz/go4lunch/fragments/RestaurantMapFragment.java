package com.jpz.go4lunch.fragments;


import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jpz.go4lunch.R;
import com.jpz.go4lunch.activities.DetailsRestaurantActivity;
import com.jpz.go4lunch.models.FieldRestaurant;
import com.jpz.go4lunch.utils.APIClient;

import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.jpz.go4lunch.activities.MainActivity.PERMS;
import static com.jpz.go4lunch.activities.MainActivity.RC_LOCATION;


/**
 * A simple {@link Fragment} subclass.
 */
public class RestaurantMapFragment extends Fragment implements OnMapReadyCallback {

    // Google Mobile Services Objects
    private MapView mMapView;
    private GoogleMap googleMap;
    private CameraPosition cameraPosition;

    // Keys for storing activity state
    private static final String MAPVIEW_BUNDLE_KEY = "map_view_bundle_key";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_CAMERA_POSITION = "camera_position";

    private FusedLocationProviderClient fusedLocationProviderClient;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location lastKnownLocation;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 16;

    // Declare Disposable a list of fields
    private Disposable disposable;

    private String currentLatLng;

    private static final String TAG = RestaurantMapFragment.class.getSimpleName();

    public RestaurantMapFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Get layout of this fragment
        View view = inflater.inflate(R.layout.fragment_restaurant_map, container, false);

        mMapView = view.findViewById(R.id.map_view);
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Construct a FusedLocationProviderClient
        if(getActivity() != null)
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        // Declare FloatingActionButton and its behavior
        FloatingActionButton floatingActionButton = view.findViewById(R.id.fragment_restaurant_map_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the current location of the device and set the position of the map
                getDeviceLocation();
            }
        });

        return view;
    }

    //----------------------------------------------------------------------------------

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Prevent the My Location button from appearing by calling
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        // If permissions are granted, turn on My Location and the related control on the map
        updateLocationUI();

        // Hide POI of business on the map
        hideBusinessPOI();

        fetchRestaurantsOnMap();

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                startDetailsRestaurantActivity();
                // Return false to indicate that we have not consumed the event and that we wish
                // for the default behavior to occur.
                return false;
            }
        });
    }

    //----------------------------------------------------------------------------------

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        mMapView.onSaveInstanceState(mapViewBundle);

        // Saves the state of the map when the activity is paused
        if (googleMap != null) {
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
            outState.putParcelable(KEY_CAMERA_POSITION, googleMap.getCameraPosition());
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    //----------------------------------------------------------------------------------

    @AfterPermissionGranted(RC_LOCATION)
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        if (getActivity() != null)
            try {
                if (EasyPermissions.hasPermissions(getActivity(), PERMS)) {
                    Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                    locationResult.addOnCompleteListener(getActivity(), new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                // Set the map's camera position to the current location of the device.
                                lastKnownLocation = task.getResult();
                                final LatLng currentLocation =
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude());
                                // Construct a CameraPosition focusing on the current location...
                                // ...and animate the camera to that position.
                                cameraPosition = new CameraPosition.Builder()
                                        .target(currentLocation)
                                        .zoom(DEFAULT_ZOOM)
                                        .build();
                                if (googleMap != null)
                                    googleMap.animateCamera(CameraUpdateFactory
                                        .newCameraPosition(cameraPosition));
                            } else {
                                Log.i(TAG, "Current location is null. Using defaults.");
                                Log.e(TAG, "Exception: %s", task.getException());
                                if (googleMap != null)
                                    googleMap.moveCamera(CameraUpdateFactory
                                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            }
                        }
                    });
                } else {
                    if (getActivity() != null)
                        EasyPermissions.requestPermissions(getActivity(),
                                getString(R.string.permission_location_access),
                                RC_LOCATION, PERMS);
                }
            } catch (SecurityException e) {
                Log.e("Exception: %s", e.getMessage());
            }
    }

    @AfterPermissionGranted(RC_LOCATION)
    private void updateLocationUI() {
        if (googleMap == null) {
            return;
        }
        try {
            if (getActivity() != null)
                if (EasyPermissions.hasPermissions(getActivity(), PERMS)) {
                    // Go to My Location and give the related control on the map
                    googleMap.setMyLocationEnabled(true);
                    getDeviceLocation();
                } else {
                    googleMap.setMyLocationEnabled(false);
                    googleMap = null;
                    EasyPermissions.requestPermissions(getActivity(),
                                    getString(R.string.permission_location_access),
                                    RC_LOCATION, PERMS);
                }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void hideBusinessPOI() {
        if (getActivity() != null)
            try {
                // Customise the styling of the base map using a JSON object defined
                // in a raw resource file.
                boolean success = googleMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                                getActivity(), R.raw.style_json));

                if (!success) {
                    Log.e(TAG, "Style parsing failed.");
                }
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Can't find style. Error: ", e);
            }
    }

    private void updateRestaurantsUI(List<FieldRestaurant> restaurantList) {
        // Add the list from the request and update the map with the restaurants
        for (FieldRestaurant fieldRestaurant : restaurantList) {
            googleMap.addMarker(new MarkerOptions()
                    //.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_restaurant))
                    .position(fieldRestaurant.latLng));
            Log.i(TAG,"list of latLng = " + fieldRestaurant.latLng);
        }
    }

    private String convertLocation() {
        //currentLatLng = lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude();
        currentLatLng = "48.874793,2.346896";
        return currentLatLng;
    }

    // HTTP (RxJAVA)
    private void fetchRestaurantsOnMap() {
        // Execute the stream subscribing to Observable defined inside APIClient
        this.disposable = APIClient.getNearbySearchRestaurantsOnMap(convertLocation())
                .subscribeWith(new DisposableObserver<List<FieldRestaurant>>() {
                    @Override
                    public void onNext(List<FieldRestaurant> fieldRestaurantList) {
                        Log.i(TAG,"On Next NearbySearch");
                        // Update UI with the list of NearbySearch
                        updateRestaurantsUI(fieldRestaurantList);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"On Error NearbySearch" + Log.getStackTraceString(e));
                    }

                    @Override
                    public void onComplete() {
                        Log.i(TAG,"On Complete NearbySearch");
                    }
                });
    }

    private void startDetailsRestaurantActivity() {
        Intent intent = new Intent(getActivity(), DetailsRestaurantActivity.class);
        startActivity(intent);
    }

}