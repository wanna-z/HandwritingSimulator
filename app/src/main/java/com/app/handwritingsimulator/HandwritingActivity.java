package com.app.handwritingsimulator;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.OutputStream;

public class HandwritingActivity extends AppCompatActivity {

    private HandwritingView handwritingView;

    private final static String text = "在漫长的历史长河中，有这样一群人，他们生而平凡，但是他们拥有着不凡的梦想，脚踩六便士，心中有月亮。\n" +
            "\n" +
            "有人会问：什么才是平凡？我认为，人类自身的渺小相对于宇宙的广阔无垠会让我们变得平凡；我认为，普通的出生会让我们被世俗地定义为平凡；我还认为，在“山外有人，人外有人”的评比方式下，每个人都有可能因为他人的光芒而显得无比平凡。什么是伟大？平凡的人们，没有出生入死的惊心动魄，亦没有名扬四海的万丈光芒。但是，他们在自己的人生赛道上执着于梦想的追求，在无数个面对艰难现实的选择时坚守心中的“月亮”，在明知梦想可能遥不可及的前提下，仍要为它拼尽全力，奋力一搏。\n" +
            "\n" +
            "平凡而伟大的追梦人，有着各种各样的“梦”。\n" +
            "\n" +
            "正值青春年华的青少年，当中大部分人多年如一日的“寒窗苦读”是为了完成心中的“大学梦”；毕业后走向基层，用自己的所学所见为乡村的发展出谋划策的大学生村官做着“乡村振兴的梦”；正在创业的企业创始人，他们心中有着将这个企业做到影响中国乃至世界的梦；每一个充满爱国心的中国同胞，心中都有着祖国统一的梦想；每一个热血方刚的中国人，都有着民族复兴的中国梦。\n" +
            "\n" +
            "平凡而伟大的追梦人，有着各种各样的身份。\n" +
            "\n" +
            "他们可能是出现在互联网中，用自己掌握的技能传播农村文明的乡村网红“帅农鸟哥”；也可能是在默默无闻地承受危险，用自己的牺牲换来更多人的平安的缉毒警察；也可能是重庆山火中，在高温中骑着摩托车，义无反顾地冲向着火点送水的快递小哥；也可能是疫情中无数个穿着白大褂的白衣天使，他们用自己的身体筑成一道无形的“人墙”，阻隔病毒的扩散，保卫着身后的人群。\n" +
            "\n" +
            "今天，我们无数个平凡而又伟大的追梦人奔跑在路上，无数个平凡而伟大的梦想组成我们的中国梦，为这个国家注入无尽的新鲜活力，推动着这个国家在历史长河中不断前进。\n" +
            "\n" +
            "我们不惧平凡，是因为我们心中有伟大的梦想，这个梦想终将成为我们毕生的信念，指引着我们朝着未来的每一步坚定地走下去。我相信：总有一天，每个驰而不息的“平凡人”都能实现自己的梦想，而中国梦，亦能在这些实现的梦想中，不断地往前推进，再推进，并最终实现！";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handwriting);

        handwritingView = findViewById(R.id.handwritingView);
        View btnText = findViewById(R.id.btnText);
        View btnBackground = findViewById(R.id.btnBackground);
        View btnFont = findViewById(R.id.btnFont);
        View btnParams = findViewById(R.id.btnParams);
        View btnExport = findViewById(R.id.btnExport);

        // 设置默认文本
        handwritingView.setText(text);

        // 加载默认背景
        try {
            handwritingView.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/今年也要加油鸭.ttf"));
            Bitmap background = BitmapFactory.decodeStream(getAssets().open("backgrounds/A4纸.png"));
            handwritingView.setBackgroundImage(background);
            handwritingView.post(()-> handwritingView.adjustDrawAreaWithBackground());
        } catch (Exception e) {
//            e.printStackTrace();
        }

        // 文本按钮：弹出文本编辑面板
        btnText.setOnClickListener(v -> {
            TextInputFragment fragment = TextInputFragment.newInstance(handwritingView.getCurrentText());
            fragment.setOnTextInputListener(new TextInputFragment.OnTextInputListener() {
                @Override
                public void onTextClear() {
                    handwritingView.setText("");
                }

                @Override
                public void onTextInput(String text) {
                    handwritingView.setText(text);
                }
            });
            fragment.show(getSupportFragmentManager(), "TextInput");
        });

        // 背景按钮：弹出背景选择面板
        btnBackground.setOnClickListener(v -> {
            BackgroundSelectFragment fragment = new BackgroundSelectFragment();
            fragment.setOnBackgroundSelectedListener(bitmap -> handwritingView.setBackgroundImage(bitmap));
            fragment.show(getSupportFragmentManager(), "BackgroundSelect");
        });

        // 字体按钮：弹出字体选择面板
        btnFont.setOnClickListener(v -> {
            FontSelectFragment fragment = new FontSelectFragment();
            fragment.setOnFontSelectedListener(typeface -> handwritingView.setTypeface(typeface));
            fragment.show(getSupportFragmentManager(), "FontSelect");
        });

        btnExport.setOnClickListener(v -> showExportScaleDialog());

        // 参数按钮：弹出参数调整面板
        btnParams.setOnClickListener(v -> {
            ParamsAdjustFragment fragment = ParamsAdjustFragment.newInstance(handwritingView);
            fragment.setOnParamsAdjustListener(new ParamsAdjustFragment.OnParamsAdjustListener() {
                @Override
                public void onTextSizeChanged(float size) {
                    handwritingView.setTextSize(size);
                }

                @Override
                public void onSpacingChanged(float spacing) {
                    handwritingView.setLetterSpacing(spacing);
                }

                @Override
                public void onLineSpacingChanged(float spacing) {
                    handwritingView.setLineSpacing(spacing);
                }

                @Override
                public void onTextColorChanged(int color) {
                    handwritingView.setTextColor(color);
                }

                @Override
                public void onUnderlineColorChanged(int color) {
                    handwritingView.setUnderlineColor(color);
                }

                @Override
                public void onTextBoldChanged(boolean bold) {
                    handwritingView.setTextBold(bold);
                }

                @Override
                public void onTextItalicChanged(boolean italic) {
                    handwritingView.setTextItalic(italic);
                }

                @Override
                public void onMarginsChanged(int left, int top, int right, int bottom) {
                    handwritingView.setMargins(left, top, right, bottom);
                }

                @Override
                public void onTextStrokeWidthChanged(float width) {
                    handwritingView.setTextStrokeWidth(width);
                }

                @Override
                public void onUnderlineChanged(boolean enable, float width) {
                    handwritingView.setUnderlineParameter(enable, width);
                }

                @Override
                public void onTextIndentChanged(float value) {
                    handwritingView.setTextIndent(value);
                }

                @Override
                public void onTextSizeFluctuationChanged(float value) {
                    handwritingView.setMaxScale(value);
                }

                @Override
                public void onSpacingFluctuationChanged(float value) {
                    handwritingView.setMaxOffsetX(value);
                }

                @Override
                public void onLineSpacingFluctuationChanged(float value) {
                    handwritingView.setMaxOffsetY(value);
                }

                @Override
                public void onRotationFluctuationChanged(float value) {
                    handwritingView.setMaxRotation(value);
                }

            });
            fragment.show(getSupportFragmentManager(), "ParamsAdjust");
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    private void showExportScaleDialog() {
        final float[] selectedScale = new float[]{1.0f};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("导出");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(10), dpToPx(20), dpToPx(10));

        // 显示比例值的 TextView
        TextView tvScale = new TextView(this);
        tvScale.setText("比例: 1.0x");
        layout.addView(tvScale);

        // 显示长宽信息的 TextView
        TextView tvSize = new TextView(this);
        tvSize.setText(getTargetImageSize(1.0f));
        layout.addView(tvSize);

        SeekBar seekBar = new SeekBar(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            seekBar.setMin(10);
        }
        seekBar.setMax((int) (calculateMaxScale() * 100));
        seekBar.setProgress(100);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scale = progress / 100.0f;
                selectedScale[0] = scale;
                tvScale.setText(String.format("比例: %.1fx", scale));
                tvSize.setText(getTargetImageSize(scale)); // 实时更新长宽
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        layout.addView(seekBar);

        builder.setView(layout);
        builder.setPositiveButton("导出", (dialog, which) -> checkPermissionAndSave(selectedScale[0]));
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private float calculateMaxScale() {
        int originalWidth = 0, originalHeight = 0;

        // 优先使用背景图片尺寸
        if (handwritingView.hasBackground()) {
            Bitmap bg = handwritingView.getBackgroundBitmap();
            if (bg != null && !bg.isRecycled()) {
                originalWidth = bg.getWidth();
                originalHeight = bg.getHeight();
            }
        }

        // 若无背景，使用视图尺寸
        if (originalWidth == 0 || originalHeight == 0) {
            originalWidth = handwritingView.getWidth();
            originalHeight = handwritingView.getHeight();
        }

        // 如果视图尚未测量，返回默认最大比例（3.0）
        if (originalWidth <= 0 || originalHeight <= 0) {
            return 3.0f;
        }

        // 设定最大允许的图片尺寸（例如 4096px，可根据需求调整）
        int maxDimension = 4096;
        float scaleW = (float) maxDimension / originalWidth;
        float scaleH = (float) maxDimension / originalHeight;
        float maxScale = Math.min(scaleW, scaleH);

        // 确保最大比例不小于 1.0（避免限制导致无法导出原图）
        return Math.max(maxScale, 1.0f);
    }

    private String getTargetImageSize(float scale) {
        int originalWidth = 0, originalHeight = 0;

        // 获取原始尺寸（优先使用背景尺寸，否则使用视图尺寸）
        if (handwritingView.hasBackground()) {
            Bitmap bg = handwritingView.getBackgroundBitmap();
            if (bg != null && !bg.isRecycled()) {
                originalWidth = bg.getWidth();
                originalHeight = bg.getHeight();
            }
        }
        if (originalWidth == 0 || originalHeight == 0) {
            originalWidth = handwritingView.getWidth();
            originalHeight = handwritingView.getHeight();
        }

        // 如果仍然无效（视图尚未测量），返回提示信息
        if (originalWidth <= 0 || originalHeight <= 0) {
            return "请等待视图加载完成";
        }

        int targetWidth = (int) (originalWidth * scale);
        int targetHeight = (int) (originalHeight * scale);
        return String.format("导出尺寸: %d × %d px", targetWidth, targetHeight);
    }

    private void checkPermissionAndSave(float scale) {
        // 显示进度弹窗（不可取消，避免用户中断）
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("正在保存图片...");
        progressDialog.show();

        // 在子线程中执行保存操作
        new Thread(() -> {
            try {
                Bitmap bitmap = handwritingView.exportAsBitmap(false, scale);
                if (bitmap == null) {
                    runOnUiThread(()-> Toast.makeText(this, "导出图片失败", Toast.LENGTH_SHORT).show());
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    saveBitmapToGalleryForR(bitmap);
                } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    saveBitmapToGalleryLegacy(bitmap);
                } else {
                    runOnUiThread(()-> Toast.makeText(this, "请先授予存储权限！", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
//                e.printStackTrace();
            }
            progressDialog.dismiss();
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void saveBitmapToGalleryForR(Bitmap bitmap) {
        String filename = System.currentTimeMillis() + ".png";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        // 保存到 Downloads/HandwritingSimulator/ 目录（自动创建子目录）
        String saveDir = Environment.DIRECTORY_DOWNLOADS + "/HandwritingSimulator";
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, saveDir);

        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                runOnUiThread(() -> Toast.makeText(this, "图片已保存到: " + saveDir, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                // 保存失败时删除无效的 MediaStore 条目
                getContentResolver().delete(uri, null, null);
            }
        } else {
            runOnUiThread(() -> Toast.makeText(this, "保存失败: 无法创建文件条目", Toast.LENGTH_SHORT).show());
        }
    }

    private void saveBitmapToGalleryLegacy(Bitmap bitmap) {
        String filename = System.currentTimeMillis() + ".png";
        java.io.File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        java.io.File appDir = new java.io.File(picturesDir, "HandwritingSimulator");
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        java.io.File file = new java.io.File(appDir, filename);
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            // 通知媒体库更新
            android.media.MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);
            runOnUiThread(()-> Toast.makeText(this, "图片已保存到: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
//            e.printStackTrace();
            runOnUiThread(()-> Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

}