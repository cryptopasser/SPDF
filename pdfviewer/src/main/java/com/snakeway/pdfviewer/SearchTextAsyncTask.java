package com.snakeway.pdfviewer;

import android.graphics.RectF;
import android.os.AsyncTask;

import com.snakeway.pdflibrary.PdfiumCore;
import com.snakeway.pdfviewer.listener.OnSearchTextListener;
import com.snakeway.pdfviewer.model.SearchTextInfo;
import com.snakeway.pdfviewer.util.UnicodeUtil;

import java.util.ArrayList;
import java.util.List;


class SearchTextAsyncTask extends AsyncTask<Void, Void, SearchTextInfo> {

    private PdfiumCore pdfiumCore;
    private String searchText;
    private List<Integer> pageIndexs;
    private List<Long> textPagesPtrs;
    private OnSearchTextListener onSearchTextListener;


    SearchTextAsyncTask(PdfiumCore pdfiumCore, String searchText, List<Integer> pageIndexs, List<Long> textPagesPtrs, OnSearchTextListener onSearchTextListener) {
        this.pdfiumCore = pdfiumCore;
        this.searchText = searchText;
        this.pageIndexs = pageIndexs;
        this.textPagesPtrs = textPagesPtrs;
        this.onSearchTextListener = onSearchTextListener;
    }

    @Override
    protected SearchTextInfo doInBackground(Void... params) {
        if (pdfiumCore == null || searchText == null) {
            return null;
        }
        if (pageIndexs == null || textPagesPtrs == null || (pageIndexs.size() != textPagesPtrs.size())) {
            return null;
        }
        for (int i = 0; i < pageIndexs.size(); i++) {
            int pageIndex = pageIndexs.get(i);
            long textPagesPtr = textPagesPtrs.get(i);
            int count = pdfiumCore.getPageTextCount(textPagesPtr);
            if (count > 0) {
                SearchTextInfo searchTextInfo = searchText(textPagesPtr, count, searchText);
                if (searchTextInfo != null) {
                    searchTextInfo.setPage(pageIndex);
                    return searchTextInfo;
                }
            }
        }
        return null;
    }

    private SearchTextInfo searchText(long textPagesPtr, int count, String searchText) {
        if (searchText == null || searchText.length() == 0) {
            return null;
        }
        boolean findSuccess = false;
        int start = 0;
        int end = 0;
        for (int i = 0; i < count; i++) {
            int[] res = doSearchText(textPagesPtr, searchText, i, count);
            if (res != null) {
                findSuccess = true;
                start = res[0];
                end = res[1];
                break;
            }
        }
        if (!findSuccess) {
            return null;
        }
        List<RectF> areaRects = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            areaRects.add(pdfiumCore.getTextRect(textPagesPtr, i));
        }
        SearchTextInfo searchTextInfo = new SearchTextInfo(start, end, areaRects);
        return searchTextInfo;
    }

    private int[] doSearchText(long textPagesPtr, String searchText, int start, int end) {
        int[] result = null;
        int nextCheckIndex = 0;
        StringBuilder append = new StringBuilder();
        int findStart = 0;
        int findCount = 0;
        for (int i = start; i < end; i++) {
            int unicode = pdfiumCore.searchTextUnicode(textPagesPtr, i);
            if (unicode <= 0) {
                continue;
            }
            String targetChar = String.valueOf(searchText.charAt(nextCheckIndex));
            String unicodeString = UnicodeUtil.convertUnicode(unicode);
            String code = UnicodeUtil.unicodeToString(unicodeString);
            if (!code.equals(targetChar)) {
                if (!append.toString().equals("") && (unicodeString.equals("\\u000d") || unicodeString.equals("\\u000a"))) {
                    findCount++;
                    continue;
                }
                break;
            }
            if (append.toString().equals("")) {
                findStart = i;
            }
            append.append(code);
            if (append.toString().equals(searchText)) {
                result = new int[2];
                result[0] = findStart;
                result[1] = findStart + findCount;
                break;
            }
            findCount++;
            nextCheckIndex++;
        }
        return result;
    }

    @Override
    protected void onPostExecute(SearchTextInfo searchTextInfo) {
        if (onSearchTextListener != null) {
            onSearchTextListener.onResult(searchTextInfo);
        }
    }

    @Override
    protected void onCancelled() {
        if (onSearchTextListener != null) {
            onSearchTextListener.onCancel();
        }
    }

}
