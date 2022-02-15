package com.snakeway.pdfviewer.source;

import android.content.Context;

import com.snakeway.pdflibrary.PdfDocument;
import com.snakeway.pdflibrary.PdfiumCore;

import java.io.IOException;

public class ByteArraySource implements DocumentSource {

    private byte[] data;

    public ByteArraySource(byte[] data) {
        this.data = data;
    }

    @Override
    public PdfDocument createDocument(Context context, PdfiumCore core, String password) throws IOException {
        return core.newDocument(data, password);
    }
}
