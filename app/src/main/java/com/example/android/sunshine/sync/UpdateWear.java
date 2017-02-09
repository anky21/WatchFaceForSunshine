package com.example.android.sunshine.sync;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;

import static com.example.android.sunshine.utilities.SunshineWeatherUtils.formatTemperature;
import static com.example.android.sunshine.utilities.SunshineWeatherUtils.getSmallArtResourceIdForWeatherCondition;

/**
 * Created by anky_ on 7/02/2017.
 */

public class UpdateWear implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = UpdateWear.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private static UpdateWear updateWear;

    private static final String DATA_PATH = "/data";
    private static final String CURRENT_TIME ="current_time";
    private static final String MAX_TEMP = "max";
    private static final String MIN_TEMP = "min";
    private static final String WEATHER_ID = "weather_id";
    private static final String ICON_RESOURCE_ID = "icon_resource_id";
    private static final String WEATHER_ICON = "weather_icon";

    private UpdateWear() {}

    public static synchronized UpdateWear getInstance(){
        if(updateWear == null)
            updateWear = new UpdateWear();
        return updateWear;
    }

    public void initialise(Context context){
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    public void sendData(Context context, ContentValues[] contentValues){
        ContentValues values = contentValues[0];
        // Constantly changing data: current time
        long currentTime = System.currentTimeMillis();
        String maxTemp = formatTemperature(context, values.getAsDouble(MAX_TEMP)).trim();
        String minTemp = formatTemperature(context, values.getAsDouble(MIN_TEMP)).trim();
        int weatherId = values.getAsInteger(WEATHER_ID);
        int iconResourceId = getSmallArtResourceIdForWeatherCondition(weatherId);

        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(DATA_PATH);
        putDataMapRequest.getDataMap().putLong(CURRENT_TIME, currentTime);
        putDataMapRequest.getDataMap().putString(MAX_TEMP, maxTemp);
        putDataMapRequest.getDataMap().putString(MIN_TEMP, minTemp);
        putDataMapRequest.getDataMap().putInt(ICON_RESOURCE_ID, iconResourceId);

        if(iconResourceId != -1){
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), iconResourceId);
            Asset weatherIcon = createAssetFromBitmap(bitmap);
            putDataMapRequest.getDataMap().putAsset(WEATHER_ICON, weatherIcon);
        }

        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                        Log.d(TAG, "Sending data was successful: " + dataItemResult.getStatus()
                                .isSuccess());
                    }
                });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: " + bundle);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult);
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }
}
