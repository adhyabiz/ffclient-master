package com.foxfire.user.DirectionHelper;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PointsParser extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
    TaskLoadedCallback taskCallback;
    String directionMode = "driving";
    private final String TAG = "PointsParser";

    public PointsParser(Context mContext, String directionMode) {
        this.taskCallback = (TaskLoadedCallback) mContext;
        this.directionMode = directionMode;
    }

    // Parsing the data in non-ui thread
    @Override
    protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

        JSONObject jObject;
        List<List<HashMap<String, String>>> routes = null;

        try {
            jObject = new JSONObject(jsonData[0]);
            Log.d("mylog", jsonData[0].toString());
            DataParser parser = new DataParser();
            Log.d("mylog", parser.toString());

            // Starts parsing data
            routes = parser.parse(jObject);
            Log.d("mylog", "Executing routes");
            Log.d("mylog", routes.toString());

        } catch (Exception e) {
            Log.e("mylog", e.getMessage());
            e.printStackTrace();
        }
        return routes;
    }

    // Executes in UI thread, after the parsing process
    @Override
    protected void onPostExecute(List<List<HashMap<String, String>>> result) {
        ArrayList<LatLng> points;
        PolylineOptions lineOptions = null;
        try {
            Log.e(TAG, "onPostExecute: enter try");
            for (int i = 0; i < result.size(); i++) {
                Log.e(TAG, "onPostExecute: enter for");
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();
                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);
                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    Log.e(TAG, "onPostExecute: enter enner for");
                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }
                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                if (directionMode.equalsIgnoreCase("driving")) {
                    Log.e(TAG, "onPostExecute: enter if");
                    lineOptions.width(10);
                    lineOptions.color(Color.MAGENTA);
                } else {
                    Log.e(TAG, "onPostExecute: enter else");
                    lineOptions.width(20);
                    lineOptions.color(Color.BLUE);
                }
                Log.e("mylog", "onPostExecute lineoptions decoded");
            }

        } catch (NullPointerException e) {
            Log.e(TAG, "onPostExecute: exception " + e.getMessage());
        }
        // Traversing through all the routes

        // Drawing polyline in the Google Map for the i-th route
        if (lineOptions != null) {
            Log.e(TAG, "onPostExecute: line option if");
            //mMap.addPolyline(lineOptions);
            taskCallback.onTaskDone(lineOptions);

        } else {
            Log.e("mylog", "without Polylines drawn");
        }
    }
}