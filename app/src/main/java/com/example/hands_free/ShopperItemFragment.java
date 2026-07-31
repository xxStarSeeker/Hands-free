package com.example.hands_free;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/* This class to display items of Shopper
* each item card have: store logo, item serial number, location of item.
* code of receive items will available only if the account active */

public class ShopperItemFragment extends Fragment {

    //Attributes
    private View view;
    private List<CardData> allCards;
    private Button[] filterButtons;
    private int selectedButtonIndex = 0; // Default to "All" button
    private LinearLayout cardContainer;
    private String shopperEmail;
    private DatabaseReference itemsRef, storeRef;

    // Required empty public constructor
    public ShopperItemFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_shopper_item, container, false);
        cardContainer = view.findViewById(R.id.cardContainer);
        itemsRef = FirebaseDatabase.getInstance().getReference("Items");
        storeRef = FirebaseDatabase.getInstance().getReference("Store Account");

        initializeButtons(); //filters
        initializeCardsUsingEmail(); //cards
        return view;
    }


    public void initializeCardsUsingEmail(){
        Shopper shopper = new Shopper();
        shopper.fetchShopperEmail(new Shopper.EmailFetchListener() {
            @Override
            public void onEmailFetched(String email) {
                shopperEmail = email;
                initializeCards(); // Initialize cards after getting the email number
            }
        });
    }

    private void initializeCards() {
        allCards = new ArrayList<>();
        if (shopperEmail == null || shopperEmail.isEmpty()) {
            Toast.makeText(getContext(), "You do not have any items yet", Toast.LENGTH_SHORT).show();
            return;
        }else {
            itemsRef.orderByChild("shopper_email").equalTo(shopperEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    allCards.clear(); // Clear existing cards
                    for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                        String storeName = itemSnapshot.child("store").getValue(String.class);
                        String serialNumber = itemSnapshot.child("serial_number").getValue(String.class);
                        String status = itemSnapshot.child("status").getValue(String.class);
                        int statusNumber = getStatusText(status);
                        if (storeName != null) {
                            fetchLogoStore(storeName, new LogoFetchListener() {
                                @Override
                                public void onLogoFetched(String logo) {
                                    //storeLogo = logo;
                                    allCards.add(new CardData(logo, "SN: " + serialNumber, statusNumber));
                                    if (allCards.size() == dataSnapshot.getChildrenCount()) {
                                        createCardViews();
                                        updateUI();
                                    }
                                }
                            });
                        }
                    }
                    createCardViews();
                    updateUI();
                }
                //Error handling
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(getContext(), "Error fetching items: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void fetchLogoStore(String storeName, LogoFetchListener listener) {
        storeRef.orderByChild("Store_name").equalTo(storeName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                    String logo = itemSnapshot.child("Logo").getValue(String.class);
                    if (logo != null) {
                        listener.onLogoFetched(logo);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error fetching logo of items: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private interface LogoFetchListener {
        void onLogoFetched(String logo);
    }

    //to convert text got from data base to integer to use in trucking.
    private int getStatusText(String status) {
        if(status.equalsIgnoreCase("Reached")){
            return 2;
        } else if (status.equalsIgnoreCase("Collected")) {
            return 1;
        }else { //in the store
            return 0;
        }
    }

    //filter
    private void initializeButtons() {
        filterButtons = new Button[]{
                view.findViewById(R.id.allButton),
                view.findViewById(R.id.reachedButton),
                view.findViewById(R.id.collectedButton),
                view.findViewById(R.id.storeButton)
        };
        for (int i = 0; i < filterButtons.length; i++) {
            final int index = i;
            filterButtons[i].setOnClickListener(v -> {
                selectedButtonIndex = index;
                updateUI();
            });
        }
    }

    private void createCardViews() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (CardData card : allCards) {
            CardView cardView = (CardView) inflater.inflate(R.layout.shopper_card_layout, cardContainer, false);
            setupCard(cardView, card);
            cardContainer.addView(cardView);
            card.cardView = cardView;
        }
    }

    private void updateUI() {
        // Update button colors
        for (int i = 0; i < filterButtons.length; i++) {
            filterButtons[i].setBackground(ContextCompat.getDrawable(requireContext(),
                    i == selectedButtonIndex ? R.drawable.shape_gradient_purple_rounded : R.drawable.shape_grey_rounded));
        }

        // Filter and display cards
        for (CardData card : allCards) {
            boolean shouldShow = selectedButtonIndex == 0 || // "All" button
                    (selectedButtonIndex == 1 && card.progress == 2) || // "Reached" button
                    (selectedButtonIndex == 2 && card.progress == 1) || // "Collected" button
                    (selectedButtonIndex == 3 && card.progress == 0);   // "In Store" button

            card.cardView.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        }
    }

    private void setupCard(CardView cardView, CardData card) {
        ImageView imageView = cardView.findViewById(R.id.cardImage);
        TextView textView = cardView.findViewById(R.id.cardText);
        ProgressBar progressBar = cardView.findViewById(R.id.progressBar);

        TextView textStart = cardView.findViewById(R.id.textStart);
        TextView textMiddle = cardView.findViewById(R.id.textMiddle);
        TextView textEnd = cardView.findViewById(R.id.textEnd);

        if (card.imageResource != null && !card.imageResource.isEmpty()) {
            Glide.with(this)
                    .load(card.imageResource)
                    .into(imageView);
        }
        textView.setText(card.cardText);
        progressBar.setProgress(card.progress);

        // Set card background color green if it reached
        cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                card.progress == 2 ? R.color.greenForComplete : android.R.color.white));

        textStart.setVisibility(card.progress == 0 ? View.VISIBLE : View.GONE);
        textMiddle.setVisibility(card.progress == 1 ? View.VISIBLE : View.GONE);
        textEnd.setVisibility(card.progress == 2 ? View.VISIBLE : View.GONE);
    }

    private static class CardData {
        String imageResource;
        String cardText;
        int progress;
        CardView cardView;

        CardData(String imageResourceId, String cardText, int progress) {
            this.imageResource = imageResourceId;
            this.cardText = cardText;
            this.progress = progress;
        }
    }
}