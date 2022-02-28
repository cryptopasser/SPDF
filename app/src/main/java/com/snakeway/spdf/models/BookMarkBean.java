package com.snakeway.spdf.models;

import com.snakeway.pdflibrary.PdfDocument;
import com.snakeway.spdf.items.BookMarkItem;
import com.snakeway.spdf.items.BookMarkSecondItem;
import com.snakeway.spdf.items.BookMarkThirdItem;
import com.snakeway.treeview.annotation.TreeDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author snakeway
 * @description:
 * @date :2021/5/25 18:30
 */
@TreeDataType(iClass = BookMarkItem.class, bindField = "type")
public class BookMarkBean extends BaseBookMarkBean {

    public List<BookMarkSecondBean> childs;

    @TreeDataType(iClass = BookMarkSecondItem.class)
    public static class BookMarkSecondBean extends BaseBookMarkBean {

        public List<BookMarkThirdBean> childs;

        @TreeDataType(iClass = BookMarkThirdItem.class)
        public static class BookMarkThirdBean extends BaseBookMarkBean {

        }
    }

    public static List<BookMarkBean> convertBookMark(List<PdfDocument.Bookmark> bookmarks, OnBookMarkListener onBookMarkListener) {
        List<BookMarkBean> bookMarkBeans = new ArrayList<>();
        if (bookmarks == null) {
            return bookMarkBeans;
        }
        for (int i = 0; i < bookmarks.size(); i++) {
            PdfDocument.Bookmark theBookmark = bookmarks.get(i);
            BookMarkBean bookMarkBean = new BookMarkBean();

            List<PdfDocument.Bookmark> childBookmarks = theBookmark.getChildren();
            bookMarkBean.title = theBookmark.getTitle();
            bookMarkBean.pageIndex = theBookmark.getPageIdx();
            bookMarkBean.isRemark = theBookmark.isRemark();
            if (childBookmarks.size() > 0) {
                bookMarkBean.childs = convertSecond(theBookmark, onBookMarkListener);
            } else {
                bookMarkBean.childs = new ArrayList<>();
            }
            bookMarkBean.onBookMarkListener = onBookMarkListener;
            bookMarkBeans.add(bookMarkBean);
        }
        return bookMarkBeans;
    }

    private static List<BookMarkSecondBean> convertSecond(PdfDocument.Bookmark bookmark, OnBookMarkListener onBookMarkListener) {
        List<BookMarkSecondBean> bookMarkSecondBeans = new ArrayList<>();
        if (bookMarkSecondBeans == null) {
            return bookMarkSecondBeans;
        }
        List<PdfDocument.Bookmark> bookmarks = bookmark.getChildren();
        for (int i = 0; i < bookmarks.size(); i++) {
            PdfDocument.Bookmark theBookmark = bookmarks.get(i);
            BookMarkSecondBean bookMarkSecondBean = new BookMarkSecondBean();

            List<PdfDocument.Bookmark> childBookmarks = theBookmark.getChildren();
            bookMarkSecondBean.title = theBookmark.getTitle();
            bookMarkSecondBean.pageIndex = theBookmark.getPageIdx();
            if (childBookmarks.size() > 0) {
                bookMarkSecondBean.childs = convertThird(theBookmark, onBookMarkListener);
            } else {
                bookMarkSecondBean.childs = new ArrayList<>();
            }
            bookMarkSecondBean.onBookMarkListener = onBookMarkListener;
            bookMarkSecondBeans.add(bookMarkSecondBean);
        }
        return bookMarkSecondBeans;
    }

    private static List<BookMarkSecondBean.BookMarkThirdBean> convertThird(PdfDocument.Bookmark bookmark, OnBookMarkListener onBookMarkListener) {
        List<BookMarkSecondBean.BookMarkThirdBean> bookMarkThirdBeans = new ArrayList<>();
        if (bookMarkThirdBeans == null) {
            return bookMarkThirdBeans;
        }
        List<PdfDocument.Bookmark> bookmarks = bookmark.getChildren();
        for (int i = 0; i < bookmarks.size(); i++) {
            PdfDocument.Bookmark theBookmark = bookmarks.get(i);
            BookMarkSecondBean.BookMarkThirdBean bookMarkThirdBean = new BookMarkSecondBean.BookMarkThirdBean();
            bookMarkThirdBean.title = theBookmark.getTitle();
            bookMarkThirdBean.pageIndex = theBookmark.getPageIdx();
            bookMarkThirdBean.onBookMarkListener = onBookMarkListener;
            bookMarkThirdBeans.add(bookMarkThirdBean);
        }
        return bookMarkThirdBeans;
    }


}
