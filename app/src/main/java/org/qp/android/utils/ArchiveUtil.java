package org.qp.android.utils;

import static org.qp.android.utils.ThreadUtil.assertNonUiThread;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
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
import java.util.Objects;
import java.util.Set;

@Deprecated
public final class ArchiveUtil {
    private static final String TAG = ArchiveUtil.class.getSimpleName();

    public static long totalSize;
    public static MutableLiveData<Long> progressInstall = new MutableLiveData<>();

    @NonNull
    public static int[] getPrimitiveLongArrayFromInt(@NonNull Set<Integer> input) {
        var ret = new int[input.size()];
        var iterator = input.iterator();
        for (int i = 0; i < ret.length; i++) {
            ret[i] = iterator.next();
        }
        return ret;
    }

    @Nullable
    public static Throwable testArchiveEntries(Context context,
                                               Uri uri,
                                               File targetFolder) {
        assertNonUiThread();
        var fileNames = new HashMap<Integer, String>();
        try (var stream = new DocumentFileRandomInStream(context, uri);
             var inArchive = SevenZip.openInArchive(null, stream)) {
            var itemCount = inArchive.getNumberOfItems();
            for (var index = 0; index <= itemCount; index++) {
                var fileName = inArchive.getStringProperty(index, PropID.PATH);
                var lastSeparator = fileName.lastIndexOf(File.separator);
                if (lastSeparator > -1) fileName = fileName.substring(lastSeparator + 1);
                fileNames.put(index, fileName);
            }
            var indexes = getPrimitiveLongArrayFromInt(fileNames.keySet());
            inArchive.extract(indexes, true,
                    new ArchiveExtractCallback(targetFolder, inArchive, context));
            return null;
        } catch (Throwable er) {
            return er;
        }
    }

    public static boolean extractArchiveEntries(Context context,
                                                Uri uri,
                                                File targetFolder) {
        assertNonUiThread();
        var fileNames = new HashMap<Integer, String>();
        try (var stream = new DocumentFileRandomInStream(context, uri);
             var inArchive = SevenZip.openInArchive(null, stream)) {
            var itemCount = inArchive.getNumberOfItems();
            for (var index = 0; index <= itemCount; index++) {
                var fileName = inArchive.getStringProperty(index, PropID.PATH);
                var lastSeparator = fileName.lastIndexOf(File.separator);
                if (lastSeparator > -1) fileName = fileName.substring(lastSeparator + 1);
                fileNames.put(index, fileName);
            }
            var indexes = getPrimitiveLongArrayFromInt(fileNames.keySet());
            inArchive.extract(indexes, false,
                    new ArchiveExtractCallback(targetFolder, inArchive, context));
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error: ", e);
            return false;
        }
    }

    public static boolean extractArchiveEntries(Context context,
                                                Uri uri,
                                                File targetFolder,
                                                String password) {
        assertNonUiThread();
        var fileNames = new HashMap<Integer, String>();
        try (var stream = new DocumentFileRandomInStream(context, uri);
             var inArchive = SevenZip.openInArchive(null, stream, password)) {
            var itemCount = inArchive.getNumberOfItems();
            for (var index = 0; index <= itemCount; index++) {
                var fileName = inArchive.getStringProperty(index, PropID.PATH);
                var lastSeparator = fileName.lastIndexOf(File.separator);
                if (lastSeparator > -1) fileName = fileName.substring(lastSeparator + 1);
                fileNames.put(index, fileName);
            }
            var indexes = getPrimitiveLongArrayFromInt(fileNames.keySet());
            inArchive.extract(indexes, false,
                    new ArchiveExtractCallback(targetFolder, inArchive, context, password));
            return true;
        } catch (IOException er) {
            Log.e(TAG , "Error: " , er);
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
            pfdInput = contentResolver.openFileDescriptor(uri, "rw");
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

        private void skipNBytes(long n) throws IOException {
            if (n > 0) {
                long ns = stream.skip(n);
                if (ns < n) {
                    n -= ns;
                    while (n > 0 && stream.read() != -1) {
                        n--;
                    }
                    if (n != 0) {
                        throw new EOFException();
                    }
                } else if (ns != n) {
                    throw new IOException("Unable to skip exactly");
                }
            }
        }

        @Override
        public int read(byte[] bytes) throws SevenZipException {
            try {
                var result = stream.read(bytes);
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

    private static class ArchiveExtractCallback implements IArchiveExtractCallback, ICryptoGetTextPassword {
        private final File targetFolder;
        private final IInArchive inArchive;
        private final Context context;
        private ExtractAskMode extractAskMode;
        private SequentialOutStream stream;
        private String textPassword;

        public ArchiveExtractCallback(
                File targetFolder,
                IInArchive inArchive,
                Context context) {
            this.targetFolder = targetFolder;
            this.inArchive = inArchive;
            this.context = context;
        }

        public ArchiveExtractCallback(
                File targetFolder,
                IInArchive inArchive,
                Context context,
                String textPassword) {
            this.targetFolder = targetFolder;
            this.inArchive = inArchive;
            this.context = context;
            this.textPassword = textPassword;
        }

        @Nullable
        @Override
        public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode)
                throws SevenZipException {
            Log.v(TAG, "Extract archive, get stream: " + index + " to: " + extractAskMode);
            this.extractAskMode = extractAskMode;
            var isFolder = (Boolean) inArchive.getProperty(index , PropID.IS_FOLDER);
            var path = (String) inArchive.getProperty(index, PropID.PATH);
            var file = new File(targetFolder.getAbsolutePath(), path);
            if (isFolder) {
                createDirectory(file);
                return null;
            }
            createDirectory(Objects.requireNonNull(file.getParentFile()));
            try {
                stream = new SequentialOutStream(context.getContentResolver()
                        .openOutputStream(Uri.parse(Uri.decode(String.valueOf(Uri.fromFile(file)))) , "rw"));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Error: ", e);
            }
            return stream;
        }

        private void createDirectory(File parentFile) throws SevenZipException {
            if (!parentFile.exists()) {
                if (!parentFile.mkdirs()) {
                    throw new SevenZipException("Error creating directory: " + parentFile.getAbsolutePath());
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
            totalSize = total;
            Log.v(TAG, String.format("Extract archive, work planned: %s", total));
        }

        @Override
        public void setCompleted(long complete) {
            progressInstall.postValue(complete);
            Log.v(TAG, String.format("Extract archive, work completed: %s", complete));
        }

        @Override
        public String cryptoGetTextPassword() {
            return textPassword;
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
