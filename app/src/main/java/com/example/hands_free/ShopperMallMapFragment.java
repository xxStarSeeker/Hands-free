package com.example.hands_free;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

/*This enable shopper to see the mall map*/
public class ShopperMallMapFragment extends Fragment {
    private ImageView mallMap;

    public ShopperMallMapFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_collector_routing, container, false);
        mallMap = view.findViewById(R.id.mallMap);
        mallMapDisplay();
        return view;
    }

    //1- check if the account is active using box value
    public void mallMapDisplay(){
        Shopper shopper = new Shopper();
        shopper.fetchMallMap(new Shopper.MallMapFetchListener() {
            @Override
            public void onMallMapFetched(String mallMapLink) {
                if (mallMapLink != null && !mallMapLink.isEmpty()) {
                    Glide.with(requireActivity()).load(mallMapLink).into(mallMap);
                }
            }
        });
    }
}
