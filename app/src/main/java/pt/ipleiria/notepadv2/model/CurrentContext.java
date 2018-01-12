package pt.ipleiria.notepadv2.model;

import android.support.annotation.NonNull;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.DetectedActivityResponse;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResponse;
import com.google.android.gms.awareness.snapshot.LocationResponse;
import com.google.android.gms.awareness.snapshot.PlacesResponse;
import com.google.android.gms.awareness.snapshot.TimeIntervalsResponse;
import com.google.android.gms.awareness.snapshot.WeatherResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Arrays;

public class CurrentContext extends AsyncTask<Context, Integer, Boolean> implements AppConstants{

    @SuppressLint("MissingPermission")
    @Override
    protected Boolean doInBackground(@NonNull Context... contexts) {

        final int[] progress = {0};

        final Status[] status = new Status[(int)NUMBER_OF_TASKS_AWARENESS_API];
        final Status[] done = new Status[(int)NUMBER_OF_TASKS_AWARENESS_API];

        //Initiating Vars
        for (int i = 0; i<(int)NUMBER_OF_TASKS_AWARENESS_API; i++) {
            status[i] = Status.PENDING;
        }
        for (int i = 0; i<(int)NUMBER_OF_TASKS_AWARENESS_API; i++) {
            done[i] = Status.FINISHED;
        }

        Awareness.getSnapshotClient(contexts[0]).getHeadphoneState().addOnCompleteListener(new OnCompleteListener<HeadphoneStateResponse>() {
            @Override
            public void onComplete(@NonNull Task<HeadphoneStateResponse> task) {
                if(task.isSuccessful()){
                    Singleton.getInstance().setHeadphonesState(task.getResult().getHeadphoneState());
                }else{
                    task.getException().printStackTrace();
                    Log.e(TAG_GETTING_CONTEXT, "-> Headphones: UNABLE TO GET HEADPHONES STATE");
                    Singleton.getInstance().setHeadphonesState(null);
                }
                Log.d(TAG_FENCE_RECEIVER, "-> Headphones: FINISH");
                progress[0]+=Math.ceil(100/NUMBER_OF_TASKS_AWARENESS_API);
                publishProgress(progress[0]);
                status[0] = Status.FINISHED;
            }
        });

        Awareness.getSnapshotClient(contexts[0]).getTimeIntervals().addOnCompleteListener(new OnCompleteListener<TimeIntervalsResponse>() {
            @Override
            public void onComplete(@NonNull Task<TimeIntervalsResponse> task) {
                if (task.isSuccessful()) {
                    Singleton.getInstance().setTimeInterval(task.getResult().getTimeIntervals());
                } else {
                    task.getException().printStackTrace();
                    Log.e(TAG_GETTING_CONTEXT, "-> Time Interval: UNABLE TO GET TIME INTERVAL");
                    Singleton.getInstance().setTimeInterval(null);
                }
                Log.d(TAG_FENCE_RECEIVER, "-> TimeIntervals: FINISH");
                progress[0] +=Math.ceil(100/NUMBER_OF_TASKS_AWARENESS_API);
                publishProgress(progress[0]);
                status[5] = Status.FINISHED;
            }
        });

        Awareness.getSnapshotClient(contexts[0]).getWeather().addOnCompleteListener(new OnCompleteListener<WeatherResponse>() {
            @Override
            public void onComplete(@NonNull Task<WeatherResponse> task) {
                if(task.isSuccessful()){
                    Singleton.getInstance().setWeather(task.getResult().getWeather());
                }else{
                    task.getException().printStackTrace();
                    Log.e(TAG_GETTING_CONTEXT, "-> Weather: UNABLE TO GET WEATHER");
                    Singleton.getInstance().setWeather(null);
                }
                Log.d(TAG_FENCE_RECEIVER, "-> Weather: FINISH");
                progress[0] +=Math.ceil(100/NUMBER_OF_TASKS_AWARENESS_API);
                publishProgress(progress[0]);
                status[1] = Status.FINISHED;
            }
        });

        Awareness.getSnapshotClient(contexts[0]).getLocation().addOnCompleteListener(new OnCompleteListener<LocationResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationResponse> task) {
                if(task.isSuccessful()){
                    Singleton.getInstance().setLocation(task.getResult().getLocation());
                }else{
                    Log.e(TAG_GETTING_CONTEXT, "-> Location: UNABLE TO GET LOCATION");
                    task.getException().printStackTrace();
                    Singleton.getInstance().setLocation(null);
                }
                Log.d(TAG_FENCE_RECEIVER, "-> Location: FINISH");
                progress[0] +=Math.ceil(100/NUMBER_OF_TASKS_AWARENESS_API);
                publishProgress(progress[0]);
                status[2] = Status.FINISHED;
            }
        });


        Awareness.getSnapshotClient(contexts[0]).getDetectedActivity().addOnCompleteListener(new OnCompleteListener<DetectedActivityResponse>() {
            @Override
            public void onComplete(@NonNull Task<DetectedActivityResponse> task) {
                if (task.isSuccessful()) {
                    Singleton.getInstance().setActivityRecognitionResult(task.getResult().getActivityRecognitionResult());
                } else {
                    task.getException().printStackTrace();
                    Log.e(TAG_GETTING_CONTEXT, "-> Detected Activity: UNABLE TO GET ACTIVITY RECOGNITION");
                    Singleton.getInstance().setActivityRecognitionResult(null);
                }
                Log.d(TAG_FENCE_RECEIVER, "-> ActivityRecognition: FINISH");
                progress[0] +=Math.ceil(100/NUMBER_OF_TASKS_AWARENESS_API);
                publishProgress(progress[0]);
                status[3] = Status.FINISHED;
            }
        });

        Awareness.getSnapshotClient(contexts[0]).getPlaces().addOnCompleteListener(new OnCompleteListener<PlacesResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlacesResponse> task) {
                if (task.isSuccessful()) {
                    Singleton.getInstance().setPlaces(task.getResult().getPlaceLikelihoods());
                } else {
                    task.getException().printStackTrace();
                    Log.e(TAG_GETTING_CONTEXT, "-> Nearby Places: UNABLE TO GET NEARBY PLACES");
                    Singleton.getInstance().setPlaces(null);
                }
                Log.d(TAG_FENCE_RECEIVER, "-> Places: FINISH");
                progress[0] +=Math.ceil(100/NUMBER_OF_TASKS_AWARENESS_API);
                publishProgress(progress[0]);
                status[4] = Status.FINISHED;
            }
        });

        /*
        Awareness.getSnapshotClient(contexts[0]).getTimeIntervals().addOnCompleteListener(new OnCompleteListener<TimeIntervalsResponse>() {
            @Override
            public void onComplete(@NonNull Task<TimeIntervalsResponse> task) {
                if (task.isSuccessful()) {
                    Singleton.getInstance().setTimeInterval(task.getResult().getTimeIntervals());
                } else {
                    task.getException().printStackTrace();
                    Log.e(TAG_GETTING_CONTEXT, "-> Time Interval: UNABLE TO GET TIME INTERVAL");
                    Singleton.getInstance().setTimeInterval(null);
                }
                Log.d(TAG_FENCE_RECEIVER, "-> TimeIntervals: FINISH");
                progress[0] +=Math.ceil(100/NUMBER_OF_TASKS_AWARENESS_API);
                publishProgress(progress[0]);
                status[5] = Status.FINISHED;
            }
        });
        */

        while(true){
            if(isCancelled()){
                return false;
            }else if(Arrays.equals(status, done)){
                return true;
            }
        }
    }
}
