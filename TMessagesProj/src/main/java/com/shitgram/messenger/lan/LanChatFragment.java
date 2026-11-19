/*
 * Shitgram LAN Chat Fragment
 * Reuses Telegram's chat UI components but connects to LAN API.
 */
package com.shitgram.messenger.lan;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;

public class LanChatFragment extends BaseFragment implements LanClient.LanClientListener, ChatActivityEnterView.ChatActivityEnterViewDelegate {

    private RecyclerListView messagesListView;
    private ChatActivityEnterView enterView;
    private MessagesAdapter messagesAdapter;
    private List<LanClient.LanMessage> messages = new ArrayList<>();
    private LanClient lanClient;
    private Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private long lastMessageId = 0;
    private boolean isPolling = false;

    @Nullable
    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        lanClient = LanClient.getInstance(getParentActivity());
        lanClient.setListener(this);

        FrameLayout frameLayout = new FrameLayout(getParentActivity());
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        // Messages list
        messagesListView = new RecyclerListView(getParentActivity()) {
            @Override
            public void requestLayout() {
                if (!isInLayout()) {
                    super.requestLayout();
                }
            }
        };
        messagesListView.setLayoutManager(new LinearLayoutManager(getParentActivity(), LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        });
        messagesListView.setVerticalScrollBarEnabled(false);
        messagesListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        messagesListView.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, android.view.Gravity.TOP | android.view.Gravity.LEFT, 0, 0, 0, AndroidUtilities.dp(56)));
        frameLayout.addView(messagesListView);

        // Enter view
        enterView = new ChatActivityEnterView(getParentActivity(), null, this);
        enterView.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, android.view.Gravity.BOTTOM));
        frameLayout.addView(enterView);

        // Adapter
        messagesAdapter = new MessagesAdapter();
        messagesListView.setAdapter(messagesAdapter);

        // Action bar
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(LocaleController.getString("ShitgramChat", R.string.ShitgramChat));
            actionBar.setSubtitle(LocaleController.getString("LANChatSubtitle", R.string.LANChatSubtitle));
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.createMenu().addItem(1, R.drawable.ic_ab_other).setIsSearchField(false);
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    } else if (id == 1) {
                        showSettingsDialog();
                    }
                }
            });
        }

        // Start polling
        startPolling();

        return frameLayout;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (lanClient != null) {
            lanClient.setListener(this);
            lanClient.fetchMe();
            // Set current user ID on adapter
            String userId = lanClient.getUserId();
            if (userId != null && messagesAdapter != null) {
                messagesAdapter.setCurrentUserId(userId);
            }
        }
        startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPolling();
        if (lanClient != null) {
            lanClient.setListener(null);
        }
    }

    private void startPolling() {
        if (isPolling) return;
        isPolling = true;
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (lanClient != null && lanClient.isLoggedIn()) {
                    lanClient.fetchMessages(lastMessageId);
                }
                if (isPolling) {
                    pollHandler.postDelayed(this, 2000);
                }
            }
        };
        pollHandler.post(pollRunnable);
    }

    private void stopPolling() {
        isPolling = false;
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }

    @Override
    public void onMessagesReceived(List<LanClient.LanMessage> newMessages) {
        if (newMessages.isEmpty()) return;

        boolean wasAtBottom = isAtBottom();
        for (LanClient.LanMessage msg : newMessages) {
            if (msg.id > lastMessageId) {
                lastMessageId = msg.id;
                // Check if message already exists
                boolean exists = false;
                for (LanClient.LanMessage existing : messages) {
                    if (existing.id == msg.id) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    messages.add(msg);
                }
            }
        }
        messagesAdapter.notifyDataSetChanged();
        if (wasAtBottom) {
            scrollToBottom();
        }
    }

    @Override
    public void onMessageSent(LanClient.LanMessage message) {
        // Message will be received via polling, but we can optimistically add it
        boolean exists = false;
        for (LanClient.LanMessage existing : messages) {
            if (existing.id == message.id) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            messages.add(message);
            messagesAdapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
        }
    }

    @Override
    public void onGiftSent(LanClient.LanMessage gift) {
        onMessageSent(gift);
    }

    @Override
    public void onStarsUpdated(int stars) {
        // Update action bar subtitle
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(LocaleController.formatString("StarsBalance", R.string.StarsBalance, stars));
        }
    }

    @Override
    public void onError(String error) {
        AndroidUtilities.runOnUIThread(() -> {
            if (getParentActivity() != null) {
                AndroidUtilities.showToast(error);
            }
        });
    }

    @Override
    public void onUserListReceived(List<LanClient.LanUser> users) {
        // Not used in chat view
    }

    private boolean isAtBottom() {
        if (messagesListView.getLayoutManager() == null || messages.isEmpty()) return true;
        int lastVisible = ((LinearLayoutManager) messagesListView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
        return lastVisible >= messages.size() - 1;
    }

    private void scrollToBottom() {
        if (!messages.isEmpty()) {
            messagesListView.scrollToPosition(messages.size() - 1);
        }
    }

    // ChatActivityEnterViewDelegate methods
    @Override
    public void onMessageSend(String message, boolean silent, int scheduleDate) {
        if (lanClient != null && lanClient.isLoggedIn()) {
            lanClient.sendMessage(message);
        }
    }

    @Override
    public void onMessageSend(long dialogId, String message, boolean silent, int scheduleDate) {
        onMessageSend(message, silent, scheduleDate);
    }

    @Override
    public void onTextChanged(CharSequence text, boolean isEmoji) {
    }

    @Override
    public void onAttachButtonPressed() {
        // Show gift sending dialog
        showGiftDialog();
    }

    @Override
    public void onRecordStart(boolean video) {
    }

    @Override
    public void onRecordStop() {
    }

    @Override
    public void onRecordCancel() {
    }

    @Override
    public void onPreAudioVideoRecord() {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    }

    private void showGiftDialog() {
        if (lanClient == null || !lanClient.isLoggedIn()) return;

        // Fetch users first
        lanClient.fetchUsers();

        // Wait a bit for users to load, then show dialog
        AndroidUtilities.runOnUIThread(() -> {
            // For now, show a simple dialog with predefined gifts
            // In a full implementation, we'd show a user picker
            String[] gifts = {
                    LocaleController.getString("GiftRose", R.string.GiftRose),
                    LocaleController.getString("GiftHeart", R.string.GiftHeart),
                    LocaleController.getString("GiftStar", R.string.GiftStar),
                    LocaleController.getString("GiftCake", R.string.GiftCake)
            };
            int[] costs = {10, 25, 50, 100};

            new android.app.AlertDialog.Builder(getParentActivity())
                    .setTitle(LocaleController.getString("SendGift", R.string.SendGift))
                    .setSingleChoiceItems(gifts, 0, (dialog, which) -> {
                        // Store selected gift index
                        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTag(which);
                    })
                    .setPositiveButton(LocaleController.getString("Send", R.string.Send), (dialog, which) -> {
                        int selectedGift = (int) dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).getTag();
                        if (selectedGift >= 0 && selectedGift < gifts.length) {
                            // For demo, send to first user in chat (or self for testing)
                            // In real implementation, we'd have a user picker
                            lanClient.sendGift(lanClient.getUserName(), gifts[selectedGift], costs[selectedGift]);
                        }
                    })
                    .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                    .show();
        }, 500);
    }

    private static class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {
        private List<LanClient.LanMessage> messages = new ArrayList<>();
        private String currentUserId;

        public void setMessages(List<LanClient.LanMessage> messages) {
            this.messages = messages;
            notifyDataSetChanged();
        }

        public void setCurrentUserId(String userId) {
            this.currentUserId = userId;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LanMessageCell cell = new LanMessageCell(parent.getContext());
            cell.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new ViewHolder(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LanClient.LanMessage message = messages.get(position);
            holder.bind(message);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            LanMessageCell cell;

            ViewHolder(LanMessageCell cell) {
                super(cell);
                this.cell = cell;
            }

            void bind(LanClient.LanMessage message) {
                boolean isOutgoing = message.userId != null && message.userId.equals(currentUserId);
                cell.setMessage(message, isOutgoing);
            }
        }
    }

    private void showSettingsDialog() {
        if (getParentActivity() == null) return;

        LanClient client = LanClient.getInstance(getParentActivity());
        String currentUser = client.getUserName();
        String currentServer = client.getServerUrl();
        int stars = client.getStars();

        new android.app.AlertDialog.Builder(getParentActivity())
                .setTitle(LocaleController.getString("Settings", R.string.Settings))
                .setMessage(LocaleController.formatString("SettingsInfo", R.string.SettingsInfo, currentUser, currentServer, stars))
                .setPositiveButton(LocaleController.getString("Logout", R.string.Logout), (dialog, which) -> {
                    client.logout();
                    // Restart the onboarding flow
                    if (getParentActivity() instanceof org.telegram.ui.LaunchActivity) {
                        org.telegram.ui.LaunchActivity launchActivity = (org.telegram.ui.LaunchActivity) getParentActivity();
                        if (launchActivity.actionBarLayout != null) {
                            launchActivity.actionBarLayout.removeFragment(this);
                            launchActivity.actionBarLayout.addFragmentToStack(launchActivity.getClientNotActivatedFragment());
                        }
                    }
                })
                .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                .show();
    }
}