package org.qp.android.model.archive;

import static org.qp.android.helpers.utils.FileUtil.findOrCreateFile;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFolder;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.file.MimeType;

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;

import java.io.IOException;
import java.io.OutputStream;

public class ArchiveUnpack {

    private static final String TAG = ArchiveUnpack.class.getSimpleName();
    private final Context context;
    private final DocumentFile targetArchive;
    private final DocumentFile destFolder;

    public ArchiveUnpack(@NonNull Context context,
                         @NonNull DocumentFile targetArchive,
                         @NonNull DocumentFile destFolder) {
        this.context = context;
        this.targetArchive = targetArchive;
        this.destFolder = destFolder;
    }

    public void extractArchiveEntries() {
        try (final var stream = new DocumentFileRandomInStream(context, targetArchive.getUri());
             final var inArchive = SevenZip.openInArchive(null, stream)) {
            inArchive.extract(
                    null,
                    false,
                    new ArchiveExtractCallback(context, destFolder, inArchive)
            );
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
    }

    private static class ArchiveExtractCallback implements IArchiveExtractCallback {
        private final Context context;
        private final DocumentFile targetFolder;
        private final IInArchive inArchive;
        private ExtractAskMode extractAskMode;
        private SequentialOutStream stream;

        public ArchiveExtractCallback(Context context, DocumentFile targetFolder, IInArchive inArchive) {
            this.context = context;
            this.targetFolder = targetFolder;
            this.inArchive = inArchive;
        }

        @Override
        public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode) throws SevenZipException {
            this.extractAskMode = extractAskMode;

            var isFolder = (Boolean) inArchive.getProperty(index, PropID.IS_FOLDER);
            var path = (String) inArchive.getProperty(index, PropID.PATH);

            if (isFolder) {
                findOrCreateFolder(context, targetFolder, path);
                return null;
            }

            var file = findOrCreateFile(context, targetFolder, path, MimeType.UNKNOWN);
            if (file == null) return null;
            if (file.isDirectory()) return null;
            try {
                stream = new SequentialOutStream(context.getContentResolver().openOutputStream(file.getUri()));
            } catch (Exception e) {
                Log.e(TAG, "Error: ", e);
            }
            return stream;
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