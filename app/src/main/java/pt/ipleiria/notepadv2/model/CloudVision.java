package pt.ipleiria.notepadv2.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.SafeSearchAnnotation;
import com.google.api.services.vision.v1.model.WebDetection;
import com.google.api.services.vision.v1.model.WebEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Lenovo on 19/12/2017.
 */

public class CloudVision extends AsyncTask<Object, Integer, Void> implements AppConstants{

    private static final int NUMBER_OF_TASKS_CLOUD_VISION = 9;

    @Override
    protected Void doInBackground(final Object... objects) {
        if(objects.length!=2){
            Log.e(TAG_CLOUD_VISION, "The parameters should be two! 1st: Context; 2nd: Bitmap Image");
            return null;
        }


        final int[] progress = {0};
        try {
            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            final Context context = (Context) objects[0];
            VisionRequestInitializer requestInitializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                /**
                 * We override this so we can inject important identifying fields into the HTTP
                 * headers. This enables use of a restricted cloud platform API key.
                 */
                @Override
                protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                        throws IOException {
                    super.initializeVisionRequest(visionRequest);

                    String packageName = context.getPackageName();
                    visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                    String sig = PackageManagerUtils.getSignature(context.getPackageManager(), packageName);

                    visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                }
            };

            progress[0]+=Math.ceil(100/NUMBER_OF_TASKS_CLOUD_VISION);
            publishProgress(progress[0]);

            Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
            builder.setVisionRequestInitializer(requestInitializer);

            Vision vision = builder.build();

            BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                    new BatchAnnotateImagesRequest();

            progress[0]+=Math.ceil(100/NUMBER_OF_TASKS_CLOUD_VISION);
            publishProgress(progress[0]);

            batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                // Add the image
                Image base64EncodedImage = new Image();
                // Convert the bitmap to a JPEG
                // Just in case it's a format that Android understands but Cloud Vision
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                Bitmap bitmap = (Bitmap) objects[1];
                scaleBitmapDown(bitmap, RESIZE_IMAGE_TO_MAX_DIMENSIONS).compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();

                // Base64 encode the JPEG
                base64EncodedImage.encodeContent(imageBytes);
                annotateImageRequest.setImage(base64EncodedImage);


                progress[0]+=Math.ceil(100/NUMBER_OF_TASKS_CLOUD_VISION);
                publishProgress(progress[0]);

                // add the features we want
                // TODO: (jose.ribeiro) to add or remove features just (un)comment the blocks bellow
                annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                    Feature labelDetection = new Feature();
                    labelDetection.setType("LABEL_DETECTION");
                    labelDetection.setMaxResults(5);
                    add(labelDetection);

                    progress[0]+=Math.ceil(100/NUMBER_OF_TASKS_CLOUD_VISION);
                    publishProgress(progress[0]);

                    Feature textDetection = new Feature();
                    textDetection.setType("TEXT_DETECTION");
                    textDetection.setMaxResults(10);
                    add(textDetection);

                    progress[0]+=Math.round(100/NUMBER_OF_TASKS_CLOUD_VISION);
                    publishProgress(progress[0]);

                    Feature logoDetection = new Feature();
                    logoDetection.setType("LOGO_DETECTION");
                    logoDetection.setMaxResults(3);
                    add(logoDetection);

                    progress[0]+=Math.ceil(100/NUMBER_OF_TASKS_CLOUD_VISION);
                    publishProgress(progress[0]);

                    Feature webDetection = new Feature();
                    webDetection.setType("WEB_DETECTION");
                    webDetection.setMaxResults(10);
                    add(webDetection);

                    progress[0]+=Math.ceil(100/NUMBER_OF_TASKS_CLOUD_VISION);
                    publishProgress(progress[0]);

                    Feature safeSearch = new Feature();
                    safeSearch.setType("SAFE_SEARCH_DETECTION");
                    safeSearch.setMaxResults(5);
                    add(safeSearch);

                    progress[0]+=Math.ceil(100/NUMBER_OF_TASKS_CLOUD_VISION);
                    publishProgress(progress[0]);
                }});

                // Add the list of one thing to the request
                add(annotateImageRequest);
            }});

            Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
            // Due to a bug: requests to Vision API containing large images fail when GZipped.
            annotateRequest.setDisableGZipContent(true);
            Log.d(TAG_CLOUD_VISION, "Created Cloud Vision request object, sending request");


            BatchAnnotateImagesResponse response = annotateRequest.execute();
            Singleton.getInstance().setCloudVisionResponses(convertResponseToStringKeywords(response));

            progress[0]+=Math.ceil(100/NUMBER_OF_TASKS_CLOUD_VISION);
            publishProgress(progress[0]);
        } catch (GoogleJsonResponseException e) {
            Log.d(TAG_CLOUD_VISION, "failed to make API request because " + e.getContent());
        } catch (IOException e) {
            Log.d(TAG_CLOUD_VISION, "failed to make API request because of other IOException " + e.getMessage());
        }

        return null;
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    // TODO: (jose.ribeiro) analyse this method to understand how to get Cloud Vision data
    private String[] convertResponseToStringKeywords(BatchAnnotateImagesResponse response) {
        String[] message = {"", "", "", "", "", ""};
        List<EntityAnnotation> annotations;
        AnnotateImageResponse annotateImageResponse = response.getResponses().get(0);

        //WEB DETECTION
        WebDetection webDetection = annotateImageResponse.getWebDetection();
        if (webDetection!=null && webDetection.getWebEntities() != null) {
            for (WebEntity webEntity : webDetection.getWebEntities()) {
                if(webEntity.getScore()>MIN_CLOUD_VISION_WEB_ENTITY_SCORE)
                    //The + "; " is to 'format' this response as the labels response
                    message[5] += webEntity.getDescription()+"; ";
            }
        }

        //LABELS - keywords
        annotations = annotateImageResponse.getLabelAnnotations();
        if (annotations != null) {
            for (EntityAnnotation annotation : annotations) {
                if(annotation.getScore()>MIN_CLOUD_VISION_LABEL_SCORE)
                    message[0] +=  annotation.getDescription()+";";
            }
        }

        //LANDMARKS
        annotations = annotateImageResponse.getLandmarkAnnotations();
        if (annotations != null) {
            for (EntityAnnotation annotation : annotations) {
                if(annotation.getScore()>MIN_CLOUD_VISION_LANDMARK_SCORE){
                    message[1] += "\n"+annotation.getLocations();
                }
            }
        }

        //TEXT
        annotations = annotateImageResponse.getTextAnnotations();
        if (annotations != null) {
            message[2] = "\nLanguage: "+annotations.get(0).getLocale();
            message[2] += "\nText:"+annotations.get(0).getDescription();
        }

        //LOGOS
        annotations = annotateImageResponse.getLogoAnnotations();
        if (annotations != null) {
            for (EntityAnnotation annotation : annotations) {
                if(annotation.getScore()>MIN_CLOUD_VISION_LOGO_SCORE){
                    message[3] += "\n"+annotation.getDescription();
                }
            }
        }

        //SAFE SEARCH DETECTION
        SafeSearchAnnotation saveSearchAnnotation = annotateImageResponse.getSafeSearchAnnotation();
        if (saveSearchAnnotation != null) {
            if(saveSearchAnnotation.getAdult().equals("VERY_LIKELY"))
                message[4]+="\nADULT CONTENT DETECTED!";
            if(saveSearchAnnotation.getMedical().equals("VERY_LIKELY"))
                message[4]+="\nMEDICAL CONTENT DETECTED!";
            if(saveSearchAnnotation.getSpoof().equals("VERY_LIKELY"))
                message[4]+="\nSPOOF CONTENT DETECTED!";
            if(saveSearchAnnotation.getViolence().equals("VERY_LIKELY"))
                message[4]+="\nVIOLENCE CONTENT DETECTED!";
        }

        for (String aMessage : message) {
            Log.d(TAG_CLOUD_VISION, "" + aMessage);
        }
        return message;
    }

}
