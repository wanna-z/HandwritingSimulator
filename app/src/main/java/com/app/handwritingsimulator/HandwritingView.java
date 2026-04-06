package com.app.handwritingsimulator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HandwritingView extends View {

    private String text = "";
    private final List<CharacterTransform> transforms = new ArrayList<>();

    // 随机参数范围
    private float MAX_ROTATION = 0.0f;
    private float MAX_SCALE = 0.0f;
    private float MAX_OFFSET_X = 0.0f;
    private float MAX_OFFSET_Y = 0.0f;
    private static final float MAX_EXTRA_SPACING = 0.0f;

    private Paint paint;
    private Paint borderPaint;
    private Paint cornerPaint;

    private Bitmap backgroundBitmap = null;
    private Paint underlinePaint;
    private final RectF backgroundArea = new RectF();

    // 可调整的绘制区域
    private final RectF drawArea = new RectF();
    private final float MIN_WIDTH = dpToPx(50);
    private final float MIN_HEIGHT = dpToPx(25);
    private final float CORNER_RADIUS = dpToPx(5);

    private final float TEXT_PADDING = dpToPx(10);

    // 触摸拖动状态
    private static final int NO_CORNER = -1;
    private static final int DRAG_MODE_MOVE = -2;
    private int draggingCorner = NO_CORNER;
    private float startTouchX, startTouchY;
    private RectF originalRectOnTouch;

    private boolean isEditing = true;
    private boolean lastIsEditing = true;

    // 字体相关属性
    private float textSize = 20;
    private float letterSpacing = 0;
    private float lineSpacing = 30f;
    private int textColor = 0xFF000000;
    private boolean isFakeBold = false;
    private int textStyle = Typeface.NORMAL;          // 统一样式：NORMAL, BOLD, ITALIC, BOLD_ITALIC
    private Typeface baseTypeface;                    // 原始手写字体
    private final Typeface defaultTypeface = Typeface.SERIF;
    private boolean underlineEnable = false;
    private float underlineStrokeWidth = 4;
    private int underlineColor = 0xFFAAAAAA;

    // 边距管理
    private float marginLeft = 50;
    private float marginTop = 50;
    private float marginRight = 50;
    private float marginBottom = 50;

    // 行首缩进
    private float textIndent = 20f;

    public HandwritingView(Context context) {
        super(context);
        init();
    }

    public HandwritingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setFakeBoldText(isFakeBold);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(0xFF888888);
        borderPaint.setStrokeWidth(2);
        borderPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10, 10}, 0));

        cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerPaint.setColor(0xFF3F51B5);
        cornerPaint.setStyle(Paint.Style.FILL);

        underlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        underlinePaint.setStyle(Paint.Style.STROKE);
        underlinePaint.setStrokeWidth(underlineStrokeWidth);
        underlinePaint.setColor(underlineColor);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void updateDrawAreaFromMargins() {
        if (getWidth() == 0 || getHeight() == 0) return;

        float left = marginLeft;
        float top = marginTop;
        float right = getWidth() - marginRight;
        float bottom = getHeight() - marginBottom;

        if (left < 0) left = 0;
        if (top < 0) top = 0;
        if (right > getWidth()) right = getWidth();
        if (bottom > getHeight()) bottom = getHeight();

        float width = right - left;
        float height = bottom - top;
        if (width < MIN_WIDTH) {
            if (right + MIN_WIDTH <= getWidth()) {
                right = left + MIN_WIDTH;
            } else {
                left = right - MIN_WIDTH;
            }
        }
        if (height < MIN_HEIGHT) {
            if (bottom + MIN_HEIGHT <= getHeight()) {
                bottom = top + MIN_HEIGHT;
            } else {
                top = bottom - MIN_HEIGHT;
            }
        }

        drawArea.set(left, top, right, bottom);
        marginLeft = drawArea.left;
        marginTop = drawArea.top;
        marginRight = getWidth() - drawArea.right;
        marginBottom = getHeight() - drawArea.bottom;

        invalidate();
    }

    private void syncMarginsFromDrawArea() {
        marginLeft = drawArea.left;
        marginTop = drawArea.top;
        marginRight = getWidth() - drawArea.right;
        marginBottom = getHeight() - drawArea.bottom;
    }

    public void setMargins(int left, int top, int right, int bottom) {
        marginLeft = Math.max(left, getPaddingLeft());
        marginTop = Math.max(top, getPaddingTop());
        marginRight = Math.max(right, getPaddingRight());
        marginBottom = Math.max(bottom, getPaddingBottom());
        updateDrawAreaFromMargins();
    }

    public int[] getMargins() {
        return new int[]{(int) marginLeft, (int) marginTop, (int) marginRight, (int) marginBottom};
    }

    public void setText(String newText) {
        if (newText == null) newText = "";
        this.text = newText;

        Random random = new Random(System.currentTimeMillis());
        transforms.clear();

        for (int i = 0; i < text.length(); i++) {
            float rotation = (random.nextFloat() * 2 - 1) * MAX_ROTATION;
            float scale = 1.0f + (random.nextFloat() * 2 - 1) * MAX_SCALE;
            float offsetX = (random.nextFloat() * 2 - 1) * MAX_OFFSET_X;
            float offsetY = (random.nextFloat() * 2 - 1) * MAX_OFFSET_Y;
            float spacing = random.nextFloat() * MAX_EXTRA_SPACING;
            transforms.add(new CharacterTransform(rotation, scale, offsetX, offsetY, spacing));
        }

        invalidate();
    }

    public String getCurrentText() {
        return text;
    }

    public void setTypeface(Typeface typeface) {
        baseTypeface = typeface;
        applyTypefaceStyle();
        invalidate();
    }

    // 统一应用样式到画笔
    private void applyTypefaceStyle() {
        if (baseTypeface != null) {
            paint.setTypeface(Typeface.create(baseTypeface, textStyle));
        } else {
            paint.setTypeface(Typeface.create(defaultTypeface, textStyle));
        }
    }

    public void setBackgroundImage(Bitmap bitmap) {
        this.backgroundBitmap = bitmap;
        invalidate();
    }

    // ---------- 字体属性 ----------
    public void setTextSize(float textSize) {
        this.textSize = textSize;
        paint.setTextSize(textSize);
        invalidate();
    }

    public float getTextSize() {
        return textSize;
    }

    public void setLetterSpacing(float letterSpacing) {
        this.letterSpacing = letterSpacing;
        invalidate();
    }

    public float getLetterSpacing() {
        return letterSpacing;
    }

    public void setLineSpacing(float multiplier) {
        this.lineSpacing = multiplier;
        invalidate();
    }

    public float getLineSpacing() {
        return lineSpacing;
    }

    public void setTextColor(int color) {
        this.textColor = color;
        paint.setColor(color);
        invalidate();
    }

    public int getTextColor() {
        return textColor;
    }

    // 设置粗体（使用 setFakeBold 替代）
    public void setTextBold(boolean bold) {
        if (isFakeBold != bold) {
            isFakeBold = bold;
            paint.setFakeBoldText(bold);
            invalidate();
        }
    }

    public boolean isTextBold() {
        return isFakeBold;
    }

    // 设置斜体（不改变粗体状态）
    public void setTextItalic(boolean italic) {
        if (italic) {
            textStyle |= Typeface.ITALIC;
        } else {
            textStyle &= ~Typeface.ITALIC;
        }
        applyTypefaceStyle();
        invalidate();
    }

    public boolean isTextItalic() {
        return (textStyle & Typeface.ITALIC) != 0;
    }

    public void setTextStyle(int style) {
        this.textStyle = style;
        applyTypefaceStyle();
        invalidate();
    }

    public int getTextStyle() {
        return textStyle;
    }

    public void setTextStrokeWidth(float width) {
        if (width > 0) {
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(width);
        } else {
            paint.setStyle(Paint.Style.FILL);
        }
        invalidate();
    }

    public float getTextStrokeWidth() {
        return paint.getStrokeWidth();
    }

    public boolean isUnderlineEnable() {
        return underlineEnable;
    }

    public void setUnderlineEnable(boolean enable) {
        this.underlineEnable = enable;
        invalidate();
    }

    public void setUnderlineColor(int color) {
        this.underlineColor = color;
        underlinePaint.setColor(color);
        invalidate();
    }

    public int getUnderlineColor() {
        return underlineColor;
    }

    public float getUnderlineStrokeWidth() {
        return underlineStrokeWidth;
    }

    public void setUnderlineStrokeWidth(float strokeWidth) {
        this.underlineStrokeWidth = strokeWidth;
        underlinePaint.setStrokeWidth(strokeWidth);
        invalidate();
    }

    public void setUnderlineParameter(boolean enable, float strokeWidth) {
        this.underlineEnable = enable;
        this.underlineStrokeWidth = strokeWidth;
        underlinePaint.setStrokeWidth(strokeWidth);
        invalidate();
    }

    private void resetTextState() {
        if (text == null) return;
        Random random = new Random(System.currentTimeMillis());
        transforms.clear();

        for (int i = 0; i < text.length(); i++) {
            float rotation = (random.nextFloat() * 2 - 1) * MAX_ROTATION;
            float scale = 1.0f + (random.nextFloat() * 2 - 1) * MAX_SCALE;
            float offsetX = (random.nextFloat() * 2 - 1) * MAX_OFFSET_X;
            float offsetY = (random.nextFloat() * 2 - 1) * MAX_OFFSET_Y;
            float spacing = random.nextFloat() * MAX_EXTRA_SPACING;
            transforms.add(new CharacterTransform(rotation, scale, offsetX, offsetY, spacing));
        }
    }

    public float getMaxRotation() {
        return MAX_ROTATION;
    }

    public void setMaxRotation(float rotation) {
        this.MAX_ROTATION = rotation;
        resetTextState();
        invalidate();
    }

    public float getMaxScale() {
        return MAX_SCALE;
    }

    public void setMaxScale(float scale) {
        this.MAX_SCALE = scale;
        resetTextState();
        invalidate();
    }

    public float getMaxOffsetX() {
        return MAX_OFFSET_X;
    }

    public void setMaxOffsetX(float offsetX) {
        this.MAX_OFFSET_X = offsetX;
        resetTextState();
        invalidate();
    }

    public float getMaxOffsetY() {
        return MAX_OFFSET_Y;
    }

    public void setMaxOffsetY(float offsetY) {
        this.MAX_OFFSET_Y = offsetY;
        resetTextState();
        invalidate();
    }

    public float getTextIndent() {
        return textIndent;
    }

    public void setTextIndent(float indent) {
        this.textIndent = indent;
        invalidate();
    }

    /**
     * 导出当前视图内容为图片（不包含编辑边框和角点）
     * @return Bitmap 图片，若视图尺寸无效则返回 null
     */
    public Bitmap exportAsBitmap() {
        return exportAsBitmap(false, 1f);
    }

    /**
     * 导出当前视图内容为图片
     * @param includeEditingElements 是否包含编辑边框和角点
     * @param scale 导出图片的缩放比例（例如 1.0 为原尺寸，2.0 为两倍大小）
     * @return Bitmap 图片
     */
    public Bitmap exportAsBitmap(boolean includeEditingElements, float scale) {
        // 有背景且背景有效时，以背景原始尺寸导出，再乘以 scale
        if (backgroundBitmap != null && !backgroundBitmap.isRecycled() && !backgroundArea.isEmpty()) {
            int bgWidth = backgroundBitmap.getWidth();
            int bgHeight = backgroundBitmap.getHeight();
            int targetWidth = (int) (bgWidth * scale);
            int targetHeight = (int) (bgHeight * scale);
            Bitmap result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            // 绘制背景（拉伸至目标尺寸）
            canvas.drawBitmap(backgroundBitmap, null, new RectF(0, 0, targetWidth, targetHeight), null);
            // 构建变换矩阵：将屏幕坐标系中的绘制区域映射到背景原始尺寸，再缩放到目标尺寸
            Matrix transform = new Matrix();
            transform.postTranslate(-backgroundArea.left, -backgroundArea.top);
            transform.postScale(bgWidth / backgroundArea.width(), bgHeight / backgroundArea.height());
            transform.postScale(scale, scale);
            canvas.concat(transform);
            if (includeEditingElements) drawEditingElementsOnCanvas(canvas);
            drawContentOnCanvas(canvas);
            return result;
        } else {
            // 无背景时按视图尺寸导出
            if (getWidth() <= 0 || getHeight() <= 0) return null;
            int width = (int) (getWidth() * scale);
            int height = (int) (getHeight() * scale);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.scale(scale, scale);
            if (includeEditingElements) {
                draw(canvas);
            } else {
                boolean originalEditing = isEditing;
                isEditing = false;
                draw(canvas);
                isEditing = originalEditing;
            }
            return bitmap;
        }
    }

    public boolean hasBackground() {
        return backgroundBitmap != null && !backgroundBitmap.isRecycled();
    }

    public Bitmap getBackgroundBitmap() {
        return backgroundBitmap;
    }

    public void adjustDrawAreaWithBackground() {
        if (backgroundBitmap != null && !backgroundBitmap.isRecycled()) {
            // 可用区域（考虑 padding）
            int availableLeft = getPaddingLeft();
            int availableTop = getPaddingTop();
            int rightPadding = getPaddingRight();
            int bottomPadding = getPaddingBottom();

            int availableRight = getWidth() - rightPadding;
            int availableBottom = getHeight() - bottomPadding;

            int availableWidth = availableRight - availableLeft;
            int availableHeight = availableBottom - availableTop;

            // 原始图片尺寸
            int bitmapWidth = backgroundBitmap.getWidth();
            int bitmapHeight = backgroundBitmap.getHeight();

            // 计算缩放比例（fitCenter）
            float scale = Math.min((float) availableWidth / bitmapWidth, (float) availableHeight / bitmapHeight);

            // 缩放后的实际绘制尺寸
            int drawWidth = (int) (bitmapWidth * scale);
            int drawHeight = (int) (bitmapHeight * scale);

            // 居中偏移
            int drawLeft = availableLeft + (availableWidth - drawWidth) / 2;
            int drawTop = availableTop + (availableHeight - drawHeight) / 2;

            // 目标矩形
            drawArea.set(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight);
            invalidate();
        }
    }

    // 绘制编辑元素（边框和角点）
    private void drawEditingElementsOnCanvas(Canvas canvas) {
        canvas.drawRect(drawArea, borderPaint);
        float[] corners = {
                drawArea.left, drawArea.top,
                drawArea.right, drawArea.top,
                drawArea.right, drawArea.bottom,
                drawArea.left, drawArea.bottom
        };
        for (int i = 0; i < 4; i++) {
            canvas.drawCircle(corners[i * 2], corners[i * 2 + 1], CORNER_RADIUS, cornerPaint);
        }
    }

    // 绘制文本、下划线
    private void drawContentOnCanvas(Canvas canvas) {
        if (text.isEmpty()) return;
        float paddingLeft = TEXT_PADDING;
        float paddingRight = TEXT_PADDING;
        float paddingTop = 0;
        float paddingBottom = 0;

        Paint.FontMetrics fm = paint.getFontMetrics();
        float lineHeight = lineSpacing;
        float topY = drawArea.top + paddingTop;

        float baselineOffset = lineHeight - fm.descent;

        float startX = drawArea.left + paddingLeft;
        float endX = drawArea.right - paddingRight;
        int rowIndex = 0;
        float startWithIndentX = startX + textIndent;
        float currentX = startWithIndentX;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch == '\n') {
                rowIndex++;
                currentX = startWithIndentX;
                if (topY + lineHeight * (rowIndex + 1) > drawArea.bottom - paddingBottom) {
                    rowIndex--;
                    break;
                }
                continue;
            }

            String chStr = String.valueOf(ch);
            CharacterTransform t = transforms.get(i);
            float rawWidth = paint.measureText(chStr);
            float charWidth = rawWidth * t.scale;

            if (currentX + charWidth > endX) {
                rowIndex++;
                currentX = startX;
                if (topY + lineHeight * (rowIndex + 1) > drawArea.bottom - paddingBottom) {
                    rowIndex--;
                    break;
                }
            }

            float baselineY = topY + lineHeight * rowIndex + baselineOffset;

            canvas.save();
            canvas.translate(currentX + t.offsetX, baselineY + t.offsetY);
            canvas.rotate(t.rotation);
            canvas.scale(t.scale, t.scale);
            canvas.drawText(chStr, 0, 0, paint);
            canvas.restore();

            currentX += charWidth + letterSpacing + t.spacing;
        }

        if (isUnderlineEnable()) {
            for (int row = 1; row <= rowIndex + 1; row++) {
                float y = topY + lineHeight * row;
                canvas.drawLine(startX, y, endX, y, underlinePaint);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateDrawAreaFromMargins();
    }

    // ---------- 绘制 ----------
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (backgroundBitmap != null && !backgroundBitmap.isRecycled()) {
            // 可用区域（考虑 padding）
            int availableLeft = getPaddingLeft();
            int availableTop = getPaddingTop();
            int rightPadding = getPaddingRight();
            int bottomPadding = getPaddingBottom();

            int availableRight = getWidth() - rightPadding;
            int availableBottom = getHeight() - bottomPadding;

            int availableWidth = availableRight - availableLeft;
            int availableHeight = availableBottom - availableTop;

            // 原始图片尺寸
            int bitmapWidth = backgroundBitmap.getWidth();
            int bitmapHeight = backgroundBitmap.getHeight();

            // 计算缩放比例（fitCenter）
            float scale = Math.min((float) availableWidth / bitmapWidth, (float) availableHeight / bitmapHeight);

            // 缩放后的实际绘制尺寸
            int drawWidth = (int) (bitmapWidth * scale);
            int drawHeight = (int) (bitmapHeight * scale);

            // 居中偏移
            int drawLeft = availableLeft + (availableWidth - drawWidth) / 2;
            int drawTop = availableTop + (availableHeight - drawHeight) / 2;

            // 目标矩形
            backgroundArea.set(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight);

            canvas.drawBitmap(backgroundBitmap, null, backgroundArea, null);
        }

        if (isEditing) drawEditingElementsOnCanvas(canvas);

        if (text.isEmpty()) return;

        drawContentOnCanvas(canvas);
    }

    // ---------- 触摸交互 ----------
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                int corner = getNearCorner(x, y);
                boolean inside = drawArea.contains(x, y);
                lastIsEditing = isEditing;

                if (corner != NO_CORNER || inside) {
                    isEditing = true;

                    if (corner != NO_CORNER) {
                        draggingCorner = corner;
                    } else {
                        draggingCorner = DRAG_MODE_MOVE;
                    }
                    startTouchX = x;
                    startTouchY = y;
                    originalRectOnTouch = new RectF(drawArea);
                } else {
                    isEditing = false;
                    draggingCorner = NO_CORNER;
                    originalRectOnTouch = null;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (lastIsEditing && draggingCorner != NO_CORNER && originalRectOnTouch != null) {
                    float dx = x - startTouchX;
                    float dy = y - startTouchY;

                    if (draggingCorner == DRAG_MODE_MOVE) {
                        float originalWidth = originalRectOnTouch.width();
                        float originalHeight = originalRectOnTouch.height();

                        float newLeft = originalRectOnTouch.left + dx;
                        float newTop = originalRectOnTouch.top + dy;
                        float newRight = newLeft + originalWidth;
                        float newBottom = newTop + originalHeight;

                        if (newLeft < 0) {
                            newLeft = 0;
                            newRight = originalWidth;
                        }
                        if (newTop < 0) {
                            newTop = 0;
                            newBottom = originalHeight;
                        }
                        if (newRight > getWidth()) {
                            newRight = getWidth();
                            newLeft = newRight - originalWidth;
                        }
                        if (newBottom > getHeight()) {
                            newBottom = getHeight();
                            newTop = newBottom - originalHeight;
                        }

                        drawArea.set(newLeft, newTop, newRight, newBottom);
                    } else {
                        RectF newRect = new RectF(originalRectOnTouch);
                        switch (draggingCorner) {
                            case 0:
                                newRect.left += dx;
                                newRect.top += dy;
                                break;
                            case 1:
                                newRect.right += dx;
                                newRect.top += dy;
                                break;
                            case 2:
                                newRect.right += dx;
                                newRect.bottom += dy;
                                break;
                            case 3:
                                newRect.left += dx;
                                newRect.bottom += dy;
                                break;
                        }

                        if (newRect.left < 0) newRect.left = 0;
                        if (newRect.top < 0) newRect.top = 0;
                        if (newRect.right > getWidth()) newRect.right = getWidth();
                        if (newRect.bottom > getHeight()) newRect.bottom = getHeight();

                        if (newRect.width() < MIN_WIDTH) {
                            if (draggingCorner == 0 || draggingCorner == 3)
                                newRect.left = newRect.right - MIN_WIDTH;
                            else
                                newRect.right = newRect.left + MIN_WIDTH;
                        }
                        if (newRect.height() < MIN_HEIGHT) {
                            if (draggingCorner == 0 || draggingCorner == 1)
                                newRect.top = newRect.bottom - MIN_HEIGHT;
                            else
                                newRect.bottom = newRect.top + MIN_HEIGHT;
                        }

                        drawArea.set(newRect);
                    }
                    syncMarginsFromDrawArea();
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                draggingCorner = NO_CORNER;
                originalRectOnTouch = null;
                if (lastIsEditing != isEditing) invalidate();
                break;
        }
        return super.onTouchEvent(event);
    }

    private int getNearCorner(float x, float y) {
        float[] corners = {
                drawArea.left, drawArea.top,
                drawArea.right, drawArea.top,
                drawArea.right, drawArea.bottom,
                drawArea.left, drawArea.bottom
        };
        for (int i = 0; i < 4; i++) {
            float cx = corners[i * 2];
            float cy = corners[i * 2 + 1];
            float distance = (float) Math.hypot(x - cx, y - cy);
            if (distance <= CORNER_RADIUS * 2) {
                return i;
            }
        }
        return NO_CORNER;
    }

    private static class CharacterTransform {
        float rotation;
        float scale;
        float offsetX;
        float offsetY;
        float spacing;

        CharacterTransform(float rotation, float scale, float offsetX, float offsetY, float spacing) {
            this.rotation = rotation;
            this.scale = scale;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.spacing = spacing;
        }
    }
}