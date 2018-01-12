package pt.ipleiria.notepadv2.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import pt.ipleiria.notepadv2.R;

/**
 * Created by Miguel on 26/10/2017.
 */

public class CustomAdapter extends ArrayAdapter<Note> {

    public CustomAdapter(Context context,ArrayList<Note> notes ) {
        super(context,0,notes);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Note note = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.custom_adapter, parent, false);
        }

        // Lookup view for data population
        TextView tvTitle = convertView.findViewById(R.id.textView_title);
        TextView tvDate = convertView.findViewById(R.id.textView_date);
        TextView keywordsTextView = convertView.findViewById(R.id.textView_keywords);
        ImageView imagev = convertView.findViewById(R.id.imageView);

        tvTitle.setText(note.getTitle());
        tvDate.setText(note.getLast_edit_date());
        keywordsTextView.setText(note.getKeywordsString());

        int ctype = note.getContentType();
        if(ctype == 1) imagev.setImageResource(R.mipmap.ic_text);
        else if(ctype == 2) imagev.setImageResource(R.mipmap.ic_photo);
        else if(ctype == 3) imagev.setImageResource(R.mipmap.ic_video);
        else if(ctype == 0) imagev.setImageResource(R.mipmap.ic_blank);
        else imagev.setImageResource(R.mipmap.ic_multinote);

        // Return the completed view to render on screen
        return convertView;
    }

}
