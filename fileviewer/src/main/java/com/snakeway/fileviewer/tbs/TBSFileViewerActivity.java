package com.snakeway.fileviewer.tbs;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.snakeway.fileviewer.R;
import com.snakeway.fileviewer.utils.FileUtil;
import com.tencent.smtt.sdk.TbsReaderView;

import java.io.File;

import static com.snakeway.fileviewer.utils.FileUtil.parseFormat;

/**
 * 支持 doc、docx、ppt、pptx、xls、xlsx、pdf、txt、epub
 *
 * @author snakeway
 * @description:
 * @date :2021/3/8 16:21
 */
public class TBSFileViewerActivity extends AppCompatActivity {
    public static final String FILE_PATH = "filePath";

    private static final String TAG = "TBSFileViewActivity";

    private FrameLayout frameLayoutRoot;
    private LinearLayout linearLayoutContent;
    private TextView textViewRetry;
    private TbsReaderView tbsReaderView;

    private String filePath;

    private View.OnClickListener onClickListener;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tbs_file_viewer);
        init();
        initUi();
    }

    private void init() {
        Intent intent = getIntent();
        filePath = intent.getStringExtra(FILE_PATH);
        if (TextUtils.isEmpty(filePath) || !new File(filePath).isFile()) {
            Toast.makeText(this.getApplicationContext(), getString(R.string.file_not_exist), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initUi() {
        frameLayoutRoot = (FrameLayout) findViewById(R.id.frameLayoutRoot);
        linearLayoutContent = (LinearLayout) findViewById(R.id.linearLayoutContent);
        textViewRetry = (TextView) findViewById(R.id.textViewRetry);

        getSupportActionBar().setTitle(FileUtil.getFileName(filePath));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tbsReaderView = new TbsReaderView(this, new TbsReaderView.ReaderCallback() {
            @Override
            public void onCallBackAction(Integer integer, Object o, Object o1) {

            }
        });
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        tbsReaderView.setLayoutParams(layoutParams);
        frameLayoutRoot.addView(tbsReaderView);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                linearLayoutContent.setVisibility(View.VISIBLE);
                displayFile(filePath);
            }
        }, 1000);
        onClickListener();
    }

    private void onClickListener() {
        onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int id = view.getId();
                if (id == R.id.textViewRetry) {
                    displayFile(filePath);
                }
            }
        };
        textViewRetry.setOnClickListener(onClickListener);
    }

    private void displayFile(String fileAbsPath) {
        Bundle bundle = new Bundle();
        bundle.putString("filePath", fileAbsPath);
        bundle.putString("tempPath", getFilesDir().getPath());
        boolean result = tbsReaderView.preOpen(parseFormat(fileAbsPath), false);
        if (result) {
            tbsReaderView.openFile(bundle);
            tbsReaderView.setVisibility(View.VISIBLE);
            linearLayoutContent.setVisibility(View.GONE);
        } else {
            tbsReaderView.setVisibility(View.GONE);
            linearLayoutContent.setVisibility(View.VISIBLE);
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        tbsReaderView.onStop();
    }
}
