package com.example.hands_free;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.google.firebase.database.DatabaseReference;

public class StoreScanQRFragment extends Fragment {

    //Attribute
    private Button scanQrBtn, addItemsBtn,printButton;
    private TextView scannedValue, serial_number, greeting;
    private String shopperEmail, serialNumber;
    private DatabaseReference databaseReference;
    private ActivityResultLauncher<ScanOptions> scanLauncher;
    private PrintManager printManager;
    private String PreviouScannedValue;

    private CardView cardView;

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_store_scan_qr, container, false);

        scanQrBtn = view.findViewById(R.id.scanQrBtn);
        scannedValue = view.findViewById(R.id.scannedValue);
        addItemsBtn = view.findViewById(R.id.addItemButton);
        serial_number = view.findViewById(R.id.serialNumberTextView);
        greeting = view.findViewById(R.id.greeting);
        cardView = view.findViewById(R.id.serialNumberCard);
        printButton = view.findViewById(R.id.printButton);
        printManager = (PrintManager) requireContext().getSystemService(Context.PRINT_SERVICE);

        // 1- greeting
        greeting();

        // 2- scan shopper QR
        scanLauncher = registerForActivityResult(new ScanContract(), this::handleScanResult);
        registerUiListener();

        // 3- generate serial number
        addItemsBtn.setOnClickListener(v -> generate_serial_number());

        // 4- print serial number
        printButton.setOnClickListener(v -> {
            if (serialNumber != null && !serialNumber.isEmpty()) {
                doWebViewPrint();
            } else {
                Toast.makeText(getContext(), "No serial number to print", Toast.LENGTH_SHORT).show();
            }
        });


        return view;
    }

    // 1- greeting
    private void greeting() {
        Store store = new Store();
        //fetch store name
        store.fetchStoreName(new Store.NameFetchListener() {
            @Override
            public void onNameFetched(String name) {
                if (name != null) {
                    greeting.setText("Hello, " + name);
                }
            }
        });
    }

    // 2- scan shopper QR
    private void registerUiListener() {
        scanQrBtn.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan QR code");
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            scanLauncher.launch(options);
        });
    }
    private void handleScanResult(ScanIntentResult result) {
        if (result.getContents() == null) {
            Toast.makeText(getContext(), "Scan Cancelled", Toast.LENGTH_SHORT).show();
        } else {
            shopperEmail = result.getContents();
            scannedValue.setText(shopperEmail);
        }
    }

    // 3- generate serial number
    private void generate_serial_number() {
        String currentEmail = scannedValue.getText().toString().trim();
        if (currentEmail.isEmpty()) {
            Toast.makeText(getContext(), "Please scan QR code first", Toast.LENGTH_SHORT).show();
            return;
        }

        Store store = new Store();
        store.setShopperEmail(currentEmail);

        //First fetch store name
        store.fetchStoreName(new Store.NameFetchListener() {
            @Override
            public void onNameFetched(String name) {
                if (name == null) {
                    showToast("Failed to fetch store name");
                    return;
                }

                // Then fetch phone number
                store.fetchShopperPhoneNumber(new Store.PhoneFetchListener() {
                    @Override
                    public void onPhoneFetched(String phone) {
                        if (phone == null) {
                            showToast("Failed to fetch shopper phone number");
                            return;
                        }

                        // Generate serial number
                        serialNumber = store.Generate_SN(currentEmail);
                        if (serialNumber == null) {
                            showToast("Failed to generate serial number");
                            return;
                        }

                        // Update UI with serial number
                        requireActivity().runOnUiThread(() -> {
                            serial_number.setText(serialNumber);
                        });

                        // Add to Firebase with callback
                        store.AssignToCollector(serialNumber, currentEmail, name,
                                new Store.FirebaseCallback() {
                                    @Override
                                    public void onSuccess() {
                                        requireActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(),
                                                    "Item added successfully",
                                                    Toast.LENGTH_SHORT).show();
                                            // Enable print button
                                            printButton.setEnabled(true);
                                            cardView.setVisibility(View.VISIBLE);
                                            scannedValue.setText("");
                                            scannedValue.setHint("Shopper email");
                                        });
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        requireActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(),
                                                    "Failed to add item: " + error,
                                                    Toast.LENGTH_SHORT).show();
                                            // Handle failure (e.g., reset UI elements)
                                            printButton.setEnabled(false);
                                            cardView.setVisibility(View.GONE);
                                        });
                                    }
                        });
                    }
                });
            }
        });
    }
    private void showToast(String message) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    // 4- print serial number
    private void doWebViewPrint() {
        WebView webView = new WebView(requireContext());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                createWebPrintJob(view);
            }
        });

        //Generate an HTML document
        String htmlDocument =
                "<html><head>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; text-align: center; margin: 20px; }" +
                        "h1 { font-size: 24px; margin: 20px 0; }" +
                        ".serial { padding: 20px; border: 2px solid #000; display: inline-block; }" +
                        "@media print { .serial { page-break-inside: avoid; } }" +
                        "</style>" +
                        "</head><body>" +
                            "<div class='serial'>" +
                                "<h1>Serial Number</h1>" +
                            "   <h2>" + serialNumber + "</h2>" +
                            "</div>" +
                        "</body></html>";

        webView.loadDataWithBaseURL(null, htmlDocument, "text/HTML", "UTF-8", null);
    }
    private void createWebPrintJob(WebView webView) {
        String jobName = getString(R.string.app_name) + " Serial Number";

        PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();

        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
        PrintJob printJob = printManager.print(jobName, printAdapter, attributes);
    }



}