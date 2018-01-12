package pt.ipleiria.notepadv2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.fence.TimeFence;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import pt.ipleiria.notepadv2.model.AppConstants;
import pt.ipleiria.notepadv2.model.CloudVision;
import pt.ipleiria.notepadv2.model.CurrentContext;
import pt.ipleiria.notepadv2.model.CustomAdapter;
import pt.ipleiria.notepadv2.model.DatePickerFragment;
import pt.ipleiria.notepadv2.model.GetPlacePhoto;
import pt.ipleiria.notepadv2.model.Note;
import pt.ipleiria.notepadv2.model.Notepad;
import pt.ipleiria.notepadv2.model.OnSwipeTouchListener;
import pt.ipleiria.notepadv2.model.ReverseGeocodingTask;
import pt.ipleiria.notepadv2.model.Singleton;
import pt.ipleiria.notepadv2.model.commonMethods;


public class MainActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, AppConstants {

    //private Notepad notepad;
    private CustomAdapter adapter;
    private View mSearchEditFrame;
    private boolean sview_state;
    private ArrayList<Note> notesfiltered = new ArrayList<>();
    private String stringToSearchFor = "";
    private ListView listView;
    private boolean[] selectedSearchF = {true, false, false, false, false, false, false, false, false};

    //2nd Part
    private GoogleApiClient mGoogleApiClient;

    Intent i;
    private String dateStart, dateEnd;
    private TextView tv_dateStart, tv_dateEnd;
    private boolean selectingStartDate;

    private View dialogView;

    //Async task are class fields because we need to check if the task are running on multiple occasions.
    AsyncTask currentContextTask = new CurrentContext();
    private int progressCurrentContextTask;
    AsyncTask cloudVisionTask = new CloudVision();
    private int progressCloudVisionTask;

    //Cloud Vision
    Uri photoURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar mainToolbar = findViewById(R.id.my_toolbar_main);
        setSupportActionBar(mainToolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(null);

        //Register API
        this.mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                //TODO Time Interval API Works very poorly on some devices :'(
                //.addApi(Awareness.API)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toast.makeText(MainActivity.this, "Connection to Google API Client Failed. Error Number: " + connectionResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                        Log.e("CONNECTION ERROR", "Connection to Google API Client Failed. Error Number: " + connectionResult.getErrorCode());
                    }
                })
                .build();
        this.mGoogleApiClient.connect();

        //load saved file of notes
        try {
            FileInputStream fileInputStream = openFileInput(SAVE_FILE_NAME);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Singleton.getInstance().setNotepad((Notepad) objectInputStream.readObject());

            objectInputStream.close();
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this,
                    R.string.unableToOpenSaveFile,
                    Toast.LENGTH_LONG).show();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, R.string.errorReadingSaveFile, Toast.LENGTH_LONG).show();
        }

        if (savedInstanceState != null) {
            this.selectedSearchF[0] = savedInstanceState.getBoolean("filter1", false);
            this.selectedSearchF[1] = savedInstanceState.getBoolean("filter2", false);
            this.selectedSearchF[2] = savedInstanceState.getBoolean("filter3", false);
            this.selectedSearchF[3] = savedInstanceState.getBoolean("filter4", false);
            this.selectedSearchF[4] = savedInstanceState.getBoolean("filter5", false);
            this.selectedSearchF[5] = savedInstanceState.getBoolean("filter6", false);
            this.selectedSearchF[6] = savedInstanceState.getBoolean("filter7", false);
            this.selectedSearchF[7] = savedInstanceState.getBoolean("filter8", false);
            this.selectedSearchF[8] = savedInstanceState.getBoolean("filter9", false);
            this.stringToSearchFor = savedInstanceState.getString("search_text", "");
            this.sview_state = savedInstanceState.getBoolean("sview_state", false);
            this.dateStart = savedInstanceState.getString("dateStart", null);
            this.dateEnd = savedInstanceState.getString("dateEnd", null);
        }

        adapter = new CustomAdapter(getApplicationContext(), Singleton.getInstance().getNotepad().getNotes());
        listView = findViewById(R.id.listView_notes);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MainActivity.this.i = new Intent(MainActivity.this, AddActivity.class);
                Note n = (Note) parent.getItemAtPosition(position);
                MainActivity.this.i.putExtra(INTENT_EXTRA_NOTE, n);

                //Permission Not Granted
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                    return;
                }

                //Permission Already Granted
                startActivity(MainActivity.this.i);

            }

        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // Use the Builder class for convenient dialog construction
                final Note n = (Note) parent.getItemAtPosition(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                String str = String.format(getString(R.string.noteDeleteMessage), n.getTitle(), n.getCreation_date());
                builder.setMessage(str)
                        .setPositiveButton(getString(R.string.dialogPositiveButtonText), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                //TODO Check Un Registering Location fences on delete note with keywords #location:lat, lng.
                                if(!n.getKeywordsLocation().isEmpty()){
                                    for(LatLng latLng : n.getKeywordsLocation()){
                                        commonMethods.unregisterFence(getApplicationContext(), FENCE_KEY_LOCATION+n.getId()+latLng.toString());
                                    }
                                }

                                Singleton.getInstance().getNotepad().removeNote(n);
                                notesfiltered.remove(n);
                                adapter.notifyDataSetChanged();
                                Toast.makeText(MainActivity.this, R.string.noteDeletedSucessToast, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(getString(R.string.dialogNegativeButtonText), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                // Create the AlertDialog object and return it
                builder.show();
                return true;
            }
        });

        currentContextTask.cancel(true);
        cloudVisionTask.cancel(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Register Fence Receiver
        registerReceiver(commonMethods.myFenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
        boolean headphonesFenceStatus = sharedPref.getBoolean(HEADPHONE_FENCE_STATE, false);
        boolean locationFenceStatus = sharedPref.getBoolean(LOCATION_FENCE_STATE, false);
        boolean activityFenceStatus = sharedPref.getBoolean(ACTIVITY_FENCE_STATE, false);
        boolean timeFenceStatus = sharedPref.getBoolean(TIME_FENCE_STATE, false);

        //Register HEADPHONES Fence is the corresponding Switch on Settings Activity is enabled
        if (headphonesFenceStatus) {
            commonMethods.registerFence(getApplicationContext(), FENCE_KEY_HEADPHONES_PLUGGING, HeadphoneFence.pluggingIn());
            commonMethods.registerFence(getApplicationContext(), FENCE_KEY_HEADPHONES_UNPLUGGING, HeadphoneFence.unplugging());
        }

        //Register LOCATION Fence is the corresponding Switch on Settings Activity is enabled
        if (locationFenceStatus) {
            //Permission Not Granted -> Ask For It
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_FINE_LOCATION_REGISTER_FENCE);
            } else {
                //Permission Already Granted
                this.registerLocationFenceStartup();
            }
        }

        //Register ACTIVITY Fence is the corresponding Switch on Settings Activity is enabled
        if(activityFenceStatus){
            commonMethods.registerFence(getApplicationContext(), FENCE_KEY_ACTIVITY_IN_VEHICLE, DetectedActivityFence.starting(DetectedActivityFence.IN_VEHICLE));
            commonMethods.registerFence(getApplicationContext(), FENCE_KEY_ACTIVITY_ON_BICYCLE, DetectedActivityFence.starting(DetectedActivityFence.ON_BICYCLE));
            commonMethods.registerFence(getApplicationContext(), FENCE_KEY_ACTIVITY_ON_FOOT, DetectedActivityFence.starting(DetectedActivityFence.ON_FOOT));
            commonMethods.registerFence(getApplicationContext(), FENCE_KEY_ACTIVITY_RUNNING, DetectedActivityFence.starting(DetectedActivityFence.RUNNING));
            commonMethods.registerFence(getApplicationContext(), FENCE_KEY_ACTIVITY_STILL, DetectedActivityFence.starting(DetectedActivityFence.STILL));
            commonMethods.registerFence(getApplicationContext(), FENCE_KEY_ACTIVITY_WALKING, DetectedActivityFence.starting(DetectedActivityFence.WALKING));
        }

        //Register TIME Fence is the corresponding Switch on Settings Activity is enabled
        if(timeFenceStatus){
            commonMethods.registerFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_WEEKDAY, TimeFence.inTimeInterval(TimeFence.TIME_INTERVAL_WEEKDAY));
            commonMethods.registerFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_WEEKEND, TimeFence.inTimeInterval(TimeFence.TIME_INTERVAL_WEEKEND));
            commonMethods.registerFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_HOLIDAY, TimeFence.inTimeInterval(TimeFence.TIME_INTERVAL_HOLIDAY));
            commonMethods.registerFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_MORNING, TimeFence.inTimeInterval(TimeFence.TIME_INTERVAL_MORNING));
            commonMethods.registerFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_AFTERNOON, TimeFence.inTimeInterval(TimeFence.TIME_INTERVAL_AFTERNOON));
            commonMethods.registerFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_EVENING, TimeFence.inTimeInterval(TimeFence.TIME_INTERVAL_EVENING));
            commonMethods.registerFence(getApplicationContext(), AppConstants.FENCE_KEY_TIME_INTERVAL_NIGHT, TimeFence.inTimeInterval(TimeFence.TIME_INTERVAL_NIGHT));

        }


        //Show the interface layout
        if(Singleton.getInstance().getHeadphonesState()!=null || Singleton.getInstance().getWeather()!=null || Singleton.getInstance().getLocation()!=null ||
                Singleton.getInstance().getActivityRecognitionResult()!=null || Singleton.getInstance().getPlaces()!=null || Singleton.getInstance().getTimeInterval()!=null){
            ConstraintLayout layout_currentContextStats = findViewById(R.id.constraintLayout_currentStatsInterface);
            layout_currentContextStats.setVisibility(View.VISIBLE);

            updateCurrentContextMainInterface();
        }

    }

    /**
     * Register All Location Fences based on Notes Keywords
     */
    @SuppressLint("MissingPermission")
    private void registerLocationFenceStartup() {

        int gpsActive;
        try {
            gpsActive = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);

            //GPS Not Enable Check
            if(gpsActive==0){
                //TODO: Ask for user enable GPS
                //Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                //startActivity(onGPS);

                Toast.makeText(MainActivity.this, "You must activate your GPS to get better results!", Toast.LENGTH_LONG).show();
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);

        float searchRadius = sharedPref.getFloat(PREFS_LOCATION_RADIUS_KEY, LOCATION_DEFAULT_RADIUS);
        float dwellTimeSec = sharedPref.getFloat(PREFS_DWELL_TIME_KEY, DWELL_DEFAULT_TIME);

        for (Note n : Singleton.getInstance().getNotepad().getNotes()) {
            if (!n.getKeywordsLocation().isEmpty()) {
                for (LatLng latLng : n.getKeywordsLocation()) {
                    commonMethods.registerFence(getApplicationContext(), FENCE_KEY_LOCATION + n.getId() + latLng.toString(),
                            LocationFence.in(latLng.latitude, latLng.longitude, searchRadius, (long) dwellTimeSec * 1000));
                }
            }
        }
    }

    /**
     * method to update listview for example when some search filters are selected and don't
     * require text input the listview can be updated right away calling this method
     */
    private void showList() {

        //Default Filter Value = Search by Title
        if (!(this.selectedSearchF[0] || this.selectedSearchF[1] || this.selectedSearchF[2] || this.selectedSearchF[3] ||
            this.selectedSearchF[4] || this.selectedSearchF[5] || this.selectedSearchF[6] || this.selectedSearchF[7] || selectedSearchF[8])){
            this.selectedSearchF[0] = true;
        }

        if (selectedSearchF[4] || selectedSearchF[5] || (selectedSearchF[6] && dateStart != null && dateEnd != null) || selectedSearchF[7] || selectedSearchF[8]) {
            boolean[] temp = {false, false, false, false, selectedSearchF[4], selectedSearchF[5], selectedSearchF[6], selectedSearchF[7], selectedSearchF[8]};
            notesfiltered = Singleton.getInstance().getNotepad().searchNotes(getApplicationContext(), temp, null, this.dateStart, this.dateEnd);
            adapter = new CustomAdapter(MainActivity.this, notesfiltered);
            listView.setAdapter(adapter);
        } else {
            if(adapter!=null && listView!=null){
                // sem texto mostra tudo
                adapter = new CustomAdapter(MainActivity.this, Singleton.getInstance().getNotepad().getNotes());
                listView.setAdapter(adapter);
            }
        }

    }

    private void searchNotes(String text) {
        notesfiltered = Singleton.getInstance().getNotepad().searchNotes(getApplicationContext(), this.selectedSearchF, text, this.dateStart, this.dateEnd);
    }

    /**
     * The dialog with the available filters and the settings button
     */
    private void filterSelectDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = this.getLayoutInflater();
        this.dialogView = inflater.inflate(R.layout.filter_dialog, null);
        dialogBuilder.setView(this.dialogView);
        dialogBuilder.setTitle("Choose a filter type");
        final AlertDialog alertDialog = dialogBuilder.create();

        //Makes the dialog not to cancel when user touch outside screen
        alertDialog.setCanceledOnTouchOutside(false);

        //Copy main array to a auxiliary array
        final boolean[] aux = MainActivity.this.selectedSearchF;

        //Default value of the Start and End Date
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        final Date now = Calendar.getInstance().getTime();
        if (this.dateStart == null) this.dateStart = dateFormat.format(now);
        if (this.dateEnd == null) this.dateEnd = dateFormat.format(now);

        //Get the dialog elements
        CheckBox checkbox1 = dialogView.findViewById(R.id.checkBox);
        CheckBox checkbox2 = dialogView.findViewById(R.id.checkBox2);
        CheckBox checkbox3 = dialogView.findViewById(R.id.checkBox3);
        CheckBox checkbox4 = dialogView.findViewById(R.id.checkBox4);
        CheckBox checkbox5 = dialogView.findViewById(R.id.checkBox5);
        CheckBox checkbox6 = dialogView.findViewById(R.id.checkBox6);
        final CheckBox checkbox7 = dialogView.findViewById(R.id.checkBox7);
        CheckBox checkbox8 = dialogView.findViewById(R.id.checkBox8);
        CheckBox checkbox9 = dialogView.findViewById(R.id.dialog_checkBox9);

        tv_dateStart = dialogView.findViewById(R.id.textView_dateStart);
        final TextView tv_dateSeparator = dialogView.findViewById(R.id.textView_dateSeparate);
        tv_dateEnd = dialogView.findViewById(R.id.textView_dateEnd);
        final FloatingActionButton btn_refresh = dialogView.findViewById(R.id.floatingActionButton_refresh);
        final FloatingActionButton btn_settings = dialogView.findViewById(R.id.floatingActionButton_settings);
        final FloatingActionButton btn_takePhoto = dialogView.findViewById(R.id.floatingActionButton_camera);
        Button btn_positive = dialogView.findViewById(R.id.button_positive);
        Button btn_negative = dialogView.findViewById(R.id.button_negative);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar_contextRefresh);
        TextView tv_currentProgress = dialogView.findViewById(R.id.progressBar_Text);

        if(currentContextTask.getStatus()!= AsyncTask.Status.FINISHED){
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(this.progressCurrentContextTask);
            tv_currentProgress.setText("Progress: "+this.progressCurrentContextTask+"%");
            btn_refresh.setImageResource(R.drawable.ic_action_cross);
        }else if(cloudVisionTask.getStatus()!= AsyncTask.Status.FINISHED){
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(this.progressCloudVisionTask);
            tv_currentProgress.setText("Progress: "+this.progressCloudVisionTask+"%");
            btn_takePhoto.setImageResource(R.drawable.ic_action_cross);
        }else{
            progressBar.setVisibility(View.GONE);
            tv_currentProgress.setVisibility(View.GONE);
            tv_currentProgress.setText("");
        }

        //Set the elements text and status
        checkbox1.setText(getString(R.string.filterTitle));
        checkbox1.setChecked(this.selectedSearchF[0]);
        checkbox2.setText(getString(R.string.filterKeywords));
        checkbox2.setChecked(this.selectedSearchF[1]);
        checkbox3.setText(getString(R.string.filterBody));
        checkbox3.setChecked(this.selectedSearchF[2]);
        checkbox4.setText(getString(R.string.filterID));
        checkbox4.setChecked(this.selectedSearchF[3]);
        checkbox5.setText(getString(R.string.filterImage));
        checkbox5.setChecked(this.selectedSearchF[4]);
        checkbox6.setText(getString(R.string.filterVideo));
        checkbox6.setChecked(this.selectedSearchF[5]);
        checkbox7.setText(getString(R.string.filterDate));
        checkbox7.setChecked(this.selectedSearchF[6]);
        checkbox8.setText("Current Context Update");
        checkbox8.setChecked(this.selectedSearchF[7]);
        checkbox9.setText("Search Notes by Photo");
        checkbox9.setChecked(this.selectedSearchF[8]);

        //Check if the date filter is active on user click on filters icon
        if (this.selectedSearchF[6]) {
            tv_dateStart.setVisibility(View.VISIBLE);
            tv_dateSeparator.setVisibility(View.VISIBLE);
            tv_dateEnd.setVisibility(View.VISIBLE);

            this.tv_dateStart.setText(this.dateStart);
            this.tv_dateEnd.setText(this.dateEnd);
        } else {
            tv_dateStart.setVisibility(View.GONE);
            tv_dateSeparator.setVisibility(View.GONE);
            tv_dateEnd.setVisibility(View.GONE);
        }

        if(this.selectedSearchF[8]) btn_takePhoto.setVisibility(View.VISIBLE);
        else btn_takePhoto.setVisibility(View.GONE);

        checkbox1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                aux[0] = isChecked;
            }
        });
        checkbox2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                aux[1] = isChecked;
            }
        });
        checkbox3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                aux[2] = isChecked;
            }
        });
        checkbox4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                aux[3] = isChecked;
            }
        });
        checkbox5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                aux[4] = isChecked;
            }
        });
        checkbox6.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                aux[5] = isChecked;
            }
        });
        //Check for change on date filter checkbox
        checkbox7.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                aux[6] = isChecked;
                if (isChecked) {
                    tv_dateStart.setVisibility(View.VISIBLE);
                    tv_dateSeparator.setVisibility(View.VISIBLE);
                    tv_dateEnd.setVisibility(View.VISIBLE);

                    if (MainActivity.this.dateStart == null)
                        MainActivity.this.tv_dateStart.setText(dateFormat.format(now));
                    else tv_dateStart.setText(MainActivity.this.dateStart);

                    if (MainActivity.this.dateEnd == null)
                        MainActivity.this.tv_dateEnd.setText(dateFormat.format(now));
                    else tv_dateEnd.setText(MainActivity.this.dateEnd);

                } else {
                    tv_dateStart.setVisibility(View.GONE);
                    tv_dateSeparator.setVisibility(View.GONE);
                    tv_dateEnd.setVisibility(View.GONE);
                }
            }
        });

        tv_dateStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dateStartFragment = new DatePickerFragment();
                dateStartFragment.show(getFragmentManager(), "Date Picker");
                //To know that the date picked is the start date.
                MainActivity.this.selectingStartDate = true;
            }
        });

        tv_dateEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dateEndFragment = new DatePickerFragment();
                dateEndFragment.show(getFragmentManager(), "Date Picker");
                //To know that the date picked is the end date.
                MainActivity.this.selectingStartDate = false;
            }
        });

        //Check if the context filter changing state
        checkbox8.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                aux[7] = isChecked;
                //Shows and Update the current Status tab interface, on the bottom screen
                if(isChecked){
                    if(Singleton.getInstance().getHeadphonesState()!=null || Singleton.getInstance().getWeather()!=null || Singleton.getInstance().getLocation()!=null ||
                            Singleton.getInstance().getActivityRecognitionResult()!=null || Singleton.getInstance().getPlaces()!=null || Singleton.getInstance().getTimeInterval()!=null){
                        ConstraintLayout layout_currentContextStats = findViewById(R.id.constraintLayout_currentStatsInterface);

                        layout_currentContextStats.setVisibility(View.VISIBLE);

                        updateCurrentContextMainInterface();
                    }else{
                        Toast.makeText(MainActivity.this, "No data available, you should refresh the context!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        checkbox9.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                aux[8] = isChecked;

                //Shows/Hide the camera button according to the check box status
                if(isChecked) btn_takePhoto.setVisibility(View.VISIBLE);
                else btn_takePhoto.setVisibility(View.GONE);
            }
        });

        btn_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Permission Not Granted -> Ask For It
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                    return;
                }

                int gpsActive;
                try {
                    gpsActive = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);

                    //GPS Not Enable Check
                    if(gpsActive==0){
                        //TODO: Ask for user enable GPS
                        //Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        //startActivity(onGPS);

                        Toast.makeText(MainActivity.this, "You must activate your GPS to get better results!", Toast.LENGTH_LONG).show();
                    }
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }

                //Permission Already Granted
                MainActivity.this.contextRefresh();
            }
        });

        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Permission Not Granted -> Ask For It
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_FINE_LOCATION_SETTINGS);
                    return;
                }

                int gpsActive;
                try {
                    gpsActive = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);

                    //GPS Not Enable Check
                    if(gpsActive==0){
                        //TODO: Ask for user enable GPS
                        //Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        //startActivity(onGPS);

                        Toast.makeText(MainActivity.this, "You must activate your GPS to get better results!", Toast.LENGTH_LONG).show();
                    }
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }

                //Permission Already Granted
                MainActivity.this.contextSettings();
            }
        });

        btn_takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Permission Not Granted
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_TAKE_PICTURE);
                    return;
                }

                //Permission Already Granted
                selectImage();
            }
        });

        btn_positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.selectedSearchF = aux;

                MainActivity.this.showList();
                alertDialog.dismiss();
            }
        });

        btn_negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.selectedSearchF[0] = true;
                for (int i = 1; i < MainActivity.this.selectedSearchF.length; i++) {
                    MainActivity.this.selectedSearchF[i] = false;
                }


                MainActivity.this.showList();
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

    /**
     * Callback Method  from Date Picker
     */
    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        //Do something with the date chosen by the user
        String s_day = "" + day;
        String s_month = "" + (month + 1);
        if (day < 10) s_day = "0" + day;
        if (month < 10) s_month = "0" + month;

        if (this.selectingStartDate) {
            this.dateStart = s_day + "/" + s_month + "/" + year;
            tv_dateStart.setText(dateStart);
        } else {
            this.dateEnd = s_day + "/" + s_month + "/" + year;
            tv_dateEnd.setText(dateEnd);
        }
    }

    /**
     * Context Refresh related Task and Code
     */
    @SuppressLint("StaticFieldLeak")
    private void contextRefresh() {
        //Cancel the current task if there is one running
        if(currentContextTask.getStatus()!= AsyncTask.Status.FINISHED){
            currentContextTask.cancel(true);

            if(currentContextTask.isCancelled()){
                ProgressBar progressBar_contextRefresh = MainActivity.this.dialogView.findViewById(R.id.progressBar_contextRefresh);
                TextView progressBar_text = dialogView.findViewById(R.id.progressBar_Text);
                FloatingActionButton refresh_btn = dialogView.findViewById(R.id.floatingActionButton_refresh);

                refresh_btn.setImageResource(R.drawable.ic_action_update);
                progressBar_contextRefresh.setVisibility(View.GONE);
                progressBar_text.setVisibility(View.GONE);
            }
        }else{
            currentContextTask = new CurrentContext(){
                @Override
                protected void onPreExecute() {
                    ProgressBar progressBar_contextRefresh = MainActivity.this.dialogView.findViewById(R.id.progressBar_contextRefresh);
                    TextView progressBar_text = dialogView.findViewById(R.id.progressBar_Text);

                    FloatingActionButton refresh_btn = dialogView.findViewById(R.id.floatingActionButton_refresh);
                    refresh_btn.setImageResource(R.drawable.ic_action_cross);
                    progressBar_contextRefresh.setVisibility(View.VISIBLE);
                    progressBar_contextRefresh.setProgress(0);
                    progressBar_text.setVisibility(View.VISIBLE);
                    progressBar_text.setText("Progress: 0%");
                }

                @Override
                protected void onPostExecute(Boolean aBoolean) {
                    if(aBoolean.equals(Boolean.TRUE)){
                        ProgressBar progressBar_contextRefresh = MainActivity.this.dialogView.findViewById(R.id.progressBar_contextRefresh);
                        TextView progressBar_text = dialogView.findViewById(R.id.progressBar_Text);

                        FloatingActionButton refresh_btn = dialogView.findViewById(R.id.floatingActionButton_refresh);
                        refresh_btn.setImageResource(R.drawable.ic_action_update);
                        progressBar_contextRefresh.setVisibility(View.GONE);
                        progressBar_text.setVisibility(View.GONE);
                    }

                    createNewNoteForContext();
                }

                @Override
                protected void onProgressUpdate(Integer... values) {
                    showList();

                    updateCurrentContextMainInterface();

                    ProgressBar progressBar_contextRefresh = MainActivity.this.dialogView.findViewById(R.id.progressBar_contextRefresh);
                    TextView progressBar_text = dialogView.findViewById(R.id.progressBar_Text);

                    MainActivity.this.progressCurrentContextTask = values[0];
                    progressBar_contextRefresh.setProgress(values[0]);
                    progressBar_text.setText("Progress: "+values[0]+"%");
                }
            }.execute(getApplicationContext());
        }
    }

    /**
     * Creates a new note suggestion based on current context
     */
    @SuppressLint("StaticFieldLeak")
    private void createNewNoteForContext() {
        //Inflate the default layout of the main notes list view.
        final ConstraintLayout layout_NewNote = findViewById(R.id.constraintLayout_innerNewNoteSuggestion);
        final ConstraintLayout innerArea = (ConstraintLayout) getLayoutInflater().inflate(R.layout.custom_adapter, layout_NewNote, false);

        //Array to store notes suggestions
        final ArrayList<Note> suggestedNotes = new ArrayList<>();

        //Removes the childes from the layout if this was some.
        if(layout_NewNote.getChildCount()>=1){
            layout_NewNote.removeAllViews();
        }

        if(layout_NewNote.getChildCount()==0){
            innerArea.findViewById(R.id.constraintLayout_customAdapter).setBackground(getDrawable(R.color.LightGreen));
            TextView temp_noteDate = innerArea.findViewById(R.id.textView_date);
            temp_noteDate.setVisibility(View.GONE);

            layout_NewNote.addView(innerArea);
        }

        //Get suggestion note based on location if there is no note with the suggested title
        if(Singleton.getInstance().getLocation()!=null){
            new ReverseGeocodingTask(){
                @Override
                protected void onPostExecute(String s) {
                    //No notes yet with the proposed title(address) = create one suggestion
                    if(Singleton.getInstance().getNotepad().searchNotesByTitle(s).isEmpty()){
                        Note n = new Note(s,
                                LOCATION + Singleton.getInstance().getLocation().getLatitude()+", "
                                        +Singleton.getInstance().getLocation().getLongitude()+";");

                        n.setText("Coordinates: Lat="+Singleton.getInstance().getLocation().getLatitude()
                                    +", Lng="+Singleton.getInstance().getLocation().getLongitude()
                                    +", Alt="+Singleton.getInstance().getLocation().getAltitude()
                                    +"\nAccuracy: "+Singleton.getInstance().getLocation().getAccuracy()
                                    +"\nTime: "+Singleton.getInstance().getLocation().getTime()
                                    +"\nSpeed: "+Singleton.getInstance().getLocation().getSpeed());

                        suggestedNotes.add(n);

                        if(suggestedNotes.size()==1){
                            updateSuggestedNoteInterface(innerArea, suggestedNotes, suggestedNotes.size()-1);
                        }
                    }
                }
            }.execute(getApplicationContext(), Singleton.getInstance().getLocation());
        }

        if(Singleton.getInstance().getPlaces()!=null){
            //Get suggestion note based on place if there is no note with the suggested title
            if (Singleton.getInstance().getNotepad().searchNotesByTitle("" + Singleton.getInstance().getPlaces().get(0).getPlace().getName()).isEmpty()){
                new GetPlacePhoto() {
                    @Override
                    protected void onPostExecute(GetPlacePhoto.AttributedPhoto attributedPhoto) {
                        if (attributedPhoto != null) {
                            // Create an file to store the image
                            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                            String imageFileName = "JPEG_" + timeStamp + "_ALZHEIMER_NOTES_AutoNoteImage.jpg";
                            File pictureFile = new File(storageDir, imageFileName);

                            FileOutputStream out = null;
                            try {
                                out = new FileOutputStream(pictureFile);
                                attributedPhoto.bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                try {
                                    if (out != null) {
                                        out.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            try{
                                Note n = new Note("" + Singleton.getInstance().getPlaces().get(0).getPlace().getName(),
                                        PLACE_TYPE + commonMethods.decodePlacesType(Singleton.getInstance().getPlaces().get(0).getPlace().getPlaceTypes().get(0))+";"
                                                +LOCATION+Singleton.getInstance().getPlaces().get(0).getPlace().getLatLng().latitude+", "+Singleton.getInstance().getPlaces().get(0).getPlace().getLatLng().longitude);



                                String aux = "";
                                if (Singleton.getInstance().getPlaces().get(0).getPlace().getAddress() != null)
                                    aux += "Address: " + Singleton.getInstance().getPlaces().get(0).getPlace().getAddress();
                                if (Singleton.getInstance().getPlaces().get(0).getPlace().getPhoneNumber() != null)
                                    aux += "\nContact: " + Singleton.getInstance().getPlaces().get(0).getPlace().getPhoneNumber();
                                if (Singleton.getInstance().getPlaces().get(0).getPlace().getWebsiteUri() != null)
                                    aux += "\nWebsite: " + Singleton.getInstance().getPlaces().get(0).getPlace().getWebsiteUri();
                                n.setText(aux);

                                if (commonMethods.fileExist(pictureFile.getAbsolutePath())) {
                                    n.setPicture(pictureFile);
                                }

                                suggestedNotes.add(n);

                            }catch(NullPointerException e){
                                e.printStackTrace();
                                Log.e(TAG_GETTING_CONTEXT, "Cannot create auto note because one of the parameters is null.");
                            }

                            if(suggestedNotes.size()==1){
                                updateSuggestedNoteInterface(innerArea, suggestedNotes, suggestedNotes.size()-1);
                            }
                        }
                    }
                }.execute(Singleton.getInstance().getPlaces().get(0).getPlace().getId(), mGoogleApiClient);
            }
        }


        final int[] j = new int[1];
        findViewById(R.id.constraintLayout_newNoteSuggestion).setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()){
            public void onSwipeRight() {
                j[0]++;
                if(j[0]>suggestedNotes.size()-1) j[0]=0;

                updateSuggestedNoteInterface(innerArea, suggestedNotes, j[0]);
            }
            public void onSwipeLeft() {
                j[0]--;
                if(j[0]<0) j[0]=suggestedNotes.size()-1;

                updateSuggestedNoteInterface(innerArea, suggestedNotes, j[0]);
            }
        });

        findViewById(R.id.floatingActionButton_addNewNote).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                Singleton.getInstance().getNotepad().addNote(suggestedNotes.get(j[0]));

                //Register location fence if Settings switch is enable
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
                if(sharedPref.getBoolean(LOCATION_FENCE_STATE, false)){
                    float searchRadius = sharedPref.getFloat(PREFS_LOCATION_RADIUS_KEY, LOCATION_DEFAULT_RADIUS);
                    float dwellTimeSec = sharedPref.getFloat(PREFS_DWELL_TIME_KEY, DWELL_DEFAULT_TIME);

                    for (LatLng latLng : suggestedNotes.get(j[0]).getKeywordsLocation()) {
                        commonMethods.registerFence(getApplicationContext(), FENCE_KEY_LOCATION + suggestedNotes.get(j[0]).getId() + latLng.toString(),
                                LocationFence.in(latLng.latitude, latLng.longitude, searchRadius, (long) dwellTimeSec * 1000));
                    }
                }

                suggestedNotes.remove(suggestedNotes.get(j[0]));

                showList();

                if (!suggestedNotes.isEmpty()){
                    j[0]--;
                    if(j[0]<0) j[0]=suggestedNotes.size()-1;

                    updateSuggestedNoteInterface(innerArea, suggestedNotes, j[0]);
                }else{
                    findViewById(R.id.constraintLayout_newNoteSuggestion).setVisibility(View.GONE);
                    findViewById(R.id.view5).setVisibility(View.GONE);
                }
            }
        });

        findViewById(R.id.floatingActionButton_cancelNewNote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                suggestedNotes.remove(suggestedNotes.get(j[0]));

                if (!suggestedNotes.isEmpty()){
                    j[0]--;
                    if(j[0]<0) j[0]=suggestedNotes.size()-1;

                    updateSuggestedNoteInterface(innerArea, suggestedNotes, j[0]);
                }else{
                    findViewById(R.id.constraintLayout_newNoteSuggestion).setVisibility(View.GONE);
                    findViewById(R.id.view5).setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * Updates the UI with the note suggestion
     * @param innerLayout the layout of the elements to be updated
     * @param suggestedNotes the array that contains the suggested notes
     * @param index the index of the note to display
     */
    private void updateSuggestedNoteInterface(ConstraintLayout innerLayout, ArrayList<Note> suggestedNotes, int index){
        findViewById(R.id.constraintLayout_newNoteSuggestion).setVisibility(View.VISIBLE);
        findViewById(R.id.view5).setVisibility(View.VISIBLE);

        TextView temp_noteTitle = innerLayout.findViewById(R.id.textView_title);
        TextView temp_noteKeyword = innerLayout.findViewById(R.id.textView_keywords);
        ImageView temp_noteImage = innerLayout.findViewById(R.id.imageView);

        temp_noteTitle.setText(suggestedNotes.get(index).getTitle());
        temp_noteKeyword.setText(suggestedNotes.get(index).getKeywordsString());

        int ctype = suggestedNotes.get(index).getContentType();
        if (ctype == 1) temp_noteImage.setImageResource(R.mipmap.ic_text);
        else if (ctype == 2) temp_noteImage.setImageResource(R.mipmap.ic_photo);
        else if (ctype == 3) temp_noteImage.setImageResource(R.mipmap.ic_video);
        else if (ctype == 0) temp_noteImage.setImageResource(R.mipmap.ic_blank);
        else temp_noteImage.setImageResource(R.mipmap.ic_multinote);
    }

    /**
     * Updates the UI with the current context values on the bottom of the Main Activity
     */
    private void updateCurrentContextMainInterface() {
        ConstraintLayout layout_currentSnapshotStats = findViewById(R.id.constraintLayout_currentStatsInterface);

        if(Singleton.getInstance().getHeadphonesState()!=null){
            layout_currentSnapshotStats.setVisibility(View.VISIBLE);
            TextView tv_headphones = findViewById(R.id.textView_headphonesMain);
            tv_headphones.setText(commonMethods.decodeHeadphonesState(Singleton.getInstance().getHeadphonesState().getState()));
        }

        if(Singleton.getInstance().getWeather()!=null){
            layout_currentSnapshotStats.setVisibility(View.VISIBLE);
            TextView tv_weather = findViewById(R.id.textView_weatherMain);

            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
            //Gets the currentTemperature according with the defined temp. units.
            if (sharedPref.getString(PREFS_TEMPERATURE_UNIT_KEY, AVAILABLE_TEMPERATURE_UNITS[0]).equals(AVAILABLE_TEMPERATURE_UNITS[1]))
                tv_weather.setText("Temp: "+Math.round(Singleton.getInstance().getWeather().getTemperature(Weather.FAHRENHEIT))+"ºF");
            else
                tv_weather.setText("Temp: "+Math.round(Singleton.getInstance().getWeather().getTemperature(Weather.CELSIUS))+"ºC");

            tv_weather.append("\nHumidity: "+Singleton.getInstance().getWeather().getHumidity()+"%");


            //Decode the first weather element
            tv_weather.append("\n"+commonMethods.decodeWeatherConditions(Singleton.getInstance().getWeather().getConditions()).get(0).replaceAll("\\[", "").replaceAll("]","").replaceAll("_", " "));
        }

        if(Singleton.getInstance().getPlaces()!=null) {
            layout_currentSnapshotStats.setVisibility(View.VISIBLE);
            TextView tv_places = findViewById(R.id.textView_placesMain);

            //Get and decode the first places type element
            tv_places.setText(commonMethods.decodePlacesTypes(Singleton.getInstance().getPlaces().get(0).getPlace().getPlaceTypes()).get(0).replaceAll("\\[", "").replaceAll("]","").replaceAll("_", " "));
        }

        if(Singleton.getInstance().getLocation()!=null){
            layout_currentSnapshotStats.setVisibility(View.VISIBLE);
            TextView tv_coordinates = findViewById(R.id.textView_coordinatesMain);
            tv_coordinates.setText("Lat:"+Singleton.getInstance().getLocation().getLatitude()+"\nLng:"+Singleton.getInstance().getLocation().getLongitude());
        }

        if(Singleton.getInstance().getActivityRecognitionResult()!=null) {
            layout_currentSnapshotStats.setVisibility(View.VISIBLE);
            TextView tv_activity = findViewById(R.id.textView_activityMain);
            tv_activity.setText("Activity: "+commonMethods.decodeActivityType(Singleton.getInstance().getActivityRecognitionResult().getMostProbableActivity().getType()));
        }

        if(Singleton.getInstance().getTimeInterval()!=null){
            layout_currentSnapshotStats.setVisibility(View.VISIBLE);
            TextView tv_timeIntervals = findViewById(R.id.textView_timeIntervalsMain);
            //Gets and decode the time intervals, presenting the array without "_", "[" and "]"
            String aux = "";
            ArrayList<String> timeIntervals = commonMethods.decodeTimeIntervals(Singleton.getInstance().getTimeInterval().getTimeIntervals());
            for (int i = 0; i < timeIntervals.size(); i++) {
                timeIntervals.set(i, timeIntervals.get(i).substring(TIME_INTERVAL_LENGHT));
                aux +=  timeIntervals.get(i).replaceAll("\\[", "").replaceAll("]","").replaceAll("_", " ");

                if (i<timeIntervals.size()-1)
                    aux+= ", ";
            }
            tv_timeIntervals.setText(aux);
        }
    }

    /**
     * Starts the Settings activity. Called when user click on settings button.
     */
    private void contextSettings() {
        startActivity(new Intent(this, AwarenessApiSettingsActivity.class));
    }

    /**
     * Options Menu
     * @param menu the menu
     * @return You must return true for the menu to be displayed; if you return false it will not be shown
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView sv = (SearchView) item.getActionView();
        if(sview_state)item.expandActionView();
        mSearchEditFrame = sv.findViewById(android.support.v7.appcompat.R.id.search_edit_frame);

        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                stringToSearchFor = query;
                if(!stringToSearchFor.isEmpty()){
                    searchNotes(query);
                    adapter = new CustomAdapter(MainActivity.this,notesfiltered );
                }else{
                    showList();
                }
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                stringToSearchFor = newText;
                if(!stringToSearchFor.isEmpty()){
                    searchNotes(newText);
                    adapter = new CustomAdapter(MainActivity.this,notesfiltered );
                }else{
                    showList();
                }
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                return false;
            }
        });
        //check if the search view is expanded or not
        ViewTreeObserver vto = mSearchEditFrame.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            int oldVisibility = -1;

            @Override
            public void onGlobalLayout() {

                int currentVisibility = mSearchEditFrame.getVisibility();

                if (currentVisibility != oldVisibility) {
                    if (currentVisibility == View.VISIBLE) {
                        //Toast.makeText(SearchActivity.this, "expanded", Toast.LENGTH_SHORT).show();
                        sview_state = true;
                    } else {
                        //Toast.makeText(SearchActivity.this, "colapsed", Toast.LENGTH_SHORT).show();
                        sview_state = false;
                    }

                    oldVisibility = currentVisibility;
                }

            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView search_button = (SearchView) item.getActionView();
        if(stringToSearchFor != null)
            if(!stringToSearchFor.isEmpty())
                search_button.setQuery(stringToSearchFor,true);

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Callback method of the menu item click
     * @param item
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.action_add):
                MainActivity.this.i = new Intent(this, AddActivity.class);

                //Permission Not Granted
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                    return true;
                }

                //Permission Already Granted
                startActivity(MainActivity.this.i);
                break;
            case (R.id.action_filter):
                String str = "";
                for (Boolean selectedSearchF_i:MainActivity.this.selectedSearchF) {
                    str += selectedSearchF_i.toString()+"; ";
                }
                Log.d(TAG_GENERAL, " -> Filter Array: "+str);
                filterSelectDialog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //showList();
        if(stringToSearchFor.isEmpty())showList();
        else{
            if(listView!=null && adapter!=null){
                searchNotes(stringToSearchFor);
                adapter = new CustomAdapter(MainActivity.this,notesfiltered );
                listView.setAdapter(adapter);
            }
        }
        if(listView!=null && adapter!=null)
            adapter.notifyDataSetChanged();

    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            FileOutputStream fileOutputStream = openFileOutput(SAVE_FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(Singleton.getInstance().getNotepad());

            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, R.string.errorWritingSaveFile,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mGoogleApiClient.disconnect();

        try{
            unregisterReceiver(commonMethods.myFenceReceiver);
        }catch(IllegalArgumentException e){
            e.printStackTrace();
            Log.d(TAG_GENERAL, "The Broadcast Receiver "+commonMethods.myFenceReceiver+" is not unregistered.");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("filter1",this.selectedSearchF[0]);
        outState.putBoolean("filter2",this.selectedSearchF[1]);
        outState.putBoolean("filter3",this.selectedSearchF[2]);
        outState.putBoolean("filter4",this.selectedSearchF[3]);
        outState.putBoolean("filter5",this.selectedSearchF[4]);
        outState.putBoolean("filter6",this.selectedSearchF[5]);
        outState.putBoolean("filter7",this.selectedSearchF[6]);
        outState.putBoolean("filter8",this.selectedSearchF[7]);
        outState.putBoolean("filter9",this.selectedSearchF[8]);
        outState.putString("search_text",this.stringToSearchFor);
        outState.putBoolean("sview_state",this.sview_state);
        outState.putString("dateStart",this.dateStart);
        outState.putString("dateEnd",this.dateEnd);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(MainActivity.this.i);
                } else {
                    Log.w(TAG_REQUEST_PERMISSION_RESULT, "PERMISSION WRITE EXTERNAL STORAGE NOT GRANTED BY USER");
                    Toast.makeText(this, "Unable to see notes details, pls grant app permissions", Toast.LENGTH_SHORT).show();
                }
                break;
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    MainActivity.this.contextRefresh();
                } else {
                    Log.w(TAG_REQUEST_PERMISSION_RESULT, "PERMISSION FINE LOCATION NOT GRANTED BY USER");
                    Toast.makeText(this, "Unable to get context updates, pls grant app permissions", Toast.LENGTH_SHORT).show();
                }
                break;
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION_SETTINGS:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    MainActivity.this.contextSettings();
                } else {
                    Log.w(TAG_REQUEST_PERMISSION_RESULT, "PERMISSION FINE LOCATION NOT GRANTED BY USER");
                    Toast.makeText(this, "Unable to go to settings, pls grant app permissions", Toast.LENGTH_SHORT).show();
                }
                break;
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION_REGISTER_FENCE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    MainActivity.this.registerLocationFenceStartup();
                } else {
                    Log.w(TAG_REQUEST_PERMISSION_RESULT, "PERMISSION FINE LOCATION NOT GRANTED BY USER");
                    Toast.makeText(this, "Unable to go to register fences, pls grant app permissions", Toast.LENGTH_SHORT).show();
                }
                break;
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_TAKE_PICTURE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    MainActivity.this.selectImage();
                } else {
                    Log.w(TAG_REQUEST_PERMISSION_RESULT, "PERMISSION FINE LOCATION NOT GRANTED BY USER");
                    Toast.makeText(this, "Unable to take picture, pls grant app permissions", Toast.LENGTH_SHORT).show();
                }
                break;
        }

            // other 'case' lines to check for other
            // permissions this app might request
    }

    /**
     * Starts the intent to open the gallery so that the users chooses an image
     */
    private void galleryIntentImage(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select Image"),REQUEST_IMAGE_GALLERY_CLOUD_VISION);
    }

    /**
     * Starts the intent to take a picture
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go

            File imageFile = null;
            try {
                imageFile = createImageFile();
            } catch (IOException ioe) {
                // Error occurred while creating the File
                ioe.printStackTrace();
            }

            // Continue only if the File was successfully created
            if (imageFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName()+".fileprovider",
                        imageFile);
                //Put the file URI as an extra for the camera intent.
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                //Starts the android camera.
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE_CLOUD_VISION);
            }
        }
    }

    /**
     * Creates a temporary file to store the image.
     * This is necessary because we need to send the file to the android camera via intent extra
     * so that the camera saves the full size photo, on the given file.
     * @throws IOException Exception
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "ALZHEIMER_NOTES_NewNotePhoto";
        //File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File image_tempFile;
        try{
            image_tempFile = File.createTempFile(
                    imageFileName,    /* prefix */
                    ".jpg",      /* suffix */
                    storageDir        /* directory */
            );

            image_tempFile.deleteOnExit();
            return image_tempFile;
        }catch(IOException e){
            e.printStackTrace();
            Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_IMAGE_CAPTURE_CLOUD_VISION && resultCode == RESULT_OK) {
            try {
                uploadImageToCloudVision();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if(requestCode == REQUEST_IMAGE_GALLERY_CLOUD_VISION && resultCode == RESULT_OK) {
            MainActivity.this.photoURI = data.getData();
            try {
                uploadImageToCloudVision();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Generates the dialog so that the users chooses if want to take a new photo or use a existing
     * one from gallery.
     */
    private void selectImage() {
        if(cloudVisionTask.getStatus()!= AsyncTask.Status.FINISHED){
            cloudVisionTask.cancel(true);

            if(currentContextTask.isCancelled()){
                ProgressBar progressBar_contextRefresh = MainActivity.this.dialogView.findViewById(R.id.progressBar_contextRefresh);
                TextView progressBar_text = dialogView.findViewById(R.id.progressBar_Text);
                FloatingActionButton btn_camera = dialogView.findViewById(R.id.floatingActionButton_camera);

                btn_camera.setImageResource(R.drawable.ic_action_photo_camera);
                progressBar_contextRefresh.setVisibility(View.GONE);
                progressBar_text.setVisibility(View.GONE);
            }
        }else {
            final CharSequence[] items = {getString(R.string.newImageDialog_capturePhoto), getString(R.string.newImageDialog_galleryPhoto), getString(R.string.dialogNegativeButtonText)};
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(getString(R.string.newImageDialog_Title));
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    if (items[item].equals(getString(R.string.newImageDialog_capturePhoto))) {
                        dispatchTakePictureIntent();
                    } else if (items[item].equals(getString(R.string.newImageDialog_galleryPhoto))) {
                        galleryIntentImage();
                    } else if (items[item].equals(getString(R.string.dialogNegativeButtonText))) {
                        dialog.dismiss();
                    }
                }
            });
            builder.show();
        }
    }

    /**
     * Async task related to the upload of the picture to Cloud Vision
     * @throws IOException
     */
    @SuppressLint("StaticFieldLeak")
    private void uploadImageToCloudVision() throws IOException {
        cloudVisionTask = new CloudVision(){
            @Override
            protected void onPreExecute() {
                ProgressBar progressBar_contextRefresh = MainActivity.this.dialogView.findViewById(R.id.progressBar_contextRefresh);
                TextView progressBar_text = dialogView.findViewById(R.id.progressBar_Text);
                FloatingActionButton btn_camera = dialogView.findViewById(R.id.floatingActionButton_camera);

                btn_camera.setImageResource(R.drawable.ic_action_cross);
                progressBar_contextRefresh.setVisibility(View.VISIBLE);
                progressBar_contextRefresh.setProgress(0);
                progressBar_text.setVisibility(View.VISIBLE);
                progressBar_text.setText("Progress: 0%");
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                ProgressBar progressBar_contextRefresh = MainActivity.this.dialogView.findViewById(R.id.progressBar_contextRefresh);
                TextView progressBar_text = dialogView.findViewById(R.id.progressBar_Text);
                FloatingActionButton btn_camera = dialogView.findViewById(R.id.floatingActionButton_camera);

                btn_camera.setImageResource(R.drawable.ic_action_photo_camera);
                progressBar_contextRefresh.setVisibility(View.GONE);
                progressBar_text.setVisibility(View.GONE);

                showList();
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                ProgressBar progressBar_contextRefresh = MainActivity.this.dialogView.findViewById(R.id.progressBar_contextRefresh);
                TextView progressBar_text = dialogView.findViewById(R.id.progressBar_Text);

                MainActivity.this.progressCloudVisionTask = values[0];
                progressBar_contextRefresh.setProgress(values[0]);
                progressBar_text.setText("Progress: "+values[0]+"%");
            }
        }.execute(getApplicationContext(), MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(), MainActivity.this.photoURI));
    }
}
