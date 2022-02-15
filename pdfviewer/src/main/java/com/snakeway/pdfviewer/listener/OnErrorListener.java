package com.snakeway.pdfviewer.listener;

public interface OnErrorListener {

    /**
     * Called if error occurred while opening PDF
     *
     * @param t Throwable with error
     */
    void onError(Throwable t);
}
