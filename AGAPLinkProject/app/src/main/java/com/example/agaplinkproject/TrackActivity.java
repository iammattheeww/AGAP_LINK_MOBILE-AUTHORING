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
    // THE DAY NUMBER TO HIGHLIGHT AS "TODAY" (SOLID ORANGE FILL) AND THE DAYS THAT
    // GET A SMALL DOT INDICATOR BECAUSE AN INCIDENT WAS LOGGED ON THEM.
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

    // BIND EVERY VIEW THIS ACTIVITY NEEDS FROM activity_track.xml: THE HEADER/NAV
    // CONTROLS, THE CALENDAR ROW CONTAINER THE GRID IS BUILT INTO, THE FIVE STAR
    // RATING ICONS AND THEIR LABEL/COMMENT/SUBMIT CONTROLS, AND THE ARCHIVED
    // INCIDENT HISTORY ROWS.
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

    // ATTACH CLICK BEHAVIOR TO EVERY TAPPABLE VIEW ON THE TRACK SCREEN: THE PROFILE
    // AVATAR, THE CALENDAR MONTH ARROWS (MOCKED — SEE TOASTS BELOW), THE FIVE
    // RATING STARS, THE SUBMIT-RATING BUTTON, AND THE ARCHIVED INCIDENT ROWS.
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

    // PROGRAMMATICALLY BUILD THE 5-ROW x 7-COLUMN SEPTEMBER 2026 CALENDAR GRID INSIDE
    // calendarRows, INFLATING ONE item_calendar_day.xml CELL PER DAY FROM THE HARD-CODED
    // CALENDAR_DAYS/OUT_OF_MONTH DATA RATHER THAN USING THE PLATFORM CalendarView WIDGET,
    // SO EACH DAY CELL CAN BE STYLED (TODAY / PRIOR-INCIDENT DOT / OUT-OF-MONTH DIMMING)
    // TO MATCH THE WIREFRAME.
    private void buildCalendarGrid() {
        if (calendarRows == null) return;

        LayoutInflater inflater = LayoutInflater.from(this);

        // BUILD ONE HORIZONTAL LinearLayout PER CALENDAR WEEK ROW.
        for (int row = 0; row < CALENDAR_DAYS.length; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            // INFLATE AND STYLE EACH DAY CELL WITHIN THE CURRENT WEEK ROW.
            for (int col = 0; col < CALENDAR_DAYS[row].length; col++) {
                int dayNumber = CALENDAR_DAYS[row][col];
                boolean outOfMonth = OUT_OF_MONTH[row][col];

                View cell = inflater.inflate(R.layout.item_calendar_day, rowLayout, false);
                TextView tvDayNumber = cell.findViewById(R.id.tvDayNumber);
                View dotIndicator = cell.findViewById(R.id.dotIndicator);

                tvDayNumber.setText(String.valueOf(dayNumber));

                boolean isToday = !outOfMonth && dayNumber == TODAY_DAY;
                boolean isPriorIncident = !outOfMonth && containsDay(PRIOR_INCIDENT_DAYS, dayNumber);

                // COLOR/BACKGROUND THE DAY NUMBER BASED ON WHETHER IT'S OUT-OF-MONTH,
                // TODAY, OR AN ORDINARY IN-MONTH DAY.
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

                // SHOW THE SMALL DOT INDICATOR ONLY ON IN-MONTH DAYS THAT HAD A LOGGED INCIDENT.
                if (dotIndicator != null) {
                    dotIndicator.setVisibility(isPriorIncident ? View.VISIBLE : View.GONE);
                }

                // ONLY IN-MONTH DAYS ARE TAPPABLE; TAPPING ONE VISUALLY SELECTS IT.
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

    // LINEAR SEARCH HELPER: TRUE IF day APPEARS IN THE GIVEN days ARRAY, USED TO
    // CHECK WHETHER A CALENDAR CELL SHOULD SHOW THE PRIOR-INCIDENT DOT.
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

    // RECORD WHICH STAR (1-5) THE CITIZEN TAPPED AS THE NEW RATING AND REFRESH THE UI.
    private void onStarTapped(int stars) {
        currentRating = stars;
        applyRatingUI();
    }

    // REPAINT THE FIVE STAR ICONS (FILLED UP TO currentRating, OUTLINE AFTER THAT)
    // AND UPDATE THE "X.0 / 5.0 - <QUALIFIER>" LABEL TO MATCH THE CURRENT RATING.
    // THIS IS A CUSTOM ROW OF ImageViews RATHER THAN THE PLATFORM RatingBar WIDGET.
    private void applyRatingUI() {
        ImageView[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] == null) continue;
            boolean filled = (i + 1) <= currentRating;
            stars[i].setImageResource(filled ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        }

        // MAP THE NUMERIC RATING TO A HUMAN-READABLE QUALIFIER LABEL.
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

    // BUILD AND SHOW A CONFIRMATION TOAST FOR THE SUBMITTED RATING (AND OPTIONAL
    // COMMENT), THEN CLEAR THE COMMENT FIELD. THIS IS MOCKED/LOCAL, NOT SENT OVER
    // A NETWORK, CONSISTENT WITH THE APP'S OFFLINE-FIRST DESIGN.
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

    // WIRE UP THE BOTTOM NAVIGATION BAR'S FOUR DESTINATIONS (HOME, REPORT, TRACK,
    // DIRECTORY). SINCE THIS ACTIVITY IS THE TRACK SCREEN, THE nav_track BRANCH
    // JUST CONFIRMS THE SELECTION INSTEAD OF STARTING A NEW ACTIVITY.
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
                    // SAME CLEAR_TOP + finish() PATTERN AS THE nav_home BRANCH ABOVE,
                    // JUST TARGETING ReportActivity INSTEAD.
                    Intent reportIntent = new Intent(TrackActivity.this, ReportActivity.class);
                    reportIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(reportIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_track) {
                    // Already on the Track screen.
                    return true;
                } else if (itemId == R.id.nav_directory) {
                    // SAME CLEAR_TOP + finish() PATTERN AS THE nav_home BRANCH ABOVE,
                    // JUST TARGETING DirectoryActivity INSTEAD.
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

    // CENTRAL CLICK ROUTER: DISPATCHES EACH TAPPED VIEW'S ID TO THE HANDLER FOR
    // THAT SPECIFIC TRACK-SCREEN ACTION (A STAR RATING TAP, THE SUBMIT-RATING
    // BUTTON, OR ONE OF THE ARCHIVED INCIDENT HISTORY ROWS).
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
