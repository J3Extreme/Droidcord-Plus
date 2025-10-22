package leap.droidcord;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import leap.droidcord.ui.MessageListAdapter;

public class ChatActivity extends Activity {
    // Unused variables `page`, `before`, `after` can be removed if not needed elsewhere.
    int page;
    long before;
    long after;

    private State s;
    private ListView mMessagesView;
    private EditText mMsgComposer;
    private Button mMsgSend;
    private Button mRefreshButton; // Button for refreshing messages

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The progress bar feature is deprecated. Consider using a ProgressBar view in your layout.
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_chat);

        s = MainActivity.s;
        s.channelIsOpen = true;

        // Initialize views
        mMessagesView = (ListView) findViewById(R.id.messages);
        mMsgComposer = (EditText) findViewById(R.id.msg_composer);
        mMsgSend = (Button) findViewById(R.id.msg_send);
        mRefreshButton = (Button) findViewById(R.id.refresh_button); // Initialize refresh button

        // The original code had `s.messagesView = ...`. It's better to keep view references within the Activity.
        // If other classes need to update this view, they should do so through a method in this Activity.

        setupUI();
        setupClickListeners();

        // Fetch initial messages
        fetchMessages();
    }

    /**
     * Sets up the title and hint text for the activity.
     */
    private void setupUI() {
        if (s.isDM) {
            String dmRecipient = "@" + s.selectedDm.toString();
            setTitle(dmRecipient);
            mMsgComposer.setHint(getResources().getString(R.string.msg_composer_hint, dmRecipient));
        } else {
            String channelName = s.selectedChannel.toString();
            setTitle(channelName);
            mMsgComposer.setHint(getResources().getString(R.string.msg_composer_hint, channelName));
        }
    }

    /**
     * Configures all click listeners for the activity.
     */
    private void setupClickListeners() {
        mMsgSend.setOnClickListener((View v) -> {
            sendMessage();
        });

        // Set the listener for the new refresh button
        mRefreshButton.setOnClickListener((View v) -> {
            fetchMessages();
        });
    }

    /**
     * Fetches messages from the API and updates the ListView.
     */
    private void fetchMessages() {
        showProgress(true);
        s.api.aFetchMessages(0, 0, () -> {
            // This callback runs on a background thread.
            // Create the adapter here.
            s.messagesAdapter = new MessageListAdapter(this, s, s.messages);

            // Switch to the UI thread to update the view.
            runOnUiThread(() -> {
                mMessagesView.setAdapter(s.messagesAdapter);
                // Scroll to the bottom to show the latest messages
                mMessagesView.setSelection(s.messagesAdapter.getCount() - 1);
                showProgress(false);
            });
        });
    }

    /**
     * Sends the message from the composer.
     */
    private void sendMessage() {
        String messageText = mMsgComposer.getText().toString();
        if (TextUtils.isEmpty(messageText.trim())) {
            // Don't send empty messages
            return;
        }

        try {
            s.sendMessage = messageText;
            s.sendReference = 0;
            s.sendPing = false;
            s.api.aSendMessage(null); // Consider adding a callback to confirm message was sent
            mMsgComposer.setText("");
        } catch (Exception e) {
            s.error("Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showProgress(final boolean show) {
        // This is a deprecated way of showing progress.
        // For modern apps, it's better to add a ProgressBar to your XML layout
        // and toggle its visibility (View.VISIBLE / View.GONE).
        setProgressBarVisibility(show);
        setProgressBarIndeterminate(show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up state to help prevent memory leaks
        s.channelIsOpen = false;
    }
}
