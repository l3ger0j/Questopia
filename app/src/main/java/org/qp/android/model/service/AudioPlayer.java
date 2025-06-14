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
        if (audioExecutor != null) {
            isPaused = true;
            sounds.values().stream()
                    .filter(player -> player != null && player.isPlaying())
                    .forEach(player -> {
                        player.stop();
                        player.reset();
                        player.release();
                    });
            audioExecutor.shutdown();
        }
    }

    public CompletableFuture<Void> playFile(final Context context,
                                            final Uri soundFileUri,
                                            final int volume) {
        if (audioExecutor != null) {
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
        return CompletableFuture.runAsync(() -> {});
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
        if (audioExecutor != null) {
            return CompletableFuture.runAsync(() -> {
                sounds.values().stream()
                        .filter(Objects::nonNull)
                        .forEach(player -> {
                            if (player.isPlaying()) {
                                player.stop();
                            }
                            player.reset();
                            player.release();
                        });
                sounds.clear();
            }, audioExecutor);
        }
        return CompletableFuture.runAsync(() -> {});
    }

    public CompletableFuture<Void> closeFile(final Uri filePath) {
        if (audioExecutor != null) {
            return CompletableFuture.runAsync(() -> {
                if (sounds.containsKey(filePath)) {
                    final var player = sounds.get(filePath);
                    if (player != null) {
                        if (player.isPlaying()) {
                            player.stop();
                        }
                        player.reset();
                        player.release();
                    }
                    sounds.remove(filePath);
                }
            }, audioExecutor);
        }
        return CompletableFuture.runAsync(() -> {});
    }

    public void pause() {
        if (isPaused) return;
        if (audioExecutor != null) {
            isPaused = true;
            CompletableFuture
                    .runAsync(() ->
                            sounds.values().stream()
                                    .filter(player -> player != null && player.isPlaying())
                                    .forEach(MediaPlayer::pause),
                            audioExecutor);
        }
    }

    public void resume() {
        if (!soundEnabled) return;
        if (!isPaused) return;
        if (audioExecutor != null) {
            isPaused = false;
            CompletableFuture
                    .runAsync(() ->
                            sounds.values().stream()
                                    .filter(player -> player != null && !player.isPlaying())
                                    .forEach(MediaPlayer::start),
                            audioExecutor);
        }
    }

    public boolean isPlayingFile(final Uri filePath) {
        return filePath != null && !filePath.equals(Uri.EMPTY) && sounds.containsKey(filePath);
    }

    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }
}