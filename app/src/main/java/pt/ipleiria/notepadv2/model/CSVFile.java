package pt.ipleiria.notepadv2.model;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static pt.ipleiria.notepadv2.model.AppConstants.IMPORT_NOTES_FIELDS_TYPES;
import static pt.ipleiria.notepadv2.model.AppConstants.NEW_FIELD_EXPORT_FILE;
import static pt.ipleiria.notepadv2.model.AppConstants.NEW_LINE_EXPORT_FILE;


public class CSVFile {

    public void importNotes(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        try {
            //String[] IMPORT_NOTES_FIELDS_TYPES = {"TITLE", "KEYWORDS", "BODY_TEXT"};
            //The Types in First Line - Don't care.
            reader.readLine();

            String csvLine;
            while ((csvLine = reader.readLine()) != null) {
                String[] row = csvLine.split(NEW_FIELD_EXPORT_FILE);
                Note n = new Note(row[0].trim(), row[1].trim());
                n.setText(row[2].trim().replaceAll(NEW_LINE_EXPORT_FILE, "\n"));
                //resultList.add(row);
                Singleton.getInstance().getNotepad().addNote(n);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error in reading CSV file: " + ex);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean exportNotes(String saveEnvironment, String fileNameComplete){

        //File file = new File(Environment.getExternalStoragePublicDirectory(saveEnvironment),
        //        "AlzheimerNotes_Backup_"+(new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()))+".cvs");

        File file = new File(Environment.getExternalStoragePublicDirectory(saveEnvironment), fileNameComplete);


        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(file.exists()){

            try {
                FileWriter fileWriter  = new FileWriter(file);
                BufferedWriter bfWriter = new BufferedWriter(fileWriter);

                for (String fieldType : IMPORT_NOTES_FIELDS_TYPES) {
                    bfWriter.write(fieldType);
                    bfWriter.write(" "+NEW_FIELD_EXPORT_FILE+"\t");
                }
                bfWriter.newLine();

                for (Note n : Singleton.getInstance().getNotepad().getNotes()) {
                    bfWriter.write(n.getTitle());
                    bfWriter.write(" "+NEW_FIELD_EXPORT_FILE+" ");

                    bfWriter.write(n.getPlainKeywords());
                    bfWriter.write(" "+NEW_FIELD_EXPORT_FILE+" ");

                    bfWriter.write(n.getText().replaceAll("\n", NEW_LINE_EXPORT_FILE));
                    //TODO É NECESSÀRIO ESTE ULTIMO SEPARADOR ?? PENSO QUE NAO
                    bfWriter.write(" "+NEW_FIELD_EXPORT_FILE+" ");

                    bfWriter.newLine();
                }

                bfWriter.flush();
                bfWriter.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
