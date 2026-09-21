package com.example.agaplinkproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SCREEN 5 — Emergency Directory & Safety Advisories
 *
 * Verified, one-tap emergency hotline directory for Bacolod City (BFP, BCPO, CDRRMO
 * EMS, and CLMMRH), plus a CDRRMO weather bulletin advisory. Content is hard-coded/
 * mock data, consistent with this prototype's offline-first design (see ReportActivity
 * and TrackActivity for the same convention). "CALL NOW" fires Intent.ACTION_DIAL only
 * (see HomeDashboardActivity.launchDialer for the same pattern) — no call is placed and
 * no CALL_PHONE permission is requested by this app.
 */
public class DirectoryActivity extends AppCompatActivity implements View.OnClickListener {

    // ---------- BACOLOD CITY EMERGENCY HOTLINES ----------
    private static final String TEL_BFP_FIRE = "0344345678";
    private static final String TEL_BCPO_POLICE = "911";
    private static final String TEL_CDRRMO_EMS = "0344323879";
    private static final String TEL_CLMMRH_HOSPITAL = "0347014400";

    // ---------- SERVICE FILTER CATEGORIES ----------
    private static final String CATEGORY_ALL = "ALL";
    private static final String CATEGORY_FIRE = "FIRE";
    private static final String CATEGORY_POLICE = "POLICE";
    private static final String CATEGORY_MEDICAL = "MEDICAL";

    // ---------- VIEWS ----------
    private View btnProfileAvatar;
    private BottomNavigationView bottomNavigation;
    private EditText etSearchDirectory;

    private TextView chipAllServices;
    private TextView chipFireRescue;
    private TextView chipPolice;
    private TextView chipMedicalEr;

    private MaterialCardView cardBfp;
    private MaterialCardView cardBcpo;
    private MaterialCardView cardEms;
    private MaterialCardView cardClmmrh;

    private MaterialButton btnCallBfp;
    private MaterialButton btnCallBcpo;
    private MaterialButton btnCallEms;
    private MaterialButton btnCallClmmrh;

    private View btnOfficialPortal;

    // ---------- STATE ----------
    private String selectedCategory = CATEGORY_ALL;

    /** One directory card plus the data needed to filter it by category and search text. */
    private static class DirectoryEntry {
        final MaterialCardView card;
        final String category;
        final String searchableText;

        DirectoryEntry(MaterialCardView card, String category, String searchableText) {
            this.card = card;
            this.category = category;
            this.searchableText = searchableText.toLowerCase(Locale.US);
        }
    }

    private final List<DirectoryEntry> directoryEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory);

        initViews();
        buildDirectoryEntries();
        registerListeners();
        setupBottomNavigation();

        // The "All Services" chip's selected look (bg_filter_chip / filter_chip_text,
        // both keyed off android:state_selected) is now driven entirely from Java —
        // no chip carries a hardcoded android:selected="true" in the layout — so set
        // the initial selection here instead.
        onChipSelected(CATEGORY_ALL);
    }

    // ------------------------------------------------------------------
    // SETUP
    // ------------------------------------------------------------------

    private void initViews() {
        btnProfileAvatar = findViewById(R.id.btnProfileAvatar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        etSearchDirectory = findViewById(R.id.etSearchDirectory);

        chipAllServices = findViewById(R.id.chipAllServices);
        chipFireRescue = findViewById(R.id.chipFireRescue);
        chipPolice = findViewById(R.id.chipPolice);
        chipMedicalEr = findViewById(R.id.chipMedicalEr);

        cardBfp = findViewById(R.id.cardBfp);
        cardBcpo = findViewById(R.id.cardBcpo);
        cardEms = findViewById(R.id.cardEms);
        cardClmmrh = findViewById(R.id.cardClmmrh);

        btnCallBfp = findViewById(R.id.btnCallBfp);
        btnCallBcpo = findViewById(R.id.btnCallBcpo);
        btnCallEms = findViewById(R.id.btnCallEms);
        btnCallClmmrh = findViewById(R.id.btnCallClmmrh);

        btnOfficialPortal = findViewById(R.id.btnOfficialPortal);
    }

    private void buildDirectoryEntries() {
        directoryEntries.add(new DirectoryEntry(cardBfp, CATEGORY_FIRE,
                "Bureau of Fire Protection BFP Emergency Fire Response Station 1"));
        directoryEntries.add(new DirectoryEntry(cardBcpo, CATEGORY_POLICE,
                "Bacolod City Police Office BCPO Law Enforcement Rapid Patrol"));
        directoryEntries.add(new DirectoryEntry(cardEms, CATEGORY_MEDICAL,
                "Bacolod CDRRMO EMS Ambulance Medical Emergencies Rescue"));
        directoryEntries.add(new DirectoryEntry(cardClmmrh, CATEGORY_MEDICAL,
                "Corazon Locsin Montelibano Memorial Regional Hospital CLMMRH Tertiary ER Trauma"));
    }

    private void registerListeners() {
        if (btnProfileAvatar != null) {
            btnProfileAvatar.setOnClickListener(v -> ProfileMenuHelper.showProfileMenu(this, btnProfileAvatar));
        }
        if (chipAllServices != null) chipAllServices.setOnClickListener(this);
        if (chipFireRescue != null) chipFireRescue.setOnClickListener(this);
        if (chipPolice != null) chipPolice.setOnClickListener(this);
        if (chipMedicalEr != null) chipMedicalEr.setOnClickListener(this);

        if (btnCallBfp != null) btnCallBfp.setOnClickListener(this);
        if (btnCallBcpo != null) btnCallBcpo.setOnClickListener(this);
        if (btnCallEms != null) btnCallEms.setOnClickListener(this);
        if (btnCallClmmrh != null) btnCallClmmrh.setOnClickListener(this);

        if (btnOfficialPortal != null) {
            btnOfficialPortal.setOnClickListener(v ->
                    Toast.makeText(this, "Opening Bacolod CDRRMO Official Portal...", Toast.LENGTH_SHORT).show());
        }

        if (etSearchDirectory != null) {
            etSearchDirectory.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFilters();
                }

                @Override
                public void afterTextChanged(Editable s) { }
            });
        }
    }

    // ------------------------------------------------------------------
    // SECTION: SEARCH + SERVICE FILTER CHIPS
    // ------------------------------------------------------------------

    private void onChipSelected(String category) {
        selectedCategory = category;

        chipAllServices.setSelected(category.equals(CATEGORY_ALL));
        chipFireRescue.setSelected(category.equals(CATEGORY_FIRE));
        chipPolice.setSelected(category.equals(CATEGORY_POLICE));
        chipMedicalEr.setSelected(category.equals(CATEGORY_MEDICAL));

        applyFilters();
    }

    /** Simple local/mock filtering: matches the selected category AND the search query, offline-first. */
    private void applyFilters() {
        String query = etSearchDirectory != null
                ? etSearchDirectory.getText().toString().trim().toLowerCase(Locale.US)
                : "";

        for (DirectoryEntry entry : directoryEntries) {
            boolean matchesCategory = selectedCategory.equals(CATEGORY_ALL)
                    || selectedCategory.equals(entry.category);
            boolean matchesQuery = query.isEmpty() || entry.searchableText.contains(query);

            entry.card.setVisibility(matchesCategory && matchesQuery ? View.VISIBLE : View.GONE);
        }
    }

    // ------------------------------------------------------------------
    // BOTTOM NAVIGATION
    // ------------------------------------------------------------------

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    Intent homeIntent = new Intent(DirectoryActivity.this, HomeDashboardActivity.class);
                    // Leaving Directory for another tab: finish Directory and reuse the
                    // existing Home instance via CLEAR_TOP, matching the pattern already
                    // used by ReportActivity/TrackActivity's nav_home handler so the back
                    // stack never grows beyond [Home, <current secondary screen>].
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(homeIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_report) {
                    Intent reportIntent = new Intent(DirectoryActivity.this, ReportActivity.class);
                    reportIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(reportIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_track) {
                    Intent trackIntent = new Intent(DirectoryActivity.this, TrackActivity.class);
                    trackIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(trackIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_directory) {
                    // Already on the Directory screen.
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
        if (bottomNavigation != null && bottomNavigation.getSelectedItemId() != R.id.nav_directory) {
            bottomNavigation.setSelectedItemId(R.id.nav_directory);
        }
    }

    // ------------------------------------------------------------------
    // CLICK ROUTING
    // ------------------------------------------------------------------

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if (viewId == R.id.chipAllServices) {
            onChipSelected(CATEGORY_ALL);
        } else if (viewId == R.id.chipFireRescue) {
            onChipSelected(CATEGORY_FIRE);
        } else if (viewId == R.id.chipPolice) {
            onChipSelected(CATEGORY_POLICE);
        } else if (viewId == R.id.chipMedicalEr) {
            onChipSelected(CATEGORY_MEDICAL);
        } else if (viewId == R.id.btnCallBfp) {
            launchDialer(TEL_BFP_FIRE);
        } else if (viewId == R.id.btnCallBcpo) {
            launchDialer(TEL_BCPO_POLICE);
        } else if (viewId == R.id.btnCallEms) {
            launchDialer(TEL_CDRRMO_EMS);
        } else if (viewId == R.id.btnCallClmmrh) {
            launchDialer(TEL_CLMMRH_HOSPITAL);
        }
    }

    private void launchDialer(String phoneNumber) {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(dialIntent);
    }
}
