package com.app.handwritingsimulator;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.math.BigDecimal;

import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog;

public class ParamsAdjustFragment extends BottomSheetDialogFragment {

    public interface OnParamsAdjustListener {
        void onTextSizeChanged(float size);
        void onSpacingChanged(float spacing);
        void onLineSpacingChanged(float spacing);
        void onTextColorChanged(int color);
        void onUnderlineColorChanged(int color);
        void onTextBoldChanged(boolean bold);      // 保留原有，但 B 按钮会调用此
        void onTextItalicChanged(boolean italic);  // 新增斜体回调
        void onMarginsChanged(int left, int top, int right, int bottom);
        void onTextStrokeWidthChanged(float width);
        void onUnderlineChanged(boolean enable, float width);
        void onTextIndentChanged(float value);
        void onTextSizeFluctuationChanged(float value);
        void onSpacingFluctuationChanged(float value);
        void onLineSpacingFluctuationChanged(float value);
        void onRotationFluctuationChanged(float value);
    }

    private OnParamsAdjustListener listener;
    private float currentSize, currentSpacing, currentLineSpacing, currentTextStrokeWidth;
    private int currentTextColor, currentUnderlineColor;
    private boolean currentBold;
    private boolean currentItalic;
    private int currentMarginLeft, currentMarginTop, currentMarginRight, currentMarginBottom;
    private static final int maxMargin = 9999;
    private static final float maxSize = 999f;
    private static final float maxSpacing = 999f;
    private static final float maxFluctuation = 9f;
    private static final float maxIndent = 999f;

    private float currentSizeFluctuation, currentSpacingFluctuation, currentLineSpacingFluctuation, currentRotationFluctuation;

    private float currentTextIndent;
    private boolean currentUnderline;
    private float currentUnderlineStrokeWidth;

    public enum SpacingType {
        spacing, spacingFluctuation, lineSpacing, lineSpacingFluctuation
    }

    public static ParamsAdjustFragment newInstance(HandwritingView handwritingView) {
        int[] margins = handwritingView.getMargins();
        ParamsAdjustFragment fragment = new ParamsAdjustFragment();
        Bundle args = new Bundle();
        args.putFloat("textSize", handwritingView.getTextSize());
        args.putFloat("spacing", handwritingView.getLetterSpacing());
        args.putFloat("lineSpacing", handwritingView.getLineSpacing());
        args.putInt("textColor", handwritingView.getTextColor());
        args.putInt("underlineColor", handwritingView.getUnderlineColor());
        args.putFloat("textStrokeWidth", handwritingView.getTextStrokeWidth());
        args.putBoolean("bold", handwritingView.isTextBold());
        args.putBoolean("italic", handwritingView.isTextItalic());
        args.putInt("marginLeft", margins[0]);
        args.putInt("marginTop", margins[1]);
        args.putInt("marginRight", margins[2]);
        args.putInt("marginBottom", margins[3]);
        args.putFloat("textIndent", handwritingView.getTextIndent());

        args.putFloat("sizeFluctuation", handwritingView.getMaxScale());
        args.putFloat("spacingFluctuation", handwritingView.getMaxOffsetX());
        args.putFloat("lineSpacingFluctuation", handwritingView.getMaxOffsetY());
        args.putFloat("rotationFluctuation", handwritingView.getMaxRotation());
        args.putBoolean("underline", handwritingView.isUnderlineEnable());
        args.putFloat("underlineStrokeWidth", handwritingView.getUnderlineStrokeWidth());

        fragment.setArguments(args);
        return fragment;
    }

    public void setOnParamsAdjustListener(OnParamsAdjustListener listener) {
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
            // 设置最大高度（单位：像素）
            int maxHeight = getResources().getDisplayMetrics().heightPixels / 3;
            behavior.setMaxHeight(maxHeight);
            behavior.setDraggable(false);
            behavior.setPeekHeight(maxHeight);
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_handwriting_params_adjust, container, false);

        if (getArguments() != null) {
            currentSize = getArguments().getFloat("textSize");
            currentSpacing = getArguments().getFloat("spacing");
            currentLineSpacing = getArguments().getFloat("lineSpacing");
            currentTextColor = getArguments().getInt("textColor");
            currentUnderlineColor = getArguments().getInt("underlineColor");
            currentTextStrokeWidth = getArguments().getFloat("textStrokeWidth");
            currentBold = getArguments().getBoolean("bold");
            currentItalic = getArguments().getBoolean("italic");
            currentMarginLeft = getArguments().getInt("marginLeft");
            currentMarginTop = getArguments().getInt("marginTop");
            currentMarginRight = getArguments().getInt("marginRight");
            currentMarginBottom = getArguments().getInt("marginBottom");
            currentTextIndent = getArguments().getFloat("textIndent");

            currentSizeFluctuation = getArguments().getFloat("sizeFluctuation");
            currentSpacingFluctuation = getArguments().getFloat("spacingFluctuation");
            currentLineSpacingFluctuation = getArguments().getFloat("lineSpacingFluctuation");
            currentRotationFluctuation = getArguments().getFloat("rotationFluctuation");
            currentUnderline = getArguments().getBoolean("underline");
            currentUnderlineStrokeWidth = getArguments().getFloat("underlineStrokeWidth");
        }

        // 文本样式
        setupTextStyleChangeView(view);

        // 页面设置 - 左边距
        setupMarginChangeView(view, R.id.marginLeftEdit, R.id.marginLeftMinus, R.id.marginLeftPlus, currentMarginLeft, true);

        // 页面设置 - 上边距
        setupMarginChangeView(view, R.id.marginTopEdit, R.id.marginTopMinus, R.id.marginTopPlus, currentMarginTop, false);

        // 页面设置 - 右边距
        setupMarginChangeView(view, R.id.marginRightEdit, R.id.marginRightMinus, R.id.marginRightPlus, currentMarginRight, true);

        // 页面设置 - 下边距
        setupMarginChangeView(view, R.id.marginBottomEdit, R.id.marginBottomMinus, R.id.marginBottomPlus, currentMarginBottom, false);

        // 文本设置 - 字体大小
        setupSizeChangeView(view, R.id.sizeEdit, R.id.sizeMinus, R.id.sizePlus, currentSize, 1, false);

        // 大小波动
        setupSizeChangeView(view, R.id.sizeFluctuateEdit, R.id.sizeFluctuateMinus, R.id.sizeFluctuatePlus, currentSizeFluctuation, 0.1f,
                true);

        // 字间距
        setupSpacingChangeView(view, R.id.spacingEdit, R.id.spacingMinus, R.id.spacingPlus, currentSpacing, 1f, SpacingType.spacing);

        // 字间距波动
        setupSpacingChangeView(view, R.id.spacingFluctuateEdit, R.id.spacingFluctuateMinus, R.id.spacingFluctuatePlus,
                currentSpacingFluctuation, 0.1f, SpacingType.spacingFluctuation);

        // 文本设置 - 行间距
        setupSpacingChangeView(view, R.id.lineSpacingEdit, R.id.lineSpacingMinus, R.id.lineSpacingPlus, currentLineSpacing, 1f,
                SpacingType.lineSpacing);

        // 行间距波动
        setupSpacingChangeView(view, R.id.lineSpacingFluctuateEdit, R.id.lineSpacingFluctuateMinus, R.id.lineSpacingFluctuatePlus,
                currentLineSpacingFluctuation, 0.1f, SpacingType.lineSpacingFluctuation);

        // 画笔粗细
        setupStrokeAngleChangeView(view, R.id.textStrokeWidthEdit, R.id.textStrokeWidthMinus, R.id.textStrokeWidthPlus,
                currentTextStrokeWidth, 0.1f, true);

        // 倾斜度
        setupStrokeAngleChangeView(view, R.id.angleFluctuateEdit, R.id.angleFluctuateMinus, R.id.angleFluctuatePlus,
                currentRotationFluctuation, 1f, false);

        // 行前缩进
        setupTextIndentChangeView(view, R.id.textIndentEdit, R.id.textIndentMinus, R.id.textIndentPlus, currentTextIndent, 5f);

        // 下划线
        setupUnderlineChangeView(view);

        return view;
    }

    private void setupTextStyleChangeView(View view) {
        // 颜色选择 ImageView
        ImageView colorView = view.findViewById(R.id.textColorView);
        setupColorViewBorder(colorView, currentTextColor);
        colorView.setOnClickListener(v -> new ColorPickerDialog()
                .withColor(currentTextColor)           // 初始颜色
                .withAlphaEnabled(true)                     // 启用透明度（支持十六进制带 Alpha）
                .withPresets(Color.BLACK, Color.RED, Color.BLUE) // 可选：预设颜色
                .withListener((pickerView, color) -> {
                    currentTextColor = color;
                    setupColorViewBorder(colorView, color);
                    if (listener != null) listener.onTextColorChanged(color);
                })
                .show(getChildFragmentManager(), "textColorPicker"));

        // B 按钮（粗体）
        TextView boldButton = view.findViewById(R.id.boldButton);
        setupTextStyleButton(boldButton, currentBold);
        boldButton.setOnClickListener(v -> {
            boolean newBold = !currentBold;
            currentBold = newBold;
            boldButton.setSelected(newBold);
            if (listener != null) listener.onTextBoldChanged(newBold);
        });

        // I 按钮（斜体）
        TextView italicButton = view.findViewById(R.id.italicButton);
        setupTextStyleButton(italicButton, currentItalic);
        italicButton.setOnClickListener(v -> {
            boolean newItalic = !currentItalic;
            currentItalic = newItalic;
            italicButton.setSelected(newItalic);
            if (listener != null) listener.onTextItalicChanged(newItalic);
        });
    }

    private void setupMarginChangeView(View view, int marginEditId, int marginMinusId, int marginPlusId, int currentMargin,
                                       boolean isHorizontal) {
        EditText marginEdit = view.findViewById(marginEditId);
        TextView marginMinus = view.findViewById(marginMinusId);
        TextView marginPlus = view.findViewById(marginPlusId);
        setupAdjustButtonBackground(marginMinus);
        setupAdjustButtonBackground(marginPlus);
        marginEdit.setText(String.valueOf(currentMargin));
        marginMinus.setOnClickListener(v -> changeMargin(marginEdit, -5, isHorizontal));
        marginPlus.setOnClickListener(v -> changeMargin(marginEdit, 5, isHorizontal));
        marginEdit.setOnFocusChangeListener((v, hasFocus) -> updateMarginFromInput(marginEdit, isHorizontal));
    }

    private void setupSizeChangeView(View view, int sizeEditId, int sizeMinusId, int sizePlusId, float currentSize, float delta,
                                     boolean fluctuational) {
        EditText sizeEdit = view.findViewById(sizeEditId);
        TextView sizeMinus = view.findViewById(sizeMinusId);
        TextView sizePlus = view.findViewById(sizePlusId);
        setupAdjustButtonBackground(sizeMinus);
        setupAdjustButtonBackground(sizePlus);
        sizeEdit.setText(String.valueOf(currentSize));
        sizeMinus.setOnClickListener(v -> changeSize(sizeEdit, -delta, fluctuational));
        sizePlus.setOnClickListener(v -> changeSize(sizeEdit, delta, fluctuational));
        sizeEdit.setOnFocusChangeListener((v, hasFocus) -> updateSizeFromInput(sizeEdit, fluctuational));
    }

    private void setupSpacingChangeView(View view, int spacingEditId, int spacingMinusId, int spacingPlusId, float currentSpacing,
                                        float delta, SpacingType spacingType) {
        EditText sizeEdit = view.findViewById(spacingEditId);
        TextView sizeMinus = view.findViewById(spacingMinusId);
        TextView sizePlus = view.findViewById(spacingPlusId);
        setupAdjustButtonBackground(sizeMinus);
        setupAdjustButtonBackground(sizePlus);
        sizeEdit.setText(String.valueOf(currentSpacing));
        sizeMinus.setOnClickListener(v -> changeSpacing(sizeEdit, -delta, spacingType));
        sizePlus.setOnClickListener(v -> changeSpacing(sizeEdit, delta, spacingType));
        sizeEdit.setOnFocusChangeListener((v, hasFocus) -> updateSpacingFromInput(sizeEdit, spacingType));
    }

    public void setupAdjustButtonBackground(TextView textView) {
        if (textView == null) return;
        Context context = textView.getContext();
        // 1. 圆角边框背景（dp 单位转 px）
        int borderWidthPx = dpToPx(1, context);
        int cornerRadiusPx = dpToPx(4, context);

        // 判断当前是否为深色模式
        boolean isDarkMode = isDarkModeEnabled(context);

        // 根据模式选择颜色
        int backgroundColor = isDarkMode ? Color.DKGRAY : Color.WHITE;
        int borderColor = isDarkMode ? Color.LTGRAY : Color.GRAY;
        int rippleColor = isDarkMode ? 0x40FFFFFF : 0x20000000;
        int pressedColor = isDarkMode ? Color.GRAY : Color.LTGRAY;

        GradientDrawable borderDrawable = new GradientDrawable();
        borderDrawable.setShape(GradientDrawable.RECTANGLE);
        borderDrawable.setColor(backgroundColor);
        borderDrawable.setStroke(borderWidthPx, borderColor);
        borderDrawable.setCornerRadius(cornerRadiusPx);

        // 2. 点击效果
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(rippleColor), borderDrawable, null);
            textView.setBackground(rippleDrawable);
        } else {
            // 低版本：StateListDrawable
            GradientDrawable pressedDrawable = (GradientDrawable) borderDrawable.getConstantState().newDrawable().mutate();
            pressedDrawable.setColor(pressedColor);
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
            stateListDrawable.addState(new int[]{}, borderDrawable);
            textView.setBackground(stateListDrawable);
        }
    }

    private void setupColorViewBorder(ImageView colorView, int color) {
        if (colorView == null) return;
        Context context = colorView.getContext();
        // 定义边框参数（单位 dp 转 px）
        int borderWidthPx = dpToPx(1, context);      // 边框宽度
        int cornerRadiusPx = dpToPx(8, context);     // 圆角半径
        int borderColor = isDarkModeEnabled(context) ? Color.LTGRAY : Color.GRAY;                // 边框颜色

        // 创建圆角矩形背景
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);             // 填充色（原有颜色）
        drawable.setStroke(borderWidthPx, borderColor); // 边框
        drawable.setCornerRadius(cornerRadiusPx);    // 圆角半径

        // 应用到 ImageView
        colorView.setBackground(drawable);
    }

    private void setupTextStyleButton(TextView button, boolean initialState) {
        if (button == null) return;
        // 获取当前主题模式
        boolean isDarkMode = isDarkModeEnabled(button.getContext());

        // 根据主题模式定义颜色（示例：选中时高亮色，未选中时普通色）
        int normalColor = isDarkMode ? Color.WHITE : Color.BLACK;      // 未选中颜色
        int selectedColor = isDarkMode ? Color.YELLOW : Color.BLUE;    // 选中颜色（可自定义）

        // 创建状态颜色列表
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_selected },  // 选中状态
                new int[] {}                                   // 默认状态（未选中）
        };
        int[] colors = new int[] { selectedColor, normalColor };
        ColorStateList colorStateList = new ColorStateList(states, colors);

        button.setTextColor(colorStateList);
        button.setSelected(initialState);
    }

    // 判断是否启用深色模式（API 29+ 支持系统深色模式）
    private boolean isDarkModeEnabled(Context context) {
        if (context == null) return false;
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    // dp 转 px 工具方法
    private int dpToPx(int dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    // 边距调整方法
    private void changeMargin(EditText editText, int delta, boolean isHorizontal) {
        String str = editText.getText().toString();
        try {
            int value = Integer.parseInt(str.isEmpty() ? "0" : str);
            int newValue = value + delta;
            if (newValue < 0) newValue = 0;
            if (newValue > maxMargin) newValue = maxMargin;
            editText.setText(String.valueOf(newValue));
            if (isHorizontal) {
                if (editText.getId() == R.id.marginLeftEdit) currentMarginLeft = newValue;
                else if (editText.getId() == R.id.marginRightEdit) currentMarginRight = newValue;
            } else {
                if (editText.getId() == R.id.marginTopEdit) currentMarginTop = newValue;
                else if (editText.getId() == R.id.marginBottomEdit) currentMarginBottom = newValue;
            }
            if (listener != null) listener.onMarginsChanged(currentMarginLeft, currentMarginTop, currentMarginRight, currentMarginBottom);
        } catch (NumberFormatException e) {
            Toast.makeText(editText.getContext(), "Invalid margin value", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMarginFromInput(EditText editText, boolean isHorizontal) {
        String str = editText.getText().toString();
        try {
            int value = Integer.parseInt(str);
            if (value < 0) value = 0;
            if (value > maxMargin) value = maxMargin;
            editText.setText(String.valueOf(value));
            if (isHorizontal) {
                if (editText.getId() == R.id.marginLeftEdit) currentMarginLeft = value;
                else if (editText.getId() == R.id.marginRightEdit) currentMarginRight = value;
            } else {
                if (editText.getId() == R.id.marginTopEdit) currentMarginTop = value;
                else if (editText.getId() == R.id.marginBottomEdit) currentMarginBottom = value;
            }
            if (listener != null) listener.onMarginsChanged(currentMarginLeft, currentMarginTop, currentMarginRight, currentMarginBottom);
        } catch (NumberFormatException e) {
            editText.setText(isHorizontal ?
                    (editText.getId() == R.id.marginLeftEdit ? String.valueOf(currentMarginLeft) : String.valueOf(currentMarginRight)) :
                    (editText.getId() == R.id.marginTopEdit ? String.valueOf(currentMarginTop) : String.valueOf(currentMarginBottom)));
        }
    }

    private void changeSize(EditText editText, float delta, boolean fluctuational) {
        String str = editText.getText().toString().trim();
        try {
            String newStr = calculateValue(str, delta, 0, fluctuational ? maxFluctuation : maxSize);
            if (!newStr.equals(str)) {
                editText.setText(newStr);
                float newSize = Float.parseFloat(newStr);
                if (listener != null) {
                    if (fluctuational) {
                        listener.onTextSizeFluctuationChanged(newSize);
                    } else {
                        listener.onTextSizeChanged(newSize);
                    }
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(editText.getContext(), "输入格式错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSizeFromInput(EditText editText, boolean fluctuational) {
        String str = editText.getText().toString();
        float currentValue = fluctuational ? currentSizeFluctuation : currentSize;
        try {
            float value = Float.parseFloat(str);
            if (value < 0) value = 0;
            float max = (fluctuational ? maxFluctuation : maxSize);
            if (value > max) value = max;
            if (currentValue != value) {
                editText.setText(String.valueOf(value));
                if (fluctuational) {
                    currentSizeFluctuation = value;
                    if (listener != null) listener.onTextSizeFluctuationChanged(value);
                } else {
                    currentSize = value;
                    if (listener != null) listener.onTextSizeChanged(value);
                }
            } else {
                editText.setText(String.valueOf(currentValue));
            }
        } catch (NumberFormatException e) {
            editText.setText(String.valueOf(currentValue));
        }
    }

    private void updateCurrentSpacingValue(float newValue, SpacingType spacingType) {
        if (listener != null) {
            switch (spacingType) {
                case spacing:
                    currentSpacing = newValue;
                    listener.onSpacingChanged(newValue);
                    break;
                case spacingFluctuation:
                    currentSpacingFluctuation = newValue;
                    listener.onSpacingFluctuationChanged(newValue);
                    break;
                case lineSpacing:
                    currentLineSpacing = newValue;
                    listener.onLineSpacingChanged(newValue);
                    break;
                case lineSpacingFluctuation:
                    currentLineSpacingFluctuation = newValue;
                    listener.onLineSpacingFluctuationChanged(newValue);
                    break;
            }
        }
    }

    private void changeSpacing(EditText editText, float delta, SpacingType spacingType) {
        String str = editText.getText().toString().trim();
        try {
            float max = spacingType == SpacingType.spacingFluctuation
                    || spacingType == SpacingType.lineSpacingFluctuation ? maxFluctuation : maxSpacing;
            String newStr = calculateValue(str, delta, 0, max);
            if (!newStr.equals(str)) {
                editText.setText(newStr);
                updateCurrentSpacingValue(Float.parseFloat(newStr), spacingType);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(editText.getContext(), "输入格式错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSpacingFromInput(EditText editText, SpacingType spacingType) {
        String str = editText.getText().toString();
        float currentValue = 0;
        float max = 0;
        switch (spacingType) {
            case spacing:
                currentValue = currentSpacing;
                max = maxSpacing;
                break;
            case spacingFluctuation:
                currentValue = currentSpacingFluctuation;
                max = maxFluctuation;
                break;
            case lineSpacing:
                currentValue = currentLineSpacing;
                max = maxSpacing;
                break;
            case lineSpacingFluctuation:
                currentValue = currentLineSpacingFluctuation;
                max = maxFluctuation;
                break;
        }
        try {
            float value = Float.parseFloat(str);
            if (value < 0) value = 0;
            if (value > max) value = max;
            if (currentValue != value) {
                editText.setText(String.valueOf(value));
                updateCurrentSpacingValue(value, spacingType);
            } else {
                editText.setText(String.valueOf(currentValue));
            }
        } catch (NumberFormatException e) {
            editText.setText(String.valueOf(currentValue));
        }
    }

    private void setupStrokeAngleChangeView(View view, int editId, int minusId, int plusId, float currentValue, float delta,
                                            boolean isStrokeWidth) {
        EditText editText = view.findViewById(editId);
        TextView minus = view.findViewById(minusId);
        TextView plus = view.findViewById(plusId);
        setupAdjustButtonBackground(minus);
        setupAdjustButtonBackground(plus);
        editText.setText(String.valueOf(currentValue));
        minus.setOnClickListener(v -> changeStrokeWidthOrAngle(editText, -delta, isStrokeWidth));
        plus.setOnClickListener(v -> changeStrokeWidthOrAngle(editText, delta, isStrokeWidth));
        editText.setOnFocusChangeListener((v, hasFocus) -> updateStrokeWidthOrAngleFromInput(editText, isStrokeWidth));
    }

    private void changeStrokeWidthOrAngle(EditText editText, float delta, boolean isStrokeWidth) {
        String str = editText.getText().toString().trim();
        try {
            String newStr = calculateValue(str, delta, 0, maxFluctuation);
            if (!newStr.equals(str)) {
                editText.setText(newStr);
                float newValue = Float.parseFloat(newStr);
                if (listener != null) {
                    if (isStrokeWidth) {
                        listener.onTextStrokeWidthChanged(newValue);
                    } else {
                        listener.onRotationFluctuationChanged(newValue);
                    }
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(editText.getContext(), "输入格式错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStrokeWidthOrAngleFromInput(EditText editText, boolean isStrokeWidth) {
        String str = editText.getText().toString();
        float currentValue = isStrokeWidth ? currentTextStrokeWidth : currentRotationFluctuation;
        try {
            float value = Float.parseFloat(str);
            if (value < 0) value = 0;
            float max = maxFluctuation;
            if (value > max) value = max;
            if (currentValue != value) {
                editText.setText(String.valueOf(value));
                if (isStrokeWidth) {
                    currentTextStrokeWidth = value;
                    if (listener != null) listener.onTextStrokeWidthChanged(value);
                } else {
                    currentRotationFluctuation = value;
                    if (listener != null) listener.onRotationFluctuationChanged(value);
                }
            } else {
                editText.setText(String.valueOf(currentValue));
            }
        } catch (NumberFormatException e) {
            editText.setText(String.valueOf(currentValue));
        }
    }

    private void setupTextIndentChangeView(View view, int indentEdit, int indentMinus, int indentPlus, float indent, float delta) {
        EditText sizeEdit = view.findViewById(indentEdit);
        TextView sizeMinus = view.findViewById(indentMinus);
        TextView sizePlus = view.findViewById(indentPlus);
        setupAdjustButtonBackground(sizeMinus);
        setupAdjustButtonBackground(sizePlus);
        sizeEdit.setText(String.valueOf(indent));
        sizeMinus.setOnClickListener(v -> changeIndent(sizeEdit, -delta));
        sizePlus.setOnClickListener(v -> changeIndent(sizeEdit, delta));
        sizeEdit.setOnFocusChangeListener((v, hasFocus) -> updateIndentFromInput(sizeEdit));
    }

    private void changeIndent(EditText editText, float delta) {
        String str = editText.getText().toString().trim();
        try {
            String newStr = calculateValue(str, delta, 0, maxIndent);
            if (!newStr.equals(str)) {
                editText.setText(newStr);
                float newValue = Float.parseFloat(newStr);
                currentTextIndent = newValue;
                if (listener != null) {
                    listener.onTextIndentChanged(newValue);
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(editText.getContext(), "输入格式错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateIndentFromInput(EditText editText) {
        String str = editText.getText().toString();
        float currentValue = currentTextIndent;
        try {
            float value = Float.parseFloat(str);
            if (value < 0) value = 0;
            float max = maxIndent;
            if (value > max) value = max;
            if (currentValue != value) {
                currentTextIndent = value;
                editText.setText(String.valueOf(value));
                if (listener != null) listener.onTextIndentChanged(value);
            } else {
                editText.setText(String.valueOf(currentValue));
            }
        } catch (NumberFormatException e) {
            editText.setText(String.valueOf(currentValue));
        }
    }

    private void setupUnderlineChangeView(View view) {
        EditText underlineEdit = view.findViewById(R.id.underlineEdit);
        TextView underlineMinus = view.findViewById(R.id.underlineMinus);
        TextView underlinePlus = view.findViewById(R.id.underlinePlus);
        setupAdjustButtonBackground(underlineMinus);
        setupAdjustButtonBackground(underlinePlus);
        underlineEdit.setText(String.valueOf(currentUnderlineStrokeWidth));
        float delta = 1f;
        underlineMinus.setOnClickListener(v -> changeUnderline(underlineEdit, -delta));
        underlinePlus.setOnClickListener(v -> changeUnderline(underlineEdit, delta));
        underlineEdit.setOnFocusChangeListener((v, hasFocus) -> updateUnderlineFromInput(underlineEdit));
        int underlineColor = currentUnderlineColor;
        // 颜色选择 ImageView
        ImageView colorView = view.findViewById(R.id.underlineColorView);
        setupColorViewBorder(colorView, underlineColor);
        // 替换原来的颜色选择代码
        colorView.setOnClickListener(v -> new ColorPickerDialog()
                .withColor(underlineColor)           // 初始颜色
                .withAlphaEnabled(true)     // 启用透明度（支持十六进制带 Alpha）
                .withPresets(Color.BLACK, Color.RED, Color.BLUE) // 可选：预设颜色
                .withListener((pickerView, color) -> {
                    currentUnderlineColor = color;
                    setupColorViewBorder(colorView, color);
                    if (listener != null) listener.onUnderlineColorChanged(color);
                })
                .show(getChildFragmentManager(), "underlineColorPicker"));
        View paramView = view.findViewById(R.id.underlineParamLayout);
        boolean enable = currentUnderline;
        paramView.setVisibility(enable ? View.VISIBLE : View.GONE);
        // 复选框
        CheckBox boldCheck = view.findViewById(R.id.underlineCheck);
        boldCheck.setChecked(enable);
        boldCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onUnderlineChanged(isChecked, currentUnderlineStrokeWidth);
            paramView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            colorView.setEnabled(isChecked);
            underlineEdit.setEnabled(isChecked);
            underlineMinus.setEnabled(isChecked);
            underlinePlus.setEnabled(isChecked);
            currentUnderline = isChecked;
        });
    }

    private void changeUnderline(EditText editText, float delta) {
        String str = editText.getText().toString().trim();
        try {
            String newStr = calculateValue(str, delta, 0, maxFluctuation);
            if (!newStr.equals(str)) {
                editText.setText(newStr);
                float newValue = Float.parseFloat(newStr);
                currentUnderlineStrokeWidth = newValue;
                if (listener != null) {
                    listener.onUnderlineChanged(currentUnderline, newValue);
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(editText.getContext(), "输入格式错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUnderlineFromInput(EditText editText) {
        String str = editText.getText().toString();
        float currentStrokeWidth = currentUnderlineStrokeWidth;
        try {
            float value = Float.parseFloat(str);
            if (value < 0) value = 0;
            float max = maxFluctuation;
            if (value > max) value = max;
            if (currentStrokeWidth != value) {
                currentUnderlineStrokeWidth = value;
                editText.setText(String.valueOf(value));
                if (listener != null) listener.onUnderlineChanged(currentUnderline, value);
            } else {
                editText.setText(String.valueOf(currentStrokeWidth));
            }
        } catch (NumberFormatException e) {
            editText.setText(String.valueOf(currentStrokeWidth));
        }
    }

    private String calculateValue(String currentStr, float delta, float min, float max) throws NumberFormatException {
        BigDecimal current = new BigDecimal(currentStr.isEmpty() ? "0" : currentStr);
        BigDecimal deltaBD = new BigDecimal(Float.toString(delta));
        BigDecimal result = current.add(deltaBD);

        BigDecimal minBD = new BigDecimal(Float.toString(min));
        BigDecimal maxBD = new BigDecimal(Float.toString(max));

        if (result.compareTo(minBD) < 0) {
            result = minBD;
        } else if (result.compareTo(maxBD) > 0) {
            result = maxBD;
        }

        // 去除末尾零，转换为普通字符串（如 12.0 → "12"）
        return result.stripTrailingZeros().toPlainString();
    }

}