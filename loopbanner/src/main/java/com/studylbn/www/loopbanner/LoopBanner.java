package com.studylbn.www.loopbanner;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.studylbn.www.loopbanner.listener.OnBannerClickListener;
import com.studylbn.www.loopbanner.listener.OnBannerListener;
import com.studylbn.www.loopbanner.loader.ImageLoaderInterface;
import com.studylbn.www.loopbanner.view.BannerViewPager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by LiuBin on 2017/12/12 22:53.
 */

public class LoopBanner extends FrameLayout implements ViewPager.OnPageChangeListener {
    public String tag = "LoopBanner";
    private int mIndicatorMargin = BannerConfig.PADDING_SIZE;
    private int mIndicatorWidth, mIndicatorHeight;
    private int indicatorSize;
    private int bannerBackgroundImage;
    private int bannerStyle = BannerConfig.CIRCLE_INDICATOR;
    private int delayTime = BannerConfig.TIME;
    private int scrollTime = BannerConfig.DURATION;
    private boolean isAutoPlay = BannerConfig.IS_AUTO_PLAY;
    private boolean isScroll = BannerConfig.IS_SCROLL;
    private int mIndicatorSelectedResId = R.drawable.gray_radius;
    private int mIndicatorUnselectedResId = R.drawable.white_radius;
    private int mLayoutResId = R.layout.loopbanner;
    private int titleHeight;
    private int titleBackground;
    private int titleTextColor;
    private int titleTextSize;
    private int count = 0;
    private int currentItem = 0;
    private int gravity = -1;
    private int lastPosition = 1;
    private int scaleType = 1;
    private List<String> titles;
    private List imageUrls;
    private List<View> imageViews;
    private List<ImageView> indicatorImages;
    private Context context;
    private BannerViewPager viewPager;
    private TextView bannerTitle, numIndicatorInside, numIndicator;
    private LinearLayout indicator, indicatorInside, titleView;
    private ImageView bannerDefaultImage;
    private ImageLoaderInterface imageLoader;
    private BannerPagerAdapter adapter;
    private ViewPager.OnPageChangeListener mOnPageChangeListener;
    private BannerScroller mScroller;
    private OnBannerClickListener bannerListener;
    private OnBannerListener listener;
    private DisplayMetrics dm;
    private WeakHandler handler = new WeakHandler();

    public LoopBanner(@NonNull Context context) {
        this(context, null);
    }

    public LoopBanner(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoopBanner(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        titles = new ArrayList<>();
        imageUrls = new ArrayList<>();
        imageViews = new ArrayList<>();
        indicatorImages = new ArrayList<>();
        dm = context.getResources().getDisplayMetrics();
        indicatorSize = dm.widthPixels / 80;
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        imageViews.clear();
        handleTypedArray(context, attrs);
        View view = LayoutInflater.from(context).inflate(mLayoutResId, this, true);
        bannerDefaultImage = (ImageView) view.findViewById(R.id.bannerDefaultImage);
        viewPager = (BannerViewPager) view.findViewById(R.id.bannerViewPager);
        titleView = (LinearLayout) view.findViewById(R.id.titleView);
        indicator = (LinearLayout) view.findViewById(R.id.circleIndicator);
        indicatorInside = (LinearLayout) view.findViewById(R.id.indicatorInside);
        bannerTitle = (TextView) view.findViewById(R.id.bannerTitle);
        numIndicator = (TextView) view.findViewById(R.id.numIndicator);
        numIndicatorInside = (TextView) view.findViewById(R.id.numIndicatorInside);
        bannerDefaultImage.setImageResource(bannerBackgroundImage);
        initViewPagerScroll();
    }

    private void initViewPagerScroll() {
        try {
            Field mField = ViewPager.class.getDeclaredField("mScroller");
            mField.setAccessible(true);
            mScroller = new BannerScroller(viewPager.getContext());
            mScroller.setDuration(scrollTime);
            mField.set(viewPager, mScroller);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public LoopBanner setAutoPlay(boolean isAutoPlay) {
        this.isAutoPlay = isAutoPlay;
        return this;
    }

    public LoopBanner setImageLoader(ImageLoaderInterface imageLoader) {
        this.imageLoader = imageLoader;
        return this;
    }

    public LoopBanner setDelayTime(int delayTime) {
        this.delayTime = delayTime;
        return this;
    }

    public LoopBanner setIndicatorGravity(int type) {
        switch (type) {
            case BannerConfig.LEFT:
                this.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
                break;
            case BannerConfig.CENTER:
                this.gravity = Gravity.CENTER;
                break;
            case BannerConfig.RIGHT:
                this.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                break;
        }
        return this;
    }

    public LoopBanner setBannerAnimation(Class<? extends ViewPager.PageTransformer> transformer) {
        try {
            setPageTransformer(true, transformer.newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * Set the number of pages that should be retained to either side of the
     * current page in the view hierarchy in an idle state. Pages beyond this
     * limit will be recreated from the adapter when needed.
     *
     * @param limit How many pages will be kept offscreen in an idle state.
     * @return Banner
     */
    public LoopBanner setOffscreenPageLimit(int limit) {
        if (viewPager != null) {
            viewPager.setOffscreenPageLimit(limit);
        }
        return this;
    }

    /**
     * Set a {@link ViewPager.PageTransformer} that will be called for each attached page whenever
     * the scroll position is changed. This allows the application to apply custom property
     * transformations to each page, overriding the default sliding look and feel.
     *
     * @param reverseDrawingOrder true if the supplied PageTransformer requires page views
     *                            to be drawn from last to first instead of first to last.
     * @param transformer         PageTransformer that will modify each page's animation properties
     * @return Banner
     */
    public LoopBanner setPageTransformer(boolean reverseDrawingOrder, ViewPager.PageTransformer transformer) {
        viewPager.setPageTransformer(reverseDrawingOrder, transformer);
        return this;
    }

    public LoopBanner setBannerTitles(List<String> titles) {
        this.titles = titles;
        return this;
    }

    public LoopBanner setBannerStyle(int bannerstyle) {
        this.bannerStyle = bannerstyle;
        return this;
    }

    public LoopBanner setViewPagerIsScroll(boolean isScroll) {
        this.isScroll = isScroll;
        return this;
    }

    public LoopBanner setImages(List<?> imageUrls) {
        this.imageUrls = imageUrls;
        this.count = imageUrls.size();
        return this;
    }

    public void update(List<?> imageUrls, List<String> titles) {
        this.titles.clear();
        this.titles.addAll(titles);
        update(imageUrls);
    }

    private void update(List<?> imageUrls) {
        this.imageUrls.clear();
        this.imageViews.clear();
        this.indicatorImages.clear();
        this.imageUrls.addAll(imageUrls);
        this.count = this.imageUrls.size();
        start();
    }

    public LoopBanner start() {
        setBannerStyleUI();
        setImageList(imageUrls);
        setData();
        return this;
    }

    private void setImageList(List<?> imageUrl) {
        if (imageUrl == null || imageUrl.size() <= 0) {
            bannerDefaultImage.setVisibility(VISIBLE);
            Log.e(tag, "The image data set is empty.");
            return;
        }
        bannerDefaultImage.setVisibility(GONE);
        initImages();
        for (int i = 0; i <= count + 1; i++) {
            View imageView = null;
            if (imageLoader != null) {
                imageView = imageLoader.createImageView(context);
            }
            if (imageView == null) {
                imageView = new ImageView(context);
            }
            setScaleType(imageView);
            Object url = null;
            if (i == 0) {
                url = imageUrl.get(count - 1);
            } else if (i == count + 1) {
                url = imageUrl.get(0);
            } else {
                url = imageUrl.get(i - 1);
            }
            imageViews.add(imageView);
            if (imageLoader != null) {
                imageLoader.displayImage(context, url, imageView);
            } else
                Log.e(tag, "Please set images loader.");
        }

    }

    private void setScaleType(View imageView) {
        if (imageView instanceof ImageView) {
            ImageView view = ((ImageView) imageView);
            switch (scaleType) {
                case 0:
                    view.setScaleType(ImageView.ScaleType.CENTER);
                    break;
                case 1:
                    view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    break;
                case 2:
                    view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    break;
                case 3:
                    view.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    break;
                case 4:
                    view.setScaleType(ImageView.ScaleType.FIT_END);
                    break;
                case 5:
                    view.setScaleType(ImageView.ScaleType.FIT_START);
                    break;
                case 6:
                    view.setScaleType(ImageView.ScaleType.FIT_XY);
                    break;
                case 7:
                    view.setScaleType(ImageView.ScaleType.MATRIX);
                    break;
            }
        }
    }

    private void initImages() {
        imageViews.clear();
        if (bannerStyle == BannerConfig.CIRCLE_INDICATOR ||
                bannerStyle == BannerConfig.CIRCLE_INDICATOR_TITLE ||
                bannerStyle == BannerConfig.CIRCLE_INDICATOR_TITLE_INSIDE) {
            createIndicator();
        } else if (bannerStyle == BannerConfig.NUM_INDICATOR) {
            numIndicator.setText("1/" + count);
        } else if (bannerStyle == BannerConfig.NUM_INDICATOR_TITLE) {
            numIndicatorInside.setText("1/" + count);
        }
    }

    private void createIndicator() {
        indicatorImages.clear();
        indicator.removeAllViews();
        indicatorInside.removeAllViews();
        for (int i = 0; i < count; i++) {
            ImageView imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mIndicatorWidth, mIndicatorHeight);
            params.leftMargin = mIndicatorMargin;
            params.rightMargin = mIndicatorMargin;
            if (i == 0) {
                imageView.setImageResource(mIndicatorSelectedResId);
            } else {
                imageView.setImageResource(mIndicatorUnselectedResId);
            }
            indicatorImages.add(imageView);
            if (bannerStyle == BannerConfig.CIRCLE_INDICATOR || bannerStyle == BannerConfig.CIRCLE_INDICATOR_TITLE) {
                indicator.addView(imageView, params);
            } else if (bannerStyle == BannerConfig.CIRCLE_INDICATOR_TITLE_INSIDE) {
                indicatorInside.addView(imageView, params);
            }
        }
    }

    private void setBannerStyleUI() {
        int visibility = count > 1 ? View.VISIBLE : View.GONE;
        switch (bannerStyle) {
            case BannerConfig.CIRCLE_INDICATOR:
                indicator.setVisibility(visibility);
                break;
            case BannerConfig.NUM_INDICATOR:
                numIndicator.setVisibility(visibility);
                break;
            case BannerConfig.NUM_INDICATOR_TITLE:
                numIndicatorInside.setVisibility(visibility);
                setTitleStyleUI();
                break;
            case BannerConfig.CIRCLE_INDICATOR_TITLE:
                indicator.setVisibility(visibility);
                setTitleStyleUI();
                break;
            case BannerConfig.CIRCLE_INDICATOR_TITLE_INSIDE:
                indicatorInside.setVisibility(visibility);
                setTitleStyleUI();
                break;
        }
    }

    private void setTitleStyleUI() {
        if (titles.size() != imageUrls.size()) {
            throw new RuntimeException("[Banner] --> The number of titles and images is different");
        }
        if (titleBackground != -1) {
            titleView.setBackgroundColor(titleBackground);
        }
        if (titleHeight != -1) {
            titleView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, titleHeight));
        }
        if (titleTextColor != -1) {
            bannerTitle.setTextColor(titleTextColor);
        }
        if (titleTextSize != -1) {
            bannerTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleTextSize);
        }
        if (titles != null && titles.size() > 0) {
            bannerTitle.setText(titles.get(0)); //?为什么是get(0)
            bannerTitle.setVisibility(View.VISIBLE);
            titleView.setVisibility(View.VISIBLE);
        }
    }


    private void handleTypedArray(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LoopBanner);
        mIndicatorWidth = typedArray.getDimensionPixelSize(R.styleable.LoopBanner_indicator_width, indicatorSize);
        mIndicatorHeight = typedArray.getDimensionPixelSize(R.styleable.LoopBanner_indicator_height, indicatorSize);
        mIndicatorMargin = typedArray.getDimensionPixelSize(R.styleable.LoopBanner_indicator_margin, BannerConfig.PADDING_SIZE);
        mIndicatorSelectedResId = typedArray.getResourceId(R.styleable.LoopBanner_indicator_drawable_selected, R.drawable.gray_radius);
        mIndicatorUnselectedResId = typedArray.getResourceId(R.styleable.LoopBanner_indicator_drawable_unselected, R.drawable.white_radius);
        scaleType = typedArray.getInt(R.styleable.LoopBanner_image_scale_type, scaleType);
        delayTime = typedArray.getInt(R.styleable.LoopBanner_delay_time, BannerConfig.TIME);
        scrollTime = typedArray.getInt(R.styleable.LoopBanner_scroll_time, BannerConfig.DURATION);
        isAutoPlay = typedArray.getBoolean(R.styleable.LoopBanner_is_auto_play, BannerConfig.IS_AUTO_PLAY);
        titleBackground = typedArray.getColor(R.styleable.LoopBanner_title_background, BannerConfig.TITLE_BACKGROUND);
        titleHeight = typedArray.getDimensionPixelSize(R.styleable.LoopBanner_title_height, BannerConfig.TITLE_HEIGHT);
        titleTextColor = typedArray.getColor(R.styleable.LoopBanner_title_textcolor, BannerConfig.TITLE_TEXT_COLOR);
        titleTextSize = typedArray.getDimensionPixelSize(R.styleable.LoopBanner_title_textsize, BannerConfig.TITLE_TEXT_SIZE);
        mLayoutResId = typedArray.getResourceId(R.styleable.LoopBanner_banner_layout, mLayoutResId);
        bannerBackgroundImage = typedArray.getResourceId(R.styleable.LoopBanner_banner_default_image, R.drawable.no_banner);
        typedArray.recycle();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrolled(toRealPosition(position), positionOffset, positionOffsetPixels);
        }
    }

    @Override
    public void onPageSelected(int position) {
        currentItem = position;
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageSelected(toRealPosition(position));
        }

        for (int i = 0; i < count; i++) {
            //将所有的圆点设置为未选中时候的图片
            indicatorImages.get(i).setImageResource(mIndicatorUnselectedResId);
        }

        if (bannerStyle == BannerConfig.CIRCLE_INDICATOR ||
                bannerStyle == BannerConfig.CIRCLE_INDICATOR_TITLE ||
                bannerStyle == BannerConfig.CIRCLE_INDICATOR_TITLE_INSIDE) {
//            indicatorImages.get((lastPosition - 1 + count) % count).setImageResource(mIndicatorUnselectedResId);
//            indicatorImages.get((position - 1 + count) % count).setImageResource(mIndicatorSelectedResId);
//            lastPosition = position;
            if (position == 0) {
                indicatorImages.get(count - 1).setImageResource(mIndicatorSelectedResId);
            } else if (position == imageViews.size() - 1) {
                indicatorImages.get(0).setImageResource(mIndicatorSelectedResId);
            } else {
                indicatorImages.get(position - 1).setImageResource(mIndicatorSelectedResId);
            }
        }
        if (position == 0) position = count;
        if (position > count) position = 1;
        switch (bannerStyle) {
            case BannerConfig.CIRCLE_INDICATOR:
                break;
            case BannerConfig.NUM_INDICATOR:
                numIndicator.setText(position + "/" + count);
                break;
            case BannerConfig.NUM_INDICATOR_TITLE:
                numIndicatorInside.setText(position + "/" + count);
                bannerTitle.setText(titles.get(position - 1));
                break;
            case BannerConfig.CIRCLE_INDICATOR_TITLE:
                bannerTitle.setText(titles.get(position - 1));
                break;
            case BannerConfig.CIRCLE_INDICATOR_TITLE_INSIDE:
                bannerTitle.setText(titles.get(position - 1));
                break;
        }

    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrollStateChanged(state);
        }
//        switch (state) {
//            case 0:
//                if (currentItem == 0) {
//                    viewPager.setCurrentItem(count, false);
//                } else if (currentItem == count + 1) {
//                    viewPager.setCurrentItem(1, false);
//                }
//                break;
//            case 1:
//                if (currentItem == count + 1) {
//                    viewPager.setCurrentItem(1, false);
//                } else if (currentItem == 0) {
//                    viewPager.setCurrentItem(count, false);
//                }
//                break;
//            case 2:
//                break;
//        }

        //        若viewpager滑动未停止，直接返回
        if (state != ViewPager.SCROLL_STATE_IDLE) return;
        if (currentItem == 0) {
            currentItem = imageViews.size() - 2;
            viewPager.setCurrentItem(currentItem, false);
        } else if (currentItem == imageViews.size() - 1) {
            currentItem = 1;
            viewPager.setCurrentItem(currentItem, false);
        }


    }

    private void setData() {
        currentItem = 0;
        if (adapter == null) {
            adapter = new BannerPagerAdapter();
            viewPager.addOnPageChangeListener(this);
        }
        viewPager.setAdapter(adapter);
        viewPager.setFocusable(true);
        viewPager.setCurrentItem(1);
        if (gravity != -1)
            indicator.setGravity(gravity);
        if (isScroll && count > 1) {
            viewPager.setScrollable(true);
        } else {
            viewPager.setScrollable(false);
        }
        if (isAutoPlay)
            startAutoPlay();
    }

    public void startAutoPlay() {
        handler.removeCallbacks(task);
        handler.postDelayed(task, delayTime);
    }

    public void stopAutoPlay() {
        handler.removeCallbacks(task);
    }

    private final Runnable task = new Runnable() {
        @Override
        public void run() {
            if (count > 1 && isAutoPlay) {
//                currentItem = currentItem % (count + 1) + 1;
//                if (currentItem == 1) {
//                    viewPager.setCurrentItem(currentItem, false);
//                    handler.post(task);
//                } else {
//                    viewPager.setCurrentItem(currentItem);
//                    handler.postDelayed(task, delayTime);
//                }
                currentItem++;
                if (currentItem > imageViews.size() - 1) {
                    currentItem = 2;
                }
                if (currentItem < 0) {
                    currentItem = imageViews.size() - 3;
                }
                viewPager.setCurrentItem(currentItem);
                handler.postDelayed(task, delayTime);
            }
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isAutoPlay) {
            int action = ev.getAction();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                    || action == MotionEvent.ACTION_OUTSIDE) {
                startAutoPlay();
            } else if (action == MotionEvent.ACTION_DOWN) {
                stopAutoPlay();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 返回真实的位置
     *
     * @param position
     * @return 下标从0开始
     */
    public int toRealPosition(int position) {
        int realPosition = (position - 1) % count;
        if (realPosition < 0)
            realPosition += count;
        return realPosition;
    }

    class BannerPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return imageViews.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            container.addView(imageViews.get(position));
            View view = imageViews.get(position);
            if (bannerListener != null) {
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bannerListener.onBannerClick(position);
                    }
                });
            }
            if (listener != null) {
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onBannerClick(position);
                    }
                });
            }
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }

    @Deprecated
    public LoopBanner setOnBannerClickListener(OnBannerClickListener listener) {
        this.bannerListener = listener;
        return this;
    }

    /**
     * 废弃了旧版接口，新版的接口下标是从1开始，同时解决下标越界问题
     *
     * @param listener
     * @return
     */
    public LoopBanner setOnBannerListener(OnBannerListener listener) {
        this.listener = listener;
        return this;
    }

    public void setOnPageChangeListener(ViewPager.OnPageChangeListener onPageChangeListener) {
        this.mOnPageChangeListener = onPageChangeListener;
    }

    public void releaseBanner() {
        handler.removeCallbacksAndMessages(null);
    }
}
