package org.qp.android.model.service;

import static org.qp.android.helpers.utils.FileUtil.fromFullPath;
import static org.qp.android.helpers.utils.PathUtil.normalizeContentPath;
import static org.qp.android.helpers.utils.StringUtil.isNotEmpty;
import static org.qp.android.helpers.utils.ThreadUtil.throwIfNotMainThread;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.ui.dialogs.GameDialogType;
import org.qp.android.ui.game.GameActivity;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioPlayer {

    private final String TAG = this.getClass().getSimpleName();

    private final ConcurrentHashMap<String, Sound> sounds = new ConcurrentHashMap<>();
    private ExecutorService audioService;
    private volatile Handler audioHandler;
    private volatile boolean isAudioServiceInit;
    private boolean soundEnabled;
    private boolean isPaused = false;
    private Context context;

    private QuestPlayerApplication getApplication() {
        return (QuestPlayerApplication) context.getApplicationContext();
    }

    private GameActivity getAcitvity() {
        return (GameActivity) context;
    }

    public void start(Context context) {
        throwIfNotMainThread();
        this.context = context;
        audioService = Executors.newSingleThreadExecutor();
        audioService.submit(() -> {
            Looper.prepare();
            audioHandler = new Handler();
            isAudioServiceInit = true;
            Looper.loop();
        });
    }

    public void stop() {
        throwIfNotMainThread();
        pause();
        if (audioService == null) return;
        if (isAudioServiceInit) {
            var handler = audioHandler;
            if (handler != null) {
                handler.getLooper().quitSafely();
            }
            isAudioServiceInit = false;
        } else {
            Log.w(TAG,"Audio thread has been started, but not initialized");
        }
        audioService = null;
    }

    public void playFile(final String path, final int volume) {
        runOnAudioThread(() -> {
            var sound = sounds.get(path);
            if (sound != null) {
                sound.volume = volume;
            } else {
                sound = new Sound();
                sound.path = path;
                sound.volume = volume;
                sounds.put(path, sound);
            }
            if (soundEnabled && !isPaused) {
                doPlay(sound);
            }
        });
    }

    private void runOnAudioThread(final Runnable runnable) {
        if (audioService == null) {
            Log.w(TAG,"Audio service has not been started");
            return;
        }
        if (!isAudioServiceInit) {
            Log.w(TAG,"Audio service has not been initialized");
            return;
        }
        var handler = audioHandler;
        if (handler != null) {
            handler.post(runnable);
        }
    }

    private void doPlay(final Sound sound) {
        var sysVolume = getSystemVolume(sound.volume);

        if (sound.player != null) {
            sound.player.setVolume(sysVolume, sysVolume);
            if (!sound.player.isPlaying()) {
                sound.player.start();
            }
            return;
        }

        var normPath = normalizeContentPath(sound.path);
        var application = (QuestPlayerApplication) context.getApplicationContext();
        var curGameDir = application.getCurrentGameDir();
        var soundFile = fromFullPath(normPath , curGameDir);

        if (soundFile == null) {
            var controller = getAcitvity().getSettingsController();
            if (controller != null && controller.isUseMusicDebug) {
                var localizedStr = getAcitvity().getString(R.string.notFoundSound) + normPath;
                getAcitvity().showSimpleDialog(
                        localizedStr,
                        GameDialogType.ERROR_DIALOG
                );
            } else {
                Log.e(TAG,"Sound file not found: " + normPath);
            }
            return;
        }

        var player = new MediaPlayer();
        try {
            player.setDataSource(context , soundFile.getUri());
            player.prepare();
        } catch (IOException ex) {
            Log.e(TAG,"Failed to initialize media player", ex);
            return;
        }
        player.setOnCompletionListener(mp -> sounds.remove(sound.path));
        player.setVolume(sysVolume, sysVolume);
        player.start();
        sound.player = player;
    }

    private float getSystemVolume(int volume) {
        return volume / 100.f;
    }

    public void closeAllFiles() {
        runOnAudioThread(() -> {
            for (var sound : sounds.values()) {
                doClose(sound);
            }
            sounds.clear();
        });
    }

    private void doClose(Sound sound) {
        if (sound.player == null) {
            return;
        }
        if (sound.player.isPlaying()) {
            sound.player.stop();
        }
        sound.player.release();
    }

    public void closeFile(final String path) {
        runOnAudioThread(() -> {
            var sound = sounds.remove(path);
            if (sound != null) {
                doClose(sound);
            }
        });
    }

    public void pause() {
        if (isPaused) return;
        isPaused = true;
        runOnAudioThread(() -> {
            for (var sound : sounds.values()) {
                if (sound.player != null && sound.player.isPlaying()) {
                    sound.player.pause();
                }
            }
        });
    }

    public void resume() {
        if (!isPaused) return;
        isPaused = false;
        if (!soundEnabled) return;
        runOnAudioThread(() -> {
            for (var sound : sounds.values()) {
                doPlay(sound);
            }
        });
    }

    public boolean isPlayingFile(String path) {
        return isNotEmpty(path) && sounds.containsKey(path);
    }

    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    private static class Sound {
        private String path;
        private int volume;
        private MediaPlayer player;
    }
}
