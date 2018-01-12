package pt.ipleiria.notepadv2.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.TimeIntervals;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Notepad implements Serializable, AppConstants {
    private ArrayList<Note> notes;

    public Notepad() {
        this.notes = new ArrayList<>();
    }

    public void addNote(Note n){
        if(!notes.contains(n)){
            notes.add(n);
        }else{
            updateNote(n);
        }
    }

    public void updateNote(Note n){
        for(Note ni : notes){
            if(ni.equals(n)){
                n.setLast_edit_date();
                notes.set(notes.indexOf(ni), n);
            }
        }
    }

    public ArrayList<Note> searchNotesByTitle(String string) {
        ArrayList<Note> res = new ArrayList<Note>();
        for (Note n : notes) {//para cada nota n em contacts
            if (n.getTitle().toLowerCase().contains(string.toLowerCase())) {//mete as strings todas para lower case
                res.add(n);
            }
        }
        return res;
    }

    public ArrayList<Note> searchNotesByBody(String string) {
        ArrayList<Note> res = new ArrayList<Note>();
        for (Note n : notes) {//para cada nota n em contacts
            if (n.getText().toLowerCase().contains(string.toLowerCase())) {//mete as strings todas para lower case
                res.add(n);
            }
        }
        return res;
    }

    public ArrayList<Note> searchNotesById(String string) {
        ArrayList<Note> res = new ArrayList<Note>();
        for (Note n : notes) {//para cada nota n em contacts
            if (n.getId().toLowerCase().contains(string.toLowerCase())) {//mete as strings todas para lower case
                res.add(n);
            }
        }
        return res;
    }

    public ArrayList<Note> searchNotesByKeyword(String string) {
        ArrayList<Note> res = new ArrayList<Note>();
        for (Note n : notes) {//para cada nota n em contacts

            for(String key : n.getKeywords()) {
                if (key.toLowerCase().contains(string.toLowerCase())) {//mete as strings todas para lower case
                    res.add(n);
                    break;
                }
            }
        }
        return res;
    }

    //Context is needed to acess shared preferences
    public ArrayList<Note> searchNotes(Context context, boolean[] filter, String string, String dateStart, String dateEnd) {
        ArrayList<Note> res = new ArrayList<Note>();

        if (filter[0]) {//search by title
            for (Note n : notes) {
                if (n.getTitle().toLowerCase().contains(string.toLowerCase()) && !res.contains(n)) {//mete as strings todas para lower case
                    res.add(n);
                }
            }
        }

        if (filter[1]) {//search by keyword
            for (Note n : notes) {

                for (String key : n.getKeywords()) {
                    if (key.toLowerCase().contains(string.toLowerCase()) && !res.contains(n)) {//mete as strings todas para lower case
                        res.add(n);
                        break;
                    }
                }
            }
        }

        if (filter[2]) {//search by body text
            for (Note n : notes) {
                if (n.getText() != null) {
                    if (n.getText().toLowerCase().contains(string.toLowerCase()) && !res.contains(n)) {//mete as strings todas para lower case
                        res.add(n);
                    }

                }

            }
        }

        if (filter[3]) {//search by id
            for (Note n : notes) {
                if (n.getId().toLowerCase().contains(string.toLowerCase()) && !res.contains(n)) {//mete as strings todas para lower case
                    res.add(n);
                }
            }
        }

        // V2 Filter
        /**
         * Keywords Snapshot Filter Types
         *  #headphones:PLUGGED_IN / #headphones:UNPLUGGED
         *  #temperature:[Operator][Value][Unit]
         *  #humidity: [Operator][Value]
         *  #weather: [CONDITION] ex.: Weather.CONDITION_CLEAR
         *  #location: [Value LAT], [Value LONG], [OPTIONAL Value RADIUS]
         *  #activity: [ACTIVITY CONSTANT]
         *  #place: [PLACE TYPE CONSTANT]
         */
        if (filter[7]) {
            List<Note> aux = new ArrayList<>();

            for (Note n : notes) {
                for (String key : n.getKeywords()) {
                    if (key.startsWith(HEADPHONES) && Singleton.getInstance().getHeadphonesState() != null) {
                        String keyValue = key.substring(HEADPHONES.length());

                        if (keyValue.equals(commonMethods.decodeHeadphonesState(Singleton.getInstance().getHeadphonesState().getState()))) {
                            aux.add(n);
                        }
                    } else if (key.startsWith(TEMPERATURE) && Singleton.getInstance().getWeather() != null) {
                        //Get the sharedPreferences file key Object
                        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);

                        //Gets the currentTemperature according with the defined temp. units.
                        int currentTemp = Math.round(Singleton.getInstance().getWeather().getTemperature(Weather.CELSIUS));
                        if (sharedPref.getString(PREFS_TEMPERATURE_UNIT_KEY, AVAILABLE_TEMPERATURE_UNITS[0]).equals(AVAILABLE_TEMPERATURE_UNITS[1]))
                            currentTemp = Math.round(Singleton.getInstance().getWeather().getTemperature(Weather.FAHRENHEIT));

                        int noteKeywordValue = Integer.parseInt(key.replaceAll("[^0-9.-]", ""));

                        if (key.charAt(TEMPERATURE.length()) == '=' && currentTemp == noteKeywordValue) {
                            aux.add(n);
                            break;
                        } else if (key.charAt(TEMPERATURE.length()) == '≤' && currentTemp <= noteKeywordValue) {
                            aux.add(n);
                            break;
                        } else if (key.charAt(TEMPERATURE.length()) == '≥' && currentTemp >= noteKeywordValue) {
                            aux.add(n);
                            break;
                        } else if (key.charAt(TEMPERATURE.length()) == '≠' && currentTemp != noteKeywordValue) {
                            aux.add(n);
                            break;
                        }
                    } else if (key.startsWith(HUMIDITY) && Singleton.getInstance().getWeather() != null) {
                        int currentHumidity = Math.round(Singleton.getInstance().getWeather().getHumidity());

                        int noteKeywordValue = Integer.parseInt(key.replaceAll("[^0-9]", ""));

                        if (key.charAt(HUMIDITY.length()) == '=' && currentHumidity == noteKeywordValue) {
                            aux.add(n);
                            break;
                        } else if (key.charAt(HUMIDITY.length()) == '≤' && currentHumidity <= noteKeywordValue) {
                            aux.add(n);
                            break;
                        } else if (key.charAt(HUMIDITY.length()) == '≥' && currentHumidity >= noteKeywordValue) {
                            aux.add(n);
                            break;
                        } else if (key.charAt(HUMIDITY.length()) == '≠' && currentHumidity != noteKeywordValue) {
                            aux.add(n);
                            break;
                        }
                    } else if (key.startsWith(WEATHER_CONDITION) && Singleton.getInstance().getWeather() != null) {
                        String keyValue = key.substring(WEATHER_CONDITION.length());
                        ArrayList<String> decodedWeatherConditions = commonMethods.decodeWeatherConditions(Singleton.getInstance().getWeather().getConditions());

                        if(decodedWeatherConditions.contains(keyValue)){
                            aux.add(n);
                            break;
                        }
                    } else if (key.startsWith(LOCATION) && Singleton.getInstance().getLocation() != null) {
                        //TODO CIRCLE AREA
                        //Gets the value of the search radius to draw circles on the map.
                        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
                        float locationSearchRadius = sharedPref.getFloat(PREFS_LOCATION_RADIUS_KEY, LOCATION_DEFAULT_RADIUS);

                        LatLng coordinatesUser = new LatLng(Singleton.getInstance().getLocation().getLatitude(),Singleton.getInstance().getLocation().getLongitude());

                        //Get the Lat and Lng Value
                        String[] split = key.split(",");
                        if(split.length==2) {
                            double lat = Double.parseDouble(split[0].replaceAll("[^0-9.-]", ""));
                            double lng = Double.parseDouble(split[1].replaceAll("[^0-9.-]", ""));
                            LatLng coordinatesKey = new LatLng(lat, lng);

                            double distanceFromCenterToCorner = locationSearchRadius * Math.sqrt(2.0);
                            LatLng southwestCorner = SphericalUtil.computeOffset(coordinatesUser, distanceFromCenterToCorner, 225.0);
                            LatLng northeastCorner = SphericalUtil.computeOffset(coordinatesUser, distanceFromCenterToCorner, 45.0);

                            if(new LatLngBounds(southwestCorner, northeastCorner).contains(coordinatesKey)){
                                aux.add(n);
                                break;
                            }
                        }
                    } else if (key.startsWith(ACTIVITY) && Singleton.getInstance().getActivityRecognitionResult() != null) {
                        String keyValue = key.substring(ACTIVITY.length());
                        String decodedActivityType = commonMethods.decodeActivityType(Singleton.getInstance().getActivityRecognitionResult().getMostProbableActivity().getType());

                        if(keyValue.equals(decodedActivityType)){
                            aux.add(n);
                            break;
                        }
                    } else if (key.startsWith(PLACE_TYPE) && Singleton.getInstance().getPlaces() != null) {
                        String keyValue = key.substring(PLACE_TYPE.length());

                        for (int i = 0; i < NEARBY_PLACES_SHOW_SIZE; i++) {
                            PlaceLikelihood p = Singleton.getInstance().getPlaces().get(i);
                            if(p.getLikelihood()>=NEARBY_PLACES_MINIMUM_LIKELIHOOD) {
                                ArrayList<String> decodedPlacesTypes = commonMethods.decodePlacesTypes(p.getPlace().getPlaceTypes());
                                if(decodedPlacesTypes.contains(keyValue)){
                                    aux.add(n);
                                    break;
                                }
                            }
                        }
                        break;
                    }else if (key.startsWith(TIME_INTERVAL) && Singleton.getInstance().getTimeInterval() != null){
                        String keyValue = key.substring(TIME_INTERVAL.length());

                        ArrayList<String> decodedTimeIntervals = commonMethods.decodeTimeIntervals(Singleton.getInstance().getTimeInterval().getTimeIntervals());

                        if(decodedTimeIntervals.contains(keyValue)){
                            aux.add(n);
                            break;
                        }
                    }
                }
            }


            //Remove Duplicated Notes
            //https://stackoverflow.com/questions/203984/how-do-i-remove-repeated-elements-from-arraylist
            // add elements to al, including duplicates
            Set<Note> hs = new HashSet<>();
            hs.addAll(aux);
            res.addAll(hs);
        }

        if(filter[8]){
            List<Note> aux = new ArrayList<>();

            //TODO Correct the WHITE SPACE!
            //Check if the note contain the web detection value on the keywords or title
            if(!Singleton.getInstance().getCloudVisionResponses()[5].isEmpty()){
                for (Note n : this.notes){
                    for(String cloudVisionWebDetection : Note.trimPlainKeywords(Singleton.getInstance().getCloudVisionResponses()[5])){
                        if(n.getTitle().toLowerCase().contains(cloudVisionWebDetection.trim().toLowerCase())){
                            aux.add(n);
                            break;
                        }else if(n.getPlainKeywords().toLowerCase().contains(cloudVisionWebDetection.trim().toLowerCase())){
                            aux.add(n);
                            break;
                        }
                    }
                }
            }

            //Check if the note contain the label value on the keywords or title
            if(!Singleton.getInstance().getCloudVisionResponses()[0].isEmpty()){
                for (Note n : this.notes){
                    for(String cloudVisionLabel : Note.trimPlainKeywords(Singleton.getInstance().getCloudVisionResponses()[0])){
                        if(n.getTitle().toLowerCase().contains(cloudVisionLabel.trim().toLowerCase())){
                            aux.add(n);
                            break;
                        }else if(n.getPlainKeywords().toLowerCase().contains(cloudVisionLabel.trim().toLowerCase())){
                            aux.add(n);
                            break;
                        }
                    }
                }
            }

            //Check if the note contain the rest of the cloud vision fields on the body text of this
            if(!Singleton.getInstance().getCloudVisionResponses()[1].isEmpty() ||
                    !Singleton.getInstance().getCloudVisionResponses()[2].isEmpty() ||
                    !Singleton.getInstance().getCloudVisionResponses()[3].isEmpty() ||
                    !Singleton.getInstance().getCloudVisionResponses()[4].isEmpty() ) {

                for (Note n : notes) {
                    if (n.getText() != null) {
                        if (n.getText().toLowerCase().contains(Singleton.getInstance().getCloudVisionResponses()[1].toLowerCase())
                                || n.getText().toLowerCase().contains(Singleton.getInstance().getCloudVisionResponses()[2].toLowerCase())
                                || n.getText().toLowerCase().contains(Singleton.getInstance().getCloudVisionResponses()[3].toLowerCase())
                                || n.getText().toLowerCase().contains(Singleton.getInstance().getCloudVisionResponses()[4].toLowerCase())) {
                            aux.add(n);
                        }
                    }
                }
            }

            //Remove Duplicated Notes
            //https://stackoverflow.com/questions/203984/how-do-i-remove-repeated-elements-from-arraylist
            // add elements to al, including duplicates
            Set<Note> hs = new HashSet<>();
            hs.addAll(aux);
            res.addAll(hs);
        }

        if (filter[4] || filter[5]) {//search by contains image or contains video, its inclusive with the text being searched
            if (res.isEmpty()) res.addAll(notes);

            for (Note n : notes) {

                if (n.getPicture() == null && !res.isEmpty() && filter[4]) {
                    if (filter[5]) {
                        if (n.getVideo() == null) res.remove(n);
                    } else {
                        res.remove(n);
                    }
                }

                if (n.getVideo() == null && !res.isEmpty() && filter[5]) {
                    if (filter[4]) {
                        if (n.getPicture() == null) res.remove(n);
                    } else {
                        res.remove(n);
                    }
                }

            }
        }

        if (filter[6] && dateStart != null && dateEnd != null) {//search by date, its inclusive with the text being searched
            int[] dateStartInt = convStringDateInt(dateStart);
            int[] dateEndInt = convStringDateInt(dateEnd);
            if (res.isEmpty()) res.addAll(notes);

            for (Note n : notes) {
                if (!res.isEmpty()) {
                    //Check if the Day DOESN'T matches
                    if (!(n.getDayOfMonth() >= dateStartInt[0] && n.getDayOfMonth() <= dateEndInt[0]))
                        res.remove(n);
                    else {
                        //Check if the Month DOESN'T matches
                        if (!(n.getMonth() >= dateStartInt[1] && n.getMonth() <= dateEndInt[1]))
                            res.remove(n);
                        else {
                            //Check if the Year DOESN'T matches
                            if (!(n.getYear() >= dateStartInt[2] && n.getYear() <= dateEndInt[2]))
                                res.remove(n);
                        }
                    }

                }
            }
        }

        //Sort Notes By Last Edit Date
        Collections.sort(res);
        return res;
    }

    private int[] convStringDateInt(String dateString){
        int[] dateInt = {0,0,0};

        if (dateString==null) return dateInt;
        if (dateString.isEmpty()) return dateInt;

        dateString = dateString.trim();
        String[] splitDate = dateString.split("/");

        dateInt[0] = Integer.parseInt(splitDate[0]);
        dateInt[1] = Integer.parseInt(splitDate[1]);
        dateInt[2] = Integer.parseInt(splitDate[2]);

        return dateInt;
    }

    public void removeNote(Note note){
        notes.remove(note);
    }

    public ArrayList<Note> getNotes() {
        //Sort Notes By Last Edit Date
        Collections.sort(notes);
        return notes;
    }


    @Override
    public String toString() {
        String res = "";
        for (Note n : notes) {
            res += n + "\n";
        }
        return res;
    }


}
