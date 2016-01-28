package io.powerinside.scrollsocket;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    public static final String
		KEY_PREF_HOST = "host_preference",
        KEY_KEEP_DISPLAY_ACTIVE = "keep_display_active_preference",
        KEY_INVERT_SCROLL = "invert_scroll_preference";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_settings);
    }

}
