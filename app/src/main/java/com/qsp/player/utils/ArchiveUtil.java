package com.qsp.player.utils;

import static com.qsp.player.utils.ThreadUtil.assertNonUiThread;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISeekableStream;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ArchiveUtil {
    public static final MutableLiveData<Long> progress = new MutableLiveData<>();
    private static final String TAG = ArchiveUtil.class.getSimpleName();

    public static int[] getPrimitiveLongArrayFromInt(Set<Integer> input) {
        int[] ret = new int[input.size()];
        Iterator<Integer> iterator = input.iterator();
        for (int i = 0; i < ret.length; i++) {
            ret[i] = iterator.next();
        }
        return ret;
    }

    public static boolean extractArchiveEntries(Context context,
                                              Uri uri,
                                              File targetFolder) {
        assertNonUiThread();
        Map<Integer, String> fileNames = new HashMap<>();
        try (DocumentFileRandomInStream stream = new DocumentFileRandomInStream(context, uri);
             IInArchive inArchive = SevenZip.openInArchive(null, stream)) {
            int itemCount = inArchive.getNumberOfItems();
            for (int index = 0; index < itemCount; index++) {
                String fileName = inArchive.getStringProperty(index, PropID.PATH);
                int lastSeparator = fileName.lastIndexOf(File.separator);
                if (lastSeparator > -1) fileName = fileName.substring(lastSeparator + 1);
                fileNames.put(index, fileName);
            }
            int[] indexes = getPrimitiveLongArrayFromInt(fileNames.keySet());
            Log.d(TAG, fileNames.toString());
            inArchive.extract(indexes, false,
                    new ArchiveExtractCallback(targetFolder, inArchive, context));
            return true;
        } catch (IOException e) {
            Log.w(TAG, "", e);
            return false;
        }
    }

    public static class DocumentFileRandomInStream implements IInStream {

        private ContentResolver contentResolver;
        private Uri uri;

        private ParcelFileDescriptor pfdInput = null;
        private FileInputStream stream = null;

        private long streamSize;
        private long position;

        public DocumentFileRandomInStream(@NonNull final Context context, @NonNull final Uri uri) {
            try {
                this.contentResolver = context.getContentResolver();
                this.uri = uri;
                openUri();
                streamSize = stream.getChannel().size();
            } catch (IOException e) {
                Log.e(TAG, "Error: ", e);
            }
        }

        private void openUri() throws IOException {
            if (stream != null) stream.close();
            if (pfdInput != null) pfdInput.close();
            pfdInput = contentResolver.openFileDescriptor(uri, "r");
            if (pfdInput != null)
                stream = new FileInputStream(pfdInput.getFileDescriptor());
        }

        @Override
        public long seek(long offset, int seekOrigin) throws SevenZipException {
            long seekDelta = 0;
            if (seekOrigin == ISeekableStream.SEEK_CUR) seekDelta = offset;
            else if (seekOrigin == ISeekableStream.SEEK_SET) seekDelta = offset - position;
            else if (seekOrigin == ISeekableStream.SEEK_END)
                seekDelta = streamSize + offset - position;
            if (position + seekDelta > streamSize) position = streamSize;
            if (seekDelta != 0) {
                try {
                    if (seekDelta < 0) {
                        openUri();
                        skipNBytes(position + seekDelta);
                    } else {
                        skipNBytes(seekDelta);
                    }
                } catch (IOException e) {
                    throw new SevenZipException(e);
                }
            }
            position += seekDelta;
            return position;
        }

        // Taken from Java14's InputStream
        // as basic skip is limited by the size of its buffer
        private void skipNBytes(long n) throws IOException {
            if (n > 0) {
                long ns = stream.skip(n);
                if (ns < n) { // skipped too few bytes
                    // adjust number to skip
                    n -= ns;
                    // read until requested number skipped or EOS reached
                    while (n > 0 && stream.read() != -1) {
                        n--;
                    }
                    // if not enough skipped, then EOFE
                    if (n != 0) {
                        throw new EOFException();
                    }
                } else if (ns != n) { // skipped negative or too many bytes
                    throw new IOException("Unable to skip exactly");
                }
            }
        }

        @Override
        public int read(byte[] bytes) throws SevenZipException {
            try {
                int result = stream.read(bytes);
                position += result;
                if (result != bytes.length)
                    Log.w(TAG, String.format("diff %s expected; %s read", bytes.length, result));
                if (result < 0) result = 0;
                return result;
            } catch (IOException e) {
                throw new SevenZipException(e);
            }
        }

        @Override
        public void close() throws IOException {
            stream.close();
            pfdInput.close();
        }
    }

    private static class ArchiveExtractCallback implements IArchiveExtractCallback {
        private final File targetFolder;
        private final IInArchive inArchive;
        private final Context context;
        private ExtractAskMode extractAskMode;
        private SequentialOutStream stream;

        public ArchiveExtractCallback(
                File targetFolder,
                IInArchive inArchive,
                Context context) {
            this.targetFolder = targetFolder;
            this.inArchive = inArchive;
            this.context = context;
        }

        @Override
        public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode)
                throws SevenZipException {
            Log.v(TAG, "Extract archive, get stream: " + index + " to: " + extractAskMode);
            this.extractAskMode = extractAskMode;
            boolean isFolder = (Boolean) inArchive.getProperty(index ,
                    PropID.IS_FOLDER);
            String path = (String) inArchive.getProperty(index, PropID.PATH);
            File file = new File(targetFolder.getAbsolutePath(), path);
            if (isFolder) {
                createDirectory(file);
                return null;
            }
            createDirectory(Objects.requireNonNull(file.getParentFile()));
            try {
                stream = new SequentialOutStream(context.getContentResolver()
                        .openOutputStream(Uri.fromFile(file) , "rwt"));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Error: ", e);
            }
            return stream;
        }

        private void createDirectory(File parentFile) throws SevenZipException {
            if (!parentFile.exists()) {
                if (!parentFile.mkdirs()) {
                    throw new SevenZipException("Error creating directory: "
                            + parentFile.getAbsolutePath());
                }
            }
        }

        @Override
        public void prepareOperation(ExtractAskMode extractAskMode) {
            Log.v(TAG, String.format("Extract archive, prepare to: %s", extractAskMode));
        }

        @Override
        public void setOperationResult(ExtractOperationResult extractOperationResult) throws SevenZipException {
            Log.v(TAG, String.format("Extract archive, %s completed with: %s", extractAskMode, extractOperationResult));

            try {
                if (stream != null) stream.close();
                stream = null;
            } catch (IOException e) {
                throw new SevenZipException(e);
            }

            if (extractOperationResult != ExtractOperationResult.OK) {
                throw new SevenZipException(extractOperationResult.toString());
            }
        }

        @Override
        public void setTotal(long total) {
            Log.v(TAG, String.format("Extract archive, work planned: %s", total));
        }

        @Override
        public void setCompleted(long complete) {
            progress.postValue(complete);
            Log.v(TAG, String.format("Extract archive, work completed: %s", complete));
        }
    }

    private static class SequentialOutStream implements ISequentialOutStream {
        private final OutputStream out;

        public SequentialOutStream(@NonNull final OutputStream stream) {
            this.out = stream;
        }

        @Override
        public int write(byte[] data) throws SevenZipException {
            if (data == null || data.length == 0) {
                throw new SevenZipException("null data");
            }
            try {
                out.write(data);
            } catch (IOException e) {
                throw new SevenZipException(e);
            }
            return data.length;
        }

        public void close() throws IOException {
            out.close();
        }
    }
}
