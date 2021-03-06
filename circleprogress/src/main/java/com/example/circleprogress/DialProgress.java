package com.example.circleprogress;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.example.circleprogress.utils.Constant;
import com.example.circleprogress.utils.MiscUtil;

/**
 * Created by LiuBin on 2017/6/3 12:01.
 */

public class DialProgress extends View {

    private static final String TAG = DialProgress.class.getSimpleName();
    private Context mContext;
    private boolean antiAlias;

    //圆心坐标
    private Point mCenterPoint;
    private float mRadius;
    private float mTextOffsetPercentInRadius;

    //绘制提示
    private TextPaint mHintPaint;
    private CharSequence mHint;
    private int mHintColor;
    private float mHintSize;
    private float mHintOffset;

    //绘制数值
    private Paint mValuePaint;
    private int mValueColor;
    private float mMaxValue;
    private float mValue;
    private float mValueSize;
    private float mValueOffset;
    private String mPrecisionFormat;

    //绘制单位
    private Paint mUnitPaint;
    private float mUnitSize;
    private int mUnitColor;
    private float mUnitOffset;
    private CharSequence mUnit;

    //前景圆弧
    private Paint mArcPaint;
    private float mArcWidth;
    private int mDialIntervalDegree;
    private float mStartAngle, mSweepAngle;
    private RectF mRectF;

    //渐变
    private int[] mGradientColors = {Color.GREEN, Color.YELLOW, Color.RED};
    //当前进度，[0.0f,1.0f]
    private float mPercent;
    //动画时间
    private long mAnimTime;
    //属性动画
    private ValueAnimator mAnimator;

    //背景圆弧
    private Paint mBgArcPaint;
    private int mBgArcColor;

    //刻度线颜色
    private Paint mDialPaint;
    private float mDialWidth;
    private int mDialColor;

    private int mDefaultSize;

    public DialProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mContext = context;
        mCenterPoint = new Point();
        mDefaultSize = MiscUtil.dipToPx(context, Constant.DEFAULT_SIZE);
        mRectF = new RectF();
        initConfig(context, attrs);
        initPaint();
        setValue(mValue);
    }

    private void initConfig(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DialProgress);
        antiAlias = typedArray.getBoolean(R.styleable.DialProgress_antiAlias, Constant.ANTI_ALIAS);
        mMaxValue = typedArray.getFloat(R.styleable.DialProgress_maxValue, Constant.DEFAULT_MAX_VALUE);
        mValue = typedArray.getFloat(R.styleable.DialProgress_value, Constant.DEFAULT_VALUE);
        mValueSize = typedArray.getDimension(R.styleable.DialProgress_valueSize, Constant.DEFAULT_VALUE_SIZE);
        mValueColor = typedArray.getColor(R.styleable.DialProgress_valueColor, Color.BLACK);
        mDialIntervalDegree = typedArray.getInt(R.styleable.DialProgress_dialIntervalDegree, 10);
        int precision = typedArray.getInt(R.styleable.DialProgress_precision, 0);
        mPrecisionFormat = MiscUtil.getPrecisionFormat(precision);

        mUnit = typedArray.getString(R.styleable.DialProgress_unit);
        mUnitColor = typedArray.getColor(R.styleable.DialProgress_unitColor, Color.BLACK);
        mUnitSize = typedArray.getDimension(R.styleable.DialProgress_unitSize, Constant.DEFAULT_UNIT_SIZE);

        mHint = typedArray.getString(R.styleable.DialProgress_hint);
        mHintColor = typedArray.getColor(R.styleable.DialProgress_hintColor, Color.BLACK);
        mHintSize = typedArray.getDimension(R.styleable.DialProgress_hintSize, Constant.DEFAULT_HINT_SIZE);

        mArcWidth = typedArray.getDimension(R.styleable.DialProgress_arcWidth, Constant.DEFAULT_ARC_WIDTH);
        mStartAngle = typedArray.getFloat(R.styleable.DialProgress_startAngle, Constant.DEFAULT_START_ANGLE);
        mSweepAngle = typedArray.getFloat(R.styleable.DialProgress_sweepAngle, Constant.DEFAULT_SWEEP_ANGLE);

        mAnimTime = typedArray.getInt(R.styleable.DialProgress_animTime, Constant.DEFAULT_ANIM_TIME);

        mBgArcColor = typedArray.getColor(R.styleable.DialProgress_bgArcColor, Color.GRAY);
        mDialWidth = typedArray.getDimension(R.styleable.DialProgress_dialWidth, 2);
        mDialColor = typedArray.getColor(R.styleable.DialProgress_dialColor, Color.WHITE);

        mTextOffsetPercentInRadius = typedArray.getFloat(R.styleable.DialProgress_textOffsetPercentInRadius, 0.33f);

        int gradientArcColors = typedArray.getResourceId(R.styleable.DialProgress_arcColors, 0);
        if (gradientArcColors != 0) {
            try {
                int[] gradientColors = getResources().getIntArray(gradientArcColors);
                if (gradientColors.length == 0) {
                    int color = getResources().getColor(gradientArcColors);
                    mGradientColors = new int[2];
                    mGradientColors[0] = color;
                    mGradientColors[1] = color;
                } else if (gradientColors.length == 1) {
                    mGradientColors = new int[2];
                    mGradientColors[0] = gradientColors[0];
                    mGradientColors[1] = gradientColors[0];
                } else {
                    mGradientColors = gradientColors;
                }
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }
        typedArray.recycle();
    }

    private void initPaint() {
        mHintPaint = new TextPaint();
        // 设置抗锯齿,会消耗较大资源，绘制图形速度会变慢。
        mHintPaint.setAntiAlias(antiAlias);
        // 设置绘制文字大小
        mHintPaint.setTextSize(mHintSize);
        // 设置画笔颜色
        mHintPaint.setColor(mHintColor);
        // 从中间向两边绘制，不需要再次计算文字
        mHintPaint.setTextAlign(Paint.Align.CENTER);

        mValuePaint = new Paint();
        mValuePaint.setAntiAlias(antiAlias);
        mValuePaint.setTextSize(mValueSize);
        mValuePaint.setColor(mValueColor);
        mValuePaint.setTypeface(Typeface.DEFAULT_BOLD);
        mValuePaint.setTextAlign(Paint.Align.CENTER);

        mUnitPaint = new Paint();
        mUnitPaint.setAntiAlias(antiAlias);
        mUnitPaint.setTextSize(mUnitSize);
        mUnitPaint.setColor(mUnitColor);
        mUnitPaint.setTextAlign(Paint.Align.CENTER);

        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(antiAlias);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mArcWidth);
        mArcPaint.setStrokeCap(Paint.Cap.BUTT);

        mBgArcPaint = new Paint();
        mBgArcPaint.setAntiAlias(antiAlias);
        mBgArcPaint.setStyle(Paint.Style.STROKE);
        mBgArcPaint.setStrokeWidth(mArcWidth);
        mBgArcPaint.setStrokeCap(Paint.Cap.BUTT);
        mBgArcPaint.setColor(mBgArcColor);

        mDialPaint = new Paint();
        mDialPaint.setAntiAlias(antiAlias);
        mDialPaint.setColor(mDialColor);
        mDialPaint.setStrokeWidth(mDialWidth);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int minSize = Math.min(w - getPaddingLeft() - getPaddingRight() - 2 * (int) mArcWidth,
                h - getPaddingTop() - getPaddingBottom() - 2 * (int) mArcWidth);
        mRadius = minSize / 2;
        mCenterPoint.x = w / 2;
        mCenterPoint.y = h / 2;

        mRectF.left = mCenterPoint.x - mRadius - mArcWidth / 2;
        mRectF.top = mCenterPoint.y - mRadius - mArcWidth / 2;
        mRectF.right = mCenterPoint.x + mRadius + mArcWidth / 2;
        mRectF.bottom = mCenterPoint.y + mRadius + mArcWidth / 2;

        mValueOffset = mCenterPoint.y + getBaselineOffsetFromY(mValuePaint);
        mHintOffset = mCenterPoint.y + getBaselineOffsetFromY(mHintPaint) - mRadius * mTextOffsetPercentInRadius;
        mUnitOffset = mCenterPoint.y + getBaselineOffsetFromY(mUnitPaint) + mRadius * mTextOffsetPercentInRadius;

        updateArcPaint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawArc(canvas);
        drawDial(canvas);
        drawText(canvas);
    }

    private void drawArc(Canvas canvas) {
        float currentAngle = mPercent * mSweepAngle;
        canvas.save();
        canvas.rotate(mStartAngle, mCenterPoint.x, mCenterPoint.y);
        canvas.drawArc(mRectF, currentAngle, mSweepAngle - currentAngle, false, mBgArcPaint);
        canvas.drawArc(mRectF, 0, currentAngle, false, mArcPaint);
        canvas.restore();
    }

    private void drawDial(Canvas canvas) {
        int total = (int) (mSweepAngle / mDialIntervalDegree);
        canvas.save();
        canvas.rotate(mStartAngle, mCenterPoint.x, mCenterPoint.y);
        for (int i = 0; i <= total; i++) {
            canvas.drawLine(mCenterPoint.x + mRadius, mCenterPoint.y,
                    mCenterPoint.x + mRadius + mArcWidth, mCenterPoint.y, mDialPaint);
            canvas.rotate(mDialIntervalDegree, mCenterPoint.x, mCenterPoint.y);
        }
        canvas.restore();
    }

    private void drawText(Canvas canvas) {
        canvas.drawText(String.format(mPrecisionFormat, mValue), mCenterPoint.x, mValueOffset, mValuePaint);
        if (mUnit != null) {
            canvas.drawText(mUnit.toString(), mCenterPoint.x, mUnitOffset, mUnitPaint);
        }
        if (mHint != null) {
            canvas.drawText(mHint.toString(), mCenterPoint.x, mHintOffset, mHintPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(MiscUtil.measure(widthMeasureSpec, mDefaultSize),
                MiscUtil.measure(heightMeasureSpec, mDefaultSize));
    }

    private float getBaselineOffsetFromY(Paint paint) {
        return MiscUtil.measureTextHeight(paint) / 2;
    }

    /**
     * 更新圆弧画笔颜色
     */
    private void updateArcPaint() {
        SweepGradient sweepGradient = new SweepGradient(mCenterPoint.x, mCenterPoint.y, mGradientColors, null);
        mArcPaint.setShader(sweepGradient);
    }

    public float getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(float maxValue) {
        mMaxValue = maxValue;
    }

    /**
     * 设置当前值
     *
     * @param value
     */
    public void setValue(float value) {
        if (value > mMaxValue) {
            value = mMaxValue;
        }
        float start = mPercent;
        float end = value / mMaxValue;
        startAnimator(start, end, mAnimTime);
    }

    private void startAnimator(float start, float end, long mAnimTime) {
        mAnimator = ValueAnimator.ofFloat(start, end);
        mAnimator.setDuration(mAnimTime);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPercent = (float) animation.getAnimatedValue();
                mValue = mPercent * mMaxValue;
                invalidate();
            }
        });
        mAnimator.start();
    }

    public int[] getGradientColors() {
        return mGradientColors;
    }

    public void setGradientColors(int[] gradientColors) {
        mGradientColors = gradientColors;
        updateArcPaint();
    }

    public void reset() {
        startAnimator(mPercent, 0.0f, 1000L);
    }


}
