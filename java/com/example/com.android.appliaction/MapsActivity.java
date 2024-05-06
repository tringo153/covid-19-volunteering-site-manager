package com.example.s3818520_assignment2;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
//import android.location.LocationRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.s3818520_assignment2.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private DatabaseManager dbManager;

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final long UPDATE_INTERVAL = 10*1000 ;
    private static final long FASTEST_INTERVAL = 5000 ;
    public static final String CHANNEL_1_ID = "channel1";

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    private Map<Marker, Map<String, Object>> markers = new HashMap<>();
    private ArrayList<String> joinedSites = new ArrayList<>();
    private HashMap<String ,Marker> hiddenMarker = new HashMap<>();

    private String currentUserId;
    private String currentName;
    private LatLng currentLocation;
    private Marker currentMarker;
    private String isSuper;

    private String jsonString = "";
    private Polyline route;

    private Marker updatedMarker;
    private SearchView searchView;
    private ListView searchResult;
    private String searchFilter;

    private String[] from = new String[] {
            DatabaseHelper.SITE_ID,
            DatabaseHelper.TITLE,
            DatabaseHelper.NAME
    };
    private int[] to = new int[]{
            R.id.lResultId,
            R.id.lResultTitle,
            R.id.lResultLeader
    };


    protected FusedLocationProviderClient client;
    protected LocationRequest mLocationRequest;
    private NotificationManager notificationManager;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbManager = new DatabaseManager(this);
        dbManager.open();

        searchView = (SearchView) findViewById(R.id.sv_location);
        searchView.setSubmitButtonEnabled(true);

        searchResult = (ListView) findViewById((R.id.list_result));
        setVisible(R.id.list_result_none, false);
        setVisible(R.id.list_result, false);

        searchFilter = "title";
        searchView.setQueryHint("Search with title...");

        Intent intent = getIntent();
        currentName = intent.getExtras().get("name")+"";
        currentUserId = intent.getExtras().get("user_id")+"";
        isSuper = intent.getExtras().get("super_user")+"";

        setVisible(R.id.buttonCancelRoute,false);

        joinedSites = dbManager.fetchSitesJoined(currentUserId);

        notificationManager = getSystemService(NotificationManager.class);
        createNotificationChannels();

        Cursor notiCursor = dbManager.fetchNotification(currentUserId);

        //If the current user has any notifications store in the db, immediately send them
        if (notiCursor != null) {
            int counter = 1;
            notiCursor.moveToPosition(-1);

            try {
                while (notiCursor.moveToNext()) {
                    String notiId = notiCursor.getString(0)+"";
                    String content = notiCursor.getString(2);

                    dbManager.deleteNotification(notiId);
                    sendNotification(counter, content);
                    counter++;
                }
            } finally {
                notiCursor.close();
            }

        }


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint({"MissingPermission", "PotentialBehaviorOverride"})
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        requestPermissions();
        client = LocationServices.getFusedLocationProviderClient(MapsActivity.this);

        mMap = googleMap;

        dbManager = new DatabaseManager(this);
        dbManager.open();

        client.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {

               if (location != null) {
                   double currentLat = location.getLatitude();
                   double currentLong = location.getLongitude();

                   currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                   currentMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("You are here").icon(scaleMarker(R.drawable.marker_here)));

                   addMarkerData(currentMarker,
                           "null",
                           "null",
                           "null",
                           "null",
                           String.valueOf(currentLat),
                           String.valueOf(currentLong),
                           "null");
                   mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
               } else {
                   Toast.makeText(MapsActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
               }
            }
        });

        mMap.getUiSettings().setZoomControlsEnabled(true);

        //Fetch sites from db
        Cursor allSiteCursor = dbManager.fetchAllSite();

        //Add markers to app
        allSiteCursor.moveToPosition(-1);
        try {
            while (allSiteCursor.moveToNext()) {
                Marker marker;
                String leaderOfSite = allSiteCursor.getString(2)+"";

                Cursor user = dbManager.fetchOneUser(leaderOfSite);

                String siteId = allSiteCursor.getString(0)+"";
                System.out.println(leaderOfSite);

                if (isSuper.equals("1")) {

                    marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(allSiteCursor.getDouble(3), allSiteCursor.getDouble(4)))
                            .icon(scaleMarker(R.drawable.marker_super)));

                } else if (leaderOfSite.equals(currentUserId)) {

                    marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(allSiteCursor.getDouble(3), allSiteCursor.getDouble(4)))
                            .icon(scaleMarker(R.drawable.marker_added)));


                } else {

                    if (joinedSites.contains(siteId)) {
                        marker = mMap.addMarker(new MarkerOptions().position(new LatLng(allSiteCursor.getDouble(3), allSiteCursor.getDouble(4))).icon(scaleMarker(R.drawable.marker_joined)));


                    } else {
                        marker = mMap.addMarker(new MarkerOptions().position(new LatLng(allSiteCursor.getDouble(3), allSiteCursor.getDouble(4))).icon(scaleMarker(R.drawable.marker_anon)));

                    }

                }

                //Add marker and its details to hashmap to keep track
                addMarkerData(marker,
                        allSiteCursor.getString(0)+"",
                        allSiteCursor.getString(1)+"",
                        allSiteCursor.getString(2)+"",
                        user.getString(1),
                        allSiteCursor.getDouble(3)+"",
                        allSiteCursor.getDouble(4)+"",
                        allSiteCursor.getString(5));
            }
        } finally {
            allSiteCursor.close();
        }

        //Code for search bar
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Cursor results = dbManager.searchSiteResult(searchView.getQuery().toString(), searchFilter);

                if (results == null){
                    setVisible(R.id.noRecordTextSite2, true);
                    setVisible(R.id.list_result_none, true);
                    setVisible(R.id.list_result, false);
                }
                else {
                    setVisible(R.id.noRecordTextSite2, false);
                    setVisible(R.id.list_result_none, false);
                    setVisible(R.id.list_result, true);
                    SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                            MapsActivity.this, R.layout.search_result, results, from, to, 0);
                    adapter.notifyDataSetChanged();
                    searchResult.setAdapter(adapter);
                    searchResult.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int
                                position, long id) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(results.getDouble(2), results.getDouble(3))));

                        }
                    });
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                if (searchView.getQuery().toString().equals("")) {
                    setVisible(R.id.list_result, false);
                    setVisible(R.id.list_result_none, false);
                    setVisible(R.id.noRecordTextSite2, false);
                }
                return false;
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                    Map<String, Object> data = markers.get(marker);
                    assert data != null;

                    Cursor joinedUser = dbManager.fetchVolunteers((String) data.get("marker_id"));

                    if (Objects.requireNonNull(data.get("marker_id")).equals("null")) {
                        return false;
                    }
                    if (isSuper.equals("1")) {
                        AlertDialog.Builder markerDetail = new AlertDialog.Builder(MapsActivity.this);
                        markerDetail.setTitle("Location Details");
                        markerDetail.setMessage(
                                "- Title: " + data.get("title") + "\n" +
                                "- Leader name: " + data.get("leader_name") + "\n" +
                                "- Latitude: " + data.get("latitude") + "\n" +
                                "- Longitude: " + data.get("longitude") + "\n" +
                                "- Number of people joined: " + joinedUser.getCount() + "\n" +
                                "- Number of tested people: " + data.get("tested_people"));

                        markerDetail.setNeutralButton("Route", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (route != null) {
                                    route.remove();
                                    route = null;
                                    setVisible(R.id.buttonCancelRoute, false);

                                }
                                new GetDirection(currentLocation, new LatLng(Double.parseDouble(Objects.requireNonNull(data.get("latitude")).toString()), Double.parseDouble(Objects.requireNonNull(data.get("longitude")).toString()))).execute();
                            }
                        });
                        markerDetail.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                updatedMarker = marker;

                                Toast.makeText(MapsActivity.this, data.get("latitude") + " - " + data.get("longitude"), Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(MapsActivity.this, SiteManagerActivity.class);
                                intent.putExtra("site_id", data.get("marker_id") + "");
                                intent.putExtra("site_title", data.get("title") + "");
                                intent.putExtra("latitude", data.get("latitude") + "");
                                intent.putExtra("longitude", data.get("longitude") + "");
                                intent.putExtra("leader_id", data.get("leader_id")+"");
                                intent.putExtra("leader_name", data.get("leader_name")+"");
                                intent.putExtra("mode", "update");
                                intent.putExtra("tested_people", data.get("tested_people") + "");
                                startActivityForResult(intent, 200);

                            }
                        }).create().show();
                    } else if (Objects.equals(data.get("leader_id"), currentUserId)) {
                        AlertDialog.Builder markerDetail = new AlertDialog.Builder(MapsActivity.this);
                        markerDetail.setTitle("Location Details");
                        markerDetail.setMessage(
                                "- Title: " + data.get("title") + "\n" +
                                "- Leader name: " + data.get("leader_name") + "\n" +
                                "- Latitude: " + data.get("latitude") + "\n" +
                                "- Longitude: " + data.get("longitude") + "\n" +
                                "- Number of people joined: " + joinedUser.getCount() + "\n" +
                                "- Number of tested people: " + data.get("tested_people"));

                        markerDetail.setNegativeButton("Joined Users", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Intent intent = new Intent(MapsActivity.this, JoinedUserActivity.class);
                                intent.putExtra("title", (String) data.get("title"));
                                intent.putExtra("site_id", (String) data.get("marker_id"));
                                dialog.dismiss();
                                startActivity(intent);

                            }
                        });
                        markerDetail.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                updatedMarker = marker;

                                Toast.makeText(MapsActivity.this, data.get("latitude") + " - " + data.get("longitude"), Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(MapsActivity.this, SiteManagerActivity.class);
                                intent.putExtra("site_id", data.get("marker_id") + "");
                                intent.putExtra("site_title", data.get("title") + "");
                                intent.putExtra("latitude", data.get("latitude") + "");
                                intent.putExtra("longitude", data.get("longitude") + "");
                                intent.putExtra("leader_id", data.get("leader_id")+"");
                                intent.putExtra("leader_name", data.get("leader_name")+"");
                                intent.putExtra("mode", "update");
                                intent.putExtra("tested_people", data.get("tested_people") + "");
                                startActivityForResult(intent, 200);

                            }
                        });
                        markerDetail.setNeutralButton("Route", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (route != null) {
                                    route.remove();
                                    route = null;
                                    setVisible(R.id.buttonCancelRoute, false);

                                }
                                new GetDirection(currentLocation, new LatLng(Double.parseDouble(Objects.requireNonNull(data.get("latitude")).toString()), Double.parseDouble(Objects.requireNonNull(data.get("longitude")).toString()))).execute();

                            }
                        }).create().show();

                    } else {

                        if (joinedSites.contains((String) data.get("marker_id"))) {
                            AlertDialog.Builder markerDetail = new AlertDialog.Builder(MapsActivity.this);
                            markerDetail.setTitle("Location Details");
                            markerDetail.setMessage(
                                    "- Title: " + data.get("title") + "\n" +
                                    "- Leader name: " + data.get("leader_name") + "\n" +
                                    "- Latitude: " + data.get("latitude") + "\n" +
                                    "- Longitude: " + data.get("longitude") + "\n" +
                                    "- Number of people joined: " + joinedUser.getCount() + "\n" +
                                    "- Number of tested people: " + data.get("tested_people"));
                            markerDetail.setNegativeButton("Route", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (route != null) {
                                        route.remove();
                                        route = null;
                                        setVisible(R.id.buttonCancelRoute, false);

                                    }
                                    new GetDirection(currentLocation, new LatLng(Double.parseDouble(Objects.requireNonNull(data.get("latitude")).toString()), Double.parseDouble(Objects.requireNonNull(data.get("longitude")).toString()))).execute();

                                }
                            }).create().show();
                        } else {
                            AlertDialog.Builder markerDetail = new AlertDialog.Builder(MapsActivity.this);
                            markerDetail.setTitle(data.get("title") + " - by " + data.get("leader_name"));
                            markerDetail.setMessage("Would you like to join this site?");
                            markerDetail.setPositiveButton("Join", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dbManager.addVolunteer(currentUserId, (String) data.get("marker_id"), "None");
                                    joinedSites.add((String) data.get("marker_id"));
                                    marker.setIcon(scaleMarker(R.drawable.marker_joined));

                                    dbManager.addNotification((String) data.get("leader_id"), "User "+currentName+" has joined your site "+data.get("title"));

                                    dialog.dismiss();

                                }
                            });
                            markerDetail.setNegativeButton("Join with friend", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    AlertDialog.Builder friendJoin = new AlertDialog.Builder(MapsActivity.this);
                                    LayoutInflater inflater = MapsActivity.this.getLayoutInflater();
                                    View dialogView = inflater.inflate(R.layout.friend_input, null);
                                    friendJoin.setTitle("Join this site with your friend");
                                    friendJoin.setView(dialogView);

                                    EditText friendInput = (EditText) dialogView.findViewById(R.id.textViewFriend);

                                    friendJoin.setPositiveButton("Confirm Join", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Toast.makeText(MapsActivity.this, "" + friendInput.getText(), Toast.LENGTH_SHORT).show();
                                            dbManager.addVolunteer(currentUserId, (String) data.get("marker_id"), "" + friendInput.getText());
                                            joinedSites.add((String) data.get("marker_id"));
                                            marker.setIcon(scaleMarker(R.drawable.marker_joined));

                                            dbManager.addNotification((String) data.get("leader_id"), "User "+currentName+" has joined your site "+data.get("title")+" with "+friendInput.getText());
                                            dialog.dismiss();
                                        }
                                    });
                                    friendJoin.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    }).create().show();


                                }
                            });
                            markerDetail.setNeutralButton("Route", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (route != null) {
                                        route.remove();
                                        route = null;
                                        setVisible(R.id.buttonCancelRoute, false);

                                    }
                                    new GetDirection(currentLocation, new LatLng(Double.parseDouble(Objects.requireNonNull(data.get("latitude")).toString()), Double.parseDouble(Objects.requireNonNull(data.get("longitude")).toString()))).execute();
                                }
                            }).create().show();


                        }

                    }


                return false;
            }
        });


        //Add a marker on map when click

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                if (!isSuper.equals("1")) {
                    AlertDialog.Builder confirmBox = new AlertDialog.Builder(MapsActivity.this);
                    confirmBox.setTitle("Create a new site");
                    confirmBox.setMessage("Do you wish add a new site at this location?");
                    confirmBox.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

//                            Toast.makeText(MapsActivity.this, latLng.latitude+" - "+latLng.longitude, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MapsActivity.this, SiteManagerActivity.class);
                            intent.putExtra("latitude", latLng.latitude);
                            intent.putExtra("longitude", latLng.longitude);
                            intent.putExtra("leader_id", currentUserId);
                            intent.putExtra("leader_name", currentName);
                            startActivityForResult(intent, 200);

                        }
                    });
                    confirmBox.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                        }
                    }).create().show();
                }

            }
        });

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;

                for (Map.Entry<Marker, Map<String, Object>> map : markers.entrySet()) {

                    map.getKey().setVisible(bounds.contains(map.getKey().getPosition()));
                }
            }
        });
    }


    //Add marker data to hash map
    public void addMarkerData(Marker marker, String markerId, String title, String leaderId, String leaderName, String markerLat, String markerLong, String testedPeople) {
        Map<String, Object> markerData = new HashMap<>();
        markerData.put("marker_id", markerId);
        markerData.put("title", title);
        markerData.put("leader_id", leaderId);
        markerData.put("leader_name", leaderName);
        markerData.put("latitude", markerLat);
        markerData.put("longitude", markerLong);
        markerData.put("tested_people", testedPeople);

        markers.put(marker, markerData);
    }

    //Resize the marker
    public BitmapDescriptor scaleMarker(int marker) {
        int height = 100;
        int width = 100;
        Bitmap b = BitmapFactory.decodeResource(getResources(), marker);
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

        return BitmapDescriptorFactory.fromBitmap(smallMarker);
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(MapsActivity.this, new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_LOCATION);
    }

    public void onLocationChanged(Location location){
        String message = "Updated location " +
                Double.toString(location.getLatitude()) + ", " +
                Double.toString(location.getLongitude());
        LatLng newLoc = new LatLng(location.getLatitude(),
                location.getLongitude());
        mMap.addMarker(new MarkerOptions().position(newLoc).title("New Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(newLoc));
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint({"MissingPermission", "RestrictedApi"})
    private void startLocationUpdate(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        client.requestLocationUpdates(mLocationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult){
                onLocationChanged(locationResult.getLastLocation());
            }
        }, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200) {
            if (resultCode == RESULT_OK) {

                String addedId = data.getExtras().get("site_id").toString();
                String addedTitle = data.getExtras().get("title").toString();
                String leaderId = data.getExtras().get("leader_id").toString();
                String leaderName = data.getExtras().get("leader_name").toString();
                String addedLongitude = data.getExtras().get("longitude").toString();
                String addedLatitude = data.getExtras().get("latitude").toString();

//                System.out.println(data.getExtras().get("mode") == null);

                if (data.getExtras().get("mode") != null) {
                    String testedPeople = data.getExtras().get("tested_people").toString();

                    Map<String, Object> returnedData = new HashMap<>();
                    returnedData.put("marker_id", addedId);
                    returnedData.put("title", addedTitle);
                    returnedData.put("leader_id", leaderId);
                    returnedData.put("leader_name", leaderName);
                    returnedData.put("latitude", addedLatitude);
                    returnedData.put("longitude", addedLongitude);
                    returnedData.put("tested_people", testedPeople);

                    markers.put(updatedMarker, returnedData);
                } else {
                    Marker newMarker =  mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(addedLatitude), Double.parseDouble(addedLongitude))).icon(scaleMarker(R.drawable.marker_added)));

                    addMarkerData(newMarker,
                            addedId,
                            addedTitle,
                            currentUserId,
                            currentName,
                            addedLatitude,
                            addedLongitude,
                            "0");
                }

            }

        }
    }

    private void setVisible(int id, boolean isVisible) {
        View aView = findViewById(id);
        if (isVisible) {
            aView.setVisibility(View.VISIBLE);
        } else {
            aView.setVisibility(View.INVISIBLE);
        }

    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel1 = new NotificationChannel(
                    CHANNEL_1_ID,
                    "Channel 1",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel1.setDescription("This is channel 1");
            notificationManager.createNotificationChannel(channel1);
        }
    }

    public void sendNotification(int number, String message) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Notification")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();
        notificationManager.notify(number, notification);
    }



    @SuppressLint("NonConstantResourceId")
    public void onFilterApply(View view) {
        AlertDialog.Builder filterDialog = new AlertDialog.Builder(MapsActivity.this);
        LayoutInflater inflater = MapsActivity.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.search_filter, null);
        filterDialog.setTitle("Search Filter");
        filterDialog.setView(dialogView);

        RadioButton titleButton = (RadioButton) dialogView.findViewById(R.id.radioButtonTitle);
        RadioButton leaderButton = (RadioButton) dialogView.findViewById(R.id.radioButtonLeader);

        if (searchFilter.equals("title")) {

            titleButton.setChecked(true);
        } else {

            leaderButton.setChecked(true);
        }

        filterDialog.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (titleButton.isChecked()) {
                    searchFilter = "title";
                    searchView.setQueryHint("Search with title...");
                } else {
                    searchFilter = "leader";
                    searchView.setQueryHint("Search with leader name...");
                }

                dialog.dismiss();
            }
        });
        filterDialog.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).create().show();

    }

    public void onRouteCancel(View view) {
        route.remove();
        route = null;
        setVisible(R.id.buttonCancelRoute, false);
    }

    /**
     *
     * Code Reference: Ketan Ramani (Stack Overflow)
     * https://stackoverflow.com/questions/43292753/android-map-how-to-animate-polyline-on-map/59622783#59622783
     */

    @SuppressLint("StaticFieldLeak")
    private class GetDirection extends AsyncTask<String, Void, Void> {

        private LatLng origin, destination;
        private PolylineOptions polylineOptions;;

        public GetDirection(LatLng origin, LatLng destination) {
            this.origin = origin;
            this.destination = destination;
        }

        @Override
        protected Void doInBackground(String... strings) {
            jsonString = HttpHandler.getRequest(getUrl(this.origin, this.destination));

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            JSONObject jsonObject;

            List<List<HashMap<String, String>>> routes;

            try {
                jsonObject = new JSONObject(jsonString);

                routes = parse(jsonObject);

                ArrayList<LatLng> points;


                for (int i = 0; i < routes.size(); i++) {
                    points = new ArrayList<>();
                    polylineOptions = new PolylineOptions();

                    List<HashMap<String, String>> path = routes.get(i);

                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);

                        double latitude = Double.parseDouble(Objects.requireNonNull(point.get("latitude")));
                        double longitude = Double.parseDouble(Objects.requireNonNull(point.get("longitude")));
                        LatLng position = new LatLng(latitude,longitude);

                        points.add(position);
                    }

                    polylineOptions.addAll(points);
                    polylineOptions.width(8);
                    polylineOptions.color(Color.RED);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (polylineOptions == null) {
                Toast.makeText(MapsActivity.this, "Unable to generate route for this location", Toast.LENGTH_SHORT).show();
            } else {
                setVisible(R.id.buttonCancelRoute,true);
                route = mMap.addPolyline(polylineOptions);
            }

        }

        synchronized public String getUrl(LatLng origin, LatLng destination) {

            String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

            String str_destination = "destination=" + destination.latitude + "," + destination.longitude;

            String mode = "mode=driving";

            String parameters = str_origin + "&" + str_destination + "&" + mode;

            String key = "key=AIzaSyAd6_wgaYL1X48Pxg0VI9u4SPw9RedE5XQ";

            String output = "json";

            System.out.println("https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters+"&"+key);

            return "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters+"&"+key;
        }

        public List<List<HashMap<String, String>>> parse(JSONObject jsonObject) {
            List<List<HashMap<String, String>>> routes = new ArrayList<>();

            JSONArray jsonRoutes;
            JSONArray jsonLegs;
            JSONArray jsonSteps;

            try {

                jsonRoutes = jsonObject.getJSONArray("routes");

                for (int i = 0; i < jsonRoutes.length(); i++) {
                    jsonLegs = ((JSONObject) jsonRoutes.get(i)).getJSONArray("legs");

                    ArrayList<HashMap<String, String>> path = new ArrayList<>();

                    for (int j = 0; j < jsonLegs.length(); j++) {
                        jsonSteps = ((JSONObject) jsonLegs.get(i)).getJSONArray("steps");

                        for (int k = 0; k < jsonSteps.length(); k++) {
                            String polyline = (String) ((JSONObject) ((JSONObject) jsonSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);

                            for (int m = 0; m < list.size(); m++) {
                                HashMap<String, String> hashMap = new HashMap<>();
                                hashMap.put("latitude", Double.toString((list.get(m)).latitude));
                                hashMap.put("longitude", Double.toString((list.get(m)).longitude));

                                path.add(hashMap);
                            }
                        }
                    }

                    routes.add(path);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return routes;
        }

        //Decode the polyline points
        private List<LatLng> decodePoly(String encoded) {

            ArrayList<LatLng> polyline = new ArrayList<>();
            int index = 0;
            int len = encoded.length();
            int latitude = 0;
            int longitude = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);

                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                latitude += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);

                int dlong = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                longitude += dlong;

                LatLng p = new LatLng((((double) latitude / 1E5)), (((double) longitude / 1E5)));
                polyline.add(p);
            }

            return polyline;
        }
    }
}