package com.qsp.player;
/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.text.*;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.*;
import android.view.View;
import android.widget.TextView;

public class
QspLinkMovementMethod
extends ScrollingMovementMethod
{
    private static final int CLICK = 1;
    private static final int UP = 2;
    private static final int DOWN = 3;

    @Override
    public boolean onKeyDown(TextView widget, Spannable buffer,
                             int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER:
            if (event.getRepeatCount() == 0) {
                if (action(CLICK, widget, buffer)) {
                    return true;
                }
            }
        }

        return super.onKeyDown(widget, buffer, keyCode, event);
    }

    @Override
    protected boolean up(TextView widget, Spannable buffer) {
        if (action(UP, widget, buffer)) {
            return true;
        }

        return super.up(widget, buffer);
    }
        
    @Override
    protected boolean down(TextView widget, Spannable buffer) {
        if (action(DOWN, widget, buffer)) {
            return true;
        }

        return super.down(widget, buffer);
    }

    @Override
    protected boolean left(TextView widget, Spannable buffer) {
        if (action(UP, widget, buffer)) {
            return true;
        }

        return super.left(widget, buffer);
    }

    @Override
    protected boolean right(TextView widget, Spannable buffer) {
        if (action(DOWN, widget, buffer)) {
            return true;
        }

        return super.right(widget, buffer);
    }

    private boolean action(int what, TextView widget, Spannable buffer) {
        Layout layout = widget.getLayout();

        int padding = widget.getTotalPaddingTop() +
                      widget.getTotalPaddingBottom();
        int areatop = widget.getScrollY();
        int areabot = areatop + widget.getHeight() - padding;

        int linetop = layout.getLineForVertical(areatop);
        int linebot = layout.getLineForVertical(areabot);

        int first = layout.getLineStart(linetop);
        int last = layout.getLineEnd(linebot);

        ClickableSpan[] candidates = buffer.getSpans(first, last, ClickableSpan.class);

        int a = Selection.getSelectionStart(buffer);
        int b = Selection.getSelectionEnd(buffer);

        int selStart = Math.min(a, b);
        int selEnd = Math.max(a, b);

        if (selStart < 0) {
            if (buffer.getSpanStart(FROM_BELOW) >= 0) {
                selStart = selEnd = buffer.length();
            }
        }

        if (selStart > last)
            selStart = selEnd = Integer.MAX_VALUE;
        if (selEnd < first)
            selStart = selEnd = -1;

        switch (what) {
        case CLICK:
            if (selStart == selEnd) {
                return false;
            }

            URLSpan[] link = buffer.getSpans(selStart, selEnd, URLSpan.class);

            if (link.length != 1)
                return false;

            urlCatcher.OnUrlClicked(link[0].getURL());
            break;

        case UP:
            int beststart, bestend;

            beststart = -1;
            bestend = -1;

            for (int i = 0; i < candidates.length; i++) {
                int end = buffer.getSpanEnd(candidates[i]);

                if (end < selEnd || selStart == selEnd) {
                    if (end > bestend) {
                        beststart = buffer.getSpanStart(candidates[i]);
                        bestend = end;
                    }
                }
            }

            if (beststart >= 0) {
                Selection.setSelection(buffer, bestend, beststart);
                return true;
            }

            break;

        case DOWN:
            beststart = Integer.MAX_VALUE;
            bestend = Integer.MAX_VALUE;

            for (int i = 0; i < candidates.length; i++) {
                int start = buffer.getSpanStart(candidates[i]);

                if (start > selStart || selStart == selEnd) {
                    if (start < beststart) {
                        beststart = start;
                        bestend = buffer.getSpanEnd(candidates[i]);
                    }
                }
            }

            if (bestend < Integer.MAX_VALUE) {
                Selection.setSelection(buffer, beststart, bestend);
                return true;
            }

            break;
        }

        return false;
    }

    public boolean onKeyUp(TextView widget, Spannable buffer,
                           int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer,
                                MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            
            //Если не попали точно в символ, относящийся к ссылке, проверяем 8 соседних 
            int[] dy = {0, -1, 1, 0, 0, -1, 1, 1, -1};            
            int[] dx = {0, 0, 0, -1, 1, -1, 1, -1, 1};
            int line;
            int off;

            URLSpan[] link = null;
            ImageSpan[] images = null;
            for (int i=0; i<9; i++)
            {
                line = layout.getLineForVertical(y) + dy[i];
                if ((line < 0) || (line > layout.getLineCount() - 1))
                    continue;
                off = layout.getOffsetForHorizontal(line, x) + dx[i];
                link = buffer.getSpans(off, off, URLSpan.class);
                if (link.length != 0) break;
                images = buffer.getSpans(off, off, ImageSpan.class);
                if (images.length != 0) break;
            }
            
            if (link.length != 0) {
                if (action == MotionEvent.ACTION_UP) {
                    urlCatcher.OnUrlClicked(link[0].getURL());
                } else if (action == MotionEvent.ACTION_DOWN) {
                    Selection.setSelection(buffer,
                                           buffer.getSpanStart(link[0]),
                                           buffer.getSpanEnd(link[0]));
                }

                return true;
            } else if (images.length != 0) {
                if (action == MotionEvent.ACTION_UP) {
                    urlCatcher.OnImageClicked(images[0].getSource());
                } else if (action == MotionEvent.ACTION_DOWN) {
                    Selection.setSelection(buffer,
                                           buffer.getSpanStart(images[0]),
                                           buffer.getSpanEnd(images[0]));
                }

                return true;
            } else {
                Selection.removeSelection(buffer);
            }
        }

        return super.onTouchEvent(widget, buffer, event);
    }

    public void initialize(TextView widget, Spannable text) {
        Selection.removeSelection(text);
        text.removeSpan(FROM_BELOW);
    }

    public void onTakeFocus(TextView view, Spannable text, int dir) {
        Selection.removeSelection(text);

        if ((dir & View.FOCUS_BACKWARD) != 0) {
            text.setSpan(FROM_BELOW, 0, 0, Spannable.SPAN_POINT_POINT);
        } else {
            text.removeSpan(FROM_BELOW);
        }
    }

    public static MovementMethod getInstance() {
        if (sInstance == null)
            sInstance = new QspLinkMovementMethod();

        return sInstance;
    }
    
    public static QspLinkMovementMethod getQspInstance() {
        if (sInstance == null)
            sInstance = new QspLinkMovementMethod();

        return sInstance;
    }
    
    public void setCatcher(UrlClickCatcher catcher)
    {
        urlCatcher = catcher;
    }

    private static QspLinkMovementMethod sInstance;
    private static Object FROM_BELOW = new Object();
    
    private UrlClickCatcher urlCatcher;
}
