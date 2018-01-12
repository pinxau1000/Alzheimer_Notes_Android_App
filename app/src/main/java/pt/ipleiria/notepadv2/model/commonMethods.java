package pt.ipleiria.notepadv2.model;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.FenceClient;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.TimeFence;
import com.google.android.gms.awareness.snapshot.DetectedActivityResponse;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResponse;
import com.google.android.gms.awareness.snapshot.LocationResponse;
import com.google.android.gms.awareness.snapshot.PlacesResponse;
import com.google.android.gms.awareness.snapshot.TimeIntervalsResponse;
import com.google.android.gms.awareness.snapshot.WeatherResponse;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.TimeIntervals;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import pt.ipleiria.notepadv2.MainActivity;

/**
 * Project: NotepadV2
 * Package: pt.ipleiria.notepadv2.model
 * <p>
 * Created by Octávio on 11/12/2017.
 */

public abstract class commonMethods implements AppConstants {

    public static MyFenceReceiver myFenceReceiver = new MyFenceReceiver();

    public static void registerFence(@NonNull final Context context, @NonNull final String fenceKey, @NonNull final AwarenessFence fence) {
        FenceClient fenceClient = Awareness.getFenceClient(context);

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        fenceClient.updateFences(new FenceUpdateRequest.Builder()
                .addFence(fenceKey, fence, mPendingIntent)
                .build()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Log.d(TAG_FENCE_REGIST, "Fence " + fenceKey + " registered.");
                }else{
                    task.getException().printStackTrace();
                    Log.e(TAG_FENCE_REGIST, "Fence " + fenceKey + " NOT registered.");
                }
            }
        });
    }

    public static void unregisterFence(@NonNull final Context context, @NonNull final String fenceKey) {
        FenceClient fenceClient = Awareness.getFenceClient(context);

        fenceClient.updateFences(new FenceUpdateRequest.Builder()
                .removeFence(fenceKey)
                .build()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Log.d(TAG_FENCE_REGIST, "Fence " + fenceKey + " removed.");
                }else{
                    task.getException().printStackTrace();
                    Log.e(TAG_FENCE_REGIST, "Fence " + fenceKey + " NOT removed.");
                }
            }
        });
    }

    public static String decodeHeadphonesState(int headphonesState){
        switch (headphonesState){
            case HeadphoneState.PLUGGED_IN:
                return AVAILABLE_HEADPHONES_TYPES[0];
            case HeadphoneState.UNPLUGGED:
                return AVAILABLE_HEADPHONES_TYPES[1];
            default: return null;
        }
    }

    public static ArrayList<String> decodeWeatherConditions(int... weatherConditions){
        ArrayList<String> decodedWeathers = new ArrayList<>();

        for (int weatherCondition : weatherConditions) {
            decodedWeathers.add(decodeWeatherCondition(weatherCondition));
        }
        if(decodedWeathers.isEmpty())
            return null;

        return decodedWeathers;
    }

    private static String decodeWeatherCondition(int weatherCondition){
        switch (weatherCondition){
            case Weather.CONDITION_CLEAR:
                return AVAILABLE_WEATHER_TYPES[1];
            case Weather.CONDITION_CLOUDY:
                return AVAILABLE_WEATHER_TYPES[2];
            case Weather.CONDITION_FOGGY:
                return AVAILABLE_WEATHER_TYPES[3];
            case Weather.CONDITION_HAZY:
                return AVAILABLE_WEATHER_TYPES[4];
            case Weather.CONDITION_ICY:
                return AVAILABLE_WEATHER_TYPES[5];
            case Weather.CONDITION_RAINY:
                return AVAILABLE_WEATHER_TYPES[6];
            case Weather.CONDITION_SNOWY:
                return AVAILABLE_WEATHER_TYPES[7];
            case Weather.CONDITION_STORMY:
                return AVAILABLE_WEATHER_TYPES[8];
            case Weather.CONDITION_WINDY:
                return AVAILABLE_WEATHER_TYPES[9];
            case Weather.CONDITION_UNKNOWN:
                return AVAILABLE_WEATHER_TYPES[0];
            default: return null;
        }
    }

    public static String decodeActivityType(int type) {
        switch(type){
            case DetectedActivity.IN_VEHICLE:
                return AVAILABLE_ACTIVITY_TYPES[0];
            case DetectedActivity.ON_BICYCLE:
                return AVAILABLE_ACTIVITY_TYPES[1];
            case DetectedActivity.ON_FOOT:
                return AVAILABLE_ACTIVITY_TYPES[2];
            case DetectedActivity.RUNNING:
                return AVAILABLE_ACTIVITY_TYPES[3];
            case DetectedActivity.STILL:
                return AVAILABLE_ACTIVITY_TYPES[4];
            case DetectedActivity.TILTING:
                return AVAILABLE_ACTIVITY_TYPES[5];
            case DetectedActivity.WALKING:
                return AVAILABLE_ACTIVITY_TYPES[6];
            case DetectedActivity.UNKNOWN:
            default: return null;

        }
    }

    public static ArrayList<String> decodeTimeIntervals(int... timeIntervals) {
        ArrayList<String> decodedTimeIntervals = new ArrayList<>();

        for (int timeInterval : timeIntervals) {
            decodedTimeIntervals.add(decodeTimeIntervals(timeInterval));
        }
        if(decodedTimeIntervals.isEmpty())
            return null;

        return decodedTimeIntervals;
    }

    private static String decodeTimeIntervals(int timeIntervals){
        switch(timeIntervals){
            case TimeFence.TIME_INTERVAL_WEEKDAY:
                return AVAILABLE_TIME_INTERVALS[0];
            case TimeFence.TIME_INTERVAL_WEEKEND:
                return AVAILABLE_TIME_INTERVALS[1];
            case TimeFence.TIME_INTERVAL_HOLIDAY:
                return AVAILABLE_TIME_INTERVALS[2];
            case TimeFence.TIME_INTERVAL_MORNING:
                return AVAILABLE_TIME_INTERVALS[3];
            case TimeFence.TIME_INTERVAL_AFTERNOON:
                return AVAILABLE_TIME_INTERVALS[4];
            case TimeFence.TIME_INTERVAL_EVENING:
                return AVAILABLE_TIME_INTERVALS[5];
            case TimeFence.TIME_INTERVAL_NIGHT:
                return AVAILABLE_TIME_INTERVALS[6];
            default: return null;
        }
    }

    public static ArrayList<String> decodePlacesTypes(List<Integer> placeTypes){
        ArrayList<String> decodedPlaces = new ArrayList<>();

        for (int placeType : placeTypes) {
            decodedPlaces.add(decodePlacesType(placeType));
        }
        if(decodedPlaces.isEmpty())
            return null;

        return decodedPlaces;
    }

    public static String decodePlacesType(int placeType){
        switch (placeType) {
            case 0:
                return "OTHER";
            case 1:
                return "ACCOUNTING";
            case 2:
                return "AIRPORT";
            case 3:
                return "AMUSEMENT_PARK";
            case 4:
                return "AQUARIUM";
            case 5:
                return "ART_GALLERY";
            case 6:
                return "ATM";
            case 7:
                return "BAKERY";
            case 8:
                return "BANK";
            case 9:
                return "BAR";
            case 10:
                return "BEAUTY_SALON";
            case 11:
                return "BICYCLE_STORE";
            case 12:
                return "BOOK_STORE";
            case 13:
                return "BOWLING_ALLEY";
            case 14:
                return "BUS_STATION";
            case 15:
                return "CAFE";
            case 16:
                return "CAMPGROUND";
            case 17:
                return "CAR_DEALER";
            case 18:
                return "CAR_RENTAL";
            case 19:
                return "CAR_REPAIR";
            case 20:
                return "CAR_WASH";
            case 21:
                return "CASINO";
            case 22:
                return "CEMETERY";
            case 23:
                return "CHURCH";
            case 24:
                return "CITY_HALL";
            case 25:
                return "CLOTHING_STORE";
            case 26:
                return "CONVENIENCE_STORE";
            case 27:
                return "COURTHOUSE";
            case 28:
                return "DENTIST";
            case 29:
                return "DEPARTMENT_STORE";
            case 30:
                return "DOCTOR";
            case 31:
                return "ELECTRICIAN";
            case 32:
                return "ELECTRONICS_STORE";
            case 33:
                return "EMBASSY";
            case 34:
                return "ESTABLISHMENT";
            case 35:
                return "FINANCE";
            case 36:
                return "FIRE_STATION";
            case 37:
                return "FLORIST";
            case 38:
                return "FOOD";
            case 39:
                return "FUNERAL_HOME";
            case 40:
                return "FURNITURE_STORE";
            case 41:
                return "GAS_STATION";
            case 42:
                return "GENERAL_CONTRACTOR";
            case 43:
                return "GROCERY_OR_SUPERMARKET";
            case 44:
                return "GYM";
            case 45:
                return "HAIR_CARE";
            case 46:
                return "HARDWARE_STORE";
            case 47:
                return "HEALTH";
            case 48:
                return "HINDU_TEMPLE";
            case 49:
                return "HOME_GOODS_STORE";
            case 50:
                return "HOSPITAL";
            case 51:
                return "INSURANCE_AGENCY";
            case 52:
                return "JEWELRY_STORE";
            case 53:
                return "LAUNDRY";
            case 54:
                return "LAWYER";
            case 55:
                return "LIBRARY";
            case 56:
                return "LIQUOR_STORE";
            case 57:
                return "LOCAL_GOVERNMENT_OFFICE";
            case 58:
                return "LOCKSMITH";
            case 59:
                return "LODGING";
            case 60:
                return "MEAL_DELIVERY";
            case 61:
                return "MEAL_TAKEAWAY";
            case 62:
                return "MOSQUE";
            case 63:
                return "MOVIE_RENTAL";
            case 64:
                return "MOVIE_THEATER";
            case 65:
                return "MOVING_COMPANY";
            case 66:
                return "MUSEUM";
            case 67:
                return "NIGHT_CLUB";
            case 68:
                return "PAINTER";
            case 69:
                return "PARK";
            case 70:
                return "PARKING";
            case 71:
                return "PET_STORE";
            case 72:
                return "PHARMACY";
            case 73:
                return "PHYSIOTHERAPIST";
            case 74:
                return "PLACE_OF_WORSHIP";
            case 75:
                return "PLUMBER";
            case 76:
                return "POLICE";
            case 77:
                return "POST_OFFICE";
            case 78:
                return "REAL_ESTATE_AGENCY";
            case 79:
                return "RESTAURANT";
            case 80:
                return "ROOFING_CONTRACTOR";
            case 81:
                return "RV_PARK";
            case 82:
                return "SCHOOL";
            case 83:
                return "SHOE_STORE";
            case 84:
                return "SHOPPING_MALL";
            case 85:
                return "SPA";
            case 86:
                return "STADIUM";
            case 87:
                return "STORAGE";
            case 88:
                return "STORE";
            case 89:
                return "SUBWAY_STATION";
            case 90:
                return "SYNAGOGUE";
            case 91:
                return "TAXI_STAND";
            case 92:
                return "TRAIN_STATION";
            case 93:
                return "TRAVEL_AGENCY";
            case 94:
                return "UNIVERSITY";
            case 95:
                return "VETERINARY_CARE";
            case 96:
                return "ZOO";
            case 1001:
                return "ADMINISTRATIVE_AREA_LEVEL_1";
            case 1002:
                return "ADMINISTRATIVE_AREA_LEVEL_2";
            case 1003:
                return "ADMINISTRATIVE_AREA_LEVEL_3";
            case 1004:
                return "COLLOQUIAL_AREA";
            case 1005:
                return "COUNTRY";
            case 1006:
                return "FLOOR";
            case 1007:
                return "GEOCODE";
            case 1008:
                return "INTERSECTION";
            case 1009:
                return "LOCALITY";
            case 1010:
                return "NATURAL_FEATURE";
            case 1011:
                return "NEIGHBORHOOD";
            case 1012:
                return "POLITICAL";
            case 1013:
                return "POINT_OF_INTEREST";
            case 1014:
                return "POST_BOX";
            case 1015:
                return "POSTAL_CODE";
            case 1016:
                return "POSTAL_CODE_PREFIX";
            case 1017:
                return "POSTAL_TOWN";
            case 1018:
                return "PREMISE";
            case 1019:
                return "ROOM";
            case 1020:
                return "ROUTE";
            case 1021:
                return "STREET_ADDRESS";
            case 1022:
                return "SUBLOCALITY";
            case 1023:
                return "SUBLOCALITY_LEVEL_1";
            case 1024:
                return "SUBLOCALITY_LEVEL_2";
            case 1025:
                return "SUBLOCALITY_LEVEL_3";
            case 1026:
                return "SUBLOCALITY_LEVEL_4";
            case 1027:
                return "SUBLOCALITY_LEVEL_5";
            case 1028:
                return "SUBPREMISE";
            case 1029:
                return "SYNTHETIC_GEOCODE";
            case 1030:
                return "TRANSIT_STATION";
            default:
                return null;
        }
    }


    /**
     * Check if file exist.
     * @param path The file path.
     * @return true if file exist and is not a directory.
     */
    public static boolean fileExist(String path){
        if(path==null) return false;
        File f = new File(path);
        return f.exists() && f.isFile();
    }

    /**
     * Returns a file path according to the file URI.
     * FROM: https://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
     * @param context the context.
     * @param uri the file URI.
     * @return The file path.
     * @throws URISyntaxException URI syntax is invalid.
     * @throws SecurityException When the app doesn't have storage permissions.
     */
    @SuppressLint("NewApi")
    public static String getFilePath(Context context, Uri uri) throws URISyntaxException, SecurityException {
        String selection = null;
        String[] selectionArgs = null;
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        if (Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{
                        split[1]
                };
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor;
            try {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Part of the getFilePath() method.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * Part of the getFilePath() method.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * Part of the getFilePath() method.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
