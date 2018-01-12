package pt.ipleiria.notepadv2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.LocationResponse;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import pt.ipleiria.notepadv2.model.AppConstants;
import pt.ipleiria.notepadv2.model.Note;
import pt.ipleiria.notepadv2.model.Singleton;
import pt.ipleiria.notepadv2.model.commonMethods;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, AppConstants {

    private GoogleMap mMap;
    private Marker mapMarker;
    private Intent returnIntent = new Intent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toolbar maps_toolbar = findViewById(R.id.toolbar_mapsActivity);
        maps_toolbar.setNavigationIcon(R.drawable.ic_action_back);
        setSupportActionBar(maps_toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Notes Map");
    }


    //At this time the permission should already be granted
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        final LatLng[] location = new LatLng[1];

        Intent intent = getIntent();

        //FROM ASSIST DIALOG
        assert intent != null;
        if(intent.getStringExtra("from").equals("KeywordAssistDialog")){

            //set the map on the default location if no location registered on Singleton
            if(Singleton.getInstance().getLocation()==null){
                location[0] = DEFAULT_LOCATION;

                // Move the camera to current location
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                        //Position, Zoom, Tilt, Bearing
                        new CameraPosition(location[0], 1, 0, 0)));
            }else{
                location[0] = new LatLng(Singleton.getInstance().getLocation().getLatitude(), Singleton.getInstance().getLocation().getLongitude());

                // Move the camera to current location
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                        //Position, Zoom, Tilt, Bearing
                        new CameraPosition(location[0], 12, 0, 0)));
            }

            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    if (mapMarker != null) {
                        mapMarker.setPosition(latLng);
                    } else {
                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(latLng)
                                .title("Filter Location")
                                .draggable(false);

                        mapMarker = mMap.addMarker(markerOptions);
                    }

                    location[0] = latLng;

                    returnIntent.putExtra("location", location[0]);
                }
            });

        //FROM AWARENESS SETTINGS ACTIVITY
        }else if (intent.getStringExtra("from").equals("AwarenessSettings")) {

            //set the map on the default location if no location registered on Singleton
            if(Singleton.getInstance().getLocation()==null){
                location[0] = DEFAULT_LOCATION;

                // Move the camera to current location
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                        //Position, Zoom, Tilt, Bearing
                        new CameraPosition(location[0], 1, 0, 0)));
            }else{
                location[0] = new LatLng(Singleton.getInstance().getLocation().getLatitude(), Singleton.getInstance().getLocation().getLongitude());

                // Move the camera to current location
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                        //Position, Zoom, Tilt, Bearing
                        new CameraPosition(location[0], 10, 0, 0)));
            }

            //Gets the value of the search radius to draw circles on the map.
            SharedPreferences sharedPref = MapsActivity.this.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
            float locationSearchRadius = sharedPref.getFloat(PREFS_LOCATION_RADIUS_KEY, LOCATION_DEFAULT_RADIUS);

            int markerColor = 0;
            for(Note n : Singleton.getInstance().getNotepad().getNotes()){
                if(!n.getKeywordsLocation().isEmpty()){
                    //Changes the color of the markers for each note.
                    BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(markerColor);

                    for(LatLng latLng : n.getKeywordsLocation()){
                                mMap.addMarker(new MarkerOptions().position(latLng)
                                        .title(n.getTitle())
                                        .snippet(n.getId())
                                        .draggable(false)
                                        .icon(icon));

                                //TODO Adjust this colors according with the markers colors.
                                mMap.addCircle(new CircleOptions().center(latLng)
                                        .radius(locationSearchRadius)
                                        .strokeColor(R.color.LightSkyBlue)
                                        .fillColor(R.color.colorWhite));

                        }
                    markerColor+=30;
                    if(markerColor>=360)
                        markerColor = 0;
                }
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_maps, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_maps_done:
                setResult(RESULT_OK, returnIntent);
                finish();
                break;

            case android.R.id.home:
                setResult(RESULT_CANCELED, returnIntent);
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();

        try{
            unregisterReceiver(commonMethods.myFenceReceiver);
        }catch(IllegalArgumentException e){
            e.printStackTrace();
            Log.d(TAG_GENERAL, "The Broadcast Receiver "+commonMethods.myFenceReceiver+" is not unregistered.");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Register Fence Receiver
        registerReceiver(commonMethods.myFenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));
    }
}
