package com.example.agaplinkproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationBarView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * SCREEN 3 — One-Tap Report & Evidence Submission
 *
 * Offline-first citizen emergency report form. The report is composed entirely
 * on-device (no live network dependency); evidence photos are captured via the
 * Android Camera intent (with a gallery fallback) and the final "transmission"
 * is mocked/queued locally, consistent with the app's offline SQLite queue design.
 */
public class ReportActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MAX_REMARKS_LENGTH = 250;
    private static final String LOCAL_TRANSACTION_ID = "#BCD-TX-9104";

    // ---------- INCIDENT TYPE STATE ----------
    private enum IncidentType { STRUCTURAL_FIRE, MEDICAL_DISTRESS, SECURITY_THREAT }

    private IncidentType selectedIncidentType = IncidentType.STRUCTURAL_FIRE;

    // ---------- VIEWS ----------
    private ImageView btnBack;
    private View btnProfileAvatar;
    private BottomNavigationView bottomNavigation;

    private MaterialCardView cardIncidentFire;
    private MaterialCardView cardIncidentMedical;
    private MaterialCardView cardIncidentSecurity;
    private ImageView ivFireRadio;
    private ImageView ivMedicalRadio;
    private ImageView ivSecurityRadio;

    private FrameLayoutRefs evidenceRefs;
    private MaterialButton btnCapturePhoto;

    private EditText etTacticalRemarks;
    private TextView tvCharCounter;

    private MaterialButton btnTransmitReport;

    // ---------- CAMERA / EVIDENCE CAPTURE ----------
    private Uri pendingPhotoUri;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    // Small holder for the evidence FrameLayout's child views
    private static class FrameLayoutRefs {
        LinearLayout emptyState;
        ImageView preview;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        registerActivityResultLaunchers();

        initViews();
        registerListeners();
        setupBottomNavigation();
        setupIncidentSelection();
        setupRemarksCharacterCounter();
    }

    // ------------------------------------------------------------------
    // SETUP
    // ------------------------------------------------------------------

    // BIND EVERY VIEW THIS ACTIVITY NEEDS FROM activity_report.xml: THE BACK/PROFILE
    // HEADER CONTROLS, THE BOTTOM NAVIGATION BAR, THE THREE INCIDENT-TYPE SELECTOR
    // CARDS AND THEIR RADIO ICONS, THE EVIDENCE PREVIEW AREA, THE REMARKS INPUT AND
    // ITS CHARACTER COUNTER, AND THE FINAL "TRANSMIT REPORT" BUTTON.
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnProfileAvatar = findViewById(R.id.btnProfileAvatar);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        cardIncidentFire = findViewById(R.id.cardIncidentFire);
        cardIncidentMedical = findViewById(R.id.cardIncidentMedical);
        cardIncidentSecurity = findViewById(R.id.cardIncidentSecurity);
        ivFireRadio = findViewById(R.id.ivFireRadio);
        ivMedicalRadio = findViewById(R.id.ivMedicalRadio);
        ivSecurityRadio = findViewById(R.id.ivSecurityRadio);

        evidenceRefs = new FrameLayoutRefs();
        evidenceRefs.emptyState = findViewById(R.id.evidenceEmptyState);
        evidenceRefs.preview = findViewById(R.id.ivEvidencePreview);
        btnCapturePhoto = findViewById(R.id.btnCapturePhoto);

        etTacticalRemarks = findViewById(R.id.etTacticalRemarks);
        tvCharCounter = findViewById(R.id.tvCharCounter);

        btnTransmitReport = findViewById(R.id.btnTransmitReport);
    }

    // ATTACH THIS ACTIVITY (View.OnClickListener) TO EVERY TAPPABLE VIEW ON THE
    // REPORT SCREEN. NULL CHECKS GUARD AGAINST A VIEW ID MISSING FROM THE LAYOUT.
    private void registerListeners() {
        if (btnBack != null) btnBack.setOnClickListener(this);
        if (btnProfileAvatar != null) btnProfileAvatar.setOnClickListener(this);
        if (cardIncidentFire != null) cardIncidentFire.setOnClickListener(this);
        if (cardIncidentMedical != null) cardIncidentMedical.setOnClickListener(this);
        if (cardIncidentSecurity != null) cardIncidentSecurity.setOnClickListener(this);
        if (btnCapturePhoto != null) btnCapturePhoto.setOnClickListener(this);
        if (btnTransmitReport != null) btnTransmitReport.setOnClickListener(this);
    }

    // WIRE UP THE BOTTOM NAVIGATION BAR'S FOUR DESTINATIONS (HOME, REPORT, TRACK,
    // DIRECTORY). SINCE THIS ACTIVITY IS THE REPORT SCREEN, THE nav_report BRANCH
    // JUST CONFIRMS THE SELECTION INSTEAD OF STARTING A NEW ACTIVITY.
    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    Intent homeIntent = new Intent(ReportActivity.this, HomeDashboardActivity.class);
                    // CLEAR_TOP + singleTop (see manifest) returns to the existing
                    // Dashboard instance instead of creating a new one, keeping the
                    // back stack clean across repeated Dashboard <-> Report switches.
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(homeIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_report) {
                    // Already on the Report screen.
                    return true;
                } else if (itemId == R.id.nav_track) {
                    Intent trackIntent = new Intent(ReportActivity.this, TrackActivity.class);
                    // CLEAR_TOP + singleTop (see manifest) reuses an existing Track
                    // instance instead of stacking a new one on every tap. Report
                    // finishes itself here (same as the nav_home branch above), so
                    // the back stack stays at [Home, Track] instead of growing with
                    // every cross-tab switch.
                    trackIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(trackIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_directory) {
                    Intent directoryIntent = new Intent(ReportActivity.this, DirectoryActivity.class);
                    directoryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(directoryIntent);
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // See HomeDashboardActivity.onResume() for why this can't live in onCreate()
        // alone: singleTop launchMode means a resumed instance skips onCreate().
        if (bottomNavigation != null && bottomNavigation.getSelectedItemId() != R.id.nav_report) {
            bottomNavigation.setSelectedItemId(R.id.nav_report);
        }
    }

    // ------------------------------------------------------------------
    // SECTION 1: INCIDENT TYPE SELECTION
    // ------------------------------------------------------------------

    private void setupIncidentSelection() {
        // Card 1 (Structural Fire) is selected by default via the XML styling;
        // make sure the Java-side state matches on launch.
        applyIncidentSelectionUI();
    }

    // UPDATE THE SELECTED INCIDENT TYPE STATE AND REFRESH ALL THREE CARDS TO MATCH.
    private void selectIncidentType(IncidentType type) {
        selectedIncidentType = type;
        applyIncidentSelectionUI();
    }

    // RE-STYLE ALL THREE INCIDENT-TYPE CARDS (FIRE, MEDICAL, SECURITY) SO ONLY THE
    // ONE MATCHING selectedIncidentType LOOKS SELECTED (FILLED RADIO + HIGHLIGHTED CARD).
    private void applyIncidentSelectionUI() {
        boolean fireSelected = selectedIncidentType == IncidentType.STRUCTURAL_FIRE;
        boolean medicalSelected = selectedIncidentType == IncidentType.MEDICAL_DISTRESS;
        boolean securitySelected = selectedIncidentType == IncidentType.SECURITY_THREAT;

        styleIncidentCard(cardIncidentFire, ivFireRadio, fireSelected);
        styleIncidentCard(cardIncidentMedical, ivMedicalRadio, medicalSelected);
        styleIncidentCard(cardIncidentSecurity, ivSecurityRadio, securitySelected);
    }

    // APPLY THE "SELECTED" OR "UNSELECTED" VISUAL TREATMENT TO A SINGLE INCIDENT-TYPE
    // CARD: BACKGROUND TINT, STROKE COLOR/WIDTH, AND WHETHER ITS RADIO ICON IS FILLED.
    private void styleIncidentCard(MaterialCardView card, ImageView radioIcon, boolean selected) {
        if (card == null || radioIcon == null) return;

        if (selected) {
            card.setCardBackgroundColor(getColor(R.color.incident_selected_bg));
            card.setStrokeColor(getColor(R.color.incident_selected_stroke));
            card.setStrokeWidth(dpToPx(1.5f));
            radioIcon.setImageResource(R.drawable.ic_radio_selected);
        } else {
            card.setCardBackgroundColor(getColor(R.color.white));
            card.setStrokeColor(getColor(R.color.incident_unselected_stroke));
            card.setStrokeWidth(dpToPx(1f));
            radioIcon.setImageResource(R.drawable.ic_radio_unselected);
        }
    }

    // CONVERT A DENSITY-INDEPENDENT (DP) VALUE INTO ACTUAL SCREEN PIXELS FOR THIS
    // DEVICE, USED WHEN SETTING THE INCIDENT CARD STROKE WIDTH PROGRAMMATICALLY.
    private int dpToPx(float dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ------------------------------------------------------------------
    // SECTION 3: EVIDENCE CAPTURE (CAMERA / GALLERY)
    // ------------------------------------------------------------------

    // REGISTER THE THREE Activity Result LAUNCHERS THIS SCREEN NEEDS, BEFORE THE
    // ACTIVITY REACHES STARTED STATE (REQUIRED BY THE ActivityResultLauncher API):
    // ONE TO LAUNCH THE CAMERA APP, ONE TO REQUEST THE CAMERA PERMISSION, AND ONE
    // TO LAUNCH THE GALLERY PICKER AS A FALLBACK.
    private void registerActivityResultLaunchers() {
        // HANDLES THE RESULT OF THE CAMERA CAPTURE INTENT: ON SUCCESS, SHOW THE
        // PHOTO THAT WAS WRITTEN TO pendingPhotoUri AS THE EVIDENCE PREVIEW.
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success != null && success && pendingPhotoUri != null) {
                        showEvidencePreview(pendingPhotoUri);
                    } else {
                        Toast.makeText(this, "Photo capture cancelled.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // HANDLES THE RESULT OF THE RUNTIME CAMERA PERMISSION REQUEST: LAUNCH THE
        // CAMERA IF GRANTED, OTHERWISE FALL BACK TO THE GALLERY PICKER.
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted != null && granted) {
                        launchCameraCapture();
                    } else {
                        Toast.makeText(this,
                                "Camera permission denied. You can still attach a photo from your gallery.",
                                Toast.LENGTH_LONG).show();
                        launchGalleryPicker();
                    }
                }
        );

        // HANDLES THE RESULT OF THE GALLERY PICKER: SHOW WHICHEVER IMAGE THE USER
        // CHOSE AS THE EVIDENCE PREVIEW.
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        showEvidencePreview(uri);
                    }
                }
        );
    }

    // ENTRY POINT FOR THE "ATTACH EVIDENCE PHOTO" BUTTON: FALLS BACK TO THE GALLERY
    // IF THE DEVICE HAS NO CAMERA, OTHERWISE REQUESTS THE CAMERA PERMISSION (IF NOT
    // ALREADY GRANTED) BEFORE LAUNCHING THE CAMERA CAPTURE FLOW.
    private void onCapturePhotoClicked() {
        boolean hasCameraHardware = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);

        if (!hasCameraHardware) {
            Toast.makeText(this, "No camera detected on this device. Opening gallery instead.", Toast.LENGTH_SHORT).show();
            launchGalleryPicker();
            return;
        }

        boolean hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        if (hasPermission) {
            launchCameraCapture();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // CREATE A DESTINATION FILE FOR THE PHOTO, WRAP IT IN A FileProvider content://
    // Uri (REQUIRED SINCE THE CAMERA APP RUNS IN A DIFFERENT PROCESS), AND LAUNCH
    // THE SYSTEM CAMERA APP TO WRITE THE CAPTURED PHOTO TO THAT URI.
    private void launchCameraCapture() {
        try {
            File photoFile = createEvidenceImageFile();
            pendingPhotoUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    photoFile
            );
            takePictureLauncher.launch(pendingPhotoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Unable to prepare camera storage. Try the gallery instead.", Toast.LENGTH_SHORT).show();
            launchGalleryPicker();
        } catch (Exception e) {
            // Defensive: never let evidence capture crash the report flow.
            Toast.makeText(this, "Camera unavailable right now.", Toast.LENGTH_SHORT).show();
        }
    }

    // LAUNCH THE SYSTEM IMAGE PICKER, RESTRICTED TO IMAGE FILES, AS THE FALLBACK
    // EVIDENCE-CAPTURE PATH WHEN THE CAMERA IS UNAVAILABLE OR ITS PERMISSION IS DENIED.
    private void launchGalleryPicker() {
        try {
            pickImageLauncher.launch("image/*");
        } catch (Exception e) {
            Toast.makeText(this, "No gallery app available.", Toast.LENGTH_SHORT).show();
        }
    }

    // CREATE A UNIQUELY-NAMED, TIMESTAMPED .jpg FILE INSIDE THE APP'S PRIVATE
    // CACHE DIRECTORY (cache/evidence/) TO HOLD THE NEXT CAPTURED EVIDENCE PHOTO.
    private File createEvidenceImageFile() throws IOException {
        File evidenceDir = new File(getCacheDir(), "evidence");
        if (!evidenceDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            evidenceDir.mkdirs();
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new java.util.Date());
        return File.createTempFile("EVIDENCE_" + timestamp + "_", ".jpg", evidenceDir);
    }

    // SWAP THE EVIDENCE SLOT FROM ITS "NO PHOTO ATTACHED" EMPTY STATE TO SHOWING
    // THE CAPTURED/PICKED IMAGE.
    private void showEvidencePreview(Uri imageUri) {
        if (evidenceRefs.preview == null || evidenceRefs.emptyState == null) return;
        evidenceRefs.preview.setImageURI(imageUri);
        evidenceRefs.preview.setVisibility(View.VISIBLE);
        evidenceRefs.emptyState.setVisibility(View.GONE);
    }

    // ------------------------------------------------------------------
    // SECTION 4: TACTICAL REMARKS CHARACTER COUNTER
    // ------------------------------------------------------------------

    // CAP THE TACTICAL REMARKS FIELD AT MAX_REMARKS_LENGTH CHARACTERS AND KEEP THE
    // "X / 250" COUNTER LABEL IN SYNC WITH THE CURRENT TEXT LENGTH ON EVERY KEYSTROKE.
    private void setupRemarksCharacterCounter() {
        if (etTacticalRemarks == null || tvCharCounter == null) return;

        etTacticalRemarks.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_REMARKS_LENGTH)});

        etTacticalRemarks.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                int length = s == null ? 0 : s.length();
                tvCharCounter.setText(length + " / " + MAX_REMARKS_LENGTH);
            }
        });
    }

    // ------------------------------------------------------------------
    // TRANSMIT (MOCK / LOCAL QUEUE)
    // ------------------------------------------------------------------

    private void onTransmitReportClicked() {
        // This is an offline-first prototype: the report is queued locally
        // rather than assuming a live network connection is available.
        String incidentLabel;
        switch (selectedIncidentType) {
            case MEDICAL_DISTRESS:
                incidentLabel = "Medical Distress";
                break;
            case SECURITY_THREAT:
                incidentLabel = "Security Threat";
                break;
            case STRUCTURAL_FIRE:
            default:
                incidentLabel = "Structural Fire";
                break;
        }

        Toast.makeText(
                this,
                "Report queued locally (" + incidentLabel + ") — Transaction " + LOCAL_TRANSACTION_ID
                        + ". Will sync when Bacolod Central Dispatch is reachable.",
                Toast.LENGTH_LONG
        ).show();
    }

    // ------------------------------------------------------------------
    // CLICK ROUTING
    // ------------------------------------------------------------------

    // CENTRAL CLICK ROUTER: DISPATCHES EACH TAPPED VIEW'S ID TO THE HANDLER FOR
    // THAT SPECIFIC REPORT-SCREEN ACTION (BACK, PROFILE MENU, INCIDENT TYPE PICK,
    // EVIDENCE CAPTURE, OR REPORT TRANSMISSION).
    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if (viewId == R.id.btnBack) {
            onBackPressed();
        } else if (viewId == R.id.btnProfileAvatar) {
            ProfileMenuHelper.showProfileMenu(this, btnProfileAvatar);
        } else if (viewId == R.id.cardIncidentFire) {
            selectIncidentType(IncidentType.STRUCTURAL_FIRE);
        } else if (viewId == R.id.cardIncidentMedical) {
            selectIncidentType(IncidentType.MEDICAL_DISTRESS);
        } else if (viewId == R.id.cardIncidentSecurity) {
            selectIncidentType(IncidentType.SECURITY_THREAT);
        } else if (viewId == R.id.btnCapturePhoto) {
            onCapturePhotoClicked();
        } else if (viewId == R.id.btnTransmitReport) {
            onTransmitReportClicked();
        }
    }
}
