package com.qsp.player;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;


public class Settings extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    final private Context uiContext = this;
    final int ACTIVITY_SELECT_DIRECTORY = 531;
    final int REQUEST_CODE_STORAGE_ACCESS = 42;
    SharedPreferences sharedPref = null;
    String SDPath;
    String backPath;
    DocumentFile downloadDir = null;
    ArrayList<File> qspBrowseDir;
    String selectedDirectory;
    boolean usingSDCard;

    public void setFullGamesPath (boolean useDownloadDir, SharedPreferences settings) {
        if (!useDownloadDir) {
            //Get external SD card flag and get directory; set extSDCard false if no external SD card
            boolean extSDCard = settings.getBoolean("storageType", true);
            if (extSDCard) {
                SDPath = System.getenv("SECONDARY_STORAGE");

                //if SECONDARY_STORAGE fails, try EXTERNAL_SDCARD_STORAGE
                if (null == SDPath)
                    SDPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
                else if (SDPath.length() == 0)
                    SDPath = System.getenv("EXTERNAL_SDCARD_STORAGE");

                //if EXTERNAL_SDCARD_STORAGE fails, check all directories in /storage/ for usable path
                if ((null == SDPath) || (SDPath.length() == 0)) {
                    Utility.WriteLog("internal DIR: " + Environment.getExternalStorageDirectory().getAbsolutePath());
                    File internalFileList[] = new File(Environment.getExternalStorageDirectory().getAbsolutePath()).listFiles();

                    File fileList[] = new File("/storage/").listFiles();
                    for (File file : fileList) {
                        Utility.WriteLog("storage DIR: " + file.getAbsolutePath());

                        //A suitable candidate (directory/readable/not internal) is found...
                        if (!file.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()) && file.isDirectory() && file.canRead()) {

                            //Check that it is not the internal directory under another name
                            File emulatedFileList[] = file.listFiles();
                            if (Utility.directoriesAreEqual(internalFileList, emulatedFileList)) continue;

                            //if it is not the internal storage, it must be the external storage
                            SDPath = file.getAbsolutePath();
                            Utility.WriteLog("chosen DIR: " + file.getAbsolutePath());
                            break;
                        }
                    }
                }

                //if that fails, go to the internal directory and set extSDCard as false
                if (null == SDPath) {
                    SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                    extSDCard = false;
                } else if (SDPath.length() == 0) {
                    SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                    extSDCard = false;
                }
            } else
                SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            SDPath += "/";
            Utility.WriteLog("TEMP:" + SDPath);
            //Get the relative games path and merge the two directories
            //Add a trailing "/" to relPath and remove change "//" to "/" if present
            String relPath = settings.getString("relGamePath", getString(R.string.defRelPath));
            if (!relPath.endsWith("/")) relPath += "/";
            if (!relPath.startsWith("/")) relPath = "/" + relPath;
            String fullGamesPath = SDPath + relPath;
            relPath = relPath.replace("//", "/");
            fullGamesPath = fullGamesPath.replace("//", "/");
            fullGamesPath = fullGamesPath.replace("//", "/");

            //Store adjusted storage type, relative path, and complete game directory
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("storageType", extSDCard);
            editor.putString("relGamePath", relPath);
            editor.putString("compGamePath", fullGamesPath);
            editor.commit();

            findPreference("downDirPath").setSummary(sharedPref.getString("downDirPath",getString(R.string.defDownDirPath)));
            Preference filePicker = findPreference("relGamePath");
            if (filePicker != null)
                filePicker.setSummary(sharedPref.getString("relGamePath",getString(R.string.defRelPath)));

            Utility.WriteLog("_NOT using downloadDir DocumentFile_");
            Utility.WriteLog("storageType: " + settings.getBoolean("storageType", true));
            Utility.WriteLog("relGamePath: " + settings.getString("relGamePath", getString(R.string.defRelPath)));
            Utility.WriteLog("compGamePath: " + settings.getString("compGamePath", getString(R.string.defGamePath)));
        }

        //if useDownloadDir is true...
        else {

            //**** START set SDPath/storageType ****
            String newDownDirPath = getString(R.string.defDownDirPath);
            if (downloadDir != null) {
                String tempDirPath = FileUtil.getFullPathFromTreeUri(downloadDir.getUri(),uiContext);
                if (tempDirPath != null) {
                    newDownDirPath = tempDirPath;
                    if (!newDownDirPath.endsWith("/")) newDownDirPath += "/";
                }
                else
                    downloadDir = null;
            }

            String tempDir = FileUtil.getSdCardPath();
            Boolean usingExtSDCard = false;
            if (newDownDirPath.startsWith(tempDir)) {
                Utility.WriteLog(newDownDirPath + " contains " + tempDir);
                SDPath = tempDir;
            }
            else {
                Utility.WriteLog(newDownDirPath+" doesn't contain "+tempDir);

                ArrayList<String> extSDPaths = FileUtil.getExtSdCardPaths(uiContext);
                for (int i=0; i<extSDPaths.size(); i++) {
                    tempDir = extSDPaths.get(i);
                    if (newDownDirPath.startsWith(tempDir)) {
                        Utility.WriteLog(newDownDirPath+" contains "+tempDir);
                        SDPath = tempDir;
                        usingExtSDCard = true;
                        break;
                    }
                    else Utility.WriteLog(newDownDirPath+" doesn't contain "+tempDir);
                }
            }
            //**** END set SDPath/storageType ****

            if (SDPath == null) SDPath = "";

            //set String "relGamePath"; "compGamePath" is newDownDirPath
            String relPath = newDownDirPath.replace(SDPath,"");

            //Store adjusted storage type, relative path, and complete game directory
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("storageType", usingExtSDCard);
            editor.putString("relGamePath", relPath);
            editor.putString("compGamePath", newDownDirPath);
            editor.putString("downDirPath", newDownDirPath);
            editor.commit();

            findPreference("downDirPath").setSummary(sharedPref.getString("downDirPath",getString(R.string.defDownDirPath)));

            Preference filePicker = findPreference("relGamePath");
            if (filePicker != null)
                filePicker.setSummary(sharedPref.getString("relGamePath",getString(R.string.defRelPath)));

            Utility.WriteLog("_Using downloadDir DocumentFile_");
            Utility.WriteLog("storageType: " + settings.getBoolean("storageType", true));
            Utility.WriteLog("relGamePath: " + settings.getString("relGamePath", getString(R.string.defRelPath)));
            Utility.WriteLog("compGamePath: " + settings.getString("compGamePath", getString(R.string.defGamePath)));
            Utility.WriteLog("downDirPath: " + settings.getString("downDirPath", getString(R.string.defDownDirPath)));
        }
    }

    private void setSDCard (SharedPreferences sharedPref) {
        //Get external SD card flag and get directory; set extSDCard false if no external SD card
        boolean extSDCard = sharedPref.getBoolean("storageType",true);
        if (extSDCard) {
            SDPath = System.getenv("SECONDARY_STORAGE");

            //Check by "EXTERNAL_SDCARD_STORAGE"
            if ((null == SDPath) || (SDPath.length() == 0)) {
                SDPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
            }

            //If that fails, check all directories in /storage/ for usable path
            if ((null == SDPath) || (SDPath.length() == 0)) {
                File fileList[] = new File("/storage/").listFiles();
                for (File file : fileList) {
                    Utility.WriteLog("storage DIR: "+file.getAbsolutePath());
                    if (!file.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()) && file.isDirectory() && file.canRead())
                        SDPath = file.getAbsolutePath();
                }
            }

            //If there is still no usable path, don't use external SD Card
            if ((null == SDPath) || (SDPath.length() == 0)) {
                SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                extSDCard = false;
            }
        } else
            SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        SDPath += "/";
    }

    public void setFullGamesPath (SharedPreferences sharedPref) {

        //Get external SD card flag and get directory; set extSDCard false if no external SD card
        setSDCard(sharedPref);
        //Get the relative games path and merge the two directories
        //Add a trailing "/" to relPath and remove change "//" to "/" if present
        String relPath = sharedPref.getString("relGamePath",getString(R.string.defRelPath));
        if (!relPath.endsWith("/")) relPath +="/";
        if (!relPath.startsWith("/")) relPath = "/" + relPath;
        String fullGamesPath = SDPath + relPath;
        relPath = relPath.replace("//","/");
        fullGamesPath = fullGamesPath.replace("//","/");

        //Store adjusted storage type, relative path, and complete game directory
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("compGamePath", fullGamesPath);
        editor.commit();

    }


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(uiContext);

        //Get the downloadDir DocumentFile from Settings if it is available and check that it is a
        //usable DocumentsProvider Uri
        String untestedUri = sharedPref.getString(getString(R.string.key_internal_uri_extsdcard), "");
        if (!untestedUri.isEmpty()) {
            downloadDir = DocumentFile.fromTreeUri(uiContext, Uri.parse(untestedUri));
        }
        else
            downloadDir = null;

        //if downloadDir is null, clear downloadDir from Settings
        if (downloadDir == null) {
            SharedPreferences.Editor ed = sharedPref.edit();
            ed.putString(getString(R.string.key_internal_uri_extsdcard),"");
            ed.apply();
        }

        CheckBoxPreference prefSDCard = (CheckBoxPreference) findPreference("storageType");
        if (prefSDCard != null) {
            prefSDCard.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    setSDCard(sharedPref);
                    setFullGamesPath(true, sharedPref);

                    String newPath = sharedPref.getString("compGamePath", getString(R.string.defGamePath));

                    return true;
                }
            });
        }

        //Directory picker for finding games path - setSummary, prepare for click
        final Preference filePicker = (Preference) findPreference("relGamePath");
        if (filePicker != null) {
            filePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    //Get external SD card flag and get directory; set extSDCard false if no external SD card
                    boolean extSDCard = sharedPref.getBoolean("storageType", true);
                    if (extSDCard) {
                        SDPath = System.getenv("SECONDARY_STORAGE");
                        if ((null == SDPath) || (SDPath.length() == 0)) {
                            SDPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
                        }
                        if ((null == SDPath) || (SDPath.length() == 0)) {
                            SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                            extSDCard = false;
                        }
                    } else
                        SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                    SDPath += "/";

                    String newPath = sharedPref.getString("compGamePath", getString(R.string.defGamePath));

                    Utility.WriteLog("SelectDirectory()");
                    SelectDirectory(newPath, false);
                    return true;
                }
            });
            filePicker.setSummary(sharedPref.getString("relGamePath", getString(R.string.defRelPath)));
        }

        //Writable directory picker for using Storage Access Framework to find a download directory
        final Preference downloadPicker = (Preference) findPreference("downDirPath");
        downloadPicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                //Use Storage Access Framework to get a new download directory
                Utility.WriteLog("getNewDownloadDir()");
                getNewDownloadDir();
                return true;
            }
        });
        downloadPicker.setSummary(sharedPref.getString("downDirPath",getString(R.string.defDownDirPath)));
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        preference.setSummary((CharSequence)newValue);
        return true;
    }

    private void SelectDirectory(String startpath, boolean start)
    {
        Utility.WriteLog("startpath: "+startpath);
        Utility.WriteLog("backPath: "+backPath);
        Utility.WriteLog("SDPath: "+SDPath);

        selectedDirectory = startpath;
        if (!start)
            if (SDPath.equals(startpath))
                start = true;
        if (!start)
        {
            int slash = startpath.lastIndexOf(File.separator, startpath.length() - 2);
            if (slash >= 0)
                backPath = startpath.substring(0, slash + 1);
            else
                start = true;
        }
        if (start)
        {
            backPath = "";
        }

        //Ищем все файлы .qsp и .gam в корне флэшки
        File sdcardRoot = new File (startpath);
        if ((sdcardRoot == null) || !sdcardRoot.exists())
        {
            String failedPath = ("/"+startpath.replace(SDPath,"")).replace("//","/");
            //If at SDPath already and File is null/doesn't exist, ShowError and exit
            if (startpath.equals(SDPath)) {
                Utility.ShowError(uiContext, getString(R.string.pathNotFound).replace("-PATHNAME-", failedPath));
                return;
            }
            //If not at SPath yet, ShowInfo and drop to lowest directory
            else {
                Utility.ShowInfo(uiContext,getString(R.string.pathNotFound).replace("-PATHNAME-", failedPath));
                SelectDirectory(SDPath,true);
                return;
            }
        }
        File[] sdcardFiles = sdcardRoot.listFiles();
        qspBrowseDir = new ArrayList<File>();

        //If sdcardFiles == null, there is no SD Card available!
        if (sdcardFiles == null) {
            return;
        }

        //Сначала добавляем все папки
        for (File currentFile : sdcardFiles)
        {
            if (currentFile.isDirectory() && !currentFile.isHidden() && !currentFile.getName().startsWith("."))
                qspBrowseDir.add(currentFile);
        }

        //Если мы не на самом верхнем уровне, то добавляем ссылку
        int shift = 1;
        if (!start)
            shift++;
        int total = shift;
        if (qspBrowseDir != null) total += qspBrowseDir.size();
        final CharSequence[] items = new String[total];
Utility.WriteLog("startpathX: "+startpath);
        String tempStr;
        if (start)
            tempStr = "/";
        else {
            tempStr = startpath.replace(SDPath,"");
            if (!tempStr.startsWith("/")) tempStr = "/" + tempStr;
        }
        if (!tempStr.endsWith("/")) tempStr += "/";// tempStr.substring(0,tempStr.length()-1);
        items[0] = getString(R.string.selectDir).replace("-DIRNAME-", tempStr);

        if (!start)
            items[1] = "[..]";
        for (int i=shift; i<total; i++)
        {
            File f = qspBrowseDir.get(i - shift);
            String displayName = f.getName();
            if (f.isDirectory())
                displayName = "["+ displayName + "]";
            items[i] = displayName;
        }
        Utility.WriteLog("test1: "+total);

        //Sort both arrays
        CharSequence[] sortedItems = new CharSequence[items.length];
        sortedItems = NameSorter(shift,items);
        qspBrowseDir = Utility.FileSorter(qspBrowseDir);

        for (int i=0; i<qspBrowseDir.size(); i++)
            Utility.WriteLog(items[i+shift]+" = "+qspBrowseDir.get(i).getName());

        //Показываем диалог выбора файла
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.selectDirTitle));
        builder.setItems(sortedItems, browseFolderClick);
        AlertDialog alert = builder.create();
        alert.show();
    }

    //Sort all names except for "1 to shift"
    private CharSequence[] NameSorter (int shift, CharSequence[] names) {
        //Skip if there aren't at least two names to sort
        if (names.length-shift<2) return names;

        if (names.length < 2+shift) return names;
        Arrays.sort(names, shift, names.length, new Comparator<CharSequence>() {
            public int compare(CharSequence name1, CharSequence name2) {
                return name1.toString().toLowerCase().compareTo(name2.toString().toLowerCase());
            }
        });
        return names;
    }

    /*
    //Sort the ArrayList of Files by file name
    public static ArrayList<File> FileSorter (ArrayList<File> files) {
        //Skip if there aren't at least two files
        if (files.size() < 2) return files;

        File[] fileArray = files.toArray(new File[files.size()]);
        Arrays.sort(fileArray, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });

        ArrayList<File> returnedList = new ArrayList<File>();

        for(int i=0; i<fileArray.length; i++)
            returnedList.add(fileArray[i]);

        return returnedList;
    }
    */

    android.content.DialogInterface.OnClickListener browseFolderClick = new DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            if (!((AlertDialog)dialog).isShowing())
                return;
            dialog.dismiss();
            boolean canGoUp = !backPath.equals("");
            int shift = 1;
            if (canGoUp)
                shift++;
            if (which == 0) {

                //Lock the new path in place, but only the relative path - remove the storage root
                String newValue = selectedDirectory;
                newValue = newValue.replace(SDPath,"");
                Utility.WriteLog("newValue: "+newValue);
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(uiContext);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("relGamePath", newValue);
                editor.commit();

                if (!newValue.startsWith("/")) newValue = "/" + newValue;
                if (!newValue.endsWith("/")) newValue += "/";
                Preference pref = findPreference("relGamePath");
                pref.setSummary(newValue);

//                finish();
            }
            else if (which == 1 && canGoUp)
                SelectDirectory(backPath, false);
            else
            {
                File f = qspBrowseDir.get(which - shift);
                SelectDirectory(f.getPath(), false);
            }
        }
    };

    //Choose a new directory for all Market downloads
    public void getNewDownloadDir() {

        Intent data = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(data, REQUEST_CODE_STORAGE_ACCESS);

    }

    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent resultData) {
        if ((requestCode == REQUEST_CODE_STORAGE_ACCESS)) {

            if (resultCode == RESULT_OK) {
                Uri treeUri = resultData.getData();

                //Skip if the user exited the directory selection
                if (treeUri == null)
                    return;

                grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                //Save the treeUri as a string for later use
                SharedPreferences.Editor ed = sharedPref.edit();
                ed.putString(getString(R.string.key_internal_uri_extsdcard),treeUri.toString());
                ed.apply();

                //Get the downloadDir DocumentFile from Settings and check that it is a usable
                //DocumentsProvider Uri
                String untestedUri = sharedPref.getString(getString(R.string.key_internal_uri_extsdcard), "");
                if (!untestedUri.isEmpty()) {
                    downloadDir = DocumentFile.fromTreeUri(uiContext, Uri.parse(untestedUri));
                }
                else
                    downloadDir = null;

                if (downloadDir != null) {
                    int takeFlags = resultData.getFlags();
                    takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    uiContext.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                }
            }

            //if user cancels the downloadDir search, try to get the downloadDir from settings
            else {
                //Get the downloadDir DocumentFile from Settings and check that it is a usable
                //DocumentsProvider Uri
                String untestedUri = sharedPref.getString(getString(R.string.key_internal_uri_extsdcard), "");
                if (!untestedUri.isEmpty()) {
                    downloadDir = DocumentFile.fromTreeUri(uiContext, Uri.parse(untestedUri));
                }
                else
                    downloadDir = null;

            }

            if (downloadDir == null) {
                updatePrefFromDD(uiContext);
                return;
            }
            //If the current (or previous) downloadDir is not a writable directory, make it null
            //and clear Settings
            if (!downloadDir.exists() || !downloadDir.isDirectory() || !downloadDir.canWrite()) {
                SharedPreferences.Editor ed = sharedPref.edit();
                ed.putString(getString(R.string.key_internal_uri_extsdcard),"");
                ed.apply();
                downloadDir = null;
            }

            //Make sure to update everything after determining downloadDir as null or valid
            updatePrefFromDD(uiContext);
        } // end requestCode == REQUEST_CODE_STORAGE_ACCESS
    }

    private void updatePrefFromDD(Context uiContext) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(uiContext);

        //if downloadDir is null, clear all relevant Settings info
        if (downloadDir == null) {
            SharedPreferences.Editor ed = sharedPref.edit();
            ed.putString(getString(R.string.key_internal_uri_extsdcard),"");
            ed.apply();
            setFullGamesPath(true,sharedPref);
            return;
        }

        //if there is a downloadDir, extract the path for use as "downDirPath"; use the
        //default value if downloadDir is null
        String newDownDirPath = getString(R.string.defDownDirPath);
        if (downloadDir != null) {
            newDownDirPath = FileUtil.getFullPathFromTreeUri(downloadDir.getUri(),uiContext);
            if (newDownDirPath == null) {
                SharedPreferences.Editor ed = sharedPref.edit();
                ed.putString(getString(R.string.key_internal_uri_extsdcard),"");
                ed.apply();
                setFullGamesPath(true,sharedPref);
                return;
            }
        }

        if (newDownDirPath.startsWith(FileUtil.getSdCardPath())) {
            Utility.WriteLog(newDownDirPath + " contains " + FileUtil.getSdCardPath());
            newDownDirPath = newDownDirPath.replace(FileUtil.getSdCardPath(), "");
            if (!newDownDirPath.endsWith("/")) newDownDirPath += "/";
        }
        else {
            Utility.WriteLog(newDownDirPath+" doesn't contain "+FileUtil.getSdCardPath());

            ArrayList<String> extSDPaths = FileUtil.getExtSdCardPaths(uiContext);
            for (int i=0; i<extSDPaths.size(); i++) {
                if (newDownDirPath.startsWith(extSDPaths.get(i))) {
                    Utility.WriteLog(newDownDirPath+" contains "+extSDPaths.get(i));
                    newDownDirPath = newDownDirPath.replace(extSDPaths.get(i),"");
                    if (!newDownDirPath.endsWith("/")) newDownDirPath += "/";
                    break;
                }
                else Utility.WriteLog(newDownDirPath+" doesn't contain "+extSDPaths.get(i));
            }
        }

        //Store the new downDirPath value
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("downDirPath", newDownDirPath);
        editor.commit();

        setFullGamesPath(true, sharedPref);

        final Preference downloadPicker = (Preference) findPreference("downDirPath");
        downloadPicker.setSummary(sharedPref.getString("downDirPath",getString(R.string.defDownDirPath)));

        final Preference filePicker = (Preference) findPreference("relGamePath");
        if (filePicker != null)
            filePicker.setSummary(sharedPref.getString("relGamePath",getString(R.string.defRelPath)));
    }

}
