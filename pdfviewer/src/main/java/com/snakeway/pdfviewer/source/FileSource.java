package com.snakeway.pdfviewer.source;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import com.snakeway.pdflibrary.PdfDocument;
import com.snakeway.pdflibrary.PdfiumCore;

import java.io.File;
import java.io.IOException;

public class FileSource implements DocumentSource {

    private File file;

    public FileSource(File file) {
        this.file = file;
    }

    @Override
    public PdfDocument createDocument(Context context, PdfiumCore core, String password) throws IOException {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        return core.newDocument(pfd, password);
    }
}
