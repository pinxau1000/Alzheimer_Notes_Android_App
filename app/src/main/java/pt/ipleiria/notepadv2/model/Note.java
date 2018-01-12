package pt.ipleiria.notepadv2.model;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;


/**
 * Created by Miguel on 24/10/2017.
 */

public class Note implements Serializable, Comparable<Note>, AppConstants {
    private String id;
    private String title;
    private ArrayList<String> keywords;
    private Calendar creation_date = new GregorianCalendar();
    private Calendar last_edit_date = new GregorianCalendar();
    private String text = "";
    private File picture;
    private String video;

    public Note( String title, String keywords) {
        this.title = title;
        //keywords recebidas como String simples
        this.keywords = trimPlainKeywords(keywords);

        this.id = UUID.randomUUID().toString(); //creates unique id for the note
        this.creation_date = Calendar.getInstance();
        this.last_edit_date = Calendar.getInstance();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title.trim();
    }

    public ArrayList<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = trimPlainKeywords(keywords);
        removeDuplicatedHeadphones();
    }

    /**
     * Used ONLY TO give the keywords to the adapter.
     * @return
     */
    public String getKeywordsString() {
        StringBuilder key = new StringBuilder();
        if(keywords != null){
            if(keywords.size()>3){
                for (int i = 0; i < 3; i++) {
                    key.append("• ").append(keywords.get(i)).append("\n");
                }
                key.append("   …");
            }else{
                for (String si : keywords) {
                    key.append("• ").append(si).append("\n");
                }
            }
        }else{
            key.append("No Keywords");
        }
        return key.toString();
    }

    /**
     *
     * @param plainKeywords The String to be converted to the ArrayList of keywords
     * @return
     */
    public static ArrayList<String> trimPlainKeywords(String plainKeywords) {
        ArrayList<String> kw_array_aux = new ArrayList<>();
        String[] kw_array = plainKeywords.trim().split(";");

        for (String kw_array_i : kw_array) {
            kw_array_aux.add(kw_array_i.trim());
        }

        return kw_array_aux;
    }

    /**
     *
     * @return The Array of Keywords as a single string with each element separated by semicolon.
     */
    public String getPlainKeywords(){
        String aux = "";

        for (String str_i : this.keywords) {
            aux += " "+str_i+";";
        }

        return aux;
    }

    public String getCreation_date() {
        SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm:ss");
        return date.format(this.creation_date.getTime());
    }

    public int getYear(){
        return this.creation_date.get(Calendar.YEAR);
    }

    public int getMonth(){
        return (this.creation_date.get(Calendar.MONTH))+1;
    }

    public int getDayOfMonth(){
        return this.creation_date.get(Calendar.DAY_OF_MONTH);
    }

    public String getLast_edit_date() {
        SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm:ss");
        return date.format(this.last_edit_date.getTime());
    }

    public void setLast_edit_date(){
        this.last_edit_date = Calendar.getInstance();
    }

    public Calendar getLastEditDate() {
        return this.last_edit_date;
    }

    public String getText() {
        return text;
    }

    public void setText(String bodyText) {
        if(bodyText!=null) this.text = bodyText.trim();
        else this.text = "";
    }

    public File getPicture() {
        return picture;
    }

    public void setPicture(File picture) {
        this.picture = picture;
    }

    public String getVideo() {
        return video;
    }

    public void setVideo(String video) {
        this.video = video;
    }

    public int getContentType() {
        int r = 0;
        if(!text.isEmpty() && picture == null && video == null){      //text only
            r = 1;
        }else if(text.isEmpty() && picture != null && video == null){ //picture only
            r = 2;
        }else if(text.isEmpty() && picture == null && video != null){ //video only
            r = 3;
        }else if(!text.isEmpty() && picture != null && video == null){ //text and picture
            r = 4;
        }else if(!text.isEmpty() && picture == null && video != null){ //text and video
            r = 5;
        }else if(!text.isEmpty() && picture != null && video != null){ //text picture and video
            r = 6;
        }else if(text.isEmpty() && picture != null && video != null){ //picture and video
            r = 7;
        }

        return r ;
    }

    @Override
    public String toString() {
        return "ID:" + this.id + "\nTitle:" + this.title + "\nKeywords: " + this.keywords;
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass() != this.getClass())
            return false;

        Note nota = (Note) o;

        if(!this.id.equals(nota.getId())){
            return false;
        }
        if(!this.creation_date.equals(nota.creation_date)){
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(@NonNull Note n) {
        return n.getLastEditDate().getTime().compareTo(this.last_edit_date.getTime());
    }

    //TODO Part 2
    public ArrayList<LatLng> getKeywordsLocation(){
        ArrayList<LatLng> keywordLatLng = new ArrayList<>();
        for (String key:this.keywords) {
            if (key.startsWith(LOCATION)) {
                //Get the Lat and Lng Value
                String[] split = key.split(",");
                if (split.length == 2) {
                    keywordLatLng.add(new LatLng(Double.parseDouble(split[0].replaceAll("[^0-9.-]", "")),
                            Double.parseDouble(split[1].replaceAll("[^0-9.-]", ""))));
                }
            }
        }
        return keywordLatLng;
    }

    private void removeDuplicatedHeadphones(){
        ArrayList<String> aux = new ArrayList<>();

        for(String key : this.keywords){
            if(key.startsWith(HEADPHONES))
                aux.add(key);
        }

        if(aux.size()>1){
            String key_aux = aux.get(aux.size()-1);
            this.keywords.removeAll(aux);
            this.keywords.add(key_aux);
        }
    }

    public String getKeywordHeadphonesState() {
        for (String key : this.keywords) {
            if (key.startsWith(HEADPHONES)) {
                String keyValue = key.substring(HEADPHONES.length());

                if (keyValue.equalsIgnoreCase(AVAILABLE_HEADPHONES_TYPES[0])) {
                    return AVAILABLE_HEADPHONES_TYPES[0];
                } else if (keyValue.equalsIgnoreCase(AVAILABLE_HEADPHONES_TYPES[1])) {
                    return AVAILABLE_HEADPHONES_TYPES[1];
                }
            }
        }
        return "";
    }

    public ArrayList<String> getKeywordsActivities() {
        ArrayList<String> keywordsActivities = new ArrayList<>();

        //To make easier comparations between keyValue and the Available Activities Types Array.
        ArrayList<String> aux = new ArrayList<>(Arrays.asList(AVAILABLE_ACTIVITY_TYPES));

        for(String key : this.keywords){
            if(key.startsWith(ACTIVITY)){
                String keyValue = key.substring(ACTIVITY.length());
                if(aux.contains(keyValue)){
                    keywordsActivities.add(keyValue);
                }
            }
        }

        return keywordsActivities;
    }

    public ArrayList<String> getKeywordsTimeInterval(){
        ArrayList<String> keywordsTimeInterval = new ArrayList<>();

        //To make easier comparations between keyValue and the Available Activities Types Array.
        ArrayList<String> aux = new ArrayList<>(Arrays.asList(AVAILABLE_TIME_INTERVALS));

        for(String key : this.keywords){
            if(key.startsWith(TIME_INTERVAL)){
                String keyValue = key.substring(TIME_INTERVAL.length());
                if(aux.contains(keyValue)){
                    keywordsTimeInterval.add(keyValue);
                }
            }
        }

        return keywordsTimeInterval;


    }
}
