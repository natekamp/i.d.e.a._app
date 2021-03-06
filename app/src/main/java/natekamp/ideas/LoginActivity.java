package natekamp.ideas;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity
{
    //firebase
    private FirebaseAuth mAuth;

    //views
    private Button loginButton, registerButton;
    private EditText userEmail, userPassword;

    //progress dialog
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //firebase
        mAuth = FirebaseAuth.getInstance();

        //views
        loginButton = (Button) findViewById(R.id.login_login);
        registerButton = (Button) findViewById(R.id.login_register);
        userEmail = (EditText) findViewById(R.id.login_email);
        userPassword = (EditText) findViewById(R.id.login_password);

        //progress dialog
        loadingBar = new ProgressDialog(this);


        registerButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendToRegisterActivity();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                loginToUserAccount();
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) sendToMainActivity();
    }

    private void loginToUserAccount()
    {
        String email = userEmail.getText().toString();
        String password = userPassword.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password))
            Toast.makeText(this, this.getString(R.string.error_field_msg), Toast.LENGTH_SHORT).show();
        else
        {
            loadingBar.setTitle(LoginActivity.this.getString(R.string.progress_title));
            loadingBar.setMessage(LoginActivity.this.getString(R.string.progress_login_msg));
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            String resultMsg = task.isSuccessful() ?
                                    LoginActivity.this.getString(R.string.success_login_msg) :
                                    "Error: " + task.getException().getMessage();

                            loadingBar.dismiss();
                            Toast.makeText(LoginActivity.this, resultMsg, Toast.LENGTH_SHORT).show();

                            if (task.isSuccessful()) sendToMainActivity();
                        }
                    });
        }
    }

    private void sendToMainActivity()
    {
        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void sendToRegisterActivity()
    {
        Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(registerIntent);
    }
}
