package com.snakeway.spdf;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;

import com.snakeway.fileviewer.ofd.OFDWebViewActivity;
import com.snakeway.fileviewer.utils.FileUtil;
import com.snakeway.spdf.databinding.ActivityFileViewerBinding;

import java.io.File;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/8 17:18
 */
public class FileViewerActivity extends BaseActivity<ActivityFileViewerBinding> {

    private View.OnClickListener onClickListener;

    public static void openFileViewerActivity(Context context) {
        Intent intent = new Intent(context, FileViewerActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        initAll();
    }

    @Override
    protected ActivityFileViewerBinding getViewBinder() {
        return ActivityFileViewerBinding.inflate(getLayoutInflater());
    }

    @Override
    public void initHandler() {

    }

    @Override
    public void initUi() {
        onClickListener();
    }

    @Override
    public void initConfigUi() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void initHttp() {
    }

    @Override
    public void initOther() {

    }

    private void onClickListener() {
        onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.buttonOpenDoc:
                        openFileWithTbs("TestDoc.doc");
                        break;
                    case R.id.buttonOpenExcel:
                        openFileWithTbs("TestExcel.xls");
                        break;
                    case R.id.buttonOpenPpt:
                        openFileWithTbs("TestPPT.ppt");
                        break;
                    case R.id.buttonOpenOfd:
                        OFDWebViewActivity.openOFDFile(FileViewerActivity.this, "odf", "http://public.coolwallpaper.cn/other/test/TestOfd.ofd");
                        break;
                    default:
                        break;
                }
            }
        };
        viewBinding.buttonOpenDoc.setOnClickListener(onClickListener);
        viewBinding.buttonOpenExcel.setOnClickListener(onClickListener);
        viewBinding.buttonOpenPpt.setOnClickListener(onClickListener);
        viewBinding.buttonOpenOfd.setOnClickListener(onClickListener);
    }

    private void openFileWithTbs(String fileName) {
        String filePath = new File(getFilesDir(), "test").getAbsolutePath() + File.separator;
        FileUtil.viewFileByTBSFileViewer(this, filePath + fileName);
    }

    @Override
    public void doBack() {
        super.doBack();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
