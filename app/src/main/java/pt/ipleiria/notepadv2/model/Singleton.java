package pt.ipleiria.notepadv2.model;

import android.location.Address;
import android.location.Location;

import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.TimeIntervals;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Miguel on 24/10/2017.
 */

public class Singleton implements AppConstants {

    private Notepad notepad;

    //Awareness API
    private HeadphoneState headphonesState;
    private Weather weather;
    private Location location;
    private ActivityRecognitionResult activityRecognitionResult;
    private List<PlaceLikelihood> places;
    private TimeIntervals timeInterval;

    //Cloud Vision
    private String[] cloudVisionResponses = {"", "", "", "", "", ""};

    private static final Singleton ourInstance = new Singleton();

    public static Singleton getInstance() {
        return ourInstance;
    }

    private Singleton() {
        this.notepad = new Notepad();
    }

    public Notepad getNotepad() {
        return notepad;
    }

    public void setNotepad(Notepad notepad) {
        this.notepad = notepad;
    }

    public HeadphoneState getHeadphonesState() {
        return headphonesState;
    }

    public void setHeadphonesState(HeadphoneState headphonesState) {
        this.headphonesState = headphonesState;
    }

    public Weather getWeather() {
        return weather;
    }

    public void setWeather(Weather weather) {
        this.weather = weather;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public ActivityRecognitionResult getActivityRecognitionResult() {
        return activityRecognitionResult;
    }

    public void setActivityRecognitionResult(ActivityRecognitionResult activityRecognitionResult) {
        this.activityRecognitionResult = activityRecognitionResult;
    }

    public List<PlaceLikelihood> getPlaces() {
        return places;
    }

    public void setPlaces(List<PlaceLikelihood> places) {
        this.places = places;
    }

    public TimeIntervals getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(TimeIntervals timeInterval) {
        this.timeInterval = timeInterval;
    }

    public String[] getCloudVisionResponses() {
        return cloudVisionResponses;
    }

    public void setCloudVisionResponses(String[] cloudVisionResponses) {
        this.cloudVisionResponses = cloudVisionResponses;
    }
}
