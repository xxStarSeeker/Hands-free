package com.example.hands_free;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/*this class fetches store locations from Firebase,
 calculates the shortest path using a nearest neighbor algorithm,
  and notifies a listener with the optimal store visitation order.*/
public class ShortestPath {
    private DatabaseReference storeRef;
    private List<Point> pointsToVisit = new ArrayList<>();
    private List<String> storeList;
    String startPointName = "office";
    double startPointX = 4.9, startPointY = 9;

    private OnPathCalculatedListener listener; //to notify the caller when the shortest path is calculated.

    public interface OnPathCalculatedListener {
        void onPathCalculated(List<String> orderedStores);
    }

    //from spacfic store
    public ShortestPath(List<String> stores, String firstStore, double x, double y) {
        storeList = new ArrayList<>(stores);
        startPointName = firstStore;
        startPointX = x;
        startPointY = y;
    }

    //from office
    public ShortestPath(List<String> stores) {
        storeList = new ArrayList<>(stores);
    }

    public void calculateShortestPath(OnPathCalculatedListener listener) {
        this.listener = listener;
        pointsToVisit.clear();
        pointsToVisit.add(new Point(startPointX, startPointY, startPointName));
        fetchStoreLocations();
    }

    /*
    * Iterates over each store name in the storeList.
    * For each store, queries the Firebase Database to retrieve its coordinates (X-axis and Y-axis).
    * Adds the retrieved store coordinates to the pointsToVisit list.
    * Once all stores have been processed, calls the processShortestPath method.
    * */
    private void fetchStoreLocations() {
        storeRef = FirebaseDatabase.getInstance().getReference("Store Account");

        for (String store : storeList) {
            storeRef.orderByChild("Store_name").equalTo(store)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                                String X_axis = itemSnapshot.child("X_axis").getValue(String.class);
                                String Y_axis = itemSnapshot.child("Y_axis").getValue(String.class);
                                if (X_axis != null && Y_axis != null) {
                                    double x = Double.parseDouble(X_axis);
                                    double y = Double.parseDouble(Y_axis);
                                    pointsToVisit.add(new Point(x, y, store));
                                }
                            }
                            processShortestPath();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Error", "Failed to store location: " + error.getMessage());
                        }
                    });
        }
    }

    private void processShortestPath() {
        List<Point> shortestPath = calculateShortestPath(pointsToVisit);
        List<String> orderedStores = new ArrayList<>();

        // Skip the first point (office) when creating the ordered store list
        for (int i = 1; i < shortestPath.size(); i++) {
            orderedStores.add(shortestPath.get(i).getStore());
        }

        // Notify listener with the result
        if (listener != null) {
            listener.onPathCalculated(orderedStores);
        }
    }

    private double calculateDistance(Point p1, Point p2) {
        double xDiff = p2.getX() - p1.getX();
        double yDiff = p2.getY() - p1.getY();
        return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    /* Implements the nearest neighbor algorithm to find the shortest path.
    Iteratively selects the nearest unvisited point until all points are visited.
    Returns the calculated shortest path.*/
    private List<Point> calculateShortestPath(List<Point> points) {
        List<Point> shortestPath = new ArrayList<>();
        List<Point> remainingPoints = new ArrayList<>(points);

        Point currentPoint = remainingPoints.remove(0); // Start from office
        shortestPath.add(currentPoint);

        while (!remainingPoints.isEmpty()) {
            Point nearestPoint = null;
            double shortestDistance = Double.MAX_VALUE;

            for (Point point : remainingPoints) {
                double distance = calculateDistance(currentPoint, point);
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    nearestPoint = point;
                }
            }

            if (nearestPoint != null) {
                shortestPath.add(nearestPoint);
                remainingPoints.remove(nearestPoint);
                currentPoint = nearestPoint;
            }
        }
        return shortestPath;
    }
}


class Point {
    private double x;
    private double y;
    private String store;

    public Point(double x, double y, String store) {
        this.store = store;
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    public String getStore(){
        return store;
    }
}