package com.app.handwritingsimulator;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

public class FontSelectFragment extends BottomSheetDialogFragment {

    public interface OnFontSelectedListener {
        void onFontSelected(Typeface typeface);
    }

    private OnFontSelectedListener listener;
    private File fontDir;                       // 用户字体存储目录
    private final List<FontItem> fontItems = new ArrayList<>();
    private FontAdapter adapter;

    // 特殊标记
    private static final String ADD_ITEM_TAG = "##ADD_ITEM##";

    private static final String defaultFontDir = "fonts/";

    // 用于选择字体文件
    private final ActivityResultLauncher<String[]> pickFontLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    saveFontToAppDir(uri);
                }
            });;

    public void setOnFontSelectedListener(OnFontSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化字体目录
        fontDir = new File(requireContext().getFilesDir(), defaultFontDir);
        if (!fontDir.exists()) fontDir.mkdirs();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(
                    dialog.getWindow().getDecorView()
                            .findViewById(com.google.android.material.R.id.design_bottom_sheet));
            behavior.setMaxHeight(getResources().getDisplayMetrics().heightPixels / 2);
            behavior.setDraggable(false);
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_handwriting_font_select, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        loadFonts();
        adapter = new FontAdapter(fontItems, new FontAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FontItem item) {
                if (item.isAddItem()) {
                    // 点击加号项：打开文件选择器
                    pickFontLauncher.launch(new String[]{"*/*"});
                    return;
                }
                // 普通字体项点击
                Typeface typeface = item.getTypeface(requireContext());
                if (listener != null) {
                    listener.onFontSelected(typeface);
                }
                dismiss();
            }

            @Override
            public void onDeleteClick(FontItem item) {
                // 只有用户字体可删除
                if (item.isCustom()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("删除字体")
                            .setMessage("确定要删除 \"" + item.displayName + "\" 吗？")
                            .setPositiveButton("删除", (dialog, which) -> {
                                File file = new File(item.path);
                                if (file.exists() && file.delete()) {
                                    Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
                                    refreshFontList();
                                } else {
                                    Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            }
        });
        recyclerView.setAdapter(adapter);
        return view;
    }

    /**
     * 加载所有字体：系统默认、assets 内置、用户自定义
     */
    private void loadFonts() {
        fontItems.clear();

        // 1. 系统默认
        fontItems.add(new FontItem("默认字体", "SYSTEM_DEFAULT", FontItem.TYPE_SYSTEM));

        // 2. assets 中的字体
        try {
            String[] assetsFiles = requireContext().getAssets().list(defaultFontDir);
            if (assetsFiles != null) {
                for (String file : assetsFiles) {
                    if (file.endsWith(".ttf") || file.endsWith(".otf")) {
                        String displayName = file.replace(".ttf", "").replace(".otf", "");
                        fontItems.add(new FontItem(displayName, defaultFontDir + file, FontItem.TYPE_ASSET));
                    }
                }
            }
        } catch (IOException e) {
//            e.printStackTrace();
        }

        // 3. 用户自定义字体（私有目录）
        File[] userFonts = fontDir.listFiles();
        if (userFonts != null) {
            for (File file : userFonts) {
                if (file.isFile()) {
                    String displayName = file.getName().replace(".ttf", "").replace(".otf", "");
                    fontItems.add(new FontItem(displayName, file.getAbsolutePath(), FontItem.TYPE_CUSTOM));
                }
            }
        }

        // 4. 加号项
        fontItems.add(new FontItem("+", ADD_ITEM_TAG, FontItem.TYPE_ADD));
    }

    /**
     * 刷新字体列表
     */
    private void refreshFontList() {
        loadFonts();
        if (adapter != null) {
            adapter.updateData(fontItems);
        }
    }

    /**
     * 将用户选择的字体文件保存到私有目录
     */
    private void saveFontToAppDir(Uri sourceUri) {
        new Thread(() -> {
            ContentResolver resolver = requireContext().getContentResolver();
            try (InputStream inputStream = resolver.openInputStream(sourceUri)) {
                if (inputStream == null) return;

                // 获取原始文件名
                String originalName = getFileNameFromUri(sourceUri);
                File destFile = new File(fontDir, originalName);

                try (OutputStream outputStream = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                }

                new Handler(Looper.getMainLooper()).post(this::refreshFontList);
            } catch (IOException e) {
//                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "保存字体失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    /**
     * 从 Uri 中提取文件名
     */
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

    // ---------------------- 字体数据封装 ----------------------
    private static class FontItem {
        static final int TYPE_SYSTEM = 0;
        static final int TYPE_ASSET = 1;
        static final int TYPE_CUSTOM = 2;
        static final int TYPE_ADD = 3;

        String displayName;
        String path;        // 文件路径或标识（如 "SYSTEM_DEFAULT"）
        int type;

        FontItem(String displayName, String path, int type) {
            this.displayName = displayName;
            this.path = path;
            this.type = type;
        }

        boolean isAddItem() {
            return type == TYPE_ADD;
        }

        boolean isCustom() {
            return type == TYPE_CUSTOM;
        }

        Typeface getTypeface(Context context) {
            switch (type) {
                case TYPE_SYSTEM:
                    return Typeface.DEFAULT;
                case TYPE_ASSET:
                    try {
                        return Typeface.createFromAsset(context.getAssets(), path);
                    } catch (Exception e) {
                        return Typeface.DEFAULT;
                    }
                case TYPE_CUSTOM:
                    return Typeface.createFromFile(path);
                default:
                    return Typeface.DEFAULT;
            }
        }
    }

    // ---------------------- 适配器 ----------------------
    private static class FontAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_NORMAL = 0;
        private static final int VIEW_TYPE_ADD = 1;

        private List<FontItem> items;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(FontItem item);
            void onDeleteClick(FontItem item);
        }

        FontAdapter(List<FontItem> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        void updateData(List<FontItem> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).isAddItem() ? VIEW_TYPE_ADD : VIEW_TYPE_NORMAL;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_handwriting_font, parent, false);
            TextView tvFontName = view.findViewById(R.id.tvFontName);
            TextView tvDelete = view.findViewById(R.id.tvDelete);
            return new ViewHolder(view, tvFontName, tvDelete);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
            FontItem item = items.get(position);

            if (viewHolder instanceof ViewHolder) {
                ViewHolder holder = (ViewHolder) viewHolder;
                if (item.isAddItem()) {
                    // 加号项
                    holder.tvFontName.setVisibility(View.VISIBLE);
                    holder.tvDelete.setVisibility(View.GONE);
                    holder.tvFontName.setText(item.displayName);
                    holder.tvFontName.setTypeface(Typeface.DEFAULT);
                    holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
                } else {
                    // 普通字体项
                    holder.tvFontName.setVisibility(View.VISIBLE);
                    holder.tvFontName.setText(item.displayName);
                    Typeface typeface = item.getTypeface(holder.itemView.getContext());
                    holder.tvFontName.setTypeface(typeface);

                    if (item.isCustom()) {
                        // 自定义字体：显示删除按钮，调整约束
                        holder.tvDelete.setVisibility(View.VISIBLE);
                        holder.tvDelete.setOnClickListener(v -> listener.onDeleteClick(item));
                    } else {
                        // 系统或内置字体：不显示删除按钮，调整约束使文本充满宽度
                        holder.tvDelete.setVisibility(View.GONE);
                    }
                    holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
                }
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvFontName, tvDelete;

            ViewHolder(View itemView, TextView tvFontName, TextView tvDelete) {
                super(itemView);
                this.tvFontName = tvFontName;
                this.tvDelete = tvDelete;
            }
        }

    }
}