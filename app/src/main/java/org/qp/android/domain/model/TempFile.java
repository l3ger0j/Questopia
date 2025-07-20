package org.qp.android.domain.model;

import androidx.documentfile.provider.DocumentFile;

public record TempFile(DocumentFile inputFile, TempFileType fileType) { }
