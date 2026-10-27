package com.gathin.tvhome;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.TextClock;

/**
 * The Home clock. Plain until a colour scheme gives it an hour colour: then the hours take that colour, the
 * separator its own colour if one is set, and the minutes keep the view's own text colour. The format (12 or
 * 24 hours) is TextClock's own.
 */
public class SchemeClock extends TextClock {
    private int hourColor;
    private int separatorColor;

    public SchemeClock(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** 0 turns the two-tone clock off. */
    void setHourColor(int argb) {
        hourColor = argb;
        setText(getText());
    }

    /** The colon's colour; 0 leaves it in the minutes' colour. */
    void setSeparatorColor(int argb) {
        separatorColor = argb;
        setText(getText());
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (hourColor == 0 || text == null || text.length() == 0) {
            super.setText(text, type);
            return;
        }
        String s = text.toString();
        int end = 0;
        while (end < s.length() && Character.isLetterOrDigit(s.charAt(end))) end++;
        SpannableString out = new SpannableString(s);
        if (end > 0) out.setSpan(new ForegroundColorSpan(hourColor), 0, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (separatorColor != 0 && end < s.length()) {
            out.setSpan(new ForegroundColorSpan(separatorColor), end, end + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        super.setText(out, BufferType.SPANNABLE);
    }
}
