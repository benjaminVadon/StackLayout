package com.mobilisens.widget.stacklayout;

import java.util.ArrayList;

import com.mobilisens.widget.stacklayout.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;

public class StackLayout extends ViewGroup{
	
	private final boolean DEBUG = false;
	private final String TAG = getClass().getSimpleName();

	private TouchController touchController;
	private MoveController moveController;
	private boolean childAlreadyMeasured =false;
	private int deviceWidth;
	private int deviveHeight;
	

	private static final LayoutAnimationController addChildAnimationController;
	private static final LayoutAnimationController removeChildAnimationController;

	static {
        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(100);
        set.addAnimation(animation);

        animation = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 1.0f,Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f
        );
        animation.setDuration(500);
        set.addAnimation(animation);

        addChildAnimationController = new LayoutAnimationController(set);
         
         
         AnimationSet removeSet = new AnimationSet(true);

         animation = new AlphaAnimation(1.0f, 0.0f);
         animation.setDuration(350);
         removeSet.addAnimation(animation);

         removeSet.addAnimation(animation);

         removeChildAnimationController = new LayoutAnimationController(removeSet);
	}
	
	public StackLayout(Context context) {
		super(context);
		initStackLayout(context);
	}
	
	public StackLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initStackLayout(context);
	}

	private void initStackLayout(Context context){
		moveController = new MoveController(this);
		touchController = new TouchController(context, moveController);
		this.setSaveEnabled(true);
		int id = getId();
		if(id==View.NO_ID){
			setId(R.id.stackLayout);
		}
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		deviceWidth = display.getWidth(); 
		deviveHeight = display.getHeight();


        mShadow = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.photo_shadow);
        mSrc = new Rect(0, 0, mShadow.getWidth(), mShadow.getHeight());
//        shadowRects = new ArrayList<Rect>(3);
	}
	
	@Override
	public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
		childAlreadyMeasured=false;
		int count = getChildCount();
		if(index==-1){
			index = count;
		}
		if (index >= 0 && index < count) {
			for (int i = index; i < count; i++) {
				View deletedView = getChildAt(i);
				deletedView.setAnimation(removeChildAnimationController.getAnimationForView(deletedView));
			}
			int nbViewToRemove = count - index;
			removeViews(index, nbViewToRemove);
			count -= nbViewToRemove;
		}
		StackLayoutParams lp = generateLayoutParams(params);
		if (count > 0) {
			View viewUnderAdded = getChildAt(count - 1);
			lp.setUnderView(viewUnderAdded);
			StackLayoutParams viewUnderAddedLp = (StackLayoutParams) viewUnderAdded.getLayoutParams();
			if (!viewUnderAddedLp.fixed) {
				int leftLimit = 0;
				int rightLimit = 0;
				if (viewUnderAddedLp.underView != null) {
					View underUnderView = viewUnderAddedLp.underView;
					StackLayoutParams underUnderViewLp = (StackLayoutParams) underUnderView.getLayoutParams();
					rightLimit = underUnderViewLp.left + underUnderView.getMeasuredWidth();
					leftLimit = underUnderViewLp.left;
				}
				int moveAmount = 0;
				if (viewUnderAddedLp.left != leftLimit
				&& viewUnderAddedLp.left != rightLimit) {
					moveAmount -= leftLimit - viewUnderAddedLp.left;
				} else if (rightLimit != 0
						&& viewUnderAddedLp.left == rightLimit) {
					moveAmount = viewUnderAdded.getMeasuredWidth() * 3 / 5;
				}

				if (moveAmount != 0) {
					for (int viewIndex = count - 1; viewIndex >= 0; viewIndex--) {
						View viewMoved = getChildAt(viewIndex);
						StackLayoutParams viewMovedLp = (StackLayoutParams) viewMoved.getLayoutParams();
						if (!viewMovedLp.fixed) {
							if (viewMovedLp.left - moveAmount < 0)
								moveAmount = viewMovedLp.left - moveAmount;
							moveController.animViewLayout(viewIndex, moveAmount, 0);
							if (!lp.isPosSet())
								lp.left = viewMovedLp.left
										+ viewMoved.getMeasuredWidth()
										- moveAmount;
						}
					}
				}
			}
			if(lp.anchorForInit!=-1){
				lp.left = lp.anchorForInit;
			}
		}

		
		super.addView(child, index, lp);
		
		if(count!=0){
			child.setAnimation(addChildAnimationController.getAnimationForView(child));
		}
		if(!child.hasOnClickListeners()){
			child.setOnTouchListener(new OnTouchListener() {
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					return true;
				}
			});
		}
	}

//	@Override
//	protected void dispatchDraw(Canvas canvas) {
//		Log.i(TAG, "dispatchDraw");
//		super.dispatchDraw(canvas);
//		drawShadowForAllChild(canvas);
//	}
	
//	@Override
//	public void draw(Canvas canvas) {
//		Log.i(TAG, "draw");
//		super.draw(canvas);
//	}

    private Bitmap mShadow;
    private Rect mSrc;
    private final Rect mDst = new Rect();
	private ArrayList<Integer> shadowPosList = new ArrayList<Integer>();
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawAllShadows(canvas);
    }
//
//	private void removeAllShadows() {
//		if(!shadowPosList.isEmpty()){
//			int shadowWidth = getShadowWidth();
//			int top = getTop();
//			int bottom = getBottom();
//			for(int left: shadowPosList){
//				Rect dirty = new Rect(left-shadowWidth, top, left, bottom);
//				invalidate(dirty);
//			}
//		}
//	}

    private void drawAllShadows(Canvas canvas) {
        final int count = getChildCount();
        for (int i = count-1; i >= 0; i--) {
            final View child = getChildAt(i);
            
            final int left = (int)child.getX();
            if(DEBUG)Log.i(TAG, "draw,\tpos:"+left+"\t"+System.currentTimeMillis());
            mDst.set(left - mShadow.getWidth(), 0, left, getHeight());

            canvas.drawBitmap(mShadow, mSrc, mDst, null);
            shadowPosList.add(left);
        }
	}
    
	public int getShadowWidth(){
    	return mShadow.getWidth();
    }
//	private void drawShadowForAllChild(Canvas canvas){
//		int count = getChildCount();
//		for (int i = 0; i < count; i++) {
//			View child = getChildAt(i);
//			drawShadow(canvas, child);
//		}
//	}
//	
//	private void drawShadow(Canvas canvas, View child) {
//		int childLeftPos = child.getLeft();
//		if(childLeftPos>getLeft()){
//			int shadowWidth = 30;
//			shadowDrawable.setBounds(childLeftPos-shadowWidth, 0, childLeftPos, getHeight());
//			shadowDrawable.draw(canvas);
//		}
//	}

    
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        int maxHeight = getSuggestedMinimumHeight();
        int maxWidth = getSuggestedMinimumWidth();

        setMeasuredDimension(getDefaultSize(maxWidth, widthMeasureSpec), getDefaultSize(maxHeight, heightMeasureSpec));

        if(childAlreadyMeasured)
        	return;
        
        int underRight = 0;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final StackLayoutParams lp = (StackLayoutParams) child.getLayoutParams();
            int childWidthMeasureSpec;
            int childHeightMeasureSpec;
            
            if (lp.width == StackLayoutParams.MATCH_PARENT) {
            	int childWidth = getMeasuredWidth();
            	if(lp.bestWidthFromParent){
            		childWidth = (deviceWidth<deviveHeight)?deviceWidth:deviveHeight;
            		childWidth = (int) ((childWidth * 2.) /3.);
            	}
        		childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
            } else {
                childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,0, lp.width);
            }
            
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY);

            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            
            if(!lp.isPosSet()){
            	if(i==0){
            		lp.left = 0;
            	}else{
            		lp.left = underRight;
            	}
            	requestLayout();
            }
            underRight = lp.left+ child.getMeasuredWidth();
        }
    }
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final StackLayoutParams lp = (StackLayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft = lp.left;
                int childTop = top;

                child.layout(childLeft, childTop, childLeft + width, childTop + height);
                
            }
		}
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
    	if(DEBUG)Log.i(TAG, "onInterceptTouchEvent");
		if (getChildCount()>0) {
			boolean result = touchController.onInterceptTouchEvent(event);
			moveController.setCurrentInterceptedTouchedView(event);
			return result;
		}
		return false;
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {
    	if(DEBUG)Log.i(TAG, "onTouchEvent");
		if (getChildCount()>0) {
			return touchController.onTouchEvent(event);
		}
		return false;
	}

	//Layout Params part
	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof StackLayoutParams;
	}
	
	@Override
	protected StackLayoutParams generateDefaultLayoutParams() {
		return new StackLayoutParams(StackLayoutParams.MATCH_PARENT, StackLayoutParams.MATCH_PARENT);
	}
	
	public StackLayoutParams generateLayoutParams(boolean fixed, boolean bestWidthFromParent) {
		StackLayoutParams params = new StackLayoutParams(StackLayoutParams.MATCH_PARENT, StackLayoutParams.MATCH_PARENT);
		params.bestWidthFromParent = bestWidthFromParent;
		params.fixed = fixed;
		return params;
	}
	
	public StackLayoutParams generateLayoutParams(boolean fixed, boolean bestWidthFromParent, int anchorForInit) {
		StackLayoutParams params = generateLayoutParams(fixed, bestWidthFromParent);
		params.anchorForInit = anchorForInit;
		return params;
	}
	
	@Override
	public StackLayoutParams generateLayoutParams(AttributeSet attrs) {
		return new StackLayoutParams(getContext(), attrs);
	}
	
	@Override
	protected StackLayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		if(p==null)
			return generateDefaultLayoutParams();
		if (checkLayoutParams(p))
			return (StackLayoutParams) p;
		else
			return new StackLayoutParams(p.width, p.height);
	}

	public static class StackLayoutParams extends ViewGroup.LayoutParams {
		private static final int POS_NOT_SET = Integer.MIN_VALUE;
		int left = POS_NOT_SET;
		View underView = null;
		boolean bestWidthFromParent = false;
		boolean fixed = false;
		private int anchorForInit = -1;
		
		public StackLayoutParams(int w, int h) {
			super(w, h);
			this.left = POS_NOT_SET;
		}

		public void setUnderView(View underView) {
			this.underView = underView;
		}

		public StackLayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StackLayoutParams);
			try {
				bestWidthFromParent = a.getBoolean(R.styleable.StackLayoutParams_best_width_from_parent, false);
				fixed = a.getBoolean(R.styleable.StackLayoutParams_fixed, false);
				anchorForInit = a.getInteger(R.styleable.StackLayoutParams_anchor_for_init, -1);
			} finally {
				a.recycle();
			}
		}

		public boolean isPosSet(){
			return this.left != POS_NOT_SET;
		}

		public void setLeft(int newLeft){
			this.left = newLeft;
		}
		
		public int getLeft(){
			return this.left;
		}
	}

	public int getActivePointerId() {
		return touchController.getActivePointerId();
	}
	
}
