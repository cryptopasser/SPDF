package com.snakeway.pdfviewer.source;

import android.content.Context;

import com.snakeway.pdflibrary.PdfDocument;
import com.snakeway.pdflibrary.PdfiumCore;

import java.io.IOException;

public interface DocumentSource {
    PdfDocument createDocument(Context context, PdfiumCore core, String password) throws IOException;
}
