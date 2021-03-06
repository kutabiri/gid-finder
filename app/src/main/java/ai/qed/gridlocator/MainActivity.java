package ai.qed.gridlocator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.Style;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.views.MapView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private TextView gidTextView;
    private TextView latTextView;
    private TextView longTextView;
    private SensorManager mSensorManager;
    private SensorEventListener mSensorEventListener;
    private View marker;
    private RelativeLayout parentView;
    private GridID currentGID = null;
    private MediaPlayer pingSound;
    private ImageView compass;
    private SharedPreferences preferences;
    private Button zoomInButton;
    private Button zoomOutButton;
    private CheckBox pingToggle;
    private GridView mapGrid;
    private View singleGrid;
    private int zoomLevel = 1;
    private Handler handler;
    private MapView mapView;

    // record the compass picture angle turned
    private float currentDegree = 0f;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0 * 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 0 * 1000 * 60 * 1; // 1 minute

    private static final int PICKFILE_REQUEST_CODE = 0;

    private static final List<String> largeGridArray = new ArrayList<>();

    static {
        for (int i = 0; i < 10; i ++) {
            for (int j = 0; j < 10; j++) {
                largeGridArray.add(i * 10 + j, Integer.toString(j * 10 + 9 - i));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) throws SecurityException {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.status_bar_color));
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        pingSound = MediaPlayer.create(this, R.raw.ping);
        handler = new Handler(getMainLooper());
        gidTextView = (TextView) findViewById(R.id.coordinates);
        latTextView = (TextView) findViewById(R.id.latitude);
        longTextView = (TextView) findViewById(R.id.longitude);
        marker = getLayoutInflater().inflate(R.layout.marker, null);
        parentView = (RelativeLayout) findViewById(R.id.parent_view);
        compass = (ImageView) findViewById(R.id.compass);
        pingToggle = (CheckBox) findViewById(R.id.ping_toggle);
        mapGrid = (GridView) findViewById(R.id.map_section);
        singleGrid = findViewById(R.id.small_grid);

        singleGrid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                return;
            }
        });

        mapView = (MapView) findViewById(R.id.mapview);
        final ITileSource tileSource = TileSourceFactory.getTileSource(TileSourceFactory.MAPNIK.name());
        mapView.setTileSource(tileSource);

        boolean pingOn = preferences.getBoolean(getString(R.string.pref_key_sound), true);

        if (pingOn) {
            pingToggle.setChecked(true);
            pingToggle.setText(R.string.ping_on);
        }
        else {
            pingToggle.setChecked(false);
            pingToggle.setText(R.string.ping_off);
        }

        pingToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    pingToggle.setText(R.string.ping_on);
                }
                else {
                    pingToggle.setText(R.string.ping_off);
                }
                preferences.edit().putBoolean(getString(R.string.pref_key_sound), isChecked).apply();
            }
        });

        zoomInButton = (Button) findViewById(R.id.zoom_in_btn);
        zoomOutButton = (Button) findViewById(R.id.zoom_out_btn);

        zoomInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentGID != null) {
                    zoomLevel--;

                    updateZoomButtons();
                }
            }
        });

        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentGID != null) {
                    zoomLevel++;

                    updateZoomButtons();
                }
            }
        });

        mLocationListener = new LocationListener() {
            public void onLocationChanged(final Location location) {
                final GridID gridID = GridID.fromLocation(location);

                MainActivity.this.setLocationMessage(gridID);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {
                gidTextView.setText(getString(R.string.initial_message));
            }

            @Override
            public void onProviderDisabled(String provider) {
                handler.removeCallbacksAndMessages(null);
                gidTextView.setText(getString(R.string.please_turn_on_gps));
                currentGID = null;
                latTextView.setText(null);
                longTextView.setText(null);
                parentView.removeView(marker);
            }
        };

        mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                // get the angle around the z-axis rotated
                float degree = Math.round(event.values[0]);

                // create a rotation animation (reverse turn degree degrees)
                RotateAnimation ra = new RotateAnimation(
                        currentDegree,
                        -degree,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);

                // how long the animation will take place
                ra.setDuration(100);

                // set the animation after the end of the reservation status
                ra.setFillAfter(true);

                // Start the animation
                compass.startAnimation(ra);
                currentDegree = -degree;

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        findViewById(R.id.load_file_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });

        mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        findViewById(R.id.location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCalc();
            }
        });
    }

    private void launchCalc() {
        View view = getLayoutInflater().inflate(R.layout.dialog_calc, null);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        final TextView lat = (TextView) view.findViewById(R.id.lat);
        final TextView lon = (TextView) view.findViewById(R.id.lon);
        final TextView gid = (TextView) view.findViewById(R.id.gid);

        view.findViewById(R.id.to_gid).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double latitude = 0;
                double longitude = 0;
                try {
                    latitude = Double.parseDouble(lat.getText().toString());
                    longitude = Double.parseDouble(lon.getText().toString());
                }
                catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, getString(R.string.latlon_validation_message), Toast.LENGTH_SHORT).show();
                    return;
                }
                GridID gridId = GridID.fromLatLong(latitude, longitude);
                gid.setText(gridId.toGIDString());

                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(lat.getWindowToken(), 0);
            }
        });

        view.findViewById(R.id.to_lat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Location location = GridID.fromGID(gid.getText().toString(), 500, 500);

                    lat.setText(Double.toString(Math.round(location.getLatitude() * 1000000.00) / 1000000.00));
                    lon.setText(Double.toString(Math.round(location.getLongitude() * 1000000.00) / 1000000.00));

                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(gid.getWindowToken(), 0);

                    final View view = getLayoutInflater().inflate(R.layout.dialog_to_latlon, null);

                    final AlertDialog gridDialog = new AlertDialog.Builder(MainActivity.this)
                            .setView(view)
                            .setCancelable(false)
                            .create();

                    ((TextView) view.findViewById(R.id.lat_lon)).setText(getLatLon(location));

                    view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            gridDialog.dismiss();
                        }
                    });

                    view.findViewById(R.id.top_left_container).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectDot((ImageView) view.findViewById(R.id.top_left), view, getLatLon(GridID.fromGID(gid.getText().toString(), 0, 1000)));
                        }
                    });

                    view.findViewById(R.id.top_center_container).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectDot((ImageView) view.findViewById(R.id.top_center), view, getLatLon(GridID.fromGID(gid.getText().toString(), 500, 1000)));
                        }
                    });

                    view.findViewById(R.id.top_right_container).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectDot((ImageView) view.findViewById(R.id.top_right), view, getLatLon(GridID.fromGID(gid.getText().toString(), 1000, 1000)));
                        }
                    });

                    view.findViewById(R.id.center_left_container).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectDot((ImageView) view.findViewById(R.id.center_left), view, getLatLon(GridID.fromGID(gid.getText().toString(), 0, 500)));
                        }
                    });

                    view.findViewById(R.id.center_center_container).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectDot((ImageView) view.findViewById(R.id.center_center), view, getLatLon(GridID.fromGID(gid.getText().toString(), 500, 500)));
                        }
                    });

                    view.findViewById(R.id.center_right_container).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectDot((ImageView) view.findViewById(R.id.center_right), view, getLatLon(GridID.fromGID(gid.getText().toString(), 1000, 500)));
                        }
                    });

                    view.findViewById(R.id.bottom_left_container).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectDot((ImageView) view.findViewById(R.id.bottom_left), view, getLatLon(GridID.fromGID(gid.getText().toString(), 0, 0)));
                        }
                    });

                    view.findViewById(R.id.bottom_center_container).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectDot((ImageView) view.findViewById(R.id.bottom_center), view, getLatLon(GridID.fromGID(gid.getText().toString(), 500, 0)));
                        }
                    });

                    view.findViewById(R.id.bottom_right_container).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectDot((ImageView) view.findViewById(R.id.bottom_right), view, getLatLon(GridID.fromGID(gid.getText().toString(), 1000, 0)));
                        }
                    });

                    gridDialog.show();
                }
                catch (IllegalArgumentException e) {
                    Toast.makeText(MainActivity.this, getString(R.string.gid_validation_message), Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private String getLatLon(Location location) {
        return getString(R.string.lat_lon, Double.toString(Math.round(location.getLatitude() * 1000000.00) / 1000000.00), Double.toString(Math.round(location.getLongitude() * 1000000.00) / 1000000.00));
    }

    private void selectDot(ImageView imageView, View container, String value) {
        ((ImageView) container.findViewById(R.id.top_left)).setImageResource(R.drawable.circle);
        ((ImageView) container.findViewById(R.id.top_center)).setImageResource(R.drawable.circle);
        ((ImageView) container.findViewById(R.id.top_right)).setImageResource(R.drawable.circle);
        ((ImageView) container.findViewById(R.id.center_left)).setImageResource(R.drawable.circle);
        ((ImageView) container.findViewById(R.id.center_center)).setImageResource(R.drawable.circle);
        ((ImageView) container.findViewById(R.id.center_right)).setImageResource(R.drawable.circle);
        ((ImageView) container.findViewById(R.id.bottom_left)).setImageResource(R.drawable.circle);
        ((ImageView) container.findViewById(R.id.bottom_center)).setImageResource(R.drawable.circle);
        ((ImageView) container.findViewById(R.id.bottom_right)).setImageResource(R.drawable.circle);
        imageView.setImageResource(R.drawable.green_circle);

        ((TextView) container.findViewById(R.id.lat_lon)).setText(value);
    }

    private void setMapZoom(final Pair<Double, Double> centeredCoord) {
        double latOffset;
        double longOffset;

        switch (zoomLevel) {
            case 1:
                // 111 kilometers / 1110 = 100 meters.
                // 1 degree of latitude = ~111 kilometers.
                // 1 / 1000 means an offset of coordinate by 111 meters.

                latOffset = 1.0 / 1110.0 / 2;
                break;
            case 2:
                latOffset = 1.0 / 1110.0 / 2 * 3;
                break;
            case 3:
                // 111 kilometers / 111 = 1000 meters.
                // 1 degree of latitude = ~111 kilometers.
                // 1 / 1000 means an offset of coordinate by 111 meters.

                latOffset = 1.0 / 111.0 / 2;
                break;
            default:
                return;
        }

        // With longitude, things are a bit more complex.
        // 1 degree of longitude = 111km only at equator (gradually shrinks to zero at the poles)
        // So need to take into account latitude too, using cos(lat).

        longOffset = latOffset * Math.cos((centeredCoord.first) * Math.PI / 180.0);

        mapView.zoomToBoundingBox(new BoundingBoxE6(
                (centeredCoord.first + latOffset),
                (centeredCoord.second + longOffset),
                (centeredCoord.first - latOffset),
                (centeredCoord.second - longOffset)));


    }

    private void updateMapGrid() {
        if (zoomLevel == 1) {
            mapView.setVisibility(View.GONE);
            mapGrid.setVisibility(View.GONE);
            singleGrid.setVisibility(View.VISIBLE);
            ((TextView) singleGrid.findViewById(R.id.cell_number)).setText(Integer.toString(currentGID.getSubcell()));
        }
        else {
            List<String> list;
            mapGrid.setVisibility(View.VISIBLE);
            singleGrid.setVisibility(View.GONE);
            mapView.setVisibility(View.VISIBLE);

            if (zoomLevel == 3) {
                mapGrid.setNumColumns(10);
                list = largeGridArray;
            } else {
                list = getCellList(currentGID.getSubcell());
                mapGrid.setNumColumns(3);
            }

            mapGrid.setAdapter(new GridAdapter<>(this, list, zoomLevel, Integer.toString(currentGID.getSubcell())));
        }

        setMapZoom(recenterMap());

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                positionMarker(currentGID);
            }
        }, 500);
    }

    private void updateZoomButtons() {
        parentView.removeView(marker);
        if (zoomLevel == 1) {
            zoomInButton.setEnabled(false);
        }
        else {
            mapGrid.setVisibility(View.VISIBLE);
            singleGrid.setVisibility(View.GONE);

            if (zoomLevel == 3) {
                zoomOutButton.setEnabled(false);
            } else {
                zoomInButton.setEnabled(true);
                zoomOutButton.setEnabled(true);
            }

        }

        updateMapGrid();
    }

    private Pair<Double, Double> recenterMap() {
        double offset = 1.0 / 1110.0;
        if (zoomLevel == 3) {
            // have to figure out the center of the map from current gid

            int latCoord = currentGID.getSubcell() % 10;
            int longCoord = currentGID.getSubcell() / 10;

            // shift latitude down if it's below middle
            double latOffset = (4.5 - latCoord) * offset;

            // shift longtiude to right if it's on the left side
            double longOffset = (4.5 - longCoord) * offset * Math.cos((currentGID.getLatitude() + latOffset) * Math.PI / 180.0);

            return new Pair<>(currentGID.getLatitude() + latOffset, currentGID.getLongitude() + longOffset);
        }
        else if (zoomLevel == 2) {
            int subcell = currentGID.getSubcell();
            // corner cells
            if (subcell == 0) {
                return new Pair<>(currentGID.getLatitude() + offset, currentGID.getLongitude() + offset * Math.cos((currentGID.getLatitude() + offset) * Math.PI / 180.0));
            }
            else if (subcell == 9) {
                return new Pair<>(currentGID.getLatitude() - offset, currentGID.getLongitude() + offset * Math.cos((currentGID.getLatitude() - offset) * Math.PI / 180.0));
            }
            else if (subcell == 99) {
                return new Pair<>(currentGID.getLatitude() - offset, currentGID.getLongitude() - offset * Math.cos((currentGID.getLatitude() - offset) * Math.PI / 180.0));
            }
            else if (subcell == 90) {
                return new Pair<>(currentGID.getLatitude() + offset, currentGID.getLongitude() - offset * Math.cos((currentGID.getLatitude() - offset) * Math.PI / 180.0));
            }
            // left side
            else if (subcell < 10) {
                return new Pair<>(currentGID.getLatitude(), currentGID.getLongitude() + offset * Math.cos((currentGID.getLatitude()) * Math.PI / 180.0));
            }
            // top side
            else if ((subcell % 10) == 9) {
                return new Pair<>(currentGID.getLatitude() - offset, currentGID.getLongitude());
            }
            // right side
            else if (subcell > 90) {
                return new Pair<>(currentGID.getLatitude(), currentGID.getLongitude() - offset * Math.cos((currentGID.getLatitude()) * Math.PI / 180.0));
            }
            // bottom side
            else if ((subcell % 10) == 0) {
                return new Pair<>(currentGID.getLatitude() + offset, currentGID.getLongitude());
            }
            // middle cells
            else {
                return new Pair<>(currentGID.getLatitude(), currentGID.getLongitude());
            }
        }
        else {
            return new Pair<>(currentGID.getLatitude(), currentGID.getLongitude());
        }
    }

    private void startTracking() {
        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);

        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                mLocationListener);
        Location latestLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (latestLocation != null) {
            mLocationListener.onLocationChanged(latestLocation);
        }

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gidTextView.setText(getString(R.string.please_turn_on_gps));
        }
    }

    private void stopTracking() {
        mLocationManager.removeUpdates(mLocationListener);
        mSensorManager.unregisterListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
    }

    private void setLocationMessage(final GridID gridId) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // only need to ping and update gid when it changed
                if (gridId != null && (currentGID == null || !gridId.toGIDHeaderString().equals(currentGID.toGIDHeaderString()))) {

                    if (preferences.getBoolean(getString(R.string.pref_key_sound), true)) {
                        pingSound.start();
                    }

                    gidTextView.setText(gridId.toGIDHeaderString());

                    if (zoomLevel == 1) {
                        ((TextView) singleGrid.findViewById(R.id.cell_number)).setText(Integer.toString(gridId.getSubcell()));
                    }
                }

                latTextView.setText(gridId.getLatString());
                longTextView.setText(gridId.getLongString());

                currentGID = gridId;
                updateMapGrid();
            }
        });
    }

    private void positionMarker(GridID gridID) {
        parentView.removeView(marker);
        int markerSize = (int) getResources().getDimension(R.dimen.marker_width);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(markerSize, markerSize);

        int leftMargin;
        int topMargin;
        if (zoomLevel == 1) {
            leftMargin = gridID.getXOffset() * singleGrid.getWidth() / 100 - (markerSize / 2) + singleGrid.getLeft();
            topMargin = (100 - gridID.getYOffset()) * singleGrid.getHeight() / 100 - (markerSize / 2) + singleGrid.getTop();
        }
        else {
            View view = null;
            for (int i = 0; i < mapGrid.getCount(); i++) {
                String item = (String) mapGrid.getAdapter().getItem(i);
                if (item.equals(Integer.toString(currentGID.getSubcell()))) {
                    view = mapGrid.getChildAt(i);
                    break;
                }
            }

            if (view != null) {
                if (zoomLevel == 2) {
                    leftMargin = gridID.getXOffset() * view.getWidth() / 100 - (markerSize / 2) + view.getLeft() + mapGrid.getLeft();
                    topMargin = (100 - gridID.getYOffset()) * view.getHeight() / 100 - (markerSize / 2) + view.getTop() + mapGrid.getTop();
                } else {
                    leftMargin = view.getWidth() / 2 - (markerSize / 2) + view.getLeft() + mapGrid.getLeft();
                    topMargin = view.getHeight() / 2 - (markerSize / 2) + view.getTop() + mapGrid.getTop();
                }
            }
            else {
                return;
            }
        }

        params.setMargins(leftMargin, topMargin, 0, 0);
        marker.setLayoutParams(params);
        parentView.addView(marker);
    }

    private List<String> getCellList(int subcell) {
        List<String> cells = new ArrayList<>();

        // corner cells
        if (subcell == 0) {
            cells.add(0, Integer.toString(subcell + 2));
            cells.add(1, Integer.toString(subcell + 12));
            cells.add(2, Integer.toString(subcell + 22));
            cells.add(3, Integer.toString(subcell + 1));
            cells.add(4, Integer.toString(subcell + 11));
            cells.add(5, Integer.toString(subcell + 21));
            cells.add(6, Integer.toString(subcell));
            cells.add(7, Integer.toString(subcell + 10));
            cells.add(8, Integer.toString(subcell + 20));
        }
        else if (subcell == 9) {
            cells.add(0, Integer.toString(subcell));
            cells.add(1, Integer.toString(subcell + 10));
            cells.add(2, Integer.toString(subcell + 20));
            cells.add(3, Integer.toString(subcell - 1));
            cells.add(4, Integer.toString(subcell + 9));
            cells.add(5, Integer.toString(subcell + 19));
            cells.add(6, Integer.toString(subcell - 2));
            cells.add(7, Integer.toString(subcell + 8));
            cells.add(8, Integer.toString(subcell + 18));
        }
        else if (subcell == 99) {
            cells.add(0, Integer.toString(subcell - 20));
            cells.add(1, Integer.toString(subcell - 10));
            cells.add(2, Integer.toString(subcell));
            cells.add(3, Integer.toString(subcell - 21));
            cells.add(4, Integer.toString(subcell - 11));
            cells.add(5, Integer.toString(subcell - 1));
            cells.add(6, Integer.toString(subcell - 22));
            cells.add(7, Integer.toString(subcell - 12));
            cells.add(8, Integer.toString(subcell - 2));
        }
        else if (subcell == 90) {
            cells.add(0, Integer.toString(subcell - 18));
            cells.add(1, Integer.toString(subcell - 8));
            cells.add(2, Integer.toString(subcell + 2));
            cells.add(3, Integer.toString(subcell - 19));
            cells.add(4, Integer.toString(subcell - 9));
            cells.add(5, Integer.toString(subcell + 1));
            cells.add(6, Integer.toString(subcell - 20));
            cells.add(7, Integer.toString(subcell - 10));
            cells.add(8, Integer.toString(subcell));
        }
        // left side
        else if (subcell < 10) {
            cells.add(0, Integer.toString(subcell + 1));
            cells.add(1, Integer.toString(subcell + 11));
            cells.add(2, Integer.toString(subcell + 21));
            cells.add(3, Integer.toString(subcell));
            cells.add(4, Integer.toString(subcell + 10));
            cells.add(5, Integer.toString(subcell + 20));
            cells.add(6, Integer.toString(subcell - 1));
            cells.add(7, Integer.toString(subcell + 9));
            cells.add(8, Integer.toString(subcell + 19));
        }
        // top side
        else if ((subcell % 10) == 9) {
            cells.add(0, Integer.toString(subcell - 10));
            cells.add(1, Integer.toString(subcell));
            cells.add(2, Integer.toString(subcell + 10));
            cells.add(3, Integer.toString(subcell - 11));
            cells.add(4, Integer.toString(subcell - 1));
            cells.add(5, Integer.toString(subcell + 9));
            cells.add(6, Integer.toString(subcell - 12));
            cells.add(7, Integer.toString(subcell - 2));
            cells.add(8, Integer.toString(subcell + 8));
        }
        // right side
        else if (subcell > 90) {
            cells.add(0, Integer.toString(subcell - 19));
            cells.add(1, Integer.toString(subcell - 9));
            cells.add(2, Integer.toString(subcell + 1));
            cells.add(3, Integer.toString(subcell - 20));
            cells.add(4, Integer.toString(subcell - 10));
            cells.add(5, Integer.toString(subcell));
            cells.add(6, Integer.toString(subcell - 21));
            cells.add(7, Integer.toString(subcell - 11));
            cells.add(8, Integer.toString(subcell - 1));
        }
        // bottom side
        else if ((subcell % 10) == 0) {
            cells.add(0, Integer.toString(subcell - 8));
            cells.add(1, Integer.toString(subcell + 2));
            cells.add(2, Integer.toString(subcell + 12));
            cells.add(3, Integer.toString(subcell - 9));
            cells.add(4, Integer.toString(subcell + 1));
            cells.add(5, Integer.toString(subcell + 11));
            cells.add(6, Integer.toString(subcell - 10));
            cells.add(7, Integer.toString(subcell));
            cells.add(8, Integer.toString(subcell + 10));
        }
        // middle cells
        else {
            cells.add(0, Integer.toString(subcell - 9));
            cells.add(1, Integer.toString(subcell + 1));
            cells.add(2, Integer.toString(subcell + 11));
            cells.add(3, Integer.toString(subcell - 10));
            cells.add(4, Integer.toString(subcell));
            cells.add(5, Integer.toString(subcell + 10));
            cells.add(6, Integer.toString(subcell - 11));
            cells.add(7, Integer.toString(subcell - 1));
            cells.add(8, Integer.toString(subcell + 9));
        }

        return cells;
    }

    @Override
    protected void onStart() {
        startTracking();
        super.onStart();
    }

    @Override
    protected void onPause() {
        stopTracking();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopTracking();
        super.onDestroy();
    }

    private void openFile() {
        startActivityForResult(ListFileActivity.makeIntent(this, "storage"), PICKFILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        switch (requestCode) {
            case PICKFILE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    new AsyncTask<Void, Void, Boolean>() {
                        private KmlDocument kmlDocument;

                        @Override
                        protected Boolean doInBackground(Void... params) {
                            kmlDocument = new KmlDocument();

                            File file = new File(data.getStringExtra(ListFileActivity.EXTRA_RESULT));
                            boolean parsed = kmlDocument.parseKMLFile(file);

                            return parsed;

                        }

                        @Override
                        protected void onPostExecute(Boolean parsed) {
                            if (parsed) {
                                Drawable defaultKmlMarker = getResources().getDrawable(R.drawable.marker_kml_point);
                                Bitmap bitmap = ((BitmapDrawable)defaultKmlMarker).getBitmap();
                                Style defaultStyle = new Style(bitmap, 0x901010AA, 3.0f, 0x20AA1010);

                                FolderOverlay kmlOverlay = (FolderOverlay) kmlDocument.mKmlRoot.buildOverlay(mapView, defaultStyle, null, kmlDocument);
                                mapView.getOverlays().add(kmlOverlay);
                                mapView.invalidate();
//                                mapView.zoomToBoundingBox(kmlDocument.mKmlRoot.getBoundingBox());
                            }
                        }
                    }.execute();
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}

