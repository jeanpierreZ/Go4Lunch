package com.jpz.go4lunch.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentChange;
import com.jpz.go4lunch.R;
import com.jpz.go4lunch.utils.ConvertData;
import com.jpz.go4lunch.utils.CurrentPlace;
import com.jpz.go4lunch.utils.MyUtilsNavigation;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.jpz.go4lunch.activities.MainActivity.PERMS;
import static com.jpz.go4lunch.activities.MainActivity.PLACES_ID_BUNDLE_KEY;
import static com.jpz.go4lunch.activities.MainActivity.RC_LOCATION;
import static com.jpz.go4lunch.api.WorkmateHelper.FIELD_RESTAURANT_DATE;
import static com.jpz.go4lunch.api.WorkmateHelper.FIELD_RESTAURANT_ID;
import static com.jpz.go4lunch.api.WorkmateHelper.getWorkmatesCollection;


/**
 * A simple {@link Fragment} subclass.
 */
public class RestaurantMapFragment extends Fragment implements OnMapReadyCallback,
        CurrentPlace.CurrentPlacesListener, CurrentPlace.PlaceDetailsListener {

    // Google Mobile Services Objects
    private MapView mMapView;
    private GoogleMap googleMap;
    private CameraPosition cameraPosition;

    // Keys for storing activity state
    private static final String MAPVIEW_BUNDLE_KEY = "map_view_bundle_key";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_CAMERA_POSITION = "camera_position";

    // Places
    private FusedLocationProviderClient fusedLocationProviderClient;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location lastKnownLocation;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 17;

    // Utils
    private MyUtilsNavigation utilsNavigation = new MyUtilsNavigation();
    private ConvertData convertData = new ConvertData();

    // For DeviceLocationListener Interface
    private DeviceLocationListener deviceLocationListener;

    // List of placesId from the autocomplete request
    private ArrayList<String> placesId;

    // This flag is required to avoid first time onResume refreshing
    private boolean loaded = false;

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

        if (getActivity() != null) {
            // Construct a FusedLocationProviderClient
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
        }

        // Declare FloatingActionButton and its behavior
        FloatingActionButton floatingActionButton = view.findViewById(R.id.fragment_restaurant_map_fab);
        floatingActionButton.setOnClickListener((View v) -> {
            // Get the current location of the device and set the position of the map
            getDeviceLocation();
        });

        return view;
    }

    //----------------------------------------------------------------------------------

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        // Prevent to show the My Location button, the MapToolbar and the Compass
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);

        // If permissions are granted, turn on My Location and the related control on the map
        updateLocationUI();
        // Hide POI of business on the map
        hideBusinessPOI();

        // Get placesId from autocomplete
        if (getArguments() != null) {
            placesId = getArguments().getStringArrayList(PLACES_ID_BUNDLE_KEY);
        }
        Log.i(TAG, "placesId = " + placesId);

        // If there is a request from autocomplete, fetch a placeDetails
        if (placesId != null) {
            if (placesId.isEmpty()) {
                Toast.makeText(getActivity(), getString(R.string.no_result), Toast.LENGTH_SHORT).show();
                // Remove the key from the bundle
                getArguments().remove(PLACES_ID_BUNDLE_KEY);
            } else {
                // Add the PlaceDetailsListener in the list of listeners from CurrentPlace Singleton...
                CurrentPlace.getInstance(getActivity()).addDetailsListener(this);
                // For each placeId from autocomplete
                for (String placeId : placesId) {
                    // request a detailsPlace
                    CurrentPlace.getInstance(getActivity()).findDetailsPlaces(placeId);
                }
            }
            // Else fetch findCurrentPlace then findDetailsPlace
        } else {
            // Add the CurrentPlacesListener in the list of listeners from CurrentPlace Singleton...
            CurrentPlace.getInstance(getActivity()).addListener(this);
            // ...to allow fetching places in the method below :
            CurrentPlace.getInstance(getActivity()).findCurrentPlace();
        }

        if (googleMap != null) {
            googleMap.setOnMarkerClickListener((Marker marker) -> {
                // Retrieve the data from the marker.
                String restaurantId = (String) marker.getTag();
                if (restaurantId != null) {
                    // Start DetailsRestaurantActivity when the user click on a restaurant
                    utilsNavigation.startDetailsRestaurantActivity(getActivity(), restaurantId);
                }
                // Return false to indicate that we have not consumed the event and that we wish
                // for the default behavior to occur.
                return false;
            });
        }
    }

    //----------------------------------------------------------------------------------
    // LifeCycle of Map

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
        if (!loaded) {
            //First time just set the loaded flag true
            loaded = true;
        } else {
            Log.i(TAG, "Back to MainActivity");
            //Reload data
            onMapReady(googleMap);
        }
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
        CurrentPlace.getInstance(getActivity()).removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    //----------------------------------------------------------------------------------
    // Methods for location and configure Map

    @AfterPermissionGranted(RC_LOCATION)
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (getActivity() != null) {
                if (EasyPermissions.hasPermissions(getActivity(), PERMS)) {
                    Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                    locationResult.addOnCompleteListener(getActivity(), (@NonNull Task<Location> task) -> {
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
                            if (googleMap != null) {
                                googleMap.animateCamera(CameraUpdateFactory
                                        .newCameraPosition(cameraPosition));
                            }

                            // Link the device location in the interface
                            deviceLocationListener.onDeviceLocationFetch(currentLocation);

                        } else {
                            Log.i(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            if (googleMap != null)
                                googleMap.moveCamera(CameraUpdateFactory
                                        .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                        }
                    });
                } else {
                    if (getActivity() != null) {
                        EasyPermissions.requestPermissions(getActivity(),
                                getString(R.string.rationale_permission_location_access),
                                RC_LOCATION, PERMS);
                    }
                }
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
            if (getActivity() != null) {
                if (EasyPermissions.hasPermissions(getActivity(), PERMS)) {
                    // Go to My Location and give the related control on the map
                    googleMap.setMyLocationEnabled(true);
                    getDeviceLocation();
                } else {
                    googleMap.setMyLocationEnabled(false);
                    googleMap = null;
                    EasyPermissions.requestPermissions(getActivity(),
                            getString(R.string.rationale_permission_location_access),
                            RC_LOCATION, PERMS);
                }
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void hideBusinessPOI() {
        try {
            if (getActivity() != null) {
                if (googleMap != null) {
                    // Customise the styling of the base map using a JSON object defined
                    // in a raw resource file.
                    boolean success = googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                    getActivity(), R.raw.style_json));
                    if (!success) {
                        Log.e(TAG, "Style parsing failed.");
                    }
                }
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
    }

    //----------------------------------------------------------------------------------
    // Methods to build and show markers on Map

    // Display markers at current places
    @AfterPermissionGranted(RC_LOCATION)
    private void showCurrentPlaces(Place place) {
        if (getActivity() != null) {
            if (EasyPermissions.hasPermissions(getActivity(), PERMS)) {
                // By default, mark a red icon
                addMarkers(place.getLatLng(), place.getId(),
                        R.drawable.ic_map_pin, R.drawable.ic_restaurant);

                // Check if workmates choose a restaurant on the map in Firestore (real-time) :
                getWorkmatesCollection()
                        .whereEqualTo(FIELD_RESTAURANT_ID, place.getId())
                        .whereEqualTo(FIELD_RESTAURANT_DATE, convertData.getTodayDate())
                        // By passing in the activity,
                        // Firestore can clean up the listeners automatically when the activity is stopped.
                        .addSnapshotListener(getActivity(), (snapshots, e) -> {
                            if (e != null) {
                                Log.w(TAG, "listen:error", e);
                                return;
                            }

                            if (snapshots != null) {
                                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                    if (dc.getType() == DocumentChange.Type.ADDED ||
                                            dc.getType() == DocumentChange.Type.MODIFIED) {
                                        // If a workmate join a restaurant on the map, mark a green icon
                                        addMarkers(place.getLatLng(), place.getId(),
                                                R.drawable.ic_map_pin_workmate,
                                                R.drawable.ic_restaurant_with_workmate);
                                    }
                                    if (dc.getType() == DocumentChange.Type.REMOVED) {
                                        // If a workmate delete a restaurant choice on the map, mark a red icon
                                        addMarkers(place.getLatLng(), place.getId(),
                                                R.drawable.ic_map_pin, R.drawable.ic_restaurant);
                                    }
                                }
                            }
                        });

            } else {
                EasyPermissions.requestPermissions(getActivity(),
                        getString(R.string.rationale_permission_location_access), RC_LOCATION, PERMS);
            }
        }
    }

    // Add different color markers depending on whether there are workmates in a restaurant or not
    private void addMarkers(LatLng latLng, String restaurantId, int icMap, int icRestaurant) {
        Marker marker;
        if (getActivity() != null) {
            marker = googleMap.addMarker(new MarkerOptions()
                    .icon(bitmapDescriptorFromVector(icMap, icRestaurant))
                    .position(latLng));
            marker.setTag(restaurantId);
        }
    }

    private BitmapDescriptor bitmapDescriptorFromVector(int backgroundResId, int vectorResId) {
        // Create background
        Drawable background = VectorDrawableCompat.create(getResources(), backgroundResId, null);
        if (background == null) {
            Log.e(TAG, "Requested vector resource was not found");
            return BitmapDescriptorFactory.defaultMarker();
        }
        background.setBounds(0, 0,
                background.getIntrinsicWidth(), background.getIntrinsicHeight());
        // Create vector
        Drawable vectorDrawable = VectorDrawableCompat.create(getResources(), vectorResId, null);
        if (vectorDrawable == null) {
            Log.e(TAG, "Requested vector resource was not found");
            return BitmapDescriptorFactory.defaultMarker();
        }
        int left = (background.getIntrinsicWidth() - vectorDrawable.getIntrinsicWidth()) / 2;
        int top = (background.getIntrinsicHeight() - vectorDrawable.getIntrinsicHeight()) / 3;
        vectorDrawable.setBounds(left, top, left + vectorDrawable.getIntrinsicWidth(),
                top + vectorDrawable.getIntrinsicHeight());
        // Create Bitmap with background and vector then draw them
        Bitmap bitmap = Bitmap.createBitmap(background.getIntrinsicWidth(),
                background.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        background.draw(canvas);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    //----------------------------------------------------------------------------------

    @Override
    public void onPlacesFetch(List<Place> places) {
        // Show the restaurants near the user location with the places from the request
        for (Place place : places) {
            showCurrentPlaces(place);
        }
    }

    @Override
    public void onPlaceDetailsFetch(Place place) {
        // Show the restaurant from the placeDetails request use after an autocomplete request
        showCurrentPlaces(place);
        // Remove the key from the bundle
        if (getArguments() != null) {
            getArguments().remove(PLACES_ID_BUNDLE_KEY);
        }
    }

    //----------------------------------------------------------------------------------

    // Interface to retrieve the device location when the task is complete.
    public interface DeviceLocationListener {
        void onDeviceLocationFetch(LatLng latLng);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Call the method that creating callback after being attached to parent activity
        this.createCallbackToParentActivity();
    }

    // Create callback to parent activity
    private void createCallbackToParentActivity() {
        try {
            //Parent activity will automatically subscribe to callback
            deviceLocationListener = (DeviceLocationListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(e.toString() + " must implement onDeviceLocationFetch");
        }
    }
}
