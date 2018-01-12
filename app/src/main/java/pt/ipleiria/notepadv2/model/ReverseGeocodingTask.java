package pt.ipleiria.notepadv2.model;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static pt.ipleiria.notepadv2.model.AppConstants.TAG_REVERSE_GEOCODE;

public class ReverseGeocodingTask extends AsyncTask<Object, Void, String> {

    private List<Address> addresses = null;

    @Override
    protected String doInBackground(Object... params) {
        if(params.length!=2){
            Log.e(TAG_REVERSE_GEOCODE, "Invalid params for the reverse geocoding task.");
            return "Error";
        }

        Context context = (Context) params[0];
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        Location location = (Location) params[1];
        try {
            // Call the synchronous getFromLocation() method by passing in the lat/long values.
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG_REVERSE_GEOCODE, "Unable to get the location address");
            // Update UI field with the exception.
        }
        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);

            // Format the first line of address (if available), city, and country name.
            return String.format("%s", address.getAddressLine(0));
        }else{
            Log.e(TAG_REVERSE_GEOCODE, "No addresses found for the given location.");
            return "Error";
        }
    }

}
