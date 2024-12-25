package org.qp.android.model.archive;

import static org.qp.android.helpers.utils.FileUtil.findOrCreateFolder;
import static org.qp.android.helpers.utils.ThreadUtil.assertNonUiThread;

import android.content.Context;
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
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

public class ArchiveUnpack {

    private static final String TAG = ArchiveUnpack.class.getSimpleName();
    private final Context context;
    private final File targetArchive;
    public File unpackFolder;
    private File destFolder;

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
                         @NonNull File targetArchive,
                         @NonNull File destFolder) {
        this.context = context;
        this.targetArchive = targetArchive;
        this.destFolder = destFolder;
    }

    public void extractArchiveEntries() {
        assertNonUiThread();

        try (var stream = new RandomAccessFileInStream(new RandomAccessFile(targetArchive, "r"));
             var inArchive = SevenZip.openInArchive(null, stream)) {
            var itemCount = inArchive.getNumberOfItems();
            var fileNames = new HashMap<Integer, String>();

            for (int ind = 0; ind < itemCount; ind++) {
                var fileName = inArchive.getStringProperty(ind, PropID.PATH);
                if (fileName.endsWith(".qsp") || fileName.endsWith(".gam")) {
                    if (fileName.split("/").length == 1) {
                        var archiveName = targetArchive.getName();
                        var pattern = Pattern.compile(".(?:rar|zip|aqsp)");
                        var folderName = pattern.matcher(archiveName).replaceAll("");
                        if (fileName.split("/").length == 1) {
                            destFolder = findOrCreateFolder(context, destFolder, folderName);
                            unpackFolder = destFolder;
                        }
                    }
                }
                if (unpackFolder == null && ind == itemCount - 1) {
                    unpackFolder = new File(destFolder, fileName);
                }
                Log.d(TAG, "index: "+ind+"\nfilename: "+fileName+"\nitemCount: "+itemCount);
                var lastSeparator = fileName.lastIndexOf(File.separator);
                if (lastSeparator > -1) fileName = fileName.substring(lastSeparator + 1);
                fileNames.put(ind, fileName);
            }

            var indexes = getPrimitiveLongArrayFromInt(fileNames.keySet());
            inArchive.extract(
                    indexes,
                    false,
                    new ArchiveExtractCallback(destFolder, inArchive)
            );
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
    }

    private static class ArchiveExtractCallback implements IArchiveExtractCallback {

        private final File targetFolder;
        private final IInArchive inArchive;
        private ExtractAskMode extractAskMode;
        private SequentialOutStream stream;

        public ArchiveExtractCallback(File targetFolder, IInArchive inArchive) {
            this.targetFolder = targetFolder;
            this.inArchive = inArchive;
        }

        @Override
        public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode) throws SevenZipException {
            Log.v(TAG, "Extract archive, get stream: " + index + " to: " + extractAskMode);

            this.extractAskMode = extractAskMode;

            var isFolder = (Boolean) inArchive.getProperty(index , PropID.IS_FOLDER);
            var path = (String) inArchive.getProperty(index, PropID.PATH);
            var file = new File(targetFolder.getAbsolutePath(), path);

            if (isFolder) {
                createDirectory(file);
                return null;
            }

            var fileParent = file.getParentFile();
            if (fileParent == null) return null;

            createDirectory(fileParent);

            try {
                stream = new SequentialOutStream(new FileOutputStream(file));
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
