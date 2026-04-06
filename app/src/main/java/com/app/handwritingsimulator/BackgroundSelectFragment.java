package com.app.handwritingsimulator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.handwritingsimulator.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundSelectFragment extends BottomSheetDialogFragment {

    public interface OnBackgroundSelectedListener {
        void onBackgroundSelected(Bitmap bitmap);
    }

    private OnBackgroundSelectedListener listener;

    // 用于启动图片选择器
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    saveImageToAppDir(getContext(), uri);
                }
            }
    );

    private static final String defaultBackgroundDir = "backgrounds/";

    // 私有目录：存储用户添加的背景图
    private static final String customBackgroundDir = "backgrounds/";
    private File backgroundDir;

    public void setOnBackgroundSelectedListener(OnBackgroundSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(dialog.getWindow().getDecorView()
                            .findViewById(com.google.android.material.R.id.design_bottom_sheet));
            behavior.setMaxHeight(getResources().getDisplayMetrics().heightPixels / 2);
            behavior.setDraggable(false);
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_handwriting_background_select, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // 确保私有目录存在
        backgroundDir = new File(requireContext().getFilesDir(), customBackgroundDir);
        if (!backgroundDir.exists()) backgroundDir.mkdirs();

        // 加载所有背景（assets + 私有目录）
        List<String> backgroundPaths = loadAllBackgrounds();

        BackgroundAdapter adapter = new BackgroundAdapter(backgroundPaths, new BackgroundAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String path) {
                // 普通背景项点击
                if (!path.equals(BackgroundAdapter.ADD_ITEM_TAG)) {
                    try {
                        Bitmap bitmap;
                        if (isDefaultBackground(path)) {
                            // assets 中的图片
                            bitmap = BitmapFactory.decodeStream(requireContext().getAssets().open(path));
                        } else {
                            // 私有目录中的图片
                            bitmap = BitmapFactory.decodeFile(path);
                        }
                        if (listener != null) {
                            listener.onBackgroundSelected(bitmap);
                        }
                        dismiss();
                    } catch (IOException e) {
//                        e.printStackTrace();
                        Toast.makeText(getContext(), "加载图片失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onAddClick() {
                // 点击加号项：启动图片选择器
                pickImageLauncher.launch("image/*");
            }

            @Override
            public void onDeleteClick(String path) {
                // 长按删除逻辑
                handleDelete(path);
            }
        });
        recyclerView.setAdapter(adapter);
        return view;
    }

    /**
     * 处理长按删除
     * @param path 图片路径（"background/xxx" 表示 assets 中的图片，否则是私有目录的绝对路径）
     */
    private void handleDelete(String path) {
        Context context = getContext();
        if (isDefaultBackground(path)) {
            // assets 中的背景不可删除
            Toast.makeText(context, "默认背景不可删除", Toast.LENGTH_SHORT).show();
            return;
        }

        // 用户导入的背景，弹出确认对话框
        new AlertDialog.Builder(context)
                .setTitle("删除背景")
                .setMessage("确定要删除这张背景图吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    File file = new File(path);
                    if (file.exists() && file.delete()) {
                        refreshBackgroundList();  // 刷新列表
                    } else {
                        Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private static boolean isDefaultBackground(String path) {
        if (path == null) return false;
        return path.startsWith(defaultBackgroundDir);
    }

    /**
     * 加载所有背景图片路径：
     * 1. assets/background 下的图片（以 "background/xxx.jpg" 形式存储）
     * 2. 私有目录 backgroundDir 下的图片（存储绝对路径）
     * 最后添加一个特殊标记项
     */
    private List<String> loadAllBackgrounds() {
        List<String> allPaths = new ArrayList<>();

        // 1. assets 中的背景
        try {
            String[] files = requireContext().getAssets().list(defaultBackgroundDir);
            if (files != null) {
                for (String file : files) {
                    if (file.endsWith(".jpg") || file.endsWith(".png")) {
                        allPaths.add(defaultBackgroundDir + file);
                    }
                }
            }
        } catch (IOException e) {
//            e.printStackTrace();
        }

        // 2. 私有目录中的背景
        File[] userFiles = backgroundDir.listFiles();
        if (userFiles != null) {
            for (File file : userFiles) {
                if (file.isFile()) {
                    allPaths.add(file.getAbsolutePath());
                }
            }
        }

        // 3. 最后添加加号标记项
        allPaths.add(BackgroundAdapter.ADD_ITEM_TAG);
        return allPaths;
    }

    /**
     * 将用户选择的图片保存到私有目录，并刷新列表
     */
    private void saveImageToAppDir(Context context, Uri sourceUri) {
        new Thread(() -> {
            ContentResolver resolver = context.getContentResolver();
            try (InputStream inputStream = resolver.openInputStream(sourceUri)) {
                if (inputStream == null) return;

                String fileName = getFileNameFromUri(sourceUri);
                File destFile = new File(backgroundDir, fileName);

                try (OutputStream outputStream = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                }
                // 保存成功后，在主线程刷新列表和显示提示
                new Handler(Looper.getMainLooper()).post(this::refreshBackgroundList);
            } catch (IOException e) {
//                e.printStackTrace();
                // 保存失败，在主线程提示错误
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "保存图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        String scheme = uri.getScheme();
        if (scheme != null && scheme.equals("content")) {
            try (android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null,
                    null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }
        if (fileName == null) fileName = "";
        return fileName;
    }

    /**
     * 刷新背景列表（重新加载数据并更新 RecyclerView）
     */
    private void refreshBackgroundList() {
        View view = getView();
        if (view != null) {
            RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
            if (recyclerView != null && recyclerView.getAdapter() != null) {
                List<String> newPaths = loadAllBackgrounds();
                ((BackgroundAdapter) recyclerView.getAdapter()).updateData(newPaths);
            }
        }
    }

    // ---------- 内部适配器 ----------
    private static class BackgroundAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        static final String ADD_ITEM_TAG = "##ADD_ITEM##";  // 加号项标识

        private static final int TYPE_NORMAL = 0;
        private static final int TYPE_ADD = 1;

        private List<String> paths;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(String path);
            void onAddClick();
            void onDeleteClick(String path);
        }

        private final ExecutorService executor = Executors.newFixedThreadPool(4);

        BackgroundAdapter(List<String> paths, OnItemClickListener listener) {
            this.paths = paths;
            this.listener = listener;
        }

        void updateData(List<String> newPaths) {
            this.paths = newPaths;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return paths.get(position).equals(ADD_ITEM_TAG) ? TYPE_ADD : TYPE_NORMAL;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.item_handwriting_background, parent, false);
            if (viewType == TYPE_ADD) {
                ImageView icon = view.findViewById(R.id.imageView);
                icon.setImageResource(R.drawable.ic_add);
                icon.setColorFilter(ContextCompat.getColor(icon.getContext(), android.R.color.darker_gray), PorterDuff.Mode.SRC_IN);
                return new AddViewHolder(view);
            } else {
                return new NormalViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            String item = paths.get(position);
            if (holder instanceof NormalViewHolder) {
                NormalViewHolder normalHolder = (NormalViewHolder) holder;
                loadThumbnail(normalHolder.imageView, item);
                normalHolder.itemView.setOnClickListener(v -> listener.onItemClick(item));

                if (!isDefaultBackground(item)) {
                    normalHolder.deleteView.setVisibility(View.VISIBLE);
                    normalHolder.deleteView.setOnClickListener(v -> listener.onDeleteClick(item));
                } else {
                    normalHolder.deleteView.setVisibility(View.GONE);
                }
            } else if (holder instanceof AddViewHolder) {
                AddViewHolder addHolder = (AddViewHolder) holder;
                addHolder.itemView.setOnClickListener(v -> listener.onAddClick());
                addHolder.deleteView.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return paths.size();
        }

        /**
         * 加载缩略图（assets 或本地文件）
         */
        private void loadThumbnail(ImageView imageView, String path) {
            Context context = imageView.getContext();
            executor.submit(() -> {
                Bitmap bitmap = null;
                try {
                    int reqWidth = 150;
                    int reqHeight = 150;
                    if (isDefaultBackground(path)) {
                        // assets 中的图片
                        InputStream is = context.getAssets().open(path);
                        bitmap = decodeSampledBitmapFromStream(context, path, is, reqWidth, reqHeight);
                        is.close();
                    } else if (!path.equals(ADD_ITEM_TAG)) {
                        // 私有目录中的图片
                        bitmap = decodeSampledBitmapFromFile(path, reqWidth, reqHeight);
                    }
                } catch (IOException e) {
//                    e.printStackTrace();
                    bitmap = null;
                }
                Bitmap finalBitmap = bitmap;
                imageView.post(()-> {
                    if (finalBitmap != null) {
                        imageView.setImageBitmap(finalBitmap);
                    } else {
                        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                });
            });
        }

        private Bitmap decodeSampledBitmapFromStream(Context context, String path, InputStream inputStream,
                                                     int reqWidth, int reqHeight) throws IOException {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;

            InputStream newStream = context.getAssets().open(path);
            Bitmap bitmap = BitmapFactory.decodeStream(newStream, null, options);
            newStream.close();
            return bitmap;
        }

        private Bitmap decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(filePath, options);
        }

        private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            int height = options.outHeight;
            int width = options.outWidth;
            int sampleSize = 1;
            if (height > reqHeight || width > reqWidth) {
                int halfHeight = height / 2;
                int halfWidth = width / 2;
                while ((halfHeight / sampleSize) >= reqHeight && (halfWidth / sampleSize) >= reqWidth) {
                    sampleSize *= 2;
                }
            }
            return sampleSize;
        }

        static class NormalViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            View deleteView;
            NormalViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView);
                deleteView = itemView.findViewById(R.id.tvDelete);
            }
        }

        static class AddViewHolder extends RecyclerView.ViewHolder {
            View deleteView;
            AddViewHolder(View itemView) {
                super(itemView);
                deleteView = itemView.findViewById(R.id.tvDelete);
            }
        }
    }
}