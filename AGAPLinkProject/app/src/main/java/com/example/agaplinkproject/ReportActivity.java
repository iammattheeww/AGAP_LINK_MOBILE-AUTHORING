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

    private void registerListeners() {
        if (btnBack != null) btnBack.setOnClickListener(this);
        if (btnProfileAvatar != null) btnProfileAvatar.setOnClickListener(this);
        if (cardIncidentFire != null) cardIncidentFire.setOnClickListener(this);
        if (cardIncidentMedical != null) cardIncidentMedical.setOnClickListener(this);
        if (cardIncidentSecurity != null) cardIncidentSecurity.setOnClickListener(this);
        if (btnCapturePhoto != null) btnCapturePhoto.setOnClickListener(this);
        if (btnTransmitReport != null) btnTransmitReport.setOnClickListener(this);
    }

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

    private void selectIncidentType(IncidentType type) {
        selectedIncidentType = type;
        applyIncidentSelectionUI();
    }

    private void applyIncidentSelectionUI() {
        boolean fireSelected = selectedIncidentType == IncidentType.STRUCTURAL_FIRE;
        boolean medicalSelected = selectedIncidentType == IncidentType.MEDICAL_DISTRESS;
        boolean securitySelected = selectedIncidentType == IncidentType.SECURITY_THREAT;

        styleIncidentCard(cardIncidentFire, ivFireRadio, fireSelected);
        styleIncidentCard(cardIncidentMedical, ivMedicalRadio, medicalSelected);
        styleIncidentCard(cardIncidentSecurity, ivSecurityRadio, securitySelected);
    }

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

    private int dpToPx(float dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ------------------------------------------------------------------
    // SECTION 3: EVIDENCE CAPTURE (CAMERA / GALLERY)
    // ------------------------------------------------------------------

    private void registerActivityResultLaunchers() {
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

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        showEvidencePreview(uri);
                    }
                }
        );
    }

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

    private void launchGalleryPicker() {
        try {
            pickImageLauncher.launch("image/*");
        } catch (Exception e) {
            Toast.makeText(this, "No gallery app available.", Toast.LENGTH_SHORT).show();
        }
    }

    private File createEvidenceImageFile() throws IOException {
        File evidenceDir = new File(getCacheDir(), "evidence");
        if (!evidenceDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            evidenceDir.mkdirs();
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new java.util.Date());
        return File.createTempFile("EVIDENCE_" + timestamp + "_", ".jpg", evidenceDir);
    }

    private void showEvidencePreview(Uri imageUri) {
        if (evidenceRefs.preview == null || evidenceRefs.emptyState == null) return;
        evidenceRefs.preview.setImageURI(imageUri);
        evidenceRefs.preview.setVisibility(View.VISIBLE);
        evidenceRefs.emptyState.setVisibility(View.GONE);
    }

    // ------------------------------------------------------------------
    // SECTION 4: TACTICAL REMARKS CHARACTER COUNTER
    // ------------------------------------------------------------------

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
