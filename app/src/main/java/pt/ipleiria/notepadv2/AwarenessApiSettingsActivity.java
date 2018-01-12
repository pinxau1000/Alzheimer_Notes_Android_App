package pt.ipleiria.notepadv2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.fence.TimeFence;
import com.google.android.gms.maps.model.LatLng;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import pt.ipleiria.notepadv2.model.AppConstants;
import pt.ipleiria.notepadv2.model.Bluetooth;
import pt.ipleiria.notepadv2.model.CSVFile;
import pt.ipleiria.notepadv2.model.CurrentContext;
import pt.ipleiria.notepadv2.model.Note;
import pt.ipleiria.notepadv2.model.Notepad;
import pt.ipleiria.notepadv2.model.Singleton;
import pt.ipleiria.notepadv2.model.commonMethods;

public class AwarenessApiSettingsActivity extends AppCompatActivity implements AppConstants {

    private View dialogViewBluetooth;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_awareness_api_settings);

        Toolbar addActivityToolbar = findViewById(R.id.toolbar_AwarenessSettings);
        addActivityToolbar.setNavigationIcon(R.drawable.ic_action_back);
        setSupportActionBar(addActivityToolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Context Filter Settings");

        //Hides the keyboard when the activity starts
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //Hides progress bar
        ProgressBar progressBar = findViewById(R.id.progressBar_contextRefresh);
        progressBar.setVisibility(View.GONE);

        //Set the text view with the values stored on singleton if they exist
        TextView tv_actualStats = findViewById(R.id.textView_actualStats);
        tv_actualStats.setText("");
        if (Singleton.getInstance().getHeadphonesState() != null)
            tv_actualStats.append("\n_______\nHeadphones: "+commonMethods.decodeHeadphonesState(Singleton.getInstance().getHeadphonesState().getState()));
        if (Singleton.getInstance().getWeather() != null)
            tv_actualStats.append("\n_______\nWeather: "+Singleton.getInstance().getWeather().toString());
        if (Singleton.getInstance().getLocation() != null)
            tv_actualStats.append("\n_______\nLocation: "+Singleton.getInstance().getLocation().toString());
        if (Singleton.getInstance().getActivityRecognitionResult() != null)
            tv_actualStats.append("\n_______\nUser Activity: "+Singleton.getInstance().getActivityRecognitionResult().toString());
        if (Singleton.getInstance().getPlaces() != null)
            tv_actualStats.append("\n_______\nPlaces: "+Singleton.getInstance().getPlaces().toString());
        if (Singleton.getInstance().getTimeInterval() != null)
            tv_actualStats.append("\n_______\nTimeIntervals: "+commonMethods.decodeTimeIntervals(Singleton.getInstance().getTimeInterval().getTimeIntervals()));

        FloatingActionButton btn_refreshContext = findViewById(R.id.floatingActionButton_refreshStats);
        btn_refreshContext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Permission Not Granted -> Ask For It
                if (ActivityCompat.checkSelfPermission(AwarenessApiSettingsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(AwarenessApiSettingsActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                    return;
                }

                int gpsActive;
                try {
                    gpsActive = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);

                    //GPS Not Enable Check
                    if (gpsActive == 0) {
                        //TODO: Ask for user enable GPS
                        //Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        //startActivity(onGPS);

                        Toast.makeText(AwarenessApiSettingsActivity.this, "You must activate your GPS to get better results!", Toast.LENGTH_LONG).show();
                    }
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }

                //Permission Already Granted
                contextRefresh();
            }
        });

        Spinner spinner_tempUnits = findViewById(R.id.spinner_tempUnit);
        spinner_tempUnits.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, AVAILABLE_TEMPERATURE_UNITS));

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);

        if (sharedPref.getString(PREFS_TEMPERATURE_UNIT_KEY, AVAILABLE_TEMPERATURE_UNITS[0]).equals(AVAILABLE_TEMPERATURE_UNITS[0]))
            spinner_tempUnits.setSelection(0);
        else if (sharedPref.getString(PREFS_TEMPERATURE_UNIT_KEY, AVAILABLE_TEMPERATURE_UNITS[1]).equals(AVAILABLE_TEMPERATURE_UNITS[1]))
            spinner_tempUnits.setSelection(1);

        spinner_tempUnits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences sharedPref = AwarenessApiSettingsActivity.this.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(PREFS_TEMPERATURE_UNIT_KEY, parent.getItemAtPosition(position).toString());
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //Nothing
            }
        });

        final EditText editText_locationRadius = findViewById(R.id.editText_locationRadius);
        editText_locationRadius.setText(sharedPref.getFloat(PREFS_LOCATION_RADIUS_KEY, LOCATION_DEFAULT_RADIUS) + "");

        editText_locationRadius.addTextChangedListener(new TextWatcher() {

            //Refreshing(Un-registering & registering) Fences because Radius was changed
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                for (Note n : Singleton.getInstance().getNotepad().getNotes()) {
                    ArrayList<LatLng> keywordsLocation = n.getKeywordsLocation();
                    if (!keywordsLocation.isEmpty()) {
                        for (LatLng latLng : keywordsLocation) {
                            commonMethods.unregisterFence(getApplicationContext(), FENCE_KEY_LOCATION + n.getId() + latLng.toString());
                        }
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                SharedPreferences sharedPref = AwarenessApiSettingsActivity.this.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                try {
                    editor.putFloat(PREFS_LOCATION_RADIUS_KEY, Float.parseFloat(editText_locationRadius.getText().toString()));
                } catch (NumberFormatException nfe) {
                    editor.putFloat(PREFS_LOCATION_RADIUS_KEY, LOCATION_DEFAULT_RADIUS);
                    Toast.makeText(AwarenessApiSettingsActivity.this, "Invalid Value. Applying default: " + LOCATION_DEFAULT_RADIUS + "m", Toast.LENGTH_SHORT).show();
                }

                editor.apply();
            }


            //Refreshing(Un-registering & registering) Fences because Radius was changed
            @SuppressLint("MissingPermission")
            @Override
            public void afterTextChanged(Editable editable) {
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);

                float searchRadius = sharedPref.getFloat(PREFS_LOCATION_RADIUS_KEY, LOCATION_DEFAULT_RADIUS);
                float dwellTimeSec = sharedPref.getFloat(PREFS_DWELL_TIME_KEY, DWELL_DEFAULT_TIME);

                for (Note n : Singleton.getInstance().getNotepad().getNotes()) {
                    ArrayList<LatLng> keywordsLocation = n.getKeywordsLocation();
                    if (!keywordsLocation.isEmpty()) {
                        for (LatLng latLng : keywordsLocation) {
                            commonMethods.registerFence(getApplicationContext(), FENCE_KEY_LOCATION + n.getId() + latLng.toString(),
                                    LocationFence.in(latLng.latitude, latLng.longitude, searchRadius, (long) dwellTimeSec * 1000));
                        }
                    }
                }
            }
        });

        final EditText editText_dwellTime = findViewById(R.id.editText_dwellTime);
        editText_dwellTime.setText(sharedPref.getFloat(PREFS_DWELL_TIME_KEY, DWELL_DEFAULT_TIME) + "");

        editText_dwellTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                for (Note n : Singleton.getInstance().getNotepad().getNotes()) {
                    ArrayList<LatLng> keywordsLocation = n.getKeywordsLocation();
                    if (!keywordsLocation.isEmpty()) {
                        for (LatLng latLng : keywordsLocation) {
                            commonMethods.unregisterFence(getApplicationContext(), FENCE_KEY_LOCATION + n.getId() + latLng.toString());
                        }
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                SharedPreferences sharedPref = AwarenessApiSettingsActivity.this.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                try {
                    editor.putFloat(PREFS_DWELL_TIME_KEY, Float.parseFloat(editText_dwellTime.getText().toString()));
                } catch (NumberFormatException nfe) {
                    editor.putFloat(PREFS_DWELL_TIME_KEY, DWELL_DEFAULT_TIME);
                    Toast.makeText(AwarenessApiSettingsActivity.this, "Invalid Value. Applying default: " + DWELL_DEFAULT_TIME + " sec", Toast.LENGTH_SHORT).show();
                }

                editor.apply();
            }

            @SuppressLint("MissingPermission")
            @Override
            public void afterTextChanged(Editable editable) {
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);

                float searchRadius = sharedPref.getFloat(PREFS_LOCATION_RADIUS_KEY, LOCATION_DEFAULT_RADIUS);
                float dwellTimeSec = sharedPref.getFloat(PREFS_LOCATION_RADIUS_KEY, DWELL_DEFAULT_TIME);

                for (Note n : Singleton.getInstance().getNotepad().getNotes()) {
                    ArrayList<LatLng> keywordsLocation = n.getKeywordsLocation();
                    if (!keywordsLocation.isEmpty()) {
                        for (LatLng latLng : keywordsLocation) {
                            commonMethods.registerFence(getApplicationContext(), FENCE_KEY_LOCATION + n.getId() + latLng.toString(),
                                    LocationFence.in(latLng.latitude, latLng.longitude, searchRadius, (long) dwellTimeSec * 1000));
                        }
                    }
                }
            }
        });

        Switch sw_headphoneFence = findViewById(R.id.switch_headphoneFence);
        sw_headphoneFence.setChecked(sharedPref.getBoolean(HEADPHONE_FENCE_STATE, false));
        sw_headphoneFence.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences sharedPref = AwarenessApiSettingsActivity.this.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
                sharedPref.edit().putBoolean(HEADPHONE_FENCE_STATE, b).apply();

                if (b) {
                    commonMethods.registerFence(getApplicationContext(), FENCE_KEY_HEADPHONES_PLUGGING, HeadphoneFence.pluggingIn());
                    commonMethods.registerFence(getApplicationContext(), FENCE_KEY_HEADPHONES_UNPLUGGING, HeadphoneFence.unplugging());
                } else {
                    commonMethods.unregisterFence(getApplicationContext(), FENCE_KEY_HEADPHONES_PLUGGING);
                    commonMethods.unregisterFence(getApplicationContext(), FENCE_KEY_HEADPHONES_UNPLUGGING);
                }
            }
        });

        Switch sw_locationFence = findViewById(R.id.switch_locationFence);
        sw_locationFence.setChecked(sharedPref.getBoolean(LOCATION_FENCE_STATE, false));
        sw_locationFence.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
                sharedPref.edit().putBoolean(LOCATION_FENCE_STATE, b).apply();

                float searchRadius = sharedPref.getFloat(PREFS_LOCATION_RADIUS_KEY, LOCATION_DEFAULT_RADIUS);
                float dwellTimeSec = sharedPref.getFloat(PREFS_DWELL_TIME_KEY, DWELL_DEFAULT_TIME);

                for (Note n : Singleton.getInstance().getNotepad().getNotes()) {
                    ArrayList<LatLng> keywordsLocation = n.getKeywordsLocation();
                    if (b && !keywordsLocation.isEmpty()) {
                        for (LatLng latLng : keywordsLocation) {
                            commonMethods.registerFence(getApplicationContext(), FENCE_KEY_LOCATION + n.getId() + latLng.toString(),
                                    LocationFence.in(latLng.latitude, latLng.longitude, searchRadius, (long) dwellTimeSec * 1000));
                        }
                    } else if (!b && !keywordsLocation.isEmpty()) {
                        for (LatLng latLng : keywordsLocation) {
                            commonMethods.unregisterFence(getApplicationContext(), FENCE_KEY_LOCATION + n.getId() + latLng.toString());
                        }
                    }
                }
            }
        });

        Switch sw_activityFence = findViewById(R.id.switch_activityFence);
        sw_activityFence.setChecked(sharedPref.getBoolean(ACTIVITY_FENCE_STATE, false));
        sw_activityFence.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
                sharedPref.edit().putBoolean(ACTIVITY_FENCE_STATE, b).apply();

                if (b) {
                    commonMethods.registerFence(getApplicationContext(), FENCE_KEY_ACTIVITY_IN_VEHICLE, DetectedActivityFence.starting(DetectedActivityFence.IN_VEHICLE));
                    commonMethods.registerFence(getApplicationContext(), FENCE_KEY_ACTIVITY_ON_BICYCLE, DetectedActivityFence.starting(DetectedActivityFence.ON_BICYCLE));
                    commonMethods.registerFence(getApplicationContext(), FENCE_KEY_ACTIVITY_ON_FOOT, DetectedActivityFence.starting(DetectedActivityFence.ON_FOOT));
                    commonMethods.registerFence(getApplicationContext(), FENCE_KEY_ACTIVITY_RUNNING, DetectedActivityFence.starting(DetectedActivityFence.RUNNING));
                    commonMethods.registerFence(getApplicationContext(), FENCE_KEY_ACTIVITY_STILL, DetectedActivityFence.starting(DetectedActivityFence.STILL));
                    commonMethods.registerFence(getApplicationContext(), FENCE_KEY_ACTIVITY_WALKING, DetectedActivityFence.starting(DetectedActivityFence.WALKING));
                } else {
                    commonMethods.unregisterFence(getApplicationContext(), FENCE_KEY_ACTIVITY_IN_VEHICLE);
                    commonMethods.unregisterFence(getApplicationContext(), FENCE_KEY_ACTIVITY_ON_BICYCLE);
                    commonMethods.unregisterFence(getApplicationContext(), FENCE_KEY_ACTIVITY_ON_FOOT);
                    commonMethods.unregisterFence(getApplicationContext(), FENCE_KEY_ACTIVITY_RUNNING);
                    commonMethods.unregisterFence(getApplicationContext(), FENCE_KEY_ACTIVITY_STILL);
                    commonMethods.unregisterFence(getApplicationContext(), FENCE_KEY_ACTIVITY_WALKING);
                }
            }
        });

        Switch sw_timeFence = findViewById(R.id.switch_timeFence);
        sw_timeFence.setChecked(sharedPref.getBoolean(TIME_FENCE_STATE, false));
        sw_timeFence.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
                sharedPref.edit().putBoolean(TIME_FENCE_STATE, b).apply();

                if (b) {
                    commonMethods.registerFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_WEEKDAY, TimeFence.inTimeInterval(TimeFence.TIME_INTERVAL_WEEKDAY));
                    commonMethods.registerFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_WEEKEND, TimeFence.inTimeInterval(TimeFence.TIME_INTERVAL_WEEKEND));
                    commonMethods.registerFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_HOLIDAY, TimeFence.inTimeInterval(TimeFence.TIME_INTERVAL_HOLIDAY));
                    commonMethods.registerFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_MORNING, TimeFence.inTimeInterval(TimeFence.TIME_INTERVAL_MORNING));
                    commonMethods.registerFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_AFTERNOON, TimeFence.inTimeInterval(TimeFence.TIME_INTERVAL_AFTERNOON));
                    commonMethods.registerFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_EVENING, TimeFence.inTimeInterval(TimeFence.TIME_INTERVAL_EVENING));
                    commonMethods.registerFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_NIGHT, TimeFence.inTimeInterval(TimeFence.TIME_INTERVAL_NIGHT));
                }else{
                    commonMethods.unregisterFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_WEEKDAY);
                    commonMethods.unregisterFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_WEEKEND);
                    commonMethods.unregisterFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_HOLIDAY);
                    commonMethods.unregisterFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_MORNING);
                    commonMethods.unregisterFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_AFTERNOON);
                    commonMethods.unregisterFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_EVENING);
                    commonMethods.unregisterFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_NIGHT);
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        getMenuInflater().inflate(R.menu.menu_awareness_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_map_item:
                Intent i = new Intent(AwarenessApiSettingsActivity.this, MapsActivity.class);
                i.putExtra("from", "AwarenessSettings");
                startActivity(i);
                break;

            case R.id.menu_import_file:


                //Permission Not Granted
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(AwarenessApiSettingsActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_IMPORT_FILE);
                    return true;
                }

                //Permission Already Granted
                importCsvFile();
                break;

            case R.id.menu_export_file:
                if(Singleton.getInstance().getNotepad().getNotes().isEmpty()){
                    Toast.makeText(this, "There are no notes yet.", Toast.LENGTH_SHORT).show();
                    return true;
                }

                //Permission Not Granted
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(AwarenessApiSettingsActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_EXPORT_FILE);
                    return true;
                }

                //Permission Already Granted
                exportCsvFile(EXPORT_FILE_FOLDER);
                break;

            case R.id.menu_import_bluetooth:
                createBluetoothDialog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void createBluetoothDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AwarenessApiSettingsActivity.this);
        LayoutInflater inflater = this.getLayoutInflater();
        this.dialogViewBluetooth = inflater.inflate(R.layout.bluetooth_dialog, null);
        dialogBuilder.setView(this.dialogViewBluetooth);
        dialogBuilder.setTitle("Bluetooth");
        AlertDialog alertDialog = dialogBuilder.create();

        Switch switch_bluetoothAccept = this.dialogViewBluetooth.findViewById(R.id.switch_bluetoothAccept);

        final Spinner spinner_bluetoothDevices = this.dialogViewBluetooth.findViewById(R.id.spinner_bluetoothDevicesToSend);
        Button btn_bluetoothSend = this.dialogViewBluetooth.findViewById(R.id.btn_bluetoothSend);

        ArrayAdapter<String> spinner_bluetoothAdapter = new ArrayAdapter<>(AwarenessApiSettingsActivity.this,
                android.R.layout.simple_list_item_1, Bluetooth.getBoundedDevicesNames());
        spinner_bluetoothDevices.setAdapter(spinner_bluetoothAdapter);

        switch_bluetoothAccept.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    if(Bluetooth.bluetoothAdapter!=null){

                        bluetoothServer(compoundButton);

                    }else{
                        requestBluetoothEnable();
                        compoundButton.setChecked(false);
                    }
                }else{
                    if (Bluetooth.bluetoothServerSocket != null) {
                        try {
                            Bluetooth.bluetoothServerSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(AwarenessApiSettingsActivity.this, "Error closing BluetoothServerSocket.", Toast.LENGTH_SHORT).show();
                        }

                        Bluetooth.bluetoothServerSocket = null;
                        Toast.makeText(AwarenessApiSettingsActivity.this, "Bluetooth Server Socket closed.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AwarenessApiSettingsActivity.this, "Error closing BluetoothServerSocket.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btn_bluetoothSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Bluetooth.bluetoothAdapter==null || !Bluetooth.bluetoothAdapter.isEnabled()){
                    requestBluetoothEnable();
                }else{
                    BluetoothDevice selectedDevice = Bluetooth.getBluetoothDevice(spinner_bluetoothDevices.getSelectedItem().toString());
                    Log.i(TAG_BLUETOOTH, "SELECTED DEVICE: Name="+selectedDevice.getName()+"; ID="+selectedDevice.getAddress());

                    bluetoothSendData(selectedDevice);
                }
            }
        });

        if(!alertDialog.isShowing())
            alertDialog.show();
    }

    @SuppressLint("StaticFieldLeak")
    private void bluetoothServer(final CompoundButton compoundButton){

        new Bluetooth.Server(){
            @Override
            protected void onPostExecute(Object o) {

                if (Bluetooth.bluetoothServerSocket != null) {
                    try {
                        Bluetooth.bluetoothServerSocket.close();
                        compoundButton.setChecked(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(AwarenessApiSettingsActivity.this, "Bluetooth Server error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                Bluetooth.bluetoothServerSocket = null;

                if(o!=null){
                    try{
                        Notepad receivedNotepad = (Notepad) o;

                        for (Note n : receivedNotepad.getNotes()){
                            Singleton.getInstance().getNotepad().addNote(n);
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                        Toast.makeText(AwarenessApiSettingsActivity.this, "Error while decoding the received notepad.", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(AwarenessApiSettingsActivity.this, "Error while decoding the received notepad.", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void bluetoothSendData(BluetoothDevice selectedDevice) {
        new Bluetooth.SendData(selectedDevice){
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                Toast.makeText(AwarenessApiSettingsActivity.this, "Data sent successful!", Toast.LENGTH_SHORT).show();
            }
        }.execute(Singleton.getInstance().getNotepad());
    }

    private void requestBluetoothEnable(){
        if (Bluetooth.bluetoothAdapter == null) {
            Toast.makeText(AwarenessApiSettingsActivity.this, "Bluetooth is not available.", Toast.LENGTH_SHORT).show();
        } else if (!Bluetooth.bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        }
    }

    private void exportCsvFile(String saveEnvironment) {
        String fileNameComplete = "AlzheimerNotes_Backup_"+(new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()))+".cvs";

        if(new CSVFile().exportNotes(saveEnvironment, fileNameComplete)){
            Toast.makeText(this, "Notes exported with success to "+EXPORT_FILE_FOLDER+" folder.", Toast.LENGTH_SHORT).show();

            // initiate media scan and put the new things into the path array to
            // make the scanner aware of the location and the files you want to see
            MediaScannerConnection.scanFile(getApplicationContext() , new String[] {saveEnvironment}, null, null);
        }else{
            Toast.makeText(this, "Something went wrong while exporting notes to "+EXPORT_FILE_FOLDER+". Check the App permissions", Toast.LENGTH_SHORT).show();
        }
    }

    private void importCsvFile() {
        //Open transfer folder in file explorer
        //From: https://developer.android.com/guide/topics/providers/document-provider.html

        Intent fileExplorerIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        //All files types
        //fileExplorerIntent.setType("*/*");
        fileExplorerIntent.setDataAndType(FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName()+".fileprovider",
                Environment.getExternalStoragePublicDirectory(EXPORT_FILE_FOLDER)),
                "*/*");
        try {
            startActivityForResult(fileExplorerIntent, REQUEST_CODE_PICK_CSV_FILE);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG_PICK_FIlE_NO_HANDLER, "No activity can handle picking a file. Showing alternatives.");
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void contextRefresh() {

        new CurrentContext(){
            @Override
            protected void onPreExecute() {

                ProgressBar progressBar = findViewById(R.id.progressBar_contextRefresh);
                TextView tv_currentProgress = findViewById(R.id.progressBar_Text2);
                TextView tv_currentStats = findViewById(R.id.textView_actualStats);
                tv_currentStats.setText("");
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
                tv_currentProgress.setVisibility(View.VISIBLE);
                tv_currentProgress.setText("Progress: 0%");
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                if(aBoolean.equals(Boolean.TRUE)){
                    ProgressBar progressBar = findViewById(R.id.progressBar_contextRefresh);
                    TextView tv_currentProgress = findViewById(R.id.progressBar_Text2);
                    progressBar.setVisibility(View.GONE);
                    tv_currentProgress.setVisibility(View.GONE);
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                TextView tv_actualStats = findViewById(R.id.textView_actualStats);

                tv_actualStats.setText("");
                if (Singleton.getInstance().getHeadphonesState() != null)
                    tv_actualStats.append("\n_______\nHeadphones: "+commonMethods.decodeHeadphonesState(Singleton.getInstance().getHeadphonesState().getState()));
                if (Singleton.getInstance().getWeather() != null)
                    tv_actualStats.append("\n_______\nWeather: "+Singleton.getInstance().getWeather().toString());
                if (Singleton.getInstance().getLocation() != null)
                    tv_actualStats.append("\n_______\nLocation: "+Singleton.getInstance().getLocation().toString());
                if (Singleton.getInstance().getActivityRecognitionResult() != null)
                    tv_actualStats.append("\n_______\nUser Activity: "+Singleton.getInstance().getActivityRecognitionResult().toString());
                if (Singleton.getInstance().getPlaces() != null)
                    tv_actualStats.append("\n_______\nPlaces: "+Singleton.getInstance().getPlaces().toString());
                if (Singleton.getInstance().getTimeInterval() != null)
                    tv_actualStats.append("\n_______\nTimeIntervals: "+commonMethods.decodeTimeIntervals(Singleton.getInstance().getTimeInterval().getTimeIntervals()));


                ProgressBar progressBar = findViewById(R.id.progressBar_contextRefresh);
                progressBar.setProgress(values[0]);
                TextView tv_currentProgress = findViewById(R.id.progressBar_Text2);
                tv_currentProgress.setText("Progress: "+values[0]+"%");
            }
        }.execute(getApplicationContext());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
         switch(requestCode){
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    contextRefresh();
                } else {
                    Log.w(TAG_REQUEST_PERMISSION_RESULT, "PERMISSION FINE LOCATION STORAGE NOT GRANTED BY USER");
                    Toast.makeText(this, "Unable to get context updates, pls grant app permissions", Toast.LENGTH_SHORT).show();
                }
            break;
             case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_IMPORT_FILE:
                 // If request is cancelled, the result arrays are empty.
                 if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                     importCsvFile();
                 } else {
                     Log.w(TAG_REQUEST_PERMISSION_RESULT, "PERMISSION WRITE EXTERNAL STORAGE NOT GRANTED BY USER");
                     Toast.makeText(this, "Unable to import file. Please grant the app permissions.", Toast.LENGTH_SHORT).show();
                 }
             break;
             case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_EXPORT_FILE:
                 // If request is cancelled, the result arrays are empty.
                 if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                     exportCsvFile(EXPORT_FILE_FOLDER);
                 } else {
                     Log.w(TAG_REQUEST_PERMISSION_RESULT, "PERMISSION WRITE EXTERNAL STORAGE NOT GRANTED BY USER");
                     Toast.makeText(this, "Unable to export file. Please grant the app permissions.", Toast.LENGTH_SHORT).show();
                 }
             break;
        }
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

        //Register Fences Receiver
        registerReceiver(commonMethods.myFenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK && requestCode == REQUEST_CODE_PICK_CSV_FILE && data!=null){
            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                new CSVFile().importNotes(inputStream);
            } catch (FileNotFoundException e) {
                Toast.makeText(this, "Something went wrong while decoding the file. Be sure that the file is the correct!", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }else if(resultCode==RESULT_OK && requestCode == REQUEST_ENABLE_BLUETOOTH){
            Toast.makeText(AwarenessApiSettingsActivity.this, "Bluetooth enabled successfully: " + Bluetooth.bluetoothAdapter.getName(), Toast.LENGTH_SHORT).show();

            //Update Spinner List
            Spinner spinner_bluetoothDevices = this.dialogViewBluetooth.findViewById(R.id.spinner_bluetoothDevicesToSend);

            ArrayAdapter<String> spinner_bluetoothAdapter = new ArrayAdapter<>(AwarenessApiSettingsActivity.this,
                    android.R.layout.simple_list_item_1, Bluetooth.getBoundedDevicesNames());
            spinner_bluetoothDevices.setAdapter(spinner_bluetoothAdapter);
        }
    }

}
