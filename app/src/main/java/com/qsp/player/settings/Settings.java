package com.qsp.player.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.R;
import com.qsp.player.Utility;

public class Settings extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private static final int REQUEST_CODE_STORAGE_ACCESS = 1;

    private final Context uiContext = this;

    private SharedPreferences settings;
    private Preference downloadDirPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        settings = PreferenceManager.getDefaultSharedPreferences(uiContext);
        initDownloadDirPreference();
    }

    private void initDownloadDirPreference() {
        downloadDirPreference = findPreference("downDirPath");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            getPreferenceScreen().removePreference(downloadDirPreference);
            return;
        }
        downloadDirPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                getNewDownloadDir();
                return true;
            }
        });
        String summary = settings.getString(
                getString(R.string.key_internal_uri_extsdcard),
                getString(R.string.defDownDirPath));

        downloadDirPreference.setSummary(summary);
    }

    private void getNewDownloadDir() {
        Intent data = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(data, REQUEST_CODE_STORAGE_ACCESS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode != REQUEST_CODE_STORAGE_ACCESS) {
            return;
        }
        if (resultCode == RESULT_OK) {
            Uri uri = resultData.getData();
            if (uri == null) {
                setDownloadDirPreference("");
                return;
            }
            DocumentFile dir = DocumentFile.fromTreeUri(uiContext, uri);
            if (Utility.isValidDownloadDir(dir)) {
                setDownloadDirPreference(uri.toString());
            }
        }
    }

    private void setDownloadDirPreference(String uri) {
        downloadDirPreference.setSummary(uri);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(getString(R.string.key_internal_uri_extsdcard), uri);
        editor.commit();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        preference.setSummary((CharSequence) newValue);
        return true;
    }
}
