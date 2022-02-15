package com.snakeway.pdfviewer.listener;

import com.snakeway.pdfviewer.model.SearchTextInfo;

public interface OnSearchTextListener {

    void onResult(SearchTextInfo searchTextInfo);

    void onCancel();

}
