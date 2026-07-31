package com.example.hands_free;

import android.graphics.Bitmap;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.concurrent.atomic.AtomicInteger;

/* This class handles the Shopper home page. It displays three elements:
 * if the account is active:
 * 1. QR code
 * 2. Timer showing remaining hours before the account becomes inactive
 * 3. Summary of item statuses
 *
 * if the account is inactive (default):
 * 1. Image of inactive QR code
 * 2. Message to help the user in activating their account
 * 3. Summary of item statuses = 0 */

public class ShopperQRFragment extends Fragment {

    //Attributes
    private String shopperEmail, StartMallWorkHour, EndMallWorkHour;
    TextView greeting;
    Shopper shopper = new Shopper();
    private String boxValue;
    View view;

    // Required empty public constructor
    public ShopperQRFragment() {}

    //onCreateView
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_shopper_qr, container, false);
        greeting = view.findViewById(R.id.greeting);
        greeting();
        itemStatus(); //display Status of items
        ActiveAccount(); //check if the account is active.
        return view;
    }

    //1- check if the account is active using box value
    public void ActiveAccount(){
        Shopper shopper = new Shopper();
        shopper.fetchShopperBox(new Shopper.BoxFetchListener() {
            @Override
            public void onBoxFetched(String box) {
                boxValue = box;
                if(!boxValue.equalsIgnoreCase("000")){
                    contentOfQR(); //to display QR
                    startCountDownTimer(); //display CountDownTimer
                }else{
                    InactiveAccount();
                }
            }
        });
    }

    //2- display QR
    public void DisplayQR() {
        ImageView QR = view.findViewById(R.id.qr_code);
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(shopperEmail, BarcodeFormat.QR_CODE, 1200, 1200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            QR.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    //3- content of QR is email of Shopper.
    public void contentOfQR(){
        shopper.fetchShopperEmail(new Shopper.EmailFetchListener() {
            @Override
            public void onEmailFetched(String email) {
                shopperEmail = email;
                DisplayQR(); // Display QR code after phone number is fetched
            }
        });
    }

    private void startCountDownTimer() {
        TextView countdownText = view.findViewById(R.id.remainingHours);
        TextView titleQR = view.findViewById(R.id.titleQR);
        titleQR.setText("Remaining time before QR inactive: ");
        titleQR.setTextColor(ContextCompat.getColor(view.getContext(), R.color.black));

        Shopper shopper = new Shopper();

        // Create an AtomicInteger to track when both fetches are complete
        AtomicInteger fetchCount = new AtomicInteger(0);

        shopper.fetchStartWorkHour(new Shopper.StartWorkHourFetchListener() {
            @Override
            public void onStartWorkFetched(String start) {
                StartMallWorkHour = start;
                if (fetchCount.incrementAndGet() == 2) {
                    startTimer(countdownText, shopper);
                }
            }
        });

        shopper.fetchEndWorkHour(new Shopper.EndWorkHourFetchListener() {
            @Override
            public void onEndWorkFetched(String end) {
                EndMallWorkHour = end;
                if (fetchCount.incrementAndGet() == 2) {
                    startTimer(countdownText, shopper);
                }
            }
        });
    }

    private void startTimer(TextView countdownText, Shopper shopper) {
        if(EndMallWorkHour != null && StartMallWorkHour != null) {
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);
            int currentSecond = now.get(Calendar.SECOND);

            int endHour = Integer.parseInt(EndMallWorkHour.substring(0, 2));
            int endMinute = Integer.parseInt(EndMallWorkHour.substring(3, 5));

            int startHour = Integer.parseInt(StartMallWorkHour.substring(0, 2));
            int startMinute = Integer.parseInt(StartMallWorkHour.substring(3, 5));

            int currentTotalMinutes = currentHour * 60 + currentMinute + 10;
            int startTotalMinutes = startHour * 60 + startMinute;
            int endTotalMinutes = endHour * 60 + endMinute;

            if(startTotalMinutes <= currentTotalMinutes && endTotalMinutes >= currentTotalMinutes) {
                // Calculate total remaining milliseconds
                long totalMillis = (endTotalMinutes - currentTotalMinutes) * 60 * 1000 - (currentSecond * 1000);

                new CountDownTimer(totalMillis, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        int totalSeconds = (int) (millisUntilFinished / 1000);
                        int hours = totalSeconds / 3600;
                        int minutes = (totalSeconds % 3600) / 60;
                        int seconds = totalSeconds % 60;

                        String timeLeftFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                        countdownText.setText(timeLeftFormatted);
                    }

                    @Override
                    public void onFinish() {
                        InactiveAccount();
                        shopper.updateShopperBox();
                    }
                }.start();
            }
        } else {
            // Handle time outside mall hours
            new CountDownTimer(getMillisUntilEndOfDay(), 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int hours = (int) (millisUntilFinished / (3600 * 1000));
                    int minutes = (int) ((millisUntilFinished % (3600 * 1000)) / (60 * 1000));
                    int seconds = (int) ((millisUntilFinished % (60 * 1000)) / 1000);

                    String timeLeftFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                    countdownText.setText(timeLeftFormatted);
                }

                @Override
                public void onFinish() {
                    InactiveAccount();
                    shopper.updateShopperBox();
                }
            }.start();
        }
    }
    private long getMillisUntilEndOfDay() {
        Calendar now = Calendar.getInstance();
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.add(Calendar.DAY_OF_MONTH, 1);
        endOfDay.set(Calendar.HOUR_OF_DAY, 0);
        endOfDay.set(Calendar.MINUTE, 0);
        endOfDay.set(Calendar.SECOND, 0);
        endOfDay.set(Calendar.MILLISECOND, 0);
        return endOfDay.getTimeInMillis() - now.getTimeInMillis();
    }

    //5- greeting the shopper
    public void greeting(){
        shopper.fetchShopperName(new Shopper.NameFetchListener(){
            @Override
            public void onNameFetched(String name) {
                if(name!=null){
                    String greetingText = "Hello, " + name;
                    greeting.setText(greetingText);
                }else{
                    String greetingText = "Hello, ";
                    greeting.setText(greetingText);
                }
            }
        });
    }

    //6- Inactive account
    public void InactiveAccount(){
        //disable QR
        ImageView QR = view.findViewById(R.id.qr_code);
        QR.setImageResource(R.drawable.ic_disable);
        // Change message text
        TextView titleQR = view.findViewById(R.id.titleQR);
        titleQR.setText(R.string.defualt_disable_text);
        titleQR.setTextColor(ContextCompat.getColor(view.getContext(), R.color.red));
        // Hide the countdown timer
        TextView countdownText = view.findViewById(R.id.remainingHours);
        countdownText.setVisibility(View.GONE);
    }

    //6- items status.
    public void itemStatus() {
        Button reachedButton = view.findViewById(R.id.reachedButton);
        Button inStoreButton = view.findViewById(R.id.inStoreButton);
        Button atCollectorButton = view.findViewById(R.id.atCllectorButton);
        TextView totalItemsTextView = view.findViewById(R.id.totalItemsTextView);
        shopper.summaryCount(new Shopper.ItemCountListener() {
            @Override
            public void onItemsCounted(int reachedCount, int collectedCount, int inStoreCount, int totalCount) {
                reachedButton.setText("Reached: " + reachedCount);
                inStoreButton.setText("In the store: " + inStoreCount);
                atCollectorButton.setText("Collected: " + collectedCount);
                totalItemsTextView.setText("Total items: " + totalCount);
            }
        });
    }

}//end class
