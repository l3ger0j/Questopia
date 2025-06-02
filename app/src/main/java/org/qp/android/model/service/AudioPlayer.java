package org.qp.android.model.service;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioPlayer {

    private final ConcurrentHashMap<Uri, MediaPlayer> sounds = new ConcurrentHashMap<>();
    private ExecutorService audioExecutor;
    private volatile boolean soundEnabled = true;
    private volatile boolean isPaused = false;

    public void start() {
        isPaused = false;
        audioExecutor = Executors.newSingleThreadExecutor();
    }

    public void stop() {
        pause();
        release();
        audioExecutor.shutdown();
    }

    public CompletableFuture<Void> playFile(final Context context,
                                            final Uri soundFileUri,
                                            final int volume) {
        return CompletableFuture.runAsync(() -> {
            var sound = sounds.get(soundFileUri);
            if (sound != null) {
                var newVolume = getSystemVolume(volume);
                sound.setVolume(newVolume, newVolume);
                if (soundEnabled && !isPaused) {
                    if (!sound.isPlaying()) {
                        sound.start();
                    }
                }
            } else {
                var newSound = createNewSound(context, soundFileUri, volume);
                if (soundEnabled && !isPaused) {
                    if (!newSound.isPlaying()) {
                        newSound.start();
                    }
                }
            }
        }, audioExecutor);
    }

    private MediaPlayer createNewSound(final Context context,
                                       final Uri filePath,
                                       final int fileVolume) {
        var sysVolume = getSystemVolume(fileVolume);
        var filePlayer = new MediaPlayer();

        try {
            filePlayer.setDataSource(context, filePath);
            filePlayer.prepare();
        } catch (IOException ex) {
            throw new CompletionException(ex);
        }

        filePlayer.setOnCompletionListener(mediaPlayer -> {
            sounds.remove(filePath);
            mediaPlayer.reset();
            mediaPlayer.release();
        });
        filePlayer.setVolume(sysVolume, sysVolume);

        sounds.put(filePath, filePlayer);
        return sounds.get(filePath);
    }

    private float getSystemVolume(int volume) {
        return volume / 100.f;
    }

    public CompletableFuture<Void> closeAllFiles() {
        return CompletableFuture.runAsync(() -> {
            sounds.values().stream()
                    .filter(Objects::nonNull)
                    .forEach(player -> {
                        if (player.isPlaying()) {
                            player.stop();
                        }
                        player.release();
                    });
            sounds.clear();
        }, audioExecutor);
    }

    public CompletableFuture<Void> closeFile(final Uri filePath) {
        return CompletableFuture.runAsync(() -> {
            final var sound = sounds.remove(filePath);
            if (sound != null) {
                if (sound.isPlaying()) {
                    sound.stop();
                }
                sound.release();
            }
        }, audioExecutor);
    }

    private void release() {
        if (audioExecutor == null) return;
        if (isPaused) return;
        isPaused = true;

        CompletableFuture
                .runAsync(() ->
                        sounds.values().stream()
                                .filter(player -> player != null && player.isPlaying())
                                .forEach(MediaPlayer::release),
                        audioExecutor);
    }

    public void pause() {
        if (isPaused) return;
        isPaused = true;

        audioExecutor.submit(() ->
                sounds.values().stream()
                        .filter(player -> player != null && player.isPlaying())
                        .forEach(MediaPlayer::pause)
        );
    }

    public void resume() {
        if (!soundEnabled) return;
        if (!isPaused) return;
        isPaused = false;

        audioExecutor.submit(() ->
                sounds.values().stream()
                        .filter(player -> player != null && !player.isPlaying())
                        .forEach(MediaPlayer::start)
        );
    }

    public boolean isPlayingFile(final Uri filePath) {
        return filePath != null && !filePath.equals(Uri.EMPTY) && sounds.containsKey(filePath);
    }

    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }
}