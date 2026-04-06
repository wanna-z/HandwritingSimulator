package com.app.handwritingsimulator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class TextInputFragment extends BottomSheetDialogFragment {

    public interface OnTextInputListener {
        void onTextClear();
        void onTextInput(String text);
    }

    private OnTextInputListener listener;

    public static TextInputFragment newInstance(String currentText) {
        TextInputFragment fragment = new TextInputFragment();
        Bundle args = new Bundle();
        args.putString("currentText", currentText);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnTextInputListener(OnTextInputListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // 创建 BottomSheetDialog
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        // 监听 Dialog 创建完成后的回调，确保布局已加载
        dialog.setOnShowListener(dialogInterface -> {
            // 获取 BottomSheetBehavior
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(dialog.getWindow()
                    .getDecorView().findViewById(com.google.android.material.R.id.design_bottom_sheet));
            int maxHeight = getResources().getDisplayMetrics().heightPixels / 2;
            behavior.setMaxHeight(maxHeight);
            behavior.setDraggable(false);
            behavior.setPeekHeight(maxHeight);          // 与 maxHeight 一致
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);  // 关键：强制展开
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_handwriting_text_input, container, false);
        EditText editText = view.findViewById(R.id.editText);
        Button btnClear = view.findViewById(R.id.btnClear);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);

        if (getArguments() != null) {
            String currentText = getArguments().getString("currentText");
            editText.setText(currentText);
            editText.setSelection(editText.length()); // 光标移至末尾
        }

        btnClear.setOnClickListener(v -> new AlertDialog.Builder(view.getContext())
                .setTitle("提示")
                .setMessage("是否清空当前内容？")
                .setPositiveButton("确认", (d, w)-> {
                    editText.setText("");
                    if (listener != null) {
                        listener.onTextClear();
                    }
                })
                .setNegativeButton("取消", null)
                .show());

        btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTextInput(editText.getText().toString());
            }
        });

        return view;
    }
}