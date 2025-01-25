package org.qp.android.model.archive;

import static org.qp.android.helpers.utils.ThreadUtil.assertNonUiThread;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ArchiveUnpack {

    private static final String TAG = ArchiveUnpack.class.getSimpleName();
    private final Context context;
    private Uri targetArchive;
    public File unpackFolder;
    private Uri destDir;

    @NonNull
    private static int[] getPrimitiveLongArrayFromInt(Set<Integer> input) {
        var ret = new int[input.size()];
        var iterator = input.iterator();
        for (var i = 0; i < ret.length; i++) {
            ret[i] = iterator.next();
        }
        return ret;
    }

    public ArchiveUnpack(@NonNull Context context,
                         @NonNull Uri targetArchiveUri,
                         @NonNull Uri destFolderUri) {
        this.context = context;
        this.targetArchive = targetArchiveUri;
        this.destDir = destFolderUri;
    }

    public void extractArchiveEntries() {
        assertNonUiThread();

        Map<Integer, String> fileNames = new HashMap<>();
        try (var stream = new DocumentFileRandomInStream(context, targetArchive);
             var inArchive = SevenZip.openInArchive(null, stream)) {
            var itemCount = inArchive.getNumberOfItems();

            for (var index = 0; index < itemCount; index++) {
                var fileName = inArchive.getStringProperty(index, PropID.PATH);
                var lastSeparator = fileName.lastIndexOf(File.separator);
                if (lastSeparator > -1) fileName = fileName.substring(lastSeparator + 1);
                fileNames.put(index, fileName);
            }

            var indexes = getPrimitiveLongArrayFromInt(fileNames.keySet());
            Log.d(TAG, fileNames.toString());
            inArchive.extract(indexes, false, new ArchiveExtractCallback(destDir, inArchive));
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
    }

    private class ArchiveExtractCallback implements IArchiveExtractCallback {

        private final Uri targetFolder;
        private final IInArchive inArchive;
        private ExtractAskMode extractAskMode;
        private SequentialOutStream stream;

        public ArchiveExtractCallback(Uri targetFolder, IInArchive inArchive) {
            this.targetFolder = targetFolder;
            this.inArchive = inArchive;
        }

        @Override
        public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode) throws SevenZipException {
            Log.v(TAG, "Extract archive, get stream: " + index + " to: " + extractAskMode);

            this.extractAskMode = extractAskMode;

            var isFolder = (Boolean) inArchive.getProperty(index , PropID.IS_FOLDER);
            var path = (String) inArchive.getProperty(index, PropID.PATH);
//            var documentFile = new File(targetFolder.getAbsolutePath(), path);
//            DocumentFileCompat.createFile(ArchiveUnpack.this.context, );

//            if (isFolder) {
//                createDirectory(file);
//                return null;
//            }
//
//            var fileParent = file.getParentFile();
//            if (fileParent == null) return null;
//
//            createDirectory(fileParent);
//
//            try {
//                stream = new SequentialOutStream(new FileOutputStream(file));
//            } catch (FileNotFoundException e) {
//                Log.e(TAG, "Error: ", e);
//            }
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
