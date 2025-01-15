package org.qp.android.dto.stock;

import androidx.documentfile.provider.DocumentFile;

public record TempFile(DocumentFile inputFile, TempFileType fileType) { }
