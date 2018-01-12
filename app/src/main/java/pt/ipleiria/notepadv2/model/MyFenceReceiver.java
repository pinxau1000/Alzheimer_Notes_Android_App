package pt.ipleiria.notepadv2.model;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Project: NotepadV2
 * Package: ${PACKAGE_NAME}
 *
 * Created by Octávio on 09/12/2017.
 */

public class MyFenceReceiver extends BroadcastReceiver implements AppConstants {

    private AlertDialog alertDialog;

    @Override
    public void onReceive(Context context, Intent intent) {
        FenceState fenceState = FenceState.extract(intent);

        if (fenceState.getFenceKey().startsWith(FENCE_KEY_HEADPHONES_HEADER) && fenceState.getCurrentState() != FenceState.UNKNOWN){
            AlertDialog.Builder builder = null;
            String match_titles = "";

            for (Note note : Singleton.getInstance().getNotepad().getNotes()) {
                String keywordHeadphonesState = note.getKeywordHeadphonesState();

                if (!keywordHeadphonesState.isEmpty()){
                    if (fenceState.getFenceKey().equals(FENCE_KEY_HEADPHONES_PLUGGING) &&
                            fenceState.getCurrentState()==FenceState.TRUE &&
                            keywordHeadphonesState.equals(AVAILABLE_HEADPHONES_TYPES[0])) {

                        builder = new AlertDialog.Builder(context).setTitle("Headphones " + AVAILABLE_HEADPHONES_TYPES[0]);
                        match_titles += "• " + note.getTitle() + "\n";
                        Log.d(TAG_FENCE_RECEIVER, "Headphones: " + AVAILABLE_HEADPHONES_TYPES[0] + "; Note ID: " + note.getId());
                    }else if (fenceState.getFenceKey().equals(FENCE_KEY_HEADPHONES_UNPLUGGING) &&
                            fenceState.getCurrentState() == FenceState.FALSE &&
                            keywordHeadphonesState.equals(AVAILABLE_HEADPHONES_TYPES[1])) {

                        builder = new AlertDialog.Builder(context).setTitle("Headphones " + AVAILABLE_HEADPHONES_TYPES[1]);
                        match_titles += "• " + note.getTitle() + "\n";
                        Log.d(TAG_FENCE_RECEIVER, "Headphones: " + AVAILABLE_HEADPHONES_TYPES[1] + "; NoteID: " + note.getId());
                    }
                }
            }

            if(builder!=null && !match_titles.isEmpty()){
                builder.setMessage(match_titles);

                if(alertDialog!=null) alertDialog.dismiss();
                alertDialog = builder.show();
            }

        }else if (fenceState.getFenceKey().startsWith(FENCE_KEY_LOCATION) && fenceState.getCurrentState()!=FenceState.UNKNOWN){
            for(Note note : Singleton.getInstance().getNotepad().getNotes()){
                ArrayList<LatLng> keywordsLocation = note.getKeywordsLocation();

                //Check if the keys contains the note id and have keywords with location filter
                if(keywordsLocation!=null && fenceState.getFenceKey().contains(note.getId())){
                    for (LatLng latLng : keywordsLocation){
                        //Match
                        if(fenceState.getFenceKey().contains(latLng.toString())){
                            if(fenceState.getCurrentState()==FenceState.TRUE) {
                                if (alertDialog != null) alertDialog.dismiss();
                                alertDialog = new AlertDialog.Builder(context)
                                        .setTitle("User Inside Location")
                                        .setMessage("• " + note.getTitle()).show();

                                Log.d(TAG_FENCE_RECEIVER, "User INSIDE Location; NoteID: " + note.getId() + " LatLng: " + latLng.toString());
                            }
                        }
                    }
                }
            }

        }else if(fenceState.getFenceKey().startsWith(FENCE_KEY_ACTIVITY_HEADER) && fenceState.getCurrentState()==FenceState.TRUE){
            AlertDialog.Builder builder = null;
            String match_titles = "";

            for (Note n : Singleton.getInstance().getNotepad().getNotes()){
                if(!n.getKeywordsActivities().isEmpty()){
                    String triggerEvent = fenceState.getFenceKey().substring(FENCE_KEY_ACTIVITY_HEADER.length());
                    if(n.getKeywordsActivities().contains(triggerEvent)){
                        builder = new AlertDialog.Builder(context).setTitle("Activity "+triggerEvent);
                        match_titles += "• " + n.getTitle() + "\n";
                        Log.d(TAG_FENCE_RECEIVER, "Activity: "+triggerEvent+"; Note ID: " + n.getId());
                    }
                }
            }

            if(builder!=null && !match_titles.isEmpty()){
                builder.setMessage(match_titles);

                if(alertDialog!=null) alertDialog.dismiss();
                alertDialog = builder.show();
            }
        }else if(fenceState.getFenceKey().startsWith(FENCE_KEY_TIME_INTERVAL_HEADER) && fenceState.getCurrentState()==FenceState.TRUE){
            AlertDialog.Builder builder = null;
            String match_titles = "";

            for (Note n : Singleton.getInstance().getNotepad().getNotes()) {
                if (!n.getKeywordsTimeInterval().isEmpty()) {
                    String triggerEvent = fenceState.getFenceKey().substring(FENCE_KEY_TIME_INTERVAL_HEADER.length());

                    if(n.getKeywordsTimeInterval().contains(triggerEvent)){
                        builder = new AlertDialog.Builder(context).setTitle("Time Interval "+triggerEvent);
                        match_titles += "• " + n.getTitle() + "\n";
                        Log.d(TAG_FENCE_RECEIVER, "Time Interval: "+triggerEvent+"; Note ID: " + n.getId());

                    }
                }
            }

            if(builder!=null && !match_titles.isEmpty()){
                builder.setMessage(match_titles);

                if(alertDialog!=null) alertDialog.dismiss();
                alertDialog = builder.show();
            }
        }
    }

}
