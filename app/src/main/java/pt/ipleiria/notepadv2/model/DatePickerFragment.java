package pt.ipleiria.notepadv2.model;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.app.Fragment;
import android.app.Dialog;
import java.util.Calendar;

/**
 * A simple {@link Fragment} subclass.
 */
public class DatePickerFragment extends DialogFragment  {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        //Use the current date as the default date in the date picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        //Create a new DatePickerDialog instance and return it
        /*
            DatePickerDialog Public Constructors - Here we uses first one
            public DatePickerDialog (Context context, DatePickerDialog.OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth)
            public DatePickerDialog (Context context, int theme, DatePickerDialog.OnDateSetListener listener, int year, int monthOfYear, int dayOfMonth)
         */
        //return new DatePickerDialog(getActivity(), this, year, month, day);
       // return new DatePickerDialog(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_DARK,this, year, month, day);
        //return new DatePickerDialog(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT, this, year, month, day);
        //return new DatePickerDialog(getActivity(), AlertDialog.THEME_HOLO_DARK, this, year, month, day);
        DatePickerDialog dpd =  new DatePickerDialog(getActivity(), AlertDialog.THEME_HOLO_LIGHT, (DatePickerDialog.OnDateSetListener)getActivity(), year, month, day);
        //return new DatePickerDialog(getActivity(), AlertDialog.THEME_TRADITIONAL, this, year, month, day);
        dpd.setTitle("Date to Search");
        return dpd;
    }
   //public void onDateSet(DatePicker view, int year, int month, int day) {
   //    //Do something with the date chosen by the user
   //    TextView tv = (TextView) getActivity().findViewById(R.id.in_date);
   //    tv.setText("Date changed...");
   //    tv.setText(tv.getText() + "\nYear: " + year);
   //    tv.setText(tv.getText() + "\nMonth: " + month);
   //    tv.setText(tv.getText() + "\nDay of Month: " + day);
   //    String stringOfDate = day + "/" + month + "/" + year;
   //    tv.setText(tv.getText() + "\n\nFormatted date: " + stringOfDate);
   //}


}