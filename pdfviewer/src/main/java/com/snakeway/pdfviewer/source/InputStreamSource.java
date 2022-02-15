package com.snakeway.pdfviewer.source;

import android.content.Context;

import com.snakeway.pdflibrary.PdfDocument;
import com.snakeway.pdflibrary.PdfiumCore;
import com.snakeway.pdfviewer.util.Util;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamSource implements DocumentSource {

    private InputStream inputStream;

    public InputStreamSource(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public PdfDocument createDocument(Context context, PdfiumCore core, String password) throws IOException {
        return core.newDocument(Util.toByteArray(inputStream), password);
    }
}
