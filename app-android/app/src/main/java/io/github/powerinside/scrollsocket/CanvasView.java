package io.github.powerinside.scrollsocket;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import io.github.powerinside.scrollsocket.NetEvent.Type;

@SuppressLint("ViewConstructor")
public class CanvasView extends View implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "ScrollSocket.CanvasView";

	private enum InRangeStatus {
		OutOfRange,
		InRange,
		FakeInRange
	}

    final SharedPreferences settings;
    NetworkClient netClient;
	int maxX, maxY;
	InRangeStatus inRangeStatus;
    boolean invert;

    // setup

    public CanvasView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        // view is disabled until a network client is set
        setEnabled(false);

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        settings.registerOnSharedPreferenceChangeListener(this);
        setBackground();
        invert = settings.getBoolean(SettingsActivity.KEY_INVERT_SCROLL, false);
		inRangeStatus = InRangeStatus.OutOfRange;
    }

    public void setNetworkClient(NetworkClient networkClient) {
        netClient = networkClient;
        setEnabled(true);
    }


    // settings

    protected void setBackground() {
        setBackgroundColor(Color.BLACK);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch(key) {
            case SettingsActivity.KEY_INVERT_SCROLL: 
                invert = settings.getBoolean(SettingsActivity.KEY_INVERT_SCROLL, true);
                break;
        }
    }
    
    @Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (BuildConfig.DEBUG)
            Log.i(TAG, "Canvas size changed: " + w + "x" + h + " (before: " + oldw + "x" + oldh + ")");
		maxX = w;
		maxY = h;
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (isEnabled()) {
			for (int ptr = 0; ptr < event.getPointerCount(); ptr++) {
                short nx = normalizeX(event.getX(ptr)),
                        ny = normalizeY(event.getY(ptr));
                Log.v(TAG, String.format("Generic motion event logged: %f|%f", event.getX(ptr), event.getY(ptr)));
                switch (event.getActionMasked()) {
                case MotionEvent.ACTION_HOVER_MOVE:
                    netClient.getQueue().add(new NetEvent(Type.TYPE_MOTION, nx, ny));
                    break;
                case MotionEvent.ACTION_HOVER_ENTER:
                    inRangeStatus = InRangeStatus.InRange;
                    netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, -1, true));
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    inRangeStatus = InRangeStatus.OutOfRange;
                    netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, -1, false));
                    break;
                }
            }
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		if (isEnabled()) {
			for (int ptr = 0; ptr < event.getPointerCount(); ptr++) {
                short nx = normalizeX(event.getX(ptr)),
                      ny = normalizeY(event.getY(ptr));
                Log.v(TAG, String.format("Touch event logged: action %d @ %d|%d", event.getActionMasked(), nx, ny));
                switch (event.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    netClient.getQueue().add(new NetEvent(Type.TYPE_MOTION, nx, ny));
                    break;
                case MotionEvent.ACTION_DOWN:
                    if (inRangeStatus == inRangeStatus.OutOfRange) {
                        inRangeStatus = inRangeStatus.FakeInRange;
                        netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, -1, true));
                    }
                    netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, 0, true));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, 0, false));
                    if (inRangeStatus == inRangeStatus.FakeInRange) {
                        inRangeStatus = inRangeStatus.OutOfRange;
                        netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, -1, false));
                    }
                    break;
                }
            }
			return true;
		}
		return false;
	}
	
	// these overflow and wrap around to negative short values, but thankfully Java will continue
	// on regardless, so we can just ignore Java's interpretation of them and send them anyway.
	short normalizeX(float x) {
		float retval = (Math.min(Math.max(0, x), maxX) * 2*Short.MAX_VALUE/maxX);
        if(!invert) {
            retval = (maxX - retval);
        }
        return (short)retval;
	}
	
	short normalizeY(float y) {
		float retval = (Math.min(Math.max(0, y), maxY) * 2*Short.MAX_VALUE/maxY);
        Log.v(TAG, String.format("Invert is.. %b", invert));
        if(!invert) {
            retval = (maxY - retval);
        }
        return (short)retval;
	}

}
