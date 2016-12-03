/*@Reference: http://blog.teamtreehouse.com/beginners-guide-location-android for location services API*/

package com.example.eoin.appproject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    //declare button and imageview & google api client, String tag, location request object & others
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    public static final String TAG = MainActivity.class.getSimpleName();
    private Button bCamera;
    private Button shareMyImage;
    private ImageView ivImage;
    private TextView gpsText;
    private Location myLocation;
    private FirebaseAnalytics mFirebaseAnalytics;

    //set int for camera start activity
    private static int REQUEST_CODE_IMAGE_CAPTURE = 1;
    private final static int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 3;
    private final static int MY_PERMISSIONS_REQUEST_WRITE_ACCESS = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //check for GPS permissions, ask user if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initFunctions();
        } else {
            Toast.makeText(getApplicationContext(), "Unable to get permissions for location services!", Toast.LENGTH_LONG).show();
        }

    }

    protected void initFunctions() {
        //get button and imageview & hidden edittext
        gpsText = (TextView) findViewById(R.id.editText);
        bCamera = (Button) findViewById(R.id.button);
        ivImage = (ImageView) findViewById(R.id.imageView);
        shareMyImage = (Button) findViewById(R.id.shareImage);
        //set initial tag
        ivImage.setTag("originalImage");

        //set onclick listener of camera button
        bCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewImage();
            }
        });

        //set onclick listener of share button
        shareMyImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ivImage.getTag().toString() == "originalImage"){
                    Toast.makeText(getApplicationContext(), "Please take a pic first!", Toast.LENGTH_LONG).show();
                }
                else{
                    shareImage(ivImage);
                }
            }
        });

        //initialise api
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 6000)        // 60 seconds
                .setFastestInterval(1 * 1000); // 1 second
    }

    //handle request permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initFunctions();
                } else {
                    Toast.makeText(getApplicationContext(), "Permissions for location service denied! This app needs this permission to function!", Toast.LENGTH_LONG).show();
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_WRITE_ACCESS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    viewImage();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Permissions for write access to storage denied! This app needs this permission to function!", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    //connect to API client with onResume method, as this will be ensure we can access location whenever the activity is visible, not just on create
    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mGoogleApiClient.connect();
        }

    }

    //on pause method to disconnect from location services when activity is paused. Good practice
    @Override
    protected void onPause() {
        super.onPause();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
            }
        }
    }

    //get location (last location first if possible)
    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if ((location == null) &&  (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            handleNewLocation(location);
        }
        ;
    }

    //get new location
    private void handleNewLocation(Location location) {
        myLocation = location;
        //Toast.makeText(getApplicationContext(), "Co-Ordinates = " + location.getLatitude(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }


    //image method to check if device can do the activity, then start camera
    private void viewImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_ACCESS);
        }
        else {
            Intent doPic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            doPic.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            if (doPic.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(doPic, REQUEST_CODE_IMAGE_CAPTURE);
            }
        }
    }

    //share image using share button
    private void shareImage(ImageView ivImage){

        ivImage.setDrawingCacheEnabled(true);

        Bitmap bitmap = ivImage.getDrawingCache();
        File root = Environment.getExternalStorageDirectory();
        File myFile = new File(root.getAbsolutePath() + "/DCIM/Camera/AppProjectImage.jpg");
        try {
            myFile.createNewFile();
            FileOutputStream ostream = new FileOutputStream(myFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
            ostream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/*");
        share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(myFile));
        startActivity(Intent.createChooser(share, "Share image using:"));

        //log share event to firebase
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "2");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "ShareButtonClick");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        new MyAsyncClass(getApplicationContext()).execute(bundle);

    }

    //get camera image and set it to the imageview & save
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            //ivImage.setImageBitmap(imageBitmap);
            ivImage.setTag("newImage");

            //add GPS coordinates to image
            //first set text of hidden edittext to GPS location
            String loc = myLocation.getLatitude() + " " + myLocation.getLongitude();
            gpsText.setText(loc);
            //then create bitmap from hidden edittext then combine with combineImages method
            gpsText.setDrawingCacheEnabled(true);
            Bitmap bmp = Bitmap.createBitmap(gpsText.getDrawingCache());
            Bitmap combined = combineImages(imageBitmap,bmp);
            ivImage.setImageBitmap(combined);

            ivImage.setDrawingCacheEnabled(true);
            Bitmap b = ivImage.getDrawingCache();
            MediaStore.Images.Media.insertImage(getContentResolver(), b, "SnapLocImage!" , "Taken with SnapLoc!");
        }
    }



    //set on click listener for the imageview
    public void setOnClickForImage(View view) {
        //get tag name of original image
        String origName = String.valueOf(ivImage.getTag());
        //if tag is same as original image, tell user they must take pic, else go to new activity
        if (origName == "originalImage") {
            Toast.makeText(getApplicationContext(), "You must take a picture first!", Toast.LENGTH_SHORT).show();
        } else {
            Intent goToNewActivity = new Intent(this, MapsActivity.class);
            goToNewActivity.putExtra("Latitude", myLocation.getLatitude());
            goToNewActivity.putExtra("Longitude", myLocation.getLongitude());
            startActivity(goToNewActivity);

            //Bundle log event and send to MyAsyncClass for logging to firebase
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "1");
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "mainImageClick");
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
            new MyAsyncClass(getApplicationContext()).execute(bundle);
        }


    }

    //Bitmap combiner
    //@REF: http://stackoverflow.com/questions/6159186/how-do-i-write-text-over-a-picture-in-android-and-save-it
    public Bitmap combineImages(Bitmap background, Bitmap foreground) {

        int width = 0, height = 0;
        Bitmap cs;

        width = ivImage.getDrawable().getIntrinsicWidth();
        height = ivImage.getDrawable().getIntrinsicHeight();

        cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas comboImage = new Canvas(cs);
        background = Bitmap.createScaledBitmap(background, width, height, true);
        comboImage.drawBitmap(background, 0, 0, null);
        comboImage.drawBitmap(foreground, 0,0, null);

        return cs;
    }


}

class MyAsyncClass extends AsyncTask<Bundle, Integer, String> {

    public static final String TAG = MyAsyncClass.class.getSimpleName();
    private FirebaseAnalytics mFirebaseAnalytics;

    //constructor - pass application context and initialize firebase
    public MyAsyncClass(Context m){
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(m);
    }

    protected void onPreExecute(){
        Log.i(TAG, "Started Async Task - Please wait!!");
    }

    @Override
    protected String doInBackground(Bundle... params) {
        Log.i(TAG, "Bundle param[0] = " + params[0].toString());
        //log event
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params[0]);

        return null;
    }

}
