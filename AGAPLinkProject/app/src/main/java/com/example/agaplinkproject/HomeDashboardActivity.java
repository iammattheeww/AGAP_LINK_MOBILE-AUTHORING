package com.example.agaplinkproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationBarView;

// SCREEN 2 — HOME DASHBOARD: THE CITIZEN'S LANDING SCREEN AFTER A SUCCESSFUL LOGIN.
// SHOWS A ONE-TAP "REPORT EMERGENCY NOW" ACTION, QUICK-DIAL CARDS FOR THE PNP, BFP,
// AND CDRRMO HOTLINES, SHORTCUTS TO THE LIVE MAP / DISPATCH LOGS, AND OWNS THE
// BOTTOM NAVIGATION BAR SHARED WITH REPORT, TRACK, AND DIRECTORY.
public class HomeDashboardActivity extends AppCompatActivity implements View.OnClickListener {

    // ---------- BACOLOD CITY EMERGENCY HOTLINES ----------
    private static final String TEL_PNP_911 = "911";
    private static final String TEL_BFP_FIRE = "0344345678";
    private static final String TEL_CDRRMO_EMS = "0344323879";

    // ---------- CLASS-LEVEL VIEW DECLARATIONS ----------
    private FrameLayout btnProfileAvatar;
    private ImageView btnRefreshLocation;
    private MaterialButton btnReportEmergencyNow;
    private MaterialButton btnViewLiveMap;
    private MaterialButton btnDispatchLogs;
    private MaterialCardView cardCallPnp;
    private MaterialCardView cardCallBfp;
    private MaterialCardView cardCallCdrrmo;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_dashboard);

        // 1. Initialize Views from XML
        initViews();

        // 2. Register Event Listeners
        registerListeners();

        // 3. Configure Bottom Navigation Bar
        setupBottomNavigation();
    }

    // BIND EVERY INTERACTIVE VIEW FROM activity_home_dashboard.xml TO ITS FIELD SO
    // THE REST OF THE ACTIVITY CAN REFERENCE THEM WITHOUT REPEATED findViewById() CALLS.
    private void initViews() {
        btnProfileAvatar = findViewById(R.id.btnProfileAvatar);
        btnRefreshLocation = findViewById(R.id.btnRefreshLocation);
        btnReportEmergencyNow = findViewById(R.id.btnReportEmergencyNow);
        btnViewLiveMap = findViewById(R.id.btnViewLiveMap);
        btnDispatchLogs = findViewById(R.id.btnDispatchLogs);
        cardCallPnp = findViewById(R.id.cardCallPnp);
        cardCallBfp = findViewById(R.id.cardCallBfp);
        cardCallCdrrmo = findViewById(R.id.cardCallCdrrmo);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    // ATTACH THIS ACTIVITY (View.OnClickListener) TO EVERY TAPPABLE VIEW ON THE
    // DASHBOARD. NULL CHECKS GUARD AGAINST A VIEW ID MISSING FROM THE INFLATED LAYOUT.
    private void registerListeners() {
        if (btnProfileAvatar != null) {
            btnProfileAvatar.setOnClickListener(this);
        }

        if (btnRefreshLocation != null) {
            btnRefreshLocation.setOnClickListener(this);
        }

        if (btnReportEmergencyNow != null) {
            btnReportEmergencyNow.setOnClickListener(this);
        }

        if (btnViewLiveMap != null) {
            btnViewLiveMap.setOnClickListener(this);
        }

        if (btnDispatchLogs != null) {
            btnDispatchLogs.setOnClickListener(this);
        }

        if (cardCallPnp != null) {
            cardCallPnp.setOnClickListener(this);
        }

        if (cardCallBfp != null) {
            cardCallBfp.setOnClickListener(this);
        }

        if (cardCallCdrrmo != null) {
            cardCallCdrrmo.setOnClickListener(this);
        }
    }

    // CENTRAL CLICK ROUTER: DISPATCHES EACH TAPPED VIEW'S ID TO THE HANDLER FOR
    // THAT SPECIFIC DASHBOARD ACTION (PROFILE MENU, GPS REFRESH, EMERGENCY REPORT,
    // LIVE MAP, DISPATCH LOGS, OR ONE OF THE THREE QUICK-DIAL HOTLINE CARDS).
    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if (viewId == R.id.btnProfileAvatar) {
            handleProfileClick();
        } else if (viewId == R.id.btnRefreshLocation) {
            Toast.makeText(this, "Refreshing GPS location via FusedLocationProvider...", Toast.LENGTH_SHORT).show();
        } else if (viewId == R.id.btnReportEmergencyNow) {
            startActivity(new Intent(this, ReportActivity.class));
        } else if (viewId == R.id.btnViewLiveMap) {
            Toast.makeText(this, "Opening Live Map for Ticket #BCD-2026-0891...", Toast.LENGTH_SHORT).show();
        } else if (viewId == R.id.btnDispatchLogs) {
            Toast.makeText(this, "Loading Incident Dispatch Timeline...", Toast.LENGTH_SHORT).show();
        } else if (viewId == R.id.cardCallPnp) {
            launchDialer(TEL_PNP_911);
        } else if (viewId == R.id.cardCallBfp) {
            launchDialer(TEL_BFP_FIRE);
        } else if (viewId == R.id.cardCallCdrrmo) {
            launchDialer(TEL_CDRRMO_EMS);
        }
    }

    // WIRE UP THE BOTTOM NAVIGATION BAR'S FOUR DESTINATIONS (HOME, REPORT, TRACK,
    // DIRECTORY). BECAUSE THIS ACTIVITY IS ALREADY THE HOME SCREEN, THE nav_home
    // BRANCH JUST CONFIRMS THE SELECTION INSTEAD OF STARTING A NEW ACTIVITY.
    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    // ALREADY ON THE HOME DASHBOARD; NO NAVIGATION IS NEEDED.
                    Toast.makeText(HomeDashboardActivity.this, "Home selected", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_report) {
                    Intent reportIntent = new Intent(HomeDashboardActivity.this, ReportActivity.class);
                    // CLEAR_TOP + singleTop (see manifest) reuses an existing Report
                    // instance instead of stacking a new one on every tap, so repeated
                    // Dashboard <-> Report switching stays lightweight and reliable.
                    reportIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(reportIntent);
                    return true;
                } else if (itemId == R.id.nav_track) {
                    Intent trackIntent = new Intent(HomeDashboardActivity.this, TrackActivity.class);
                    // CLEAR_TOP + singleTop (see manifest) reuses an existing Track
                    // instance instead of stacking a new one on every tap, mirroring
                    // the Dashboard -> Report handling above. Home is intentionally
                    // NOT finished here, so it stays underneath Track on the back
                    // stack and the physical back button returns here correctly.
                    trackIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(trackIntent);
                    return true;
                } else if (itemId == R.id.nav_directory) {
                    Intent directoryIntent = new Intent(HomeDashboardActivity.this, DirectoryActivity.class);
                    // CLEAR_TOP + singleTop (see manifest) reuses an existing Directory
                    // instance instead of stacking a new one on every tap, mirroring the
                    // Dashboard -> Report/Track handling above. Home is intentionally NOT
                    // finished here, so it stays underneath Directory on the back stack.
                    directoryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(directoryIntent);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-assert the active tab every time this screen becomes visible, not just on
        // first creation. Because HomeDashboardActivity is launchMode="singleTop" and is
        // never finished when the user leaves it, returning here (e.g. Report -> Home)
        // resumes this SAME instance via onNewIntent() rather than re-running onCreate().
        // Without this, the BottomNavigationView kept showing whichever tab was selected
        // right before the user navigated away, even though Home was the visible screen.
        if (bottomNavigation != null && bottomNavigation.getSelectedItemId() != R.id.nav_home) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }

    // OPEN THE DEVICE'S DIALER PRE-FILLED WITH THE GIVEN NUMBER VIA Intent.ACTION_DIAL.
    // THIS ONLY OPENS THE DIALER (REQUIRES THE USER TO PRESS CALL THEMSELVES), SO NO
    // CALL_PHONE PERMISSION IS NEEDED.
    private void launchDialer(String phoneNumber) {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(dialIntent);
    }

    private void handleProfileClick() {
        // Tapping the avatar now opens a small profile/options menu instead of
        // logging the user out immediately; ProfileMenuHelper handles the actual
        // "Log Out" action (and the session-clearing/back-stack behavior) once the
        // user explicitly chooses it. Shared with ReportActivity, TrackActivity and
        // DirectoryActivity so the interaction is identical everywhere the avatar
        // appears.
        ProfileMenuHelper.showProfileMenu(this, btnProfileAvatar);
    }
}