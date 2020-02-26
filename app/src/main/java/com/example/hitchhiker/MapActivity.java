package com.example.hitchhiker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener, GoogleMap.OnMarkerClickListener {

    GoogleMap mGoogleMap;   // Our Google Map, later used to modify the Map
    FusedLocationProviderClient fusedLocationClient;
    String profileType = "Driver";
    GoogleApiClient mGoogleApiClient;
    public static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    DatabaseReference database =
            FirebaseDatabase.getInstance().getReference();
    DatabaseReference ref = database.child("/Markers");     // Reference on the database child "Markers" where our Markers are stored
    DatabaseReference msg = database.child("/Messages");    // Reference on the database child "Messages" where our Messages "who picks up who" are stroed
    DatabaseReference dest = database.child("/Destination");
    String userMail = MainActivity.firebaseAuth.getCurrentUser().getEmail();
    String[] email = userMail.split("[@._]");
    char firstLetter = email[0].charAt(0);
    String firstLetterUp = String.valueOf(firstLetter).toUpperCase();
    String emailName =  firstLetterUp + email[0].substring(1);
    String getMsg = emailName + "";
    String uid = "/" + MainActivity.firebaseAuth.getUid() + "_" + emailName;
    final String userid = MainActivity.firebaseAuth.getUid() + "_" + emailName; // These lines are for getting the driver Name and for visualizing it correctly in the messages.
    Marker ownMarker;
    Marker searchMarker; // Destination Marker
    Marker del; // Delete Marker for deleting own location Marker
    EditText searchBar; // Used for searching for the destination
    TextView tvProgressLabel;
    List<Marker> mMarkers = new ArrayList<Marker>();    // Stores the markers from other Hikers on the map, reloaded everytime the App restarts.
    Integer distance = 40; // Max distance in km for a Hiker to be shown on the Map of a Driver.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (googleServiceAvailable()) {
            //Toast.makeText(this, "Perfect!", Toast.LENGTH_LONG).show();
            setContentView(R.layout.activity_map);
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            initMap();          //initializes the Google Map fragment with its settings
            reqPermissions();   //initializes the permission request
            initBtns();         //initializes different buttons
            initLocation();     //sets/updates/deletes the markers of other hikers within a given radius
            initIfOwnLoc();     //resets the own location if it was stored in firebase
            initDestination();  //resets zhe destination if it was stored in firebase
            initSeekBar();      //to change the radius of the Markers available
            initSearchbar();    //to search for destinations
        } else {
            // No Google Maps Layout
        }
    }

    public void onResume(){
        super.onResume();
        initIfOwnLoc();
        initGettingLocations();
        initDestination();
    }

    public void initSeekBar(){
        SeekBar seekBar = findViewById(R.id.seekBar);
        int progress = seekBar.getProgress();
        tvProgressLabel = findViewById(R.id.textView);
        tvProgressLabel.setText("Distance: " + progress + "km");
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // updated continuously as the user slides the thumb
                tvProgressLabel.setText("Distance: " + progress + "km");
                int zoom = 17;
                if(progress <=15){
                    zoom = 15;
                } else if(progress > 15 && progress <= 30){
                    zoom = 13;
                } else if(progress > 30 && progress <= 70){
                    zoom = 11;
                } else if(progress > 70 && progress <= 110){
                    zoom = 10;
                } else if(progress > 110 && progress <= 160){
                    zoom = 9;
                } else if(progress > 160 && progress <= 200){
                    zoom = 8;
                }
                CameraUpdate update = CameraUpdateFactory.zoomTo(zoom);
                mGoogleMap.moveCamera(update);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // called when the user first touches the SeekBar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // called after the user finishes moving the SeekBar
                distance = seekBar.getProgress();
                for(Marker marker: mMarkers){
                    marker.remove();
                }
                mMarkers.clear();
                //Reloads all Markers from firebase which are located within the new distance
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot child: dataSnapshot.getChildren()){
                            if(!child.getKey().equals(userid)){
                                Double lat = (Double) child.child("/latitude").getValue();
                                Double lng = (Double) child.child("/longitude").getValue();
                                String key = child.getKey();
                                calcDistance(lat, lng, key);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
    }

    // Checks if the User had set a destination before.
    public void initDestination(){
        dest.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot child: dataSnapshot.getChildren()){
                    if(child.getKey().equals(userid)){
                        String destination = child.getValue().toString();
                        Geocoder geocoder = new Geocoder(MapActivity.this);
                        List<Address> list = new ArrayList<>();
                        try{
                            list = geocoder.getFromLocationName(destination, 1);
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                        if(list.size() > 0){
                            //Toast.makeText(this,list.get(0).toString(), Toast.LENGTH_LONG).show();
                            Address address = list.get(0);
                            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                            //CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, 16);
                            //mGoogleMap.animateCamera(update);
                            if(searchMarker != null){
                                searchMarker.remove();
                                //dest.child(uid).removeValue();
                            }
                            searchMarker = mGoogleMap.addMarker(new MarkerOptions().position(latLng).title(getString(R.string.deinZiel) + address.getAddressLine(0)).icon(BitmapDescriptorFactory.fromResource(R.drawable.finishmarker)));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // Checks if own Location was set (From firebase) and resets its, and according to that sets the profile type
    public void initIfOwnLoc(){
        final Button carPerson = findViewById(R.id.profileTypeBtn);
        final Button markerBtn = findViewById(R.id.markerBtn);
        final ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) carPerson.getLayoutParams();
        // Request to firebase if a Marker has been set with key == userid
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(ownMarker == null) { // Resets the own Marker if it was removed but its still available in the Database
                    for (DataSnapshot childSnap : dataSnapshot.getChildren()) {
                        if (childSnap.getKey().equals(userid)) {
                            Double lat = (Double) childSnap.child("/latitude").getValue();
                            Double lng = (Double) childSnap.child("/longitude").getValue();
                            LatLng latlng = new LatLng(lat, lng);
                            ownMarker = mGoogleMap.addMarker(new MarkerOptions().position(latlng)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.locmarker)));
                            setHiker(markerBtn, carPerson, params); //If the ownMarker is available in the database, setHiker sets the profiletype to hiker + sets the buttons
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //Initializes the mapFragment
    private void initMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);

        mapFragment.getMapAsync(this); //Sets a callback object which will be triggered when the GoogleMap instance is ready to be used.
    }

    // Initializes the main Buttons for later use
    public void initBtns() {
        // Sets the Map to the current location
        Button loc = findViewById(R.id.locBtn);
        loc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getCurrentLocation();
            }
        });
        // The finish flag, which activates the search layout to set a destination
        Button finish = findViewById(R.id.finishBtn);
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RelativeLayout rel = findViewById(R.id.relLayout);
                closeView(rel);
            }
        });
        // The Mail Object, where the Hiker can check which Driver will pick him up
        Button mailBtn = findViewById(R.id.mailBtn);
        mailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RelativeLayout rel = findViewById(R.id.msg_layout);
                closeView(rel);
            }
        });

        // Offers the driver an option to change the radius within the hikers are shown
        Button radarBtn = findViewById(R.id.radarBtn);
        if(profileType == "Driver") radarBtn.setVisibility(View.VISIBLE);
        radarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//Activates the seekbar to change the radius
                LinearLayout lay = findViewById(R.id.seekBarLayout);
                if(lay.getVisibility() == View.INVISIBLE){
                    lay.setVisibility(View.VISIBLE);
                } else {
                    lay.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    // Initializes the Searchbar to set the destination
    private void initSearchbar() {
        searchBar = findViewById(R.id.search_bar);

        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || event.getAction() == KeyEvent.ACTION_DOWN
                        || event.getAction() == KeyEvent.KEYCODE_ENTER) {

                    //execute our method for searching
                    onSearch(findViewById(R.id.search));
                    return true;

                }
                return false;
            }
        });
    }

// Sets the own Location, which is stored in the firebase db at the Marker reference "ref".
public void shareLocation(View view){
    if(searchMarker != null) {
        fusedLocationClient.getLastLocation()   // Gets the current Location
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        final Location loc = location;
                        LatLng curr = new LatLng(location.getLatitude(), location.getLongitude());
                        // Starts an alert Dialog if a Location is already set, prevents the user from spamming the firebase database
                        if (ownMarker != null) {
                            AlertDialog alertDialog = new AlertDialog.Builder(MapActivity.this).create();
                            alertDialog.setTitle(getString(R.string.titleStandortAktual));
                            alertDialog.setMessage(getString(R.string.standortAktual));
                            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            ownMarker.remove();
                                            LatLng curr = new LatLng(loc.getLatitude(), loc.getLongitude());    //Creates a LatLng Object with the current Latitude and Longitude
                                            ownMarker = mGoogleMap.addMarker(new MarkerOptions().position(curr) //Creates a Marker on the Map with the current Location gatherd above
                                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.locmarker)));
                                            ref.child(uid).setValue(curr); // Overrides (if exists) the location stored in the child thats named after the uid and the username
                                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(curr)); //Updates the current view on the map to the current location where the Marker is set
                                        }
                                    });
                            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();


                                        }
                                    });
                            alertDialog.show();
                        } else {
                            // If no Location was set, a new Location is saved in the Firebase db
                            ownMarker = mGoogleMap.addMarker(new MarkerOptions().position(curr)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.locmarker)));
                            ref.child(uid).setValue(curr);
                            //ref.push().getKey();
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(curr));
                        }

                    }
                });
    } else {
        // Alert dialog to prevent the user from sharing his location without determining a destination
        AlertDialog alertDialog = new AlertDialog.Builder(MapActivity.this).create();
        alertDialog.setTitle(getString(R.string.titleFehlendesZiel));
        alertDialog.setMessage(getString(R.string.fehlendesZiel));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        RelativeLayout search = findViewById(R.id.relLayout); // Opens the search layout for the user
                        search.setVisibility(View.VISIBLE);
                    }
                });
        alertDialog.show();
    }
}

    // Deletes the own Location, starts an alert button to confirm the delete
    public void delLocation(){
        String message = getString(R.string.standortLoeschen);
        if(messageActive()){
            message = message + getString(R.string.wirstAbgeholt);
        }
        AlertDialog alertDialog = new AlertDialog.Builder(MapActivity.this).create();
        alertDialog.setTitle(getString(R.string.titleStandortLoeschen));
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ref.child(uid).removeValue();
                        msg.child(uid).removeValue(); // If a message is stored in the message reference that references the hiker uid, the Driver who was supposed to pick up the hiker will be informed about the removed Location
                        if(ownMarker != null){ // Preventing NullPointerException
                            ownMarker.remove();
                            ownMarker = null;
                        }
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    // Method for reuse purpose, sets a Marker at a given location and stores the uid of the hiker (who has pushed this location) as a key in the snippet of the Marker. The key is to assign the Markers to the respective Hikers
    public void setMarker(Double lat, Double lng, String key){
        LatLng latLng = new LatLng(lat, lng);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.snippet(key); // Contains the user id of the hiker located at this position (To identify the markers)
        Marker marker = mGoogleMap.addMarker(markerOptions);
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.locmarkeryellow));
        mMarkers.add(marker); // Stores every Marker in a list object
        initGettingLocations(); // Updates the Markers of the Hikers which the driver gas marked to pick up, they will appear green instead of yellow.
    }

    // Checks in realtime if a Hiker has pushed/updated/deleted his location to the firebase DB, if so the Marker will appear/update/disappear on the map for every user within a given distance (Currently 40km)
    public void initLocation(){

        ref.addChildEventListener(new ChildEventListener() {
            @Override
            //Fires when a new Marker has been stored
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if(!dataSnapshot.getKey().equals(userid)){ //If its not the users marker
                    Double lat = (Double) dataSnapshot.child("/latitude").getValue();
                    Double lng = (Double) dataSnapshot.child("/longitude").getValue();
                    String key = dataSnapshot.getKey(); //The uid of the Markers hiker
                    calcDistance(lat, lng, key);    //Only sets the Marker if the Marker is within the given distance (default 40km)
                }

            }

            // Fires when a Marker has been updated by the user
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if(!dataSnapshot.getKey().equals(userid)){ //If its not the users marker
                    String key = dataSnapshot.getKey();
                    for (int i = 0; i < mMarkers.size(); i++) { // searchs the Marker in the mMarkers list and deletes it if the snippet contains the key of the changed Marker
                        if (mMarkers.get(i).getSnippet().contains(key)) {
                            mMarkers.get(i).remove();
                        }
                    }
                    //Then resets it at the new position
                    Double lat = (Double) dataSnapshot.child("/latitude").getValue();
                    Double lng = (Double) dataSnapshot.child("/longitude").getValue();
                    calcDistance(lat, lng, key);
                }
            }

            @Override
            //Fires when a Marker has been deleted
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.getKey().equals(userid)){ //If its not the users marker
                    String key = dataSnapshot.getKey();
                    for (int i = 0; i < mMarkers.size(); i++) { // Deletes the Marker if the snippet contains the uid of the deleted Marker
                        if (mMarkers.get(i).getSnippet().contains(key)) {
                            mMarkers.get(i).remove();
                        }
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }

        });

        // Checks in realtime if a driver has sent or deleted a pick up message to a hiker
        msg.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if(dataSnapshot.getKey().equals(userid)){
                    Button mailButton = findViewById(R.id.mailBtn);
                    mailButton.setVisibility(View.VISIBLE);
                    TextView msgText = findViewById(R.id.msg_window);
                        String msg = dataSnapshot.child("/message").getValue().toString();
                        msgText.setText(msg);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            //If a message is removed it will be checked if the message was removed by a driver or by a hiker
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getKey().equals(userid)){
                    Button mailButton = findViewById(R.id.mailBtn);
                    mailButton.setVisibility(View.INVISIBLE);
                    RelativeLayout rel = findViewById(R.id.msg_layout);
                    rel.setVisibility(View.INVISIBLE);
                } else if(dataSnapshot.child("/driverID").getValue().equals(userid)){ // If the message was removed by a hiker, it is checked if the hiker deleted its location.
                    // If so the driver will be informed with an Alert Message
                    final String keyPath = "/" + dataSnapshot.getKey(); //To lookup if the Hiker uid equals an Hiker uid of the Marker reference
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(!dataSnapshot.hasChild(keyPath)){
                                initAlertBtn();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    //Initializes the Alert Button, it will appear to the driver if a hiker deletes his location while having an pickup message active
    public void initAlertBtn(){
        final Button alertBtn = findViewById(R.id.alertBtn);
        alertBtn.setVisibility(View.VISIBLE);
        alertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alertDialog = new AlertDialog.Builder(MapActivity.this).create();
                alertDialog.setTitle(getString(R.string.titleHikerGone));
                alertDialog.setMessage(getString(R.string.hikerGone));
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                alertBtn.setVisibility(View.INVISIBLE); //dismisses the alertBtn after the driver has seen it

                            }
                        });
                alertDialog.show();
            }
        });
    }

    public void closeView(RelativeLayout rel){
        if(rel.getVisibility() == View.VISIBLE){
            rel.setVisibility(View.INVISIBLE);
        } else {
            rel.setVisibility(View.VISIBLE);
        }
    }

    public void onSearch (View view){
        RelativeLayout search = findViewById(R.id.relLayout);
        searchBar = findViewById(R.id.search_bar);
        String destination = searchBar.getText().toString();
        Geocoder geocoder = new Geocoder(this);
        List<Address> list = new ArrayList<>();
        try{
            list = geocoder.getFromLocationName(destination, 1);
        }catch (IOException e){
            e.printStackTrace();
        }
        if(list.size() > 0){
            //Toast.makeText(this,list.get(0).toString(), Toast.LENGTH_LONG).show();
            Address address = list.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, 16);
            mGoogleMap.animateCamera(update);
            if(searchMarker != null){
                searchMarker.remove();
                //dest.child(uid).removeValue();
            }
            searchMarker = mGoogleMap.addMarker(new MarkerOptions().position(latLng).title(getString(R.string.deinZiel) + address.getAddressLine(0)).icon(BitmapDescriptorFactory.fromResource(R.drawable.finishmarker)));
            dest.child(uid).setValue(address.getAddressLine(0));
            MainActivity.hideKeyboard(this);
            search.setVisibility(View.INVISIBLE);
        }
    }

    //Checks if the google Play services are available
    public boolean googleServiceAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (api.isUserResolvableError(isAvailable)) {
            Dialog dialog = api.getErrorDialog(this, isAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(this, "Cant connect to play services", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    // Initializes the Google Map and adjusts some parameters
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mGoogleMap.setTrafficEnabled(true);
        //Toast.makeText(this, "On Map ready!", Toast.LENGTH_LONG).show();
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setOnMyLocationClickListener(new GoogleMap.OnMyLocationClickListener() {
            @Override
            public void onMyLocationClick(@NonNull Location location) {
                Toast.makeText(MapActivity.this, getString(R.string.dasBistDu), Toast.LENGTH_SHORT).show();
            }
        });
        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        mGoogleMap.setOnMyLocationButtonClickListener(this);
        mGoogleMap.getUiSettings().setCompassEnabled(true);
        mGoogleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if(Math.abs(searchMarker.getPosition().latitude - latLng.latitude) < 0.03 && Math.abs(searchMarker.getPosition().longitude - latLng.longitude) < 0.03) {
                    AlertDialog alertDialog = new AlertDialog.Builder(MapActivity.this).create();
                    alertDialog.setTitle(getString(R.string.titelZielLoesch));
                    alertDialog.setMessage(getString(R.string.stringZielLoesch));
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    dest.child(userid).removeValue();
                                    searchMarker.remove();
                                }
                            });
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();


                                }
                            });
                    alertDialog.show();
                }
            }
        });
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    private void goToLocationZoom(double lat, double lng, int zoom) {
        LatLng ll = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);
        mGoogleMap.moveCamera(update);
    }

    public void changeProfile(View view) {

        final Button carPerson = findViewById(R.id.profileTypeBtn);
        final Button markerBtn = findViewById(R.id.markerBtn);
        final ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) carPerson.getLayoutParams();
        if (profileType == "Driver") {
            msg.addListenerForSingleValueEvent(new ValueEventListener() {
                boolean alert = false;
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot child: dataSnapshot.getChildren()){
                            if(child.child("/driverID").getValue().equals(userid)) alert = true;
                    }
                    if(alert){
                        profileAlert();
                    } else {
                        setHiker(markerBtn, carPerson, params);
                        setButtonVisibility(false);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        } else if (profileType == "Hitchhiker") {
            if(ownMarker != null) {
                AlertDialog alertDialog = new AlertDialog.Builder(MapActivity.this).create();
                alertDialog.setTitle(getString(R.string.achtung));
                alertDialog.setMessage(getString(R.string.modusWechselnFahrer));
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            } else {
                setDriver(markerBtn, carPerson, params);
            }
        } else {
            // Nothing
        }
    }

    public void setHiker(Button markerButton, Button profileButton, ConstraintLayout.LayoutParams layout){
        profileButton.setBackground(getResources().getDrawable(R.drawable.tourist));
        layout.height = 155;
        layout.width = 155;
        layout.topMargin = 50;
        layout.rightMargin = 60;
        markerButton.setVisibility(View.VISIBLE);
        profileType = "Hitchhiker";
        Button radar = findViewById(R.id.radarBtn);
        LinearLayout lay = findViewById(R.id.seekBarLayout);
        radar.setVisibility(View.INVISIBLE);
        lay.setVisibility(View.INVISIBLE);
        distance = 40;
        for(Marker marker: mMarkers){
            marker.remove();
        }
        mMarkers.clear();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot child: dataSnapshot.getChildren()){
                    if(!child.getKey().equals(userid)){
                        Double lat = (Double) child.child("/latitude").getValue();
                        Double lng = (Double) child.child("/longitude").getValue();
                        String key = child.getKey();
                        calcDistance(lat, lng, key);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void setDriver(Button markerButton, Button profileButton, ConstraintLayout.LayoutParams layout){
        profileButton.setBackground(getResources().getDrawable(R.drawable.car_blue));
        layout.height = 250;
        layout.width = 250;
        layout.topMargin = 10;
        layout.rightMargin = 30;
        markerButton.setVisibility(View.INVISIBLE);
        profileType = "Driver";
        Button radar = findViewById(R.id.radarBtn);
        radar.setVisibility(View.VISIBLE);
    }

    public void setButtonVisibility(Boolean bool){
        Button get = findViewById(R.id.getHimBtn);
        Button leave = findViewById(R.id.leaveHimBtn);
        LinearLayout text = findViewById(R.id.mitnehmenLay);
        if(bool){
            get.setVisibility(View.VISIBLE);
            leave.setVisibility(View.VISIBLE);
            text.setVisibility(View.VISIBLE);
        } else{
            get.setVisibility(View.INVISIBLE);
            leave.setVisibility(View.INVISIBLE);
            text.setVisibility(View.INVISIBLE);
        }
    }

    LocationRequest mLocationRequest;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);
        getCurrentLocation();
        //LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    public void getCurrentLocation() {
        LocationServices.getFusedLocationProviderClient(this).getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, 15);
                            mGoogleMap.animateCamera(update, 400, null);
                        }
                    }
                });
    }

    public void calcDistance(final double lat, final double lng, final String key){
        LocationServices.getFusedLocationProviderClient(this).getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            Location myLocation = new Location("My Loc");
                            myLocation.setLatitude(location.getLatitude());
                            myLocation.setLongitude(location.getLongitude());
                            Location hikerLocation = new Location("Hiker Loc");
                            hikerLocation.setLatitude(lat);
                            hikerLocation.setLongitude(lng);
                            if(myLocation.distanceTo(hikerLocation) < (distance * 1000)) {
                                setMarker(lat, lng, key);
                            }
                        }
                    }
                });
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            Toast.makeText(this, "Cant get current location", Toast.LENGTH_LONG).show();
        } else {
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, 16);
            mGoogleMap.animateCamera(update);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLoc Btn clicked", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
    //    Toast.makeText(this, "Current Loc:\n" + location, Toast.LENGTH_LONG).show();
    }

    public void reqPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(this, "Permission already granted.", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                setContentView(R.layout.activity_main);
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish();
                }
                return;
            }
        }
    }

    public void logout(){
        MainActivity.firebaseAuth.signOut();
        startActivity(new Intent(MapActivity.this, MainActivity.class));
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { switch(item.getItemId()) {
        case R.id.logout:
            logout();
            return(true);
        case R.id.home:
            finish();
            return(true);
    }
        return(super.onOptionsItemSelected(item));
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public boolean onMarkerClick(final Marker mymarker) {
        if (mymarker.equals(ownMarker)) {
            if (del != null) {
                removeDel();
            } else {
                setDel();
            }
            return true;
        } else if (mymarker.equals(del)) {
            delLocation();
            removeDel();
        } else if(mymarker.equals(searchMarker)){

        } else {
            if(profileType != "Hitchhiker") {
                final Button getHim = findViewById(R.id.getHimBtn);
                final Button leaveHim = findViewById(R.id.leaveHimBtn);
                final String snipID = "/" + mymarker.getSnippet();
                final String[] hikerSnip = mymarker.getSnippet().split("_");
                final String hikerName = hikerSnip[1];
                final String[] hikerDest = new String[1];
                dest.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot child: dataSnapshot.getChildren()){
                            if(child.getKey().equals(mymarker.getSnippet())){
                                 hikerDest[0] = child.getValue().toString();
                            }
                        }
                        showHikerInfo(hikerName, hikerDest[0]);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                if(getHim.getVisibility() == View.VISIBLE) {
                    setButtonVisibility(false);
                } else {
                    setButtonVisibility(true);
                    getHim.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.child(snipID).exists()) {
                                            writeNewMsg(userid, getMsg, snipID);
                                            mymarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.locmarkergreen));
                                            Toast.makeText(MapActivity.this, getString(R.string.messageTo) + hikerName + getString(R.string.sent), Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MapActivity.this, getString(R.string.messageFail), Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                                    setButtonVisibility(false);
                            } catch(Exception e) {
                                setButtonVisibility(false);
                            }
                        }
                    });
                }
                leaveHim.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            msg.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                                        if (dataSnapshot.hasChild(snipID)) {
                                            if (dataSnapshot.child(snipID).child("/driverID").getValue().toString().equals(userid)) {
                                                msg.child(snipID).removeValue();
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                            setButtonVisibility(false);
                            mymarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.locmarkeryellow));
                            Toast toast = Toast.makeText(MapActivity.this, getString(R.string.schade), Toast.LENGTH_SHORT);
                            TextView vv = (TextView) toast.getView().findViewById(android.R.id.message);
                            if (vv != null) vv.setGravity(Gravity.CENTER);
                            toast.show();
                        } catch(Exception e) {
                            setButtonVisibility(false);
                        }
                    }
                });
            }
        }
        //return true;
        return false;
    }

    public void showHikerInfo(String name, String dest){
        TextView hikerText = findViewById(R.id.mitnehmenTV);
        TextView uName = findViewById(R.id.userName);
        TextView uDest = findViewById(R.id.destination);
        String message = getString(R.string.willstDu) + name + getString(R.string.abholen);
        uName.setText(getString(R.string.sName) + name);
        uDest.setText(getString(R.string.sDest) + dest);
        hikerText.setText(message);
    }

    public void setDel(){
        LatLng pos = ownMarker.getPosition();
        del = mGoogleMap.addMarker(new MarkerOptions().position(pos)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.cancelbig)));
    }

    public void removeDel(){
        if(del != null){
            del.remove();
            del = null;
        }
    }

    public void initMsgPush(){
        RelativeLayout rel = findViewById(R.id.msg_layout);
        if(profileType == "hitchhiker") rel.setVisibility(View.VISIBLE);
    }

    public class Message {
        public String driverID;
        public String message;
        public Message() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public Message(String uid, String msg) {
            driverID = uid;
            message = msg;
        }

    }

    private void writeNewMsg(String userS, String messageS, String snipS) {
        Message msgObj = new Message(userS, messageS);
        msg.child(snipS).setValue(msgObj);
    }

    public boolean messageActive(){
        Button msgBtn = findViewById(R.id.mailBtn);
        if(msgBtn.getVisibility() == View.VISIBLE){
            return true;
        }
        return false;
    }

    public void profileAlert(){
        AlertDialog alertDialog = new AlertDialog.Builder(MapActivity.this).create();
        alertDialog.setTitle(getString(R.string.achtung));
        alertDialog.setMessage(getString(R.string.modusWechselnHiker));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public void initGettingLocations() {
            msg.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                            for (Marker marker : mMarkers) {
                                if (marker.getSnippet().contains(child.getKey()) && child.child("/driverID").getValue().equals(userid)) {
                                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.locmarkergreen));
                                }
                            }
                        }
                    }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

    }
}