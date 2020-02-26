package com.example.hitchhiker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.example.hitchhiker.MapActivity.MY_PERMISSIONS_REQUEST_READ_CONTACTS;

public class MainActivity extends AppCompatActivity {

    private EditText Name;
    private EditText Password;
    private TextView Info;
    private Button Login;
    private int counter = 5;
    private TextView userRegistration;
    public static FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        reqPermissions();
        Name = (EditText)findViewById(R.id.etName);
        Password = (EditText)findViewById(R.id.etPassword);
        //Info = (TextView)findViewById(R.id.tvInfo);
        Login = (Button)findViewById(R.id.btnLogin);
        userRegistration = (TextView) findViewById(R.id.tvRegister);
        //Info.setText("Versuche übrig: 5");
        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);

        FirebaseUser user = firebaseAuth.getCurrentUser();  // Current logged user


        // If a user is logged in, the map Activity starts
        if (user != null){
            finish();
            startActivity(new Intent(MainActivity.this, MapActivity.class));
        }

        // Logs the user, if the validation is successful
        Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(MainActivity.this);
                validate(Name.getText().toString(), Password.getText().toString());
            }
        });

        // Changes to the registration activity
        userRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RegistrationActivity.class));
                finish();
            }
        });
    }

    // moves the Task to the back, prevents the user from going back to the map activity if he was logged out
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    /*@Override
    protected void onResume() {
        super.onResume();
        if (firebaseAuth.getCurrentUser() != null){
            finish();
            startActivity(new Intent(MainActivity.this, MapActivity.class));
        }
    }*/

    //validates the user with a request to the Firebase Authentication DB
    private void validate(String userName, String userPassword){
        userName = userName + "@moco.de"; // The firebase Authentication uses an email to validate the user but the hitchhiker app uses a combination of username and password so the username is concatenated with an @moco String
        if(userName.isEmpty() || userPassword.isEmpty()){
            Toast.makeText(MainActivity.this, getString(R.string.logindatenEingeben), Toast.LENGTH_SHORT).show();
        } else {
            progressDialog.setMessage(getString(R.string.loginDone));
            progressDialog.show();
            firebaseAuth.signInWithEmailAndPassword(userName, userPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, getString(R.string.loginDone), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, MapActivity.class));
                    } else {
                        progressDialog.dismiss();
                        Toast toast = Toast.makeText(MainActivity.this, getString(R.string.loginNotDone), Toast.LENGTH_SHORT);
                        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
                        if( v != null) v.setGravity(Gravity.CENTER);
                        toast.show();
                    }
                }
            });
        }
    }

    //Requests the permission that are needed to use the App
    public void reqPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(this, "Permission already granted.", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                return;
            }
        }
    }

    //handles the reqPermission results
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                } else {
                    // permission denied
                    finish();
                }
                return;
            }
        }
    }

    // Method that hides the Keyboard when called
    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}