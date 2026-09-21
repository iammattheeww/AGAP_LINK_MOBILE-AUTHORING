package com.example.agaplinkproject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

/**
 * Shared "profile avatar" tap behavior for every authenticated screen
 * (Home, Report, Track, Directory).
 *
 * Previously, tapping the avatar terminated the session immediately. That is
 * no longer the desired behavior: tapping the avatar now opens a small
 * profile/options PopupMenu, and the user must explicitly choose "Log Out"
 * before the session is cleared. This keeps the interaction consistent
 * across every screen that shows the avatar without duplicating the same
 * PopupMenu/logout code in four separate Activities.
 */
final class ProfileMenuHelper {

    // Matches the session prefs name each Activity already used for its own
    // (now-removed) immediate-logout handler.
    private static final String PREFS_NAME = "AGAP_LINK_SESSION";

    // Built entirely in code (no menu/*.xml needed), so a plain local id is enough.
    private static final int MENU_ITEM_LOG_OUT = 1;

    private ProfileMenuHelper() {
        // Static helper; not instantiable.
    }

    /** Shows the profile/options menu anchored to the tapped avatar view. */
    static void showProfileMenu(Activity activity, View anchor) {
        PopupMenu popupMenu = new PopupMenu(activity, anchor);
        popupMenu.getMenu().add(0, MENU_ITEM_LOG_OUT, 0, "Log Out");
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_ITEM_LOG_OUT) {
                logOut(activity);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    /**
     * Clears the local/mock session, shows a short confirmation, and returns the
     * user to MainActivity with the back stack cleared so the physical Back
     * button cannot return to an authenticated screen after logging out.
     */
    private static void logOut(Activity activity) {
        SharedPreferences preferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().clear().apply();

        Toast.makeText(activity, "Logged out successfully.", Toast.LENGTH_SHORT).show();

        Intent logoutIntent = new Intent(activity, MainActivity.class);
        logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(logoutIntent);
        activity.finish();
    }
}
