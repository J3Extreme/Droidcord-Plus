package leap.droidcord.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import leap.droidcord.R;
import leap.droidcord.State;
import leap.droidcord.data.Messages;
import leap.droidcord.model.Attachment;
import leap.droidcord.model.Message;

public class MessageListAdapter extends BaseAdapter {

    private Context context;
    private State s;
    private Messages messages;
    private Drawable defaultAvatar;
    private int iconSize;
    private int replyIconSize;

    private Map<String, Bitmap> emojiCache = new HashMap<>();
    private String[] emojiCodes;

    public MessageListAdapter(Context context, State s, Messages messages) {
        this.context = context;
        this.s = s;
        this.messages = messages;
        this.defaultAvatar = context.getResources().getDrawable(R.drawable.ic_launcher);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        iconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, metrics);
        replyIconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, metrics);

        loadEmojiCodes();
    }

    private void loadEmojiCodes() {
        try {
            InputStream is = context.getAssets().open("emojicodes.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            List<String> codes = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) codes.add(line);
            }
            reader.close();
            emojiCodes = codes.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
            emojiCodes = new String[0];
        }
    }

    @Override
    public int getCount() {
        return messages != null ? messages.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return messages != null ? messages.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private SpannableStringBuilder replaceEmotesWithImages(String text) {
        if (text == null) text = "";
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);

        if (emojiCodes == null) return ssb;

        for (String code : emojiCodes) {
            int index = text.indexOf(code);
            while (index >= 0) {
                Bitmap emojiBitmap = getEmojiBitmap(code.substring(1, code.length() - 1));
                if (emojiBitmap != null) {
                    ImageSpan span = new ImageSpan(context, emojiBitmap);
                    ssb.setSpan(span, index, index + code.length(), SpannableStringBuilder.SPAN_INCLUSIVE_EXCLUSIVE);
                }
                index = text.indexOf(code, index + 1);
            }
        }

        return ssb;
    }

    private Bitmap getEmojiBitmap(String emojiName) {
        if (emojiCache.containsKey(emojiName)) return emojiCache.get(emojiName);

        try {
            InputStream is = context.getAssets().open("emojis/" + emojiName + ".png");
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            emojiCache.put(emojiName, bmp);
            return bmp;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        Message message = (Message) getItem(position);
        if (message == null) return new View(context); // safe fallback

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.message, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (message.isStatus) {
            viewHolder.msg.setVisibility(View.GONE);
            viewHolder.status.setVisibility(View.VISIBLE);

            SpannableStringBuilder sb = new SpannableStringBuilder(
                    message.author != null ? message.author.name + " " + message.content : message.content);
            if (message.author != null) {
                sb.setSpan(new StyleSpan(Typeface.BOLD), 0,
                        message.author.name.length(),
                        SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
            }
            viewHolder.statusText.setText(sb);
            viewHolder.statusTimestamp.setText(message.timestamp != null ? message.timestamp : "");
        } else {
            viewHolder.msg.setVisibility(View.VISIBLE);
            viewHolder.status.setVisibility(View.GONE);

            if (message.author != null)
                s.icons.load(viewHolder.avatar, defaultAvatar, message.author, iconSize);

            s.guildInformation.load(viewHolder.author, message.author);
            viewHolder.timestamp.setText(message.timestamp != null ? message.timestamp : "");

            if (TextUtils.isEmpty(message.content))
                viewHolder.content.setVisibility(View.GONE);
            else
                viewHolder.content.setText(replaceEmotesWithImages(message.content));

            if (message.attachments != null && !message.attachments.isEmpty()) {
                viewHolder.attachments.removeAllViews();
                viewHolder.attachments.setVisibility(View.VISIBLE);
                for (Attachment attachment : message.attachments) {
                    if (attachment.supported) {
                        final ImageView image = new ImageView(context);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                        int bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics);
                        lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, bottomMargin);

                        image.setLayoutParams(lp);
                        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        image.setAdjustViewBounds(true);

                        s.attachments.load(image, defaultAvatar, message, attachment);
                        viewHolder.attachments.addView(image);
                    }
                }
            } else {
                viewHolder.attachments.setVisibility(View.GONE);
            }

            if (!message.showAuthor && message.recipient == null) {
                viewHolder.metadata.setVisibility(View.GONE);
                viewHolder.avatar.getLayoutParams().height = 0;
            } else {
                viewHolder.metadata.setVisibility(View.VISIBLE);
                viewHolder.avatar.getLayoutParams().height = iconSize;
            }

            if (message.recipient != null) {
                viewHolder.reply.setVisibility(View.VISIBLE);
                s.icons.load(viewHolder.replyAvatar, defaultAvatar, message.recipient, replyIconSize);
                s.guildInformation.load(viewHolder.replyAuthor, message.recipient);
                viewHolder.replyContent.setText(replaceEmotesWithImages(message.refContent));
            } else {
                viewHolder.reply.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    private static class ViewHolder {
        View msg;
        View metadata;
        TextView author;
        TextView timestamp;
        TextView content;
        ImageView avatar;
        LinearLayout attachments;

        View reply;
        TextView replyAuthor;
        TextView replyContent;
        ImageView replyAvatar;

        View status;
        TextView statusText;
        TextView statusTimestamp;

        public ViewHolder(View view) {
            msg = view.findViewById(R.id.message);
            metadata = view.findViewById(R.id.msg_metadata);

            author = (TextView) view.findViewById(R.id.msg_author);
            timestamp = (TextView) view.findViewById(R.id.msg_timestamp);
            content = (TextView) view.findViewById(R.id.msg_content);
            avatar = (ImageView) view.findViewById(R.id.msg_avatar);
            attachments = (LinearLayout) view.findViewById(R.id.msg_attachments);

            reply = view.findViewById(R.id.msg_reply);
            replyAuthor = (TextView) view.findViewById(R.id.reply_author);
            replyContent = (TextView) view.findViewById(R.id.reply_content);
            replyAvatar = (ImageView) view.findViewById(R.id.reply_avatar);

            status = view.findViewById(R.id.status);
            statusText = (TextView) view.findViewById(R.id.status_text);
            statusTimestamp = (TextView) view.findViewById(R.id.status_timestamp);
        }
    }
}
