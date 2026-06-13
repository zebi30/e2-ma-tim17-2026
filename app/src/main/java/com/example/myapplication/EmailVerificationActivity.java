package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.UserRepository;
import com.example.myapplication.util.EmailSender;

public class EmailVerificationActivity extends AppCompatActivity {

    private UserRepository userRepository;
    private EditText identifierInput;
    private EditText codeInput;
    private TextView statusText;
    private TextView emailText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        userRepository = new UserRepository(this);

        identifierInput = findViewById(R.id
                .identifier_input);
        codeInput = findViewById(R.id.code_input);
        statusText = findViewById(R.id.status_text);
        emailText = findViewById(R.id.email_text);

        Button sendEmailButton = findViewById(R.id.send_email_button);
        Button verifyButton = findViewById(R.id.continue_button);
        TextView resendLink = findViewById(R.id.resend_link);
        TextView backButton = findViewById(R.id.back_button);

        String identifier = getIntent().getStringExtra("identifier");
        String email = getIntent().getStringExtra("email");
        if (identifier != null && !identifier.isEmpty()) {
            identifierInput.setText(identifier);
        }
        if (email != null && !email.isEmpty()) {
            emailText.setText(email);
        }

        sendEmailButton.setOnClickListener(v -> sendVerificationEmail());
        resendLink.setOnClickListener(v -> sendVerificationEmail());
        verifyButton.setOnClickListener(v -> verifyCode());
        backButton.setOnClickListener(v -> goToLogin());

        // Auto-send verification email when arriving from registration
        if (identifier != null && !identifier.isEmpty()) {
            sendVerificationEmail();
        }
    }

    private void sendVerificationEmail() {
        String identifier = textOf(identifierInput);
        if (identifier.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        String recipientEmail = userRepository.getEmailForIdentifier(identifier);
        String code = userRepository.getVerificationCode(identifier);
        if (recipientEmail == null || code == null) {
            Toast.makeText(this, R.string.verification_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        emailText.setText(recipientEmail);
        statusText.setText(getString(R.string.sending_email));

        Button sendBtn = findViewById(R.id.send_email_button);
        sendBtn.setEnabled(false);

        EmailSender.sendVerificationCode(recipientEmail, identifier, code, new EmailSender.Callback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    statusText.setText(getString(R.string.verification_mail_ready, recipientEmail));
                    sendBtn.setEnabled(true);
                    Toast.makeText(EmailVerificationActivity.this, R.string.email_sent, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    statusText.setText(getString(R.string.email_send_failed));
                    sendBtn.setEnabled(true);
                    Toast.makeText(EmailVerificationActivity.this, R.string.email_send_failed, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void verifyCode() {
        String identifier = textOf(identifierInput);
        String code = textOf(codeInput);
        if (identifier.isEmpty() || code.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (userRepository.verifyEmail(identifier, code)) {
            Toast.makeText(this, R.string.verification_success, Toast.LENGTH_SHORT).show();
            goToLogin();
        } else {
            Toast.makeText(this, R.string.verification_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
