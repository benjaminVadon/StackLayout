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
import android.widget.ProgressBar;

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

	}


	@Override
	public void addView(View viewToAdd, int index, android.view.ViewGroup.LayoutParams params) {
		View decorView = new View(getContext());
		decorView.setBackgroundResource(R.drawable.photo_shadow);
		decorView.setAlpha((float) 0.5);
		addView(viewToAdd, decorView, index, params);
//no more processing after this line
	}
	
	public void addView(View viewToAdd, View decorView, int index, android.view.ViewGroup.LayoutParams params) {
		if(viewToAdd==null){
			Log.e(TAG, "You need to provide at least a content view");
			return;
		}
		int count = getChildCount();
		index = checkAndGetIndex(index, count);
		count = removeAllUselessView(index, count);
		
		if(viewToAdd instanceof StackViewContainer){
			if(decorView!=null){
				Log.e(TAG, "Your decorView won't be used because you already provide a StackViewContainer");
			}
			addStackViewContainer((StackViewContainer)viewToAdd, generateLayoutParams(params), index);
		}else{
			addViewInStackViewContainer(viewToAdd, decorView, index, params);
		}
	}
	
	private int checkAndGetIndex(int index, int count) {
		if(index==-1){
			index = count;
		}
		return index;
	}
	
	private int removeAllUselessView(int index, int count) {
		int finalCount = count;
		if (index >= 0 && index < count) {
			for (int i = index; i < count; i++) {
				View deletedView = getChildAt(i);
				deletedView.setAnimation(removeChildAnimationController.getAnimationForView(deletedView));
			}
			int nbViewToRemove = count - index;
			removeViews(index, nbViewToRemove);
			finalCount -= nbViewToRemove;
		}
		return finalCount;
	}

    private void addStackViewContainer(StackViewContainer stackViewContainer, StackLayoutParams params, int index) {
    	stackViewContainer.setIndexInParent(index);
    	StackLayoutParams containerParams = buildStackViewContainerLayoutParams(stackViewContainer, params);

		super.addView(stackViewContainer, index, containerParams);
		
		if(getChildCount()!=0){
			stackViewContainer.setAnimation(addChildAnimationController.getAnimationForView(stackViewContainer));
		}
	}


	private void addViewInStackViewContainer(View viewToAdd, View decorView, int index, LayoutParams params) {
		StackViewContainer viewContainer = buildViewContainer(params);
		viewContainer.addContentAndDecorViews(viewToAdd, decorView);
		addStackViewContainer(viewContainer, (StackLayoutParams) viewContainer.getLayoutParams(), index);
	}

	private StackViewContainer buildViewContainer(LayoutParams params) {
		StackViewContainer viewContainer = new StackViewContainer(getContext());
		StackLayoutParams containerLayoutParams = generateLayoutParams(params);
		viewContainer.setLayoutParams(containerLayoutParams);
		return viewContainer;
	}
	
	private StackLayoutParams buildStackViewContainerLayoutParams(StackViewContainer stackViewContainer, StackLayoutParams stackContainerParams) {
		childAlreadyMeasured=false;
		int count = getChildCount();

		if (count > 0) {
			StackViewContainer viewUnderAdded = (StackViewContainer) getChildAt(count - 1);
			stackContainerParams.setUnderView(viewUnderAdded);
			StackLayoutParams viewUnderAddedLp = (StackLayoutParams) viewUnderAdded.getLayoutParams();

//			
//			if (!viewUnderAddedLp.fixed) {
//				int leftLimit = 0;
//				int rightLimit = 0;
//				if (viewUnderAddedLp.underView != null) {
//					StackViewContainer underUnderView = viewUnderAddedLp.underView;
//					StackLayoutParams underUnderViewLp = (StackLayoutParams) underUnderView.getLayoutParams();
//					rightLimit += underUnderViewLp.left + underUnderView.getMeasuredWidth();
//					leftLimit += underUnderViewLp.left;
//				}
//				int moveAmount = 0;
//				if (viewUnderAddedLp.left != leftLimit
//				&& viewUnderAddedLp.left != rightLimit) {
//					moveAmount -= leftLimit - viewUnderAddedLp.left;
//				} else if (rightLimit != 0
//						&& viewUnderAddedLp.left == rightLimit) {
//					moveAmount = viewUnderAdded.getMeasuredWidth() * 3 / 5;
//				}
////part for anim under view when adding a new one upper
//				if (moveAmount != 0) {
//					for (int viewIndex = count - 1; viewIndex >= 0; viewIndex--) {
//						View viewMoved = getChildAt(viewIndex);
//						StackLayoutParams viewMovedLp = (StackLayoutParams) viewMoved.getLayoutParams();
//						if (!viewMovedLp.fixed) {
//							if (viewMovedLp.left - moveAmount < 0)
//								moveAmount = viewMovedLp.left - moveAmount;
//							moveController.animViewLayout(viewIndex, moveAmount, 0);
//							if (!stackContainerParams.isPosSet())
//								stackContainerParams.left = viewMovedLp.left
//										+ viewMoved.getMeasuredWidth()
//										- moveAmount;
//						}
//					}
//				}
//			}
		}
		
		return stackContainerParams;
	}

    
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        int maxHeight = getSuggestedMinimumHeight();
        int maxWidth = getSuggestedMinimumWidth();

        setMeasuredDimension(getDefaultSize(maxWidth, widthMeasureSpec), getDefaultSize(maxHeight, heightMeasureSpec));

        if(childAlreadyMeasured)
        	return;
        
        for (int i = 0; i < count; i++) {
            final StackViewContainer child = (StackViewContainer) getChildAt(i);
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

                int childLeft = lp.getViewPos();//lp.left;
                int childTop = top;

                child.layout(childLeft, childTop, childLeft + width, childTop + height);
                
            }
		}
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
    	if(DEBUG)Log.i(TAG, "onInterceptTouchEvent "+event.toString());
		if (getChildCount()>0) {
			boolean result = touchController.onInterceptTouchEvent(event);
			if(result){
				moveController.setCurrentInterceptedTouchedView(event);
			}
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


	public int getActivePointerId() {
		return touchController.getActivePointerId();
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
		
		private final String TAG = getClass().getSimpleName();
		private static final int POS_NOT_SET = Integer.MIN_VALUE;
		StackViewContainer underView = null;
		boolean bestWidthFromParent = false;
		boolean fixed = false;
		int needShadow = 0;
		
		
		private int contentViewPos = POS_NOT_SET;
		private int[] anchors;
		private int decorViewWidth = 0;
		
		
		public StackLayoutParams(int w, int h) {
			super(w, h);
		}

		public void setUnderView(StackViewContainer underView) {
			this.underView = underView;
		}

		public StackLayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StackLayoutParams);
			try {
				bestWidthFromParent = a.getBoolean(R.styleable.StackLayoutParams_best_width_from_parent, false);
				fixed = a.getBoolean(R.styleable.StackLayoutParams_fixed, false);
				
				needShadow = a.getInt(R.styleable.StackLayoutParams_need_shadow, 0);
				
			} finally {
				a.recycle();
			}
		}

		public int changeContentViewPos(int moveAmount, boolean hasUpperChild) {
			if(contentViewPos!=POS_NOT_SET){
				if(hasUpperChild){
					int diff = getContentViewPos() - getLeftAnchor();
					if(diff<=moveAmount){
						moveAmount = diff;
					}
				}

				contentViewPos -= moveAmount;
			}
			return moveAmount;
		}

		public void setContentViewPos(int leftPos) {
			contentViewPos = leftPos;
		}
		
		public int getContentViewPos(){
			return contentViewPos;
		}

		public int getViewPos(){
			return contentViewPos-decorViewWidth;
		}
		
		public void setDecorViewWidth(int viewWidth) {
			decorViewWidth = viewWidth;
		}

		public int getDecorViewWidth() {
			return decorViewWidth;
		}
		
		public boolean isViewPosSet() {
			return contentViewPos != POS_NOT_SET;
		}

		public int howMuchShouldMoveFromUpper(int moveAmount, StackLayoutParams upperParams) {
			if(MoveController.isMovingToRight(moveAmount)){
				int diffToBringUnderPanel = upperParams.getRightAnchor() - upperParams.getContentViewPos();
				if(diffToBringUnderPanel<=0){
					return diffToBringUnderPanel;
				}else{
					return 0;
				}
			}else{
				int stopToLeftAnchors = getContentViewPos() - getLeftAnchor();
				if(stopToLeftAnchors<=0)
					return stopToLeftAnchors;
				if(stopToLeftAnchors-moveAmount<=0)
					return stopToLeftAnchors;
				return moveAmount;
			}
			
		}
		

		public void initAnchors(int[] anchorsArray){
			anchors = anchorsArray;
		}
		
		public void updateAnchors(int moveAmount){
			for(int i=0; i<anchors.length; i++){
				anchors[i]-=moveAmount;
			}
		}

		private int getLeftAnchor() {
			return anchors[0];
		}
		
		public int getRightAnchor() {
			return anchors[anchors.length-1];
		}

		public int getDistanceToNearestAnchor() {
			Log.i(TAG, "getDistanceToNearestAnchor");
			int diff = Integer.MAX_VALUE;
			for(int i=0; i<anchors.length; i++){
				int value = getContentViewPos()-anchors[i];
				if(Math.abs(diff)>= Math.abs(value)){
					diff = value;
				}
				Log.i(TAG, "diff " +diff);
			}
			return diff;
		}
		
	}
}
