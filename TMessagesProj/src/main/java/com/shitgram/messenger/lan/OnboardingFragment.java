/*
 * Shitgram Onboarding Fragment
 * Entry screen for nickname and server IP.
 */
package com.shitgram.messenger.lan;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

public class OnboardingFragment extends BaseFragment {

    private EditTextBoldCursor serverEditText;
    private EditTextBoldCursor nameEditText;
    private Button continueButton;
    private TextView errorTextView;
    private OnboardingCallback callback;

    public interface OnboardingCallback {
        void onComplete(String name, String serverUrl);
    }

    public void setCallback(OnboardingCallback callback) {
        this.callback = callback;
    }

    @Nullable
    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = new View(getParentActivity()) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                int width = getMeasuredWidth();
                int height = getMeasuredHeight();
                if (width > AndroidUtilities.dp(400)) {
                    width = AndroidUtilities.dp(400);
                }
                setMeasuredDimension(width, height);
            }
        };
        view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        view.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Main container
        androidx.core.widget.NestedScrollView scrollView = new androidx.core.widget.NestedScrollView(getParentActivity());
        scrollView.setFillViewport(true);
        scrollView.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        view.addView(scrollView);

        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(getParentActivity());
        linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        linearLayout.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        scrollView.addView(linearLayout);

        // Logo/Title
        TextView titleView = new TextView(getParentActivity());
        titleView.setText("Shitgram");
        titleView.setTextSize(32);
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setGravity(android.view.Gravity.CENTER);
        titleView.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, android.view.Gravity.CENTER_HORIZONTAL, 0, 0, 0, AndroidUtilities.dp(8)));
        linearLayout.addView(titleView);

        TextView subtitleView = new TextView(getParentActivity());
        subtitleView.setText(LocaleController.getString("LocalChatSubtitle", R.string.LocalChatSubtitle));
        subtitleView.setTextSize(16);
        subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitleView.setGravity(android.view.Gravity.CENTER);
        subtitleView.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, android.view.Gravity.CENTER_HORIZONTAL, 0, 0, 0, AndroidUtilities.dp(24)));
        linearLayout.addView(subtitleView);

        // Server URL input
        TextView serverLabel = new TextView(getParentActivity());
        serverLabel.setText(LocaleController.getString("ServerAddress", R.string.ServerAddress));
        serverLabel.setTextSize(14);
        serverLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        serverLabel.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, android.view.Gravity.CENTER_HORIZONTAL, 0, 0, 0, AndroidUtilities.dp(4)));
        linearLayout.addView(serverLabel);

        serverEditText = new EditTextBoldCursor(getParentActivity());
        serverEditText.setHint(LocaleController.getString("ServerAddressHint", R.string.ServerAddressHint));
        serverEditText.setTextSize(16);
        serverEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        serverEditText.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhiteInputField)));
        serverEditText.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        serverEditText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        serverEditText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        serverEditText.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, android.view.Gravity.CENTER_HORIZONTAL, 0, 0, 0, AndroidUtilities.dp(16)));
        linearLayout.addView(serverEditText);

        // Load saved server URL
        LanClient client = LanClient.getInstance(getParentActivity());
        if (client.getServerUrl() != null && !client.getServerUrl().isEmpty()) {
            serverEditText.setText(client.getServerUrl());
        }

        // Nickname input
        TextView nameLabel = new TextView(getParentActivity());
        nameLabel.setText(LocaleController.getString("YourNickname", R.string.YourNickname));
        nameLabel.setTextSize(14);
        nameLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        nameLabel.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, android.view.Gravity.CENTER_HORIZONTAL, 0, 0, 0, AndroidUtilities.dp(4)));
        linearLayout.addView(nameLabel);

        nameEditText = new EditTextBoldCursor(getParentActivity());
        nameEditText.setHint(LocaleController.getString("NicknameHint", R.string.NicknameHint));
        nameEditText.setTextSize(16);
        nameEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        nameEditText.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhiteInputField)));
        nameEditText.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        nameEditText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        nameEditText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        nameEditText.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, android.view.Gravity.CENTER_HORIZONTAL, 0, 0, 0, AndroidUtilities.dp(16)));
        linearLayout.addView(nameEditText);

        // Error text
        errorTextView = new TextView(getParentActivity());
        errorTextView.setTextSize(13);
        errorTextView.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
        errorTextView.setVisibility(View.GONE);
        errorTextView.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, android.view.Gravity.CENTER_HORIZONTAL, 0, 0, 0, AndroidUtilities.dp(16)));
        linearLayout.addView(errorTextView);

        // Continue button
        continueButton = new Button(getParentActivity());
        continueButton.setText(LocaleController.getString("Continue", R.string.Continue));
        continueButton.setTextSize(16);
        continueButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        continueButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_actionBarDefault)));
        continueButton.setTextColor(Theme.getColor(Theme.key_buttonText));
        continueButton.setEnabled(false);
        continueButton.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(48), android.view.Gravity.CENTER_HORIZONTAL, 0, 0, 0, AndroidUtilities.dp(24)));
        linearLayout.addView(continueButton);

        // Text watchers
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateInput();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        serverEditText.addTextChangedListener(watcher);
        nameEditText.addTextChangedListener(watcher);

        continueButton.setOnClickListener(v -> {
            String serverUrl = serverEditText.getText().toString().trim();
            String name = nameEditText.getText().toString().trim();
            if (validateInput()) {
                errorTextView.setVisibility(View.GONE);
                continueButton.setEnabled(false);
                continueButton.setText(LocaleController.getString("Connecting", R.string.Connecting));
                if (callback != null) {
                    callback.onComplete(name, serverUrl);
                }
            }
        });

        validateInput();
        return view;
    }

    private boolean validateInput() {
        String serverUrl = serverEditText.getText().toString().trim();
        String name = nameEditText.getText().toString().trim();
        boolean valid = !serverUrl.isEmpty() && !name.isEmpty() && name.length() >= 2;
        continueButton.setEnabled(valid);
        return valid;
    }

    public void showError(String error) {
        errorTextView.setText(error);
        errorTextView.setVisibility(View.VISIBLE);
        continueButton.setEnabled(true);
        continueButton.setText(LocaleController.getString("Continue", R.string.Continue));
    }

    public void resetButton() {
        continueButton.setEnabled(true);
        continueButton.setText(LocaleController.getString("Continue", R.string.Continue));
    }
}