package com.example.agaplinkproject;

// IMPORT NECESSARY ANDROID UI WIDGETS AND BUNDLE FOR ACTIVITY LIFECYCLE
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

// IMPORT ANDROIDX CLASSES FOR MODERN EDGE-TO-EDGE DISPLAY SUPPORT
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// IMPORT TRANSFORMATION METHODS FOR PASSWORD MASKING/UNMASKING
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;

// IMPORT IMAGEVIEW FOR TOGGLE ICON AND TRANSITION CLASSES FOR SMOOTH UI ANIMATIONS
import android.widget.ImageView;
import android.transition.AutoTransition;
import android.transition.TransitionManager;

// IMPORT REGEX PATTERN CLASS FOR VALIDATING USER INPUT
import java.util.regex.Pattern;

// MAIN ACTIVITY CLASS EXTENDING APPCOMPATACTIVITY FOR BACKWARD COMPATIBILITY
public class MainActivity extends AppCompatActivity {

    // HARDCODED VALID CREDENTIALS FOR MOCKING THE LOGIN PROCESS
    private static final String VALID_NAME = "admin";
    private static final String VALID_PASSWORD = "admin123";

    // REGEX PATTERN: ENSURES FIRST AND LAST NAME USING LETTERS, ALLOWING SPACES, DOTS, APOSTROPHES, OR DASHES
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^\\p{L}[\\p{L}.'-]*(\\s+\\p{L}[\\p{L}.'-]*)+$");

    // REGEX PATTERN: MATCHES PHILIPPINE MOBILE NUMBER FORMATS (STARTS WITH 09 OR +639 FOLLOWED BY 9 DIGITS)
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(09|\\+639)\\d{9}$");

    // REGEX PATTERN: REQUIRES AT LEAST 8 CHARACTERS, 1 UPPERCASE LETTER, AND 1 NUMBER
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{8,}$");

    // ---------- VIEWS ----------
    // DECLARE UI COMPONENTS FOR TABS, FORMS, AND INPUT FIELDS
    private TextView tabSignIn, tabCreate;
    private View registerForm, loginForm;
    private EditText loginName, loginPassword;
    private EditText regName, regPhone, regPassword;
    private Spinner regBarangay;
    private CheckBox regCheck;

    // ONCREATE METHOD: THE ENTRY POINT OF THE ACTIVITY'S LIFECYCLE
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ENABLE EDGE-TO-EDGE DISPLAY, DRAWING BEHIND SYSTEM BARS
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // APPLY WINDOW INSETS TO PREVENT UI FROM OVERLAPPING WITH THE STATUS BAR OR NAVIGATION BAR
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // FIND AND BIND TOGGLE BUTTONS AND FORM CONTAINERS FROM XML
        tabSignIn = findViewById(R.id.tabSignIn);
        tabCreate = findViewById(R.id.tabCreate);
        registerForm = findViewById(R.id.registerForm);
        loginForm = findViewById(R.id.loginForm);

        // FIND AND BIND LOGIN INPUT FIELDS
        loginName = findViewById(R.id.loginName);
        loginPassword = findViewById(R.id.loginPassword);

        // FIND AND BIND REGISTRATION INPUT FIELDS AND CHECKBOX
        regName = findViewById(R.id.regName);
        regBarangay = findViewById(R.id.regBarangay);
        regPhone = findViewById(R.id.regPhone);
        regPassword = findViewById(R.id.regPassword);
        regCheck = findViewById(R.id.regCheck);

        // SET CLICK LISTENERS ON TABS AND LINKS TO TOGGLE BETWEEN LOGIN AND REGISTER FORMS
        tabSignIn.setOnClickListener(v -> showForm(false));
        tabCreate.setOnClickListener(v -> showForm(true));
        findViewById(R.id.linkLogIn).setOnClickListener(v -> showForm(false));
        findViewById(R.id.linkRegister).setOnClickListener(v -> showForm(true));

        // SET CLICK LISTENERS ON SUBMIT BUTTONS TO TRIGGER VALIDATION AND AUTHENTICATION LOGIC
        findViewById(R.id.btnRegister).setOnClickListener(v -> attemptRegister());
        findViewById(R.id.btnLogin).setOnClickListener(v -> attemptLogin());

        // ALLOW CLICKING THE WRAPPER BOX TO OPEN THE SPINNER DROPDOWN
        findViewById(R.id.barangayBox).setOnClickListener(v -> regBarangay.performClick());

        // INITIALIZE UI BY SHOWING THE REGISTER FORM FIRST
        showForm(true);

        // SET UP PASSWORD VISIBILITY TOGGLE FUNCTIONALITY FOR BOTH PASSWORD FIELDS
        setupPasswordToggle(regPassword, findViewById(R.id.regPasswordToggle));
        setupPasswordToggle(loginPassword, findViewById(R.id.loginPasswordToggle));

    }

    // HELPER METHOD TO TOGGLE PASSWORD VISIBILITY (MASK/UNMASK TEXT)
    private void setupPasswordToggle(EditText field, ImageView toggle) {
        // START WITH PASSWORD HIDDEN (MASKED AS DOTS)
        field.setTransformationMethod(PasswordTransformationMethod.getInstance());
        toggle.setImageResource(R.drawable.ic_visibility_off);

        // SET CLICK LISTENER ON THE EYE ICON TO TOGGLE STATE
        toggle.setOnClickListener(v -> {
            // CHECK IF THE CURRENT STATE IS HIDDEN
            boolean hidden = field.getTransformationMethod() instanceof PasswordTransformationMethod;

            // SAVE CURRENT CURSOR POSITION TO MAINTAIN IT AFTER TRANSFORMATION
            int cursor = field.getSelectionEnd();

            // IF HIDDEN, SWITCH TO PLAIN TEXT; ELSE, SWITCH BACK TO HIDDEN
            if (hidden) {
                field.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                toggle.setImageResource(R.drawable.ic_visibility);
            } else {
                field.setTransformationMethod(PasswordTransformationMethod.getInstance());
                toggle.setImageResource(R.drawable.ic_visibility_off);
            }
            // RESTORE THE CURSOR POSITION
            field.setSelection(cursor);
        });
    }

    // HELPER METHOD TO ANIMATE AND SWITCH BETWEEN REGISTER AND LOGIN FORMS
    private void showForm(boolean register) {
        // CONFIGURE AN AUTO-TRANSITION ANIMATION WITH A 125MS DURATION
        AutoTransition transition = new AutoTransition();
        transition.setDuration(125);

        // APPLY THE TRANSITION TO THE FORM CONTAINER BEFORE CHANGING VISIBILITY
        TransitionManager.beginDelayedTransition(findViewById(R.id.formContainer), transition);

        // UPDATE TAB SELECTION STATES
        tabCreate.setSelected(register);
        tabSignIn.setSelected(!register);

        // TOGGLE VISIBILITY OF THE UNDERLYING FORMS BASED ON THE BOOLEAN FLAG
        registerForm.setVisibility(register ? View.VISIBLE : View.GONE);
        loginForm.setVisibility(register ? View.GONE : View.VISIBLE);
    }

    // HELPER METHOD TO VALIDATE INPUTS AND PROCESS REGISTRATION
    private void attemptRegister() {
        // RETRIEVE AND TRIM TEXT FROM INPUT FIELDS
        String name = regName.getText().toString().trim();

        // CLEAN THE PHONE NUMBER BY REMOVING ALL SPACES AND DASHES USING REGEX REPLACEMENT
        String phone = regPhone.getText().toString().replaceAll("[\\s-]", "");
        String password = regPassword.getText().toString();

        // RESET ANY PREVIOUS ERROR MESSAGES
        regName.setError(null);
        regPhone.setError(null);
        regPassword.setError(null);

        // KEEP TRACK OF THE FIRST INVALID VIEW TO REQUEST FOCUS LATER
        View firstInvalid = null;

        // VALIDATE FULL NAME AGAINST REGEX PATTERN
        if (!NAME_PATTERN.matcher(name).matches()) {
            regName.setError("Enter your first and last name (letters only)");
            firstInvalid = regName;
        }

        // VALIDATE PHONE NUMBER AGAINST REGEX PATTERN
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            regPhone.setError("Use 09XXXXXXXXX or +639XXXXXXXXX");
            if (firstInvalid == null) firstInvalid = regPhone;
        }

        // VALIDATE PASSWORD COMPLEXITY AGAINST REGEX PATTERN
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            regPassword.setError("At least 8 characters, 1 number, 1 uppercase letter");
            if (firstInvalid == null) firstInvalid = regPassword;
        }

        // IF ANY FIELD IS INVALID, BRING FOCUS TO IT AND ABORT REGISTRATION
        if (firstInvalid != null) {
            firstInvalid.requestFocus();
            return;
        }

        // VERIFY THAT THE USER HAS CHECKED THE REQUIRED RESIDENCY CHECKBOX
        if (!regCheck.isChecked()) {
            Toast.makeText(this, "Please Verify if you are a resident in Bacolod", Toast.LENGTH_SHORT).show();
            return;
        }

        // RETRIEVE THE SELECTED BARANGAY FROM THE SPINNER
        String barangay = regBarangay.getSelectedItem().toString();

        // SHOW A SUCCESS MESSAGE USING A TOAST
        Toast.makeText(this,
                "Registered: " + name + " (" + barangay + ")",
                Toast.LENGTH_LONG).show();

        // CLEAR ALL REGISTRATION FIELDS AFTER A SUCCESSFUL PROCESS
        regName.setText("");
        regPhone.setText("");
        regPassword.setText("");
        regCheck.setChecked(false);

        // SWITCH TO THE LOGIN FORM
        showForm(false);
    }

    // HELPER METHOD TO PROCESS USER LOGIN
    // HELPER METHOD TO PROCESS USER LOGIN
    private void attemptLogin() {
        // RETRIEVE AND TRIM TEXT FROM INPUT FIELDS
        String name = loginName.getText().toString().trim();
        String password = loginPassword.getText().toString();

        // RESET ANY PREVIOUS ERROR MESSAGES
        loginName.setError(null);
        loginPassword.setError(null);

        // ENSURE THE NAME FIELD IS NOT EMPTY
        if (name.isEmpty()) {
            loginName.setError("Enter your full name");
            loginName.requestFocus();
            return;
        }

        // ENSURE THE PASSWORD FIELD IS NOT EMPTY
        if (password.isEmpty()) {
            loginPassword.setError("Enter your password");
            loginPassword.requestFocus();
            return;
        }

        // CHECK INPUT AGAINST HARDCODED MOCK CREDENTIALS
        if (name.equalsIgnoreCase(VALID_NAME) && password.equals(VALID_PASSWORD)){
            Toast.makeText(this, "Welcome, " + VALID_NAME + "!", Toast.LENGTH_SHORT).show();

            // HAND OFF TO THE DEDICATED HomeDashboardActivity INSTEAD OF SWAPPING THIS
            // ACTIVITY'S CONTENT VIEW IN PLACE. setContentView() HERE WOULD DESTROY AND
            // REPLACE THE ENTIRE VIEW HIERARCHY (INCLUDING ANY BottomNavigationView AND
            // ITS LISTENER) EVERY TIME THE SCREEN CHANGED, WHICH IS WHY BOTTOM NAV
            // BECAME DISCONNECTED AFTER LEAVING THE DASHBOARD. HomeDashboardActivity AND
            // ReportActivity EACH OWN AND WIRE UP THEIR OWN BOTTOM NAVIGATION IN THEIR
            // OWN onCreate(), SO NAVIGATING BETWEEN THEM VIA startActivity() KEEPS THE
            // BOTTOM NAV FUNCTIONAL AND CORRECTLY SELECTED ON EVERY SCREEN.
            startActivity(new Intent(MainActivity.this, HomeDashboardActivity.class));
            finish();
        } else {
            // FAILED LOGIN: DISPLAY ERROR MESSAGE
            Toast.makeText(this, "Incorrect full name or password", Toast.LENGTH_SHORT).show();
        }
    }
}