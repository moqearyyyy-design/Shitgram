/*
 * Shitgram LAN Message Cell
 * Simple message bubble that mimics Telegram's style.
 */
package com.shitgram.messenger.lan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class LanMessageCell extends FrameLayout {

    private TextView nameTextView;
    private TextView messageTextView;
    private TextView timeTextView;
    private Paint bubblePaint;
    private RectF bubbleRect = new RectF();
    private StaticLayout messageLayout;
    private boolean isOutgoing;
    private int bubbleColor;
    private int textColor;
    private int nameColor;
    private int timeColor;

    public LanMessageCell(Context context) {
        super(context);
        init(context);
    }

    public LanMessageCell(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setWillNotDraw(false);

        bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        nameTextView = new TextView(context);
        nameTextView.setTextSize(13);
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        nameTextView.setGravity(Gravity.START);
        nameTextView.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.START, 12, 8, 12, 0));
        addView(nameTextView);

        messageTextView = new TextView(context);
        messageTextView.setTextSize(16);
        messageTextView.setGravity(Gravity.START);
        messageTextView.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.START, 12, 28, 12, 0));
        addView(messageTextView);

        timeTextView = new TextView(context);
        timeTextView.setTextSize(11);
        timeTextView.setGravity(Gravity.END);
        timeTextView.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END, 0, 0, 8, 4));
        addView(timeTextView);

        updateColors();
    }

    private void updateColors() {
        bubbleColor = Theme.getColor(isOutgoing ? Theme.key_chat_outBubble : Theme.key_chat_inBubble);
        textColor = Theme.getColor(isOutgoing ? Theme.key_chat_messageTextOut : Theme.key_chat_messageTextIn);
        nameColor = Theme.getColor(isOutgoing ? Theme.key_chat_messageTextOut : Theme.key_chat_messageTextIn);
        timeColor = Theme.getColor(isOutgoing ? Theme.key_chat_messageTextOut : Theme.key_chat_messageTextIn);
        timeColor = (timeColor & 0x00FFFFFF) | 0x80000000; // 50% alpha

        bubblePaint.setColor(bubbleColor);
        nameTextView.setTextColor(nameColor);
        messageTextView.setTextColor(textColor);
        timeTextView.setTextColor(timeColor);
    }

    public void setMessage(LanClient.LanMessage message, boolean isOutgoing) {
        this.isOutgoing = isOutgoing;
        updateColors();

        nameTextView.setText(message.name);
        messageTextView.setText(message.text);

        // Format time
        String time = LocaleController.formatTime(message.sentAt);
        timeTextView.setText(time);

        // Measure message layout for bubble drawing
        TextPaint paint = messageTextView.getPaint();
        int maxWidth = AndroidUtilities.dp(280);
        messageLayout = new StaticLayout(message.text, paint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int maxBubbleWidth = AndroidUtilities.dp(300);
        int contentWidth = Math.min(width - AndroidUtilities.dp(24), maxBubbleWidth);

        // Measure children
        int nameWidthSpec = MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.AT_MOST);
        int nameHeightSpec = MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.AT_MOST);
        nameTextView.measure(nameWidthSpec, nameHeightSpec);

        int msgWidthSpec = MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.AT_MOST);
        int msgHeightSpec = MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(200), MeasureSpec.AT_MOST);
        messageTextView.measure(msgWidthSpec, msgHeightSpec);

        int timeWidthSpec = MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.AT_MOST);
        int timeHeightSpec = MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(16), MeasureSpec.AT_MOST);
        timeTextView.measure(timeWidthSpec, timeHeightSpec);

        int bubbleHeight = nameTextView.getMeasuredHeight() + messageTextView.getMeasuredHeight() + timeTextView.getMeasuredHeight() + AndroidUtilities.dp(24);
        int bubbleWidth = Math.max(
                Math.max(nameTextView.getMeasuredWidth(), messageTextView.getMeasuredWidth()),
                timeTextView.getMeasuredWidth()
        ) + AndroidUtilities.dp(24);

        // Position views based on incoming/outgoing
        int gravity = isOutgoing ? Gravity.END : Gravity.START;
        FrameLayout.LayoutParams nameParams = (FrameLayout.LayoutParams) nameTextView.getLayoutParams();
        nameParams.gravity = Gravity.TOP | gravity;
        nameParams.leftMargin = isOutgoing ? 0 : AndroidUtilities.dp(12);
        nameParams.rightMargin = isOutgoing ? AndroidUtilities.dp(12) : 0;
        nameTextView.setLayoutParams(nameParams);

        FrameLayout.LayoutParams msgParams = (FrameLayout.LayoutParams) messageTextView.getLayoutParams();
        msgParams.gravity = Gravity.TOP | gravity;
        msgParams.leftMargin = isOutgoing ? 0 : AndroidUtilities.dp(12);
        msgParams.rightMargin = isOutgoing ? AndroidUtilities.dp(12) : 0;
        msgParams.topMargin = AndroidUtilities.dp(28);
        messageTextView.setLayoutParams(msgParams);

        FrameLayout.LayoutParams timeParams = (FrameLayout.LayoutParams) timeTextView.getLayoutParams();
        timeParams.gravity = Gravity.BOTTOM | gravity;
        timeParams.leftMargin = isOutgoing ? AndroidUtilities.dp(8) : 0;
        timeParams.rightMargin = isOutgoing ? 0 : AndroidUtilities.dp(8);
        timeParams.bottomMargin = AndroidUtilities.dp(4);
        timeTextView.setLayoutParams(timeParams);

        bubbleRect.set(0, 0, bubbleWidth, bubbleHeight);
        if (isOutgoing) {
            bubbleRect.offset(width - bubbleWidth, 0);
        }

        setMeasuredDimension(width, bubbleHeight + AndroidUtilities.dp(8));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw bubble background with rounded corners
        float radius = AndroidUtilities.dp(16);
        float smallRadius = AndroidUtilities.dp(4);

        if (isOutgoing) {
            // Outgoing: top-left, top-right, bottom-left rounded; bottom-right small
            canvas.drawRoundRect(bubbleRect, radius, radius, bubblePaint);
            // Fix bottom-right corner
            canvas.drawRect(bubbleRect.right - smallRadius, bubbleRect.bottom - smallRadius, bubbleRect.right, bubbleRect.bottom, bubblePaint);
        } else {
            // Incoming: top-left, top-right, bottom-right rounded; bottom-left small
            canvas.drawRoundRect(bubbleRect, radius, radius, bubblePaint);
            // Fix bottom-left corner
            canvas.drawRect(bubbleRect.left, bubbleRect.bottom - smallRadius, bubbleRect.left + smallRadius, bubbleRect.bottom, bubblePaint);
        }
    }
}