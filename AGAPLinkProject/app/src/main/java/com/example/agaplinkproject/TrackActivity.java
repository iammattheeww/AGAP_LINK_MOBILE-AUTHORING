package com.example.agaplinkproject;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationBarView;

/**
 * SCREEN 4 — Track & Incident History
 *
 * Shows the citizen's dispatch tracker: a September 2026 incident calendar,
 * the currently active/most-recent incident with its dispatch progress chain,
 * a response-speed rating control, and a short list of archived incidents.
 *
 * Content is hard-coded/mock data, consistent with this prototype's offline-first
 * design (see ReportActivity for the same convention).
 */
public class TrackActivity extends AppCompatActivity implements View.OnClickListener {

    // ---------- CALENDAR (SEPTEMBER 2026, HARD-CODED TO MATCH THE WIREFRAME) ----------
    // Each cell: day number to display. Numbers belonging to the previous/next month
    // are flagged in PREV_NEXT_MONTH_ROWCOLS so they can be dimmed instead of styled
    // as part of the current month.
    private static final int[][] CALENDAR_DAYS = {
            {30, 31, 1, 2, 3, 4, 5},
            {6, 7, 8, 9, 10, 11, 12},
            {13, 14, 15, 16, 17, 18, 19},
            {20, 21, 22, 23, 24, 25, 26},
            {27, 28, 29, 30, 1, 2, 3}
    };
    // Cells that belong to August/October rather than September (row, col) — 0-indexed.
    private static final boolean[][] OUT_OF_MONTH = {
            {true, true, false, false, false, false, false},
            {false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false},
            {false, false, false, false, true, true, true}
    };
    private static final int TODAY_DAY = 19;
    private static final int[] PRIOR_INCIDENT_DAYS = {12, 15};

    // ---------- VIEWS ----------
    private View btnProfileAvatar;
    private BottomNavigationView bottomNavigation;
    private LinearLayout calendarRows;

    private ImageView star1, star2, star3, star4, star5;
    private TextView tvRatingScoreLabel;
    private EditText etCitizenComment;
    private MaterialButton btnSubmitRating;

    private RelativeLayout rowHistoricalFlood;
    private RelativeLayout rowHistoricalTraffic;

    private ImageView btnCalendarPrev;
    private ImageView btnCalendarNext;

    // ---------- STATE ----------
    private int currentRating = 4;
    private View selectedCalendarCell; // the day cell the citizen last tapped, so it can be un-highlighted

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        initViews();
        buildCalendarGrid();
        registerListeners();
        setupBottomNavigation();
        applyRatingUI();
    }

    // ------------------------------------------------------------------
    // SETUP
    // ------------------------------------------------------------------

    private void initViews() {
        btnProfileAvatar = findViewById(R.id.btnProfileAvatar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        calendarRows = findViewById(R.id.calendarRows);

        btnCalendarPrev = findViewById(R.id.btnCalendarPrev);
        btnCalendarNext = findViewById(R.id.btnCalendarNext);

        star1 = findViewById(R.id.star1);
        star2 = findViewById(R.id.star2);
        star3 = findViewById(R.id.star3);
        star4 = findViewById(R.id.star4);
        star5 = findViewById(R.id.star5);
        tvRatingScoreLabel = findViewById(R.id.tvRatingScoreLabel);
        etCitizenComment = findViewById(R.id.etCitizenComment);
        btnSubmitRating = findViewById(R.id.btnSubmitRating);

        rowHistoricalFlood = findViewById(R.id.rowHistoricalFlood);
        rowHistoricalTraffic = findViewById(R.id.rowHistoricalTraffic);
    }

    private void registerListeners() {
        if (btnProfileAvatar != null) {
            btnProfileAvatar.setOnClickListener(v -> ProfileMenuHelper.showProfileMenu(this, btnProfileAvatar));
        }
        if (btnCalendarPrev != null) {
            btnCalendarPrev.setOnClickListener(v ->
                    Toast.makeText(this, "Loading August 2026 audit log...", Toast.LENGTH_SHORT).show());
        }
        if (btnCalendarNext != null) {
            btnCalendarNext.setOnClickListener(v ->
                    Toast.makeText(this, "Loading October 2026 audit log...", Toast.LENGTH_SHORT).show());
        }

        if (star1 != null) star1.setOnClickListener(this);
        if (star2 != null) star2.setOnClickListener(this);
        if (star3 != null) star3.setOnClickListener(this);
        if (star4 != null) star4.setOnClickListener(this);
        if (star5 != null) star5.setOnClickListener(this);

        if (btnSubmitRating != null) btnSubmitRating.setOnClickListener(this);

        if (rowHistoricalFlood != null) rowHistoricalFlood.setOnClickListener(this);
        if (rowHistoricalTraffic != null) rowHistoricalTraffic.setOnClickListener(this);
    }

    // ------------------------------------------------------------------
    // SECTION: SEPTEMBER 2026 CALENDAR
    // ------------------------------------------------------------------

    private void buildCalendarGrid() {
        if (calendarRows == null) return;

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int row = 0; row < CALENDAR_DAYS.length; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            for (int col = 0; col < CALENDAR_DAYS[row].length; col++) {
                int dayNumber = CALENDAR_DAYS[row][col];
                boolean outOfMonth = OUT_OF_MONTH[row][col];

                View cell = inflater.inflate(R.layout.item_calendar_day, rowLayout, false);
                TextView tvDayNumber = cell.findViewById(R.id.tvDayNumber);
                View dotIndicator = cell.findViewById(R.id.dotIndicator);

                tvDayNumber.setText(String.valueOf(dayNumber));

                boolean isToday = !outOfMonth && dayNumber == TODAY_DAY;
                boolean isPriorIncident = !outOfMonth && containsDay(PRIOR_INCIDENT_DAYS, dayNumber);

                if (outOfMonth) {
                    tvDayNumber.setTextColor(getColor(R.color.slate_300));
                    tvDayNumber.setBackground(null);
                } else if (isToday) {
                    tvDayNumber.setTextColor(getColor(R.color.white));
                    tvDayNumber.setBackground(getDrawable(R.drawable.bg_calendar_day_today));
                } else {
                    tvDayNumber.setTextColor(getColor(R.color.slate_900));
                    tvDayNumber.setBackground(null);
                }

                if (dotIndicator != null) {
                    dotIndicator.setVisibility(isPriorIncident ? View.VISIBLE : View.GONE);
                }

                if (!outOfMonth) {
                    final int selectedDay = dayNumber;
                    final boolean wasToday = isToday;
                    cell.setClickable(true);
                    cell.setFocusable(true);
                    cell.setOnClickListener(v -> onCalendarDaySelected(tvDayNumber, selectedDay, wasToday));
                }

                rowLayout.addView(cell);
            }

            calendarRows.addView(rowLayout);
        }
    }

    private boolean containsDay(int[] days, int day) {
        for (int d : days) {
            if (d == day) return true;
        }
        return false;
    }

    /** Visually marks the tapped date as selected (outlined circle), clearing any previous tap-selection. */
    private void onCalendarDaySelected(TextView tvDayNumber, int day, boolean isToday) {
        // Clear the previous tap-selection outline (unless it was actually "today", whose
        // solid orange fill is a separate, permanent indicator and must be left alone).
        if (selectedCalendarCell != null && selectedCalendarCell != tvDayNumber) {
            TextView previous = (TextView) selectedCalendarCell;
            Object tag = previous.getTag();
            boolean previousWasToday = tag != null && (boolean) tag;
            if (!previousWasToday) {
                previous.setBackground(null);
            }
        }

        if (!isToday) {
            tvDayNumber.setBackground(getDrawable(R.drawable.bg_calendar_day_selected_outline));
        }
        tvDayNumber.setTag(isToday);
        selectedCalendarCell = tvDayNumber;

        Toast.makeText(this, "Selected: September " + day + ", 2026", Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------
    // SECTION: RESPONSE-SPEED RATING
    // ------------------------------------------------------------------

    private void onStarTapped(int stars) {
        currentRating = stars;
        applyRatingUI();
    }

    private void applyRatingUI() {
        ImageView[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] == null) continue;
            boolean filled = (i + 1) <= currentRating;
            stars[i].setImageResource(filled ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        }

        String qualifier;
        if (currentRating >= 5) {
            qualifier = "Excellent Response";
        } else if (currentRating == 4) {
            qualifier = "Very Prompt Response";
        } else if (currentRating == 3) {
            qualifier = "Adequate Response";
        } else if (currentRating == 2) {
            qualifier = "Delayed Response";
        } else {
            qualifier = "Needs Improvement";
        }

        if (tvRatingScoreLabel != null) {
            tvRatingScoreLabel.setText(currentRating + ".0 / 5.0 - " + qualifier);
        }
    }

    private void onSubmitRatingClicked() {
        String comment = etCitizenComment != null ? etCitizenComment.getText().toString().trim() : "";
        String message = "Feedback submitted (" + currentRating + "/5) for Incident #BCD-2026-0891";
        if (!comment.isEmpty()) {
            message += " with comment.";
        } else {
            message += ".";
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        if (etCitizenComment != null) {
            etCitizenComment.setText("");
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
                    Intent homeIntent = new Intent(TrackActivity.this, HomeDashboardActivity.class);
                    // Leaving Track for another tab: finish Track and reuse the existing
                    // Home instance via CLEAR_TOP, matching the pattern already used by
                    // ReportActivity's nav_home handler so the back stack never grows
                    // beyond [Home, <current secondary screen>].
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(homeIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_report) {
                    Intent reportIntent = new Intent(TrackActivity.this, ReportActivity.class);
                    reportIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(reportIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_track) {
                    // Already on the Track screen.
                    return true;
                } else if (itemId == R.id.nav_directory) {
                    Intent directoryIntent = new Intent(TrackActivity.this, DirectoryActivity.class);
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
        if (bottomNavigation != null && bottomNavigation.getSelectedItemId() != R.id.nav_track) {
            bottomNavigation.setSelectedItemId(R.id.nav_track);
        }
    }

    // ------------------------------------------------------------------
    // CLICK ROUTING
    // ------------------------------------------------------------------

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if (viewId == R.id.star1) {
            onStarTapped(1);
        } else if (viewId == R.id.star2) {
            onStarTapped(2);
        } else if (viewId == R.id.star3) {
            onStarTapped(3);
        } else if (viewId == R.id.star4) {
            onStarTapped(4);
        } else if (viewId == R.id.star5) {
            onStarTapped(5);
        } else if (viewId == R.id.btnSubmitRating) {
            onSubmitRatingClicked();
        } else if (viewId == R.id.rowHistoricalFlood) {
            Toast.makeText(this, "Incident #BCD-2026-0884 · Urban Flood (Archived)", Toast.LENGTH_SHORT).show();
        } else if (viewId == R.id.rowHistoricalTraffic) {
            Toast.makeText(this, "Incident #BCD-2026-0879 · Traffic Collision (Archived)", Toast.LENGTH_SHORT).show();
        }
    }
}
