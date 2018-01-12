package pt.ipleiria.notepadv2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import pt.ipleiria.notepadv2.model.AppConstants;
import pt.ipleiria.notepadv2.model.CloudVision;
import pt.ipleiria.notepadv2.model.Note;
import pt.ipleiria.notepadv2.model.Singleton;
import pt.ipleiria.notepadv2.model.commonMethods;

import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

public class AddActivity extends AppCompatActivity implements AppConstants {

    Note n;
    boolean edit_mode;
    String text = "";
    File image;
    Uri video;
    EditText editText_adicionalParams;

    //Cloud Vision Task
    AsyncTask cloudVisionTask = new CloudVision();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        Toolbar addActivityToolbar = findViewById(R.id.toolbar_addActivity);
        addActivityToolbar.setNavigationIcon(R.drawable.ic_action_back);
        setSupportActionBar(addActivityToolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("New Note");


        //Hides the keyboard when the activity starts
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //Gets the intent extra
        Intent intent = getIntent();
        this.n = (Note) intent.getSerializableExtra(INTENT_EXTRA_NOTE);

        EditText title_editText = findViewById(R.id.editText_Title);
        EditText keywords_editText = findViewById(R.id.editText_keywords);
        EditText body_editText = findViewById(R.id.editText_BodyText);
        ImageView body_imageView = findViewById(R.id.imageView_BodyImage);
        ImageView body_videoView = findViewById(R.id.imageView_BodyVideo);
        TextView textView_ID = findViewById(R.id.textView_ID);
        TextView textView_createDate = findViewById(R.id.textView_createDate);
        TextView textView_lastEdit = findViewById(R.id.textView_lastEdit);
        FloatingActionButton btn_assistKeywords = findViewById(R.id.floatingActionButton_assistant);

        //Auto complete the fields if the note exists.
        if (this.n != null) {
            this.edit_mode = false;

            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(this.n.getTitle());

            title_editText.setText(this.n.getTitle());
            title_editText.setEnabled(false);
            keywords_editText.setText(this.n.getPlainKeywords());
            keywords_editText.setEnabled(false);
            Resources res = getResources();
            String str = String.format(res.getString(R.string.activity_add_id_field), this.n.getId());
            textView_ID.setText(str);
            str = String.format(res.getString(R.string.activity_add_createDate_field), this.n.getCreation_date());
            textView_createDate.setText(str);
            str = String.format(res.getString(R.string.activity_add_lastEdit_field), this.n.getLast_edit_date());
            textView_lastEdit.setText(str);

            btn_assistKeywords.setEnabled(false);

            //Check if note was text body
            if (!this.n.getText().isEmpty()) {
                body_editText.setVisibility(View.VISIBLE);
                body_editText.setEnabled(false);
                this.text = n.getText();
                body_editText.setText(n.getText());
            } else {
                body_editText.setVisibility(View.GONE);
            }

            //Check if note was image body
            if (this.n.getPicture() != null) {
                body_imageView.setVisibility(View.VISIBLE);
                this.image = this.n.getPicture();
                setImageThumbnail();
            } else {
                body_imageView.setVisibility(View.GONE);
            }

            //Check if note was video body
            if (this.n.getVideo() != null) {
                body_videoView.setVisibility(View.VISIBLE);
                //Convert Uri to String
                this.video = Uri.parse(this.n.getVideo());
                setVideoThumbnail();
            } else {
                body_videoView.setVisibility(View.GONE);
            }
            //The note doesn't exist (New Note)
        } else {
            edit_mode = true;
            body_editText.setVisibility(View.GONE);
            body_imageView.setVisibility(View.GONE);
            body_videoView.setVisibility(View.GONE);
            textView_ID.setVisibility(View.GONE);
            textView_createDate.setVisibility(View.GONE);
            textView_lastEdit.setVisibility(View.GONE);
        }

        //Listener of note image viewer. On click open system view.
        body_imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AddActivity.this.image != null) {
                    showImageVideoIntent(true);
                } else {
                    selectImage();
                }
            }
        });

        //Listener of note video viewer. On click open system view.
        body_videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AddActivity.this.video != null) {
                    showImageVideoIntent(false);
                } else {
                    selectVideo();
                }
            }
        });

        //Long Listener of note image viewer. On click if edit mode is enable prompt message to remove the image from the note.
        body_imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (edit_mode)
                    deleteAlertDialog(getString(R.string.deleteDialogTitle), getString(R.string.deleteDialogMessage), view);
                return false;
            }
        });

        //Long Listener of note video viewer. On click if edit mode is enable prompt message to remove the image from the note.
        body_videoView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (edit_mode)
                    deleteAlertDialog(getString(R.string.deleteDialogTitle), getString(R.string.deleteDialogMessage), view);
                return false;
            }
        });

        body_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                AddActivity.this.text = charSequence.toString();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        btn_assistKeywords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                keywordsAssistanceDialog();
            }
        });

        //Needed to avoid double initial double click on buttons.
        cloudVisionTask.cancel(true);
    }

    private void keywordsAssistanceDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AddActivity.this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.keywords_assistance, null);
        dialogBuilder.setView(dialogView);
        final AlertDialog alertDialog = dialogBuilder.create();

        alertDialog.create();

        //Get the dialog elements
        final Spinner spinner_filterType = dialogView.findViewById(R.id.spinner_contextFilters);
        final TextView tv_filterValue = dialogView.findViewById(R.id.textView2);
        final Spinner spinner_filterValue = dialogView.findViewById(R.id.spinner_filterValue);
        final TextView tv_adicionalParameters = dialogView.findViewById(R.id.textView3);
        editText_adicionalParams = dialogView.findViewById(R.id.editText_adcionalParameters);
        Button btn_add = dialogView.findViewById(R.id.btn_assistanceAdd);
        Button btn_done = dialogView.findViewById(R.id.btn_assistanceDone);

        ArrayAdapter<String> spinnerFilterTypeAdapter = new ArrayAdapter<>(AddActivity.this,
                android.R.layout.simple_list_item_1, AVAILABLE_FILTERS);
        spinner_filterType.setAdapter(spinnerFilterTypeAdapter);


        spinner_filterType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View selectedItemView, int pos, long id) {
                String clickedItem = adapterView.getItemAtPosition(pos).toString();

                //Get the index of the correspondent Filter
                int i;
                for (i = 0; i < AVAILABLE_FILTERS.length; i++) {
                    if (Objects.equals(AVAILABLE_FILTERS[i], clickedItem))
                        break;
                }

                //Default Values
                tv_adicionalParameters.setVisibility(View.GONE);
                editText_adicionalParams.setVisibility(View.GONE);
                tv_filterValue.setText("Filter Value:");
                tv_filterValue.setClickable(false);
                tv_filterValue.setBackground(getDrawable(R.color.colorTransparent));
                tv_filterValue.setElevation(0);
                tv_filterValue.setOnClickListener(null);
                spinner_filterValue.setVisibility(View.VISIBLE);
                spinner_filterValue.setSelection(0);
                editText_adicionalParams.setText("");

                ArrayList<String> filterValues = new ArrayList<>();
                ArrayAdapter<String> spinnerFilterValueAdapter;
                List<String> auxArray;

                switch (i) {
                    //Headphones
                    case 0:
                        filterValues.add(AVAILABLE_HEADPHONES_TYPES[0]);
                        filterValues.add(AVAILABLE_HEADPHONES_TYPES[1]);
                        spinnerFilterValueAdapter = new ArrayAdapter<>(AddActivity.this,
                                android.R.layout.simple_list_item_1, filterValues);
                        spinner_filterValue.setAdapter(spinnerFilterValueAdapter);
                        break;

                    //Temperature == Humidity
                    case 1:
                        //Humidity
                    case 2:
                        tv_adicionalParameters.setVisibility(View.VISIBLE);
                        editText_adicionalParams.setVisibility(View.VISIBLE);

                        editText_adicionalParams.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);

                        filterValues.add("=");
                        filterValues.add("≤");
                        filterValues.add("≥");
                        filterValues.add("≠");
                        spinnerFilterValueAdapter = new ArrayAdapter<>(AddActivity.this,
                                android.R.layout.simple_list_item_1, filterValues);
                        spinner_filterValue.setAdapter(spinnerFilterValueAdapter);

                        break;

                    //Weather
                    case 3:
                        auxArray = Arrays.asList(AVAILABLE_WEATHER_TYPES);
                        filterValues.addAll(auxArray);
                        spinnerFilterValueAdapter = new ArrayAdapter<>(AddActivity.this,
                                android.R.layout.simple_list_item_1, filterValues);
                        spinner_filterValue.setAdapter(spinnerFilterValueAdapter);

                        break;

                    //Location
                    case 4:
                        tv_adicionalParameters.setVisibility(View.VISIBLE);
                        tv_adicionalParameters.setText("or Type the Coordinates:");
                        editText_adicionalParams.setVisibility(View.VISIBLE);

                        tv_filterValue.setText("Click here to open Maps");
                        tv_filterValue.setClickable(true);
                        tv_filterValue.setBackground(getDrawable(R.color.colorVeryLightGrey));
                        tv_filterValue.setElevation(3);

                        tv_filterValue.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //Permission Not Granted
                                if (ActivityCompat.checkSelfPermission(AddActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(AddActivity.this,
                                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                            MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                                    return;
                                }

                                //Permission Already Granted
                                openMapsActivity();
                            }
                        });

                        spinner_filterValue.setVisibility(View.INVISIBLE);

                        editText_adicionalParams.setHint("Latitude, Longitude");
                        editText_adicionalParams.setInputType(InputType.TYPE_CLASS_TEXT);

                        break;

                    //Activity
                    case 5:
                        auxArray = Arrays.asList(AVAILABLE_ACTIVITY_TYPES);
                        filterValues.addAll(auxArray);
                        spinnerFilterValueAdapter = new ArrayAdapter<>(AddActivity.this,
                                android.R.layout.simple_list_item_1, filterValues);
                        spinner_filterValue.setAdapter(spinnerFilterValueAdapter);

                        break;

                    //Place
                    case 6:
                        auxArray = Arrays.asList(ALL_PLACES_TYPES);
                        filterValues.addAll(auxArray);
                        spinnerFilterValueAdapter = new ArrayAdapter<>(AddActivity.this,
                                android.R.layout.simple_list_item_1, filterValues);
                        spinner_filterValue.setAdapter(spinnerFilterValueAdapter);

                        break;
                    //todo finish #time: keywords
                    case 7:
                        auxArray = Arrays.asList(AVAILABLE_TIME_INTERVALS);
                        filterValues.addAll(auxArray);
                        spinnerFilterValueAdapter = new ArrayAdapter<>(AddActivity.this,
                                android.R.layout.simple_list_item_1, filterValues);
                        spinner_filterValue.setAdapter(spinnerFilterValueAdapter);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //NOTHING
            }
        });

        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText keywords_editText = findViewById(R.id.editText_keywords);

                if (!keywords_editText.getText().toString().isEmpty()) {
                    if (keywords_editText.getText().toString().charAt(keywords_editText.getText().toString().length() - 1) != ';') {
                        keywords_editText.append(";");
                    }
                }

                //Humidity and Temperature Validation
                if (spinner_filterType.getSelectedItemPosition() == 1 || spinner_filterType.getSelectedItemPosition() == 2) {
                    if (editText_adicionalParams.getText().toString().isEmpty())
                        Toast.makeText(AddActivity.this, "The Adicional Parameters Box Should Contain the Temperature", Toast.LENGTH_SHORT).show();
                    else {
                        keywords_editText.append(" #");
                        keywords_editText.append(spinner_filterType.getSelectedItem().toString());
                        keywords_editText.append(":");
                        keywords_editText.append(spinner_filterValue.getSelectedItem().toString());
                        keywords_editText.append(editText_adicionalParams.getText().toString());
                    }
                } else if (spinner_filterType.getSelectedItemPosition() == 4) {
                    //Location Validation
                    if (spinner_filterType.getSelectedItemPosition() == 4) {
                        String[] trim = editText_adicionalParams.getText().toString().split(",");
                        if (trim.length == 2) {
                            try {
                                float lat = Float.parseFloat(trim[0]);
                                float lng = Float.parseFloat(trim[1]);

                                keywords_editText.append(" #");
                                keywords_editText.append(spinner_filterType.getSelectedItem().toString());
                                keywords_editText.append(":");
                                keywords_editText.append("" + lat);
                                keywords_editText.append(", " + lng);
                            } catch (NumberFormatException nfe) {
                                Toast.makeText(AddActivity.this, "The Coordinates Should be in a Correct Format. [LATITUDE, LONGITUDE]", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(AddActivity.this, "The Coordinates Should be in a Correct Format. [LATITUDE, LONGITUDE]", Toast.LENGTH_SHORT).show();
                        }
                    }
                    //Normal Fields Validation
                } else {
                    keywords_editText.append(" #");
                    keywords_editText.append(spinner_filterType.getSelectedItem().toString());
                    keywords_editText.append(":");
                    keywords_editText.append(spinner_filterValue.getSelectedItem().toString());
                    keywords_editText.append(editText_adicionalParams.getText().toString());
                }

            }
        });

        btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

    private void openMapsActivity() {
        Intent i = new Intent(AddActivity.this, MapsActivity.class);
        i.putExtra("from", "KeywordAssistDialog");
        startActivityForResult(i, REQUEST_MAPS_COORDINATES);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add, menu);

        final MenuItem btn_done = menu.findItem(R.id.btn_done);
        MenuItem item = menu.findItem(R.id.edit_switch);
        View actionView = item.getActionView();
        Switch edit_switch = actionView.findViewById(R.id.switch1);

        // Edit switch (Switch Button) listener
        edit_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //Flag to know the state of edit mode
                edit_mode = b;

                btn_done.setVisible(b);

                EditText note_title = findViewById(R.id.editText_Title);
                EditText note_keywords = findViewById(R.id.editText_keywords);
                EditText note_bodytext = findViewById(R.id.editText_BodyText);
                note_title.setEnabled(b);
                note_keywords.setEnabled(b);
                note_bodytext.setEnabled(b);

                FloatingActionButton btn_assistKeywords = findViewById(R.id.floatingActionButton_assistant);
                btn_assistKeywords.setEnabled(b);

                Toast.makeText(AddActivity.this, getString(R.string.edit_mode) + ": " + b, Toast.LENGTH_SHORT).show();
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {


        // Test if the notes contain the different body elements to update the menu item status,
        // according to the visibility of the component.

        MenuItem item = menu.findItem(R.id.edit_switch);
        View actionView = item.getActionView();
        Switch edit_switch = actionView.findViewById(R.id.switch1);

        MenuItem btn_done = menu.findItem(R.id.btn_done);
        MenuItem add_text = menu.findItem(R.id.add_text);
        MenuItem add_image = menu.findItem(R.id.add_image);
        MenuItem add_video = menu.findItem(R.id.add_video);
        EditText body_editText = findViewById(R.id.editText_BodyText);
        ImageView body_imageView = findViewById(R.id.imageView_BodyImage);
        ImageView body_videoView = findViewById(R.id.imageView_BodyVideo);
        FloatingActionButton btn_assistKeywords = findViewById(R.id.floatingActionButton_assistant);

        if (edit_mode) {
            //Edit Mode on Add new Note
            btn_done.setVisible(true);
            edit_switch.setChecked(true);
            add_text.setEnabled(true);
            add_image.setEnabled(true);
            add_video.setEnabled(true);
            btn_assistKeywords.setEnabled(true);
        } else {
            //Edit Mode Off
            btn_done.setVisible(false);
            edit_switch.setChecked(false);
            add_text.setEnabled(false);
            add_image.setEnabled(false);
            add_video.setEnabled(false);
            btn_assistKeywords.setEnabled(false);
        }

        if (body_editText.getVisibility() == View.VISIBLE) {
            add_text.setChecked(true);
        } else {
            add_text.setChecked(false);
        }

        if (body_imageView.getVisibility() == View.VISIBLE) {
            add_image.setChecked(true);
        } else {
            add_image.setChecked(false);
        }

        if (body_videoView.getVisibility() == View.VISIBLE) {
            add_video.setChecked(true);
        } else {
            add_video.setChecked(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        EditText title_editText = findViewById(R.id.editText_Title);
        EditText keywords_editText = findViewById(R.id.editText_keywords);
        EditText body_editText = findViewById(R.id.editText_BodyText);
        ImageView body_imageView = findViewById(R.id.imageView_BodyImage);
        ImageView body_videoView = findViewById(R.id.imageView_BodyVideo);

        //Toggle the check status of the item if this is checkable.
        if (item.isCheckable()) item.setChecked(!item.isChecked());

        //Check what item was pressed on the menu.
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.btn_done:

                //Note has to have title, keywords and text or image or video
                /*
                if(title_editText.getText().toString().isEmpty() || keywords_editText.getText().toString().isEmpty() ||
                        (this.text.isEmpty() && this.image==null && this.video==null)){
                        */

                //The note has to have Title and Keywords.
                if (title_editText.getText().toString().isEmpty() || keywords_editText.getText().toString().isEmpty()) {
                    //(body_editText.getVisibility() == View.GONE && body_imageView.getVisibility() == View.GONE && body_videoView.getVisibility() == View.GONE )){
                    Toast toast = Toast.makeText(AddActivity.this, R.string.noteNotCompleteToast, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                    //The note required fields to have "something".
                } else {
                    //Nova Nota = New note
                    Note nova_nota;
                    if (this.n == null) {
                        nova_nota = new Note(title_editText.getText().toString(), keywords_editText.getText().toString());
                    } else {
                        nova_nota = this.n;
                        nova_nota.setTitle(title_editText.getText().toString());
                        nova_nota.setKeywords(keywords_editText.getText().toString());
                    }

                    //Save the Text Body if the View is visible(=enable)
                    if (body_editText.getVisibility() == View.VISIBLE && !body_editText.getText().toString().isEmpty()) {
                        nova_nota.setText(this.text);
                    } else {
                        nova_nota.setText("");
                    }

                    //Save the Image Body if the View is visible(=enable)
                    if (body_imageView.getVisibility() == View.VISIBLE && this.image != null) {
                        nova_nota.setPicture(this.image);
                    } else {
                        nova_nota.setPicture(null);
                    }

                    //Save the Video Body if the View is visible(=enable)
                    if (body_videoView.getVisibility() == View.VISIBLE && this.video != null) {
                        nova_nota.setVideo(this.video.toString());
                    } else {
                        nova_nota.setVideo(null);
                    }

                    //TODO Check Registering location fences on new note with keywords #location:lat, lng.
                    SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
                    boolean locationFenceStatus = sharedPref.getBoolean(LOCATION_FENCE_STATE, false);

                    if (!nova_nota.getKeywordsLocation().isEmpty() && locationFenceStatus) {

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            Toast.makeText(getApplicationContext(), "Unable to add this note. The note cointains a location filter, meaning that the app needs Location Permissions.", Toast.LENGTH_LONG).show();
                            return false;
                        }

                        float searchRadius = sharedPref.getFloat(PREFS_LOCATION_RADIUS_KEY, LOCATION_DEFAULT_RADIUS);
                        float dwellTimeSec = sharedPref.getFloat(PREFS_DWELL_TIME_KEY, DWELL_DEFAULT_TIME);

                        for (LatLng latLng : nova_nota.getKeywordsLocation()) {
                            commonMethods.registerFence(getApplicationContext(), FENCE_KEY_LOCATION + nova_nota.getId() + latLng.toString(),
                                    LocationFence.in(latLng.latitude, latLng.longitude, searchRadius, (long) dwellTimeSec * 1000));
                        }
                    }

                    //Add the Note to the Singleton
                    Singleton.getInstance().getNotepad().addNote(nova_nota);

                    setResult(RESULT_OK);
                    finish();
                }
                break;

            case R.id.add_text:
                //Update to the Visibility if checked or Delete Message Prompt if unchecked.
                if(item.isChecked()){
                    body_editText.setVisibility(View.VISIBLE);
                }else{
                    deleteAlertDialog(getString(R.string.deleteDialogTitle),getString(R.string.deleteDialogMessage),body_editText);
                }
                break;

            case R.id.add_image:
                //Update to the Visibility if checked or Delete Message Prompt if unchecked.
                if(item.isChecked()){
                    body_imageView.setVisibility(View.VISIBLE);
                    selectImage();
                }else{
                    deleteAlertDialog(getString(R.string.deleteDialogTitle),getString(R.string.deleteDialogMessage),body_imageView);
                }
                break;

            case R.id.add_video:
                //Update to the Visibility if checked or Delete Message Prompt if unchecked.
                if(item.isChecked()){
                    body_videoView.setVisibility(View.VISIBLE);
                    selectVideo();
                }else{
                    deleteAlertDialog(getString(R.string.deleteDialogTitle),getString(R.string.deleteDialogMessage),body_videoView);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //Receiving and handling the Intents
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Camara Capture Intent
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            setImageThumbnail();

            try {
                //TODO Handle exception ?
                generateKeywordsFromImage(Uri.fromFile(this.image));
            } catch (IOException e) {
                e.printStackTrace();
            }

        //Camara Record Intent
        }else if(requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK){
            onCaptureVideoResult(data);

        //Gallery Image Pick Intent
        }else if(requestCode == REQUEST_IMAGE_GALLERY && resultCode == RESULT_OK){
            onSelectImageFromGalleryResult(data);

            try {
                //TODO Handle exception ?
                generateKeywordsFromImage(data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Gallery Video Pick Intent
        }else if(requestCode == REQUEST_VIDEO_GALLERY && resultCode == RESULT_OK){
            onSelectVideoFromGalleryResult(data);

        //Getting the Coordinates from maps
        }else if(requestCode == REQUEST_MAPS_COORDINATES && resultCode==RESULT_OK){
                LatLng location = data.getParcelableExtra("location");
                editText_adicionalParams.setText(location.latitude+", "+location.longitude);
        }
    }

    /**
     * Generates keywords according to the image on the note using Cloud Vision API
     * @param data Image Uri
     */
    @SuppressLint("StaticFieldLeak")
    private void generateKeywordsFromImage(Uri data) throws IOException {
        if(cloudVisionTask.getStatus()!= AsyncTask.Status.FINISHED){
            cloudVisionTask.cancel(true);
        }

        cloudVisionTask = new CloudVision(){
            @Override
            protected void onPostExecute(Void aVoid) {
                EditText editText_title = findViewById(R.id.editText_Title);
                EditText editText_keywords = findViewById(R.id.editText_keywords);
                EditText editText_bodyText = findViewById(R.id.editText_BodyText);


                //Set the notes title if this is empty else appends on the keywords box
                if(editText_title.getText().toString().isEmpty() && !Singleton.getInstance().getCloudVisionResponses()[5].isEmpty()){
                    //Deconstruct the plain message to a array and set the title as the 1st element of this
                    editText_title.setText(Note.trimPlainKeywords(Singleton.getInstance().getCloudVisionResponses()[5]).get(0));
                }


                //Format the keywords editTextBox with the correct syntax to append keywords
                if(!editText_keywords.getText().toString().isEmpty()){
                    if(editText_keywords.getText().toString().trim().charAt(editText_keywords.getText().toString().trim().length()-1)!=';'){
                        editText_keywords.setText(editText_keywords.getText().toString()+"; ");
                    }
                }

                //Append the web detection results in the keywords edit box as well.
                editText_keywords.append(Singleton.getInstance().getCloudVisionResponses()[5]);

                //Append the labels on keywords
                for(String labelKeyword : Note.trimPlainKeywords(Singleton.getInstance().getCloudVisionResponses()[0])){
                    if(!editText_keywords.getText().toString().contains(labelKeyword)){
                        editText_keywords.append(labelKeyword+"; ");
                    }
                }

                //The rest of the elements on the body text
                if(!Singleton.getInstance().getCloudVisionResponses()[1].isEmpty() ||
                        !Singleton.getInstance().getCloudVisionResponses()[2].isEmpty() ||
                        !Singleton.getInstance().getCloudVisionResponses()[3].isEmpty() ||
                        !Singleton.getInstance().getCloudVisionResponses()[4].isEmpty()){

                    editText_bodyText.setVisibility(View.VISIBLE);
                    editText_bodyText.append("\n\t(AUTOMATIC TEXT)");
                    editText_bodyText.append(Singleton.getInstance().getCloudVisionResponses()[1]);
                    editText_bodyText.append(Singleton.getInstance().getCloudVisionResponses()[2]);
                    editText_bodyText.append(Singleton.getInstance().getCloudVisionResponses()[3]);
                    editText_bodyText.append(Singleton.getInstance().getCloudVisionResponses()[4]);
                }
            }
        }.execute(getApplicationContext(), MediaStore.Images.Media.getBitmap(AddActivity.this.getContentResolver(), data));

    }

    /**
     * Called on activity result from record video intent.
     * @param data The data returned by the intent. Uri type.
     */
    private void onCaptureVideoResult(Intent data){
        this.video = data.getData();
        setVideoThumbnail();
    }

    /**
     * Called on activity result from select video from galley intent.
     * @param data The data returned by the intent. Uri type.
     */
    private void onSelectVideoFromGalleryResult(Intent data) {
        this.video = data.getData();
        setVideoThumbnail();
    }

    /**
     * Method called when the user want to add a video to the note. This message show a dialog to
     * let the user define if the video is provided by the camara or by the gallery.
     */
    private void selectVideo(){
        final CharSequence[] items = { getString(R.string.newVideoDialog_captureVideo), getString(R.string.newVideoDialog_galleryVideo), getString(R.string.dialogNegativeButtonText) };
        AlertDialog.Builder builder = new AlertDialog.Builder(AddActivity.this);
        builder.setTitle(getString(R.string.newVideoDialog_Title));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals(getString(R.string.newVideoDialog_captureVideo))) {
                    cameraIntentVideo();
                } else if (items[item].equals(getString(R.string.newVideoDialog_galleryVideo))) {
                    galleryIntentVideo();
                } else if (items[item].equals(getString(R.string.dialogNegativeButtonText))) {
                    ImageView body_videoView = findViewById(R.id.imageView_BodyVideo);
                    body_videoView.setVisibility(View.GONE);
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    /**
     * Starts the intent to record video.
     */
    private void cameraIntentVideo(){
        Intent recordVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (recordVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(recordVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    /**
     * Starts the intent to pick the video from gallery
     */
    private void galleryIntentVideo(){
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"),REQUEST_VIDEO_GALLERY);
    }

    /**
     * Gets the video URI, check's if the video exists, check if the app has storage permissions
     * and displays the video thumbnail on a image view.
     */
    private void setVideoThumbnail(){
        ImageView body_videoView = findViewById(R.id.imageView_BodyVideo) ;

        String video_path;
        try {
            //Gets the video path from the URI resource
            video_path = commonMethods.getFilePath(getApplicationContext(), this.video);

            //Check if file exist
            if(commonMethods.fileExist(video_path)){
                Glide.with(AddActivity.this).load(this.video).apply(centerCropTransform()).thumbnail(0.5f).into(body_videoView);
            }else{
                resourceNotFoundAlertDialog(getString(R.string.resourceNotFoundTitle), getString(R.string.resourceNotFoundMessage), body_videoView);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch(SecurityException se){
            se.printStackTrace();
            resourceNotFoundAlertDialog(getString(R.string.permissionDeniedTitle), getString(R.string.permissionDeniedMessage), body_videoView);
        }
    }

    /**
     * Generates the dialog so that the users chooses if want to take a new photo or use a existing
     * one from gallery.
     */
    private void selectImage() {
        final CharSequence[] items = { getString(R.string.newImageDialog_capturePhoto), getString(R.string.newImageDialog_galleryPhoto), getString(R.string.dialogNegativeButtonText) };
        AlertDialog.Builder builder = new AlertDialog.Builder(AddActivity.this);
        builder.setTitle(getString(R.string.newImageDialog_Title));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals(getString(R.string.newImageDialog_capturePhoto))) {
                    dispatchTakePictureIntent();
                } else if (items[item].equals(getString(R.string.newImageDialog_galleryPhoto))) {
                    galleryIntentImage();
                } else if (items[item].equals(getString(R.string.dialogNegativeButtonText))) {
                    ImageView body_imageView = findViewById(R.id.imageView_BodyImage);
                    body_imageView.setVisibility(View.GONE);
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    /**
     * Starts the intent to take a picture
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go

            try {
                createImageFile();
            } catch (IOException ioe) {
                // Error occurred while creating the File
                ioe.printStackTrace();
            }

            // Continue only if the File was successfully created
            if (this.image != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName()+".fileprovider",
                        this.image);
                //Put the file URI as an extra for the camera intent.
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                //Starts the android camera.
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    /**
     * Creates a temporary file to store the image.
     * This is necessary because we need to send the file to the android camera via intent extra
     * so that the camera saves the full size photo, on the given file.
     * @throws IOException Exception
     */
    private void createImageFile() throws IOException {
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
            this.image = image_tempFile;
        }catch(IOException e){
            e.printStackTrace();
            resourceNotFoundAlertDialog(getString(R.string.permissionDeniedTitle), getString(R.string.permissionDeniedMessage), findViewById(R.id.imageView_BodyImage));
        }
    }

    /**
     * Starts the intent to open the gallery so that the users chooses an image
     */
    private void galleryIntentImage(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select Image"),REQUEST_IMAGE_GALLERY);
    }

    /**
     * If the users selects the image from the gallery the activity returns a URI, so we need to
     * get this URI path to store the image on a File type object.
     * @param data Intent returned data
     */
    private void onSelectImageFromGalleryResult(Intent data){
        Uri image_uri = data.getData();

        String image_path;
        try {
            image_path = commonMethods.getFilePath(getApplicationContext(), image_uri);

            if(image_path!=null){
                this.image = new File(image_path);

                //Refreshes the image thumbnail, adds the image to the note and refresh the gallery
                setImageThumbnail();
            }else{
                resourceNotFoundAlertDialog(getString(R.string.nullFilePathTitle),getString(R.string.nullFilePathMessage), findViewById(R.id.imageView_BodyImage));
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch(SecurityException se){
            se.printStackTrace();
            resourceNotFoundAlertDialog(getString(R.string.permissionDeniedTitle), getString(R.string.permissionDeniedMessage), findViewById(R.id.imageView_BodyImage));
        }
    }

    /**
     * Set the image thumbnail on the respective image view.
     */
    private void setImageThumbnail(){
        ImageView body_imageView = findViewById(R.id.imageView_BodyImage);

        if(commonMethods.fileExist(this.image.getAbsolutePath())){
            Glide.with(AddActivity.this).load(Uri.fromFile(this.image)).apply(centerCropTransform()).thumbnail(0.5f).into(body_imageView);
            galleryAddPic();
        }else{
            resourceNotFoundAlertDialog(getString(R.string.resourceNotFoundTitle), getString(R.string.resourceNotFoundMessage), body_imageView);
        }
    }

    /**
     * Adds to photo to the gallery. This is only added if the image is not on the gallery yet.
     */
    public void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(this.image);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    /**
     * Shows the image or video on the system player.
     * @param image if true shows the image, else shows the video
     */
    private void showImageVideoIntent(boolean image){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);

        if(image){
            intent.setDataAndType(Uri.fromFile(this.image), "image/*");
        }else{
            intent.setDataAndType(this.video, "video/*");
        }
        startActivity(intent);
    }

    /**
     * Show an delete Alert Dialog, which means that if the user presses the positive button
     * a delete method is called that delete an note element based on this element layout view.
     * @param title The dialog title.
     * @param message The dialog message.
     * @param v The view connected to the note element to delete.
     */
    private void deleteAlertDialog(String title, String message, final View v){

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(AddActivity.this);
        builder.setTitle(title);
        builder.setMessage(message)
                .setPositiveButton(R.string.dialogPositiveButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteElementConfirmed(v);
                    }
                })
                .setNegativeButton(R.string.dialogNegativeButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        // Create the AlertDialog object and return it
        builder.show();
    }

    /**
     * Confirmed delete provided by the above method (deleteAlertDialog)
     * @param v set the view visibility as GONE, and delete the note element displayed by this view.
     */
    private void deleteElementConfirmed(View v){

        v.setVisibility(View.GONE);
        int id = v.getId();
        switch(id){
            case R.id.editText_BodyText:
                this.text = "";
                EditText editText_bodyText = findViewById(R.id.editText_BodyText);
                editText_bodyText.setText("");
                break;
            case R.id.imageView_BodyImage:
                this.image = null;
                ImageView imageView_bodyImage = findViewById(R.id.imageView_BodyImage);
                imageView_bodyImage.setImageResource(R.mipmap.ic_photo);
                break;
            case R.id.imageView_BodyVideo:
                this.video = null;
                ImageView imageView_BodyVideo = findViewById(R.id.imageView_BodyVideo);
                imageView_BodyVideo.setImageResource(R.mipmap.ic_photo);
                break;
        }
    }


    /**
     * Dialog that is displayed when a note body resource is deleted by the user on the gallery system
     * gallery and the app is unable to find the file. This dialog only have 1 button, because is
     * impossible to recover the file.
     * @param title The dialog title.
     * @param message The dialog Message.
     * @param v The view associated with the note element that is lost.
     */
    private void resourceNotFoundAlertDialog(String title, String message, View v) {
        //Deletes the element associated with the view
        deleteElementConfirmed(v);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(AddActivity.this);
        builder.setTitle(title);
        builder.setMessage(message)
                .setNeutralButton(R.string.dialogNeutralButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        // Create the AlertDialog object and return it
        builder.show();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    AddActivity.this.openMapsActivity();
                } else {
                    Log.w(TAG_REQUEST_PERMISSION_RESULT, "PERMISSION FINE LOCATION STORAGE NOT GRANTED BY USER");
                    Toast.makeText(this, "Unable to get context updates, pls grant app permissions", Toast.LENGTH_SHORT).show();
                    //return;
                }
                break;
        }
    }

}
