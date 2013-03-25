package com.mobilisens.widget.stacklayout;

import com.mobilisens.widget.stacklayout.R;
import com.mobilisens.widget.stacklayout.StackController.OnMoveListener;

import com.nineoldandroids.animation.*;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;

public class StackLayout extends ViewGroup implements OnMoveListener {
	
	private final boolean DEBUG = false;
	
	private static String TAG = "StackLayout";

	private static final int NO_CHILD_TOUCHED = Integer.MAX_VALUE;
	private static final boolean RIGHT = true;
	private static final boolean LEFT = false;
	private static final long MAX_ANIMATION_DURATION = 400;
	private StackController stackController;
	private int currentInterceptedTouchedView = NO_CHILD_TOUCHED;
	private boolean lastDirection;

	private static final LayoutAnimationController addChildAnimationController;
	private static final LayoutAnimationController removeChildAnimationController;

	static {
        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(50);
        set.addAnimation(animation);

        animation = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 1.0f,Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f
        );
        animation.setDuration(200);
        set.addAnimation(animation);

         addChildAnimationController = new LayoutAnimationController(set, 0.5f);
         
         AnimationSet removeSet = new AnimationSet(true);

         animation = new AlphaAnimation(1.0f, 0.0f);
         animation.setDuration(350);
         removeSet.addAnimation(animation);

         removeSet.addAnimation(animation);

         removeChildAnimationController = new LayoutAnimationController(removeSet, 0.5f);
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
		stackController = new StackController(context, this);
		


//        this.setLayoutAnimation(controller);
	}
	
	@Override
	public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
		int count = getChildCount();
		if(index>=0 && index<count){
			for(int i = index; i<count; i++){
				View deletedView = getChildAt(i);
				deletedView.setAnimation(removeChildAnimationController.getAnimationForView(deletedView));
//				removeView(deletedView);
			}
			int nbViewToRemove = count-index;
			removeViews(index, nbViewToRemove);
			count -= nbViewToRemove;  
		}
		LayoutParams lp  = generateLayoutParams(params);
		if(count>0){
			View viewUnderAdded = getChildAt(count-1);
			lp.setUnderView(viewUnderAdded);
			LayoutParams viewUnderAddedLp = (LayoutParams) viewUnderAdded.getLayoutParams();
			if(!viewUnderAddedLp.fixed){
				int leftLimit = 0;
				int rightLimit = 0;
				if(viewUnderAddedLp.underView!=null){
					View underUnderView = viewUnderAddedLp.underView;
					LayoutParams underUnderViewLp = (LayoutParams) underUnderView.getLayoutParams();
					rightLimit = underUnderViewLp.left+underUnderView.getMeasuredWidth();
					leftLimit = underUnderViewLp.left;
				}
				int moveAmount = 0;
				if(viewUnderAddedLp.left!=leftLimit && viewUnderAddedLp.left!=rightLimit){
					moveAmount -= leftLimit - viewUnderAddedLp.left;
				}else if(rightLimit != 0 && viewUnderAddedLp.left == rightLimit){
					moveAmount = viewUnderAdded.getMeasuredWidth()*3/5;
				}
				
				if(moveAmount!=0){
					for (int viewIndex = count-1; viewIndex >=0 ; viewIndex--) {
						View viewMoved = getChildAt(viewIndex);
						LayoutParams viewMovedLp = (LayoutParams) viewMoved.getLayoutParams();
						if(!viewMovedLp.fixed){
							if(viewMovedLp.left - moveAmount<0)
								moveAmount = viewMovedLp.left - moveAmount; 
							animViewLayout(viewIndex, moveAmount, 0);
							if(!lp.isPosSet())
								lp.left = viewMovedLp.left + viewMoved.getMeasuredWidth() - moveAmount;
						}
			    	}
				}
			}

		}


		super.addView(child, index, lp);
		child.setAnimation(addChildAnimationController.getAnimationForView(child));
	}

	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        int maxHeight = getSuggestedMinimumHeight();
        int maxWidth = getSuggestedMinimumWidth();

        setMeasuredDimension(getDefaultSize(maxWidth, widthMeasureSpec), getDefaultSize(maxHeight, heightMeasureSpec));
        int underRight = 0;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childWidthMeasureSpec;
            int childHeightMeasureSpec;
            
            if (lp.width == LayoutParams.MATCH_PARENT) {
            	int childWidth = getMeasuredWidth();
            	if(lp.bestWidthFromParent){
            		int height = getMeasuredHeight();
            		childWidth = (childWidth>height)?height:childWidth;
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
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

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
		if (getChildCount()>0) {
			boolean result = stackController.onInterceptTouchEvent(event);
			currentInterceptedTouchedView = getCurrentInterceptedTouchedView(event);
			if (DEBUG)Log.i(TAG,"currentInterceptedTouchedView "+currentInterceptedTouchedView );
			return result;
		}
		return false;
	}

	private int getCurrentInterceptedTouchedView(MotionEvent event) {
		final int childrenCount = getChildCount();
		if (childrenCount != 0) {
			int actionIndex = stackController.getCurrentPointerIndex(event);
			final int x = (int) event.getX(actionIndex);
			final int y = (int) event.getY(actionIndex);

			for (int i = childrenCount - 1; i >= 0; i--) {
				final View child = getChildAt(i);
				if (isTransformedTouchPointInView(x, y, child)) {
					return i;
				}
			}
		}
		return NO_CHILD_TOUCHED;
	}
	
	
	private boolean isTransformedTouchPointInView(int x, int y, View child) {
        final Rect frame = new Rect();
        child.getHitRect(frame);
        return frame.contains(x, y);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (getChildCount()>0) {
			return stackController.onTouchEvent(event);
		}
		return false;
	}

	//OnMoveListener part
	@Override
	public void onStartMove() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onMove(int moveAmount) {
    	if(DEBUG)Log.i(TAG, "onMove "+moveAmount);
    	
    	int count = getChildCount();
    	if(count<1)
    		return;
    	
    	int upperChild = count -1;
    	if(isMovingToRight(moveAmount)){
    		lastDirection = RIGHT;
    		int viewReferenceIndex = upperChild;
    		if(currentInterceptedTouchedView!=NO_CHILD_TOUCHED){
    			viewReferenceIndex = currentInterceptedTouchedView;
    		}
    		moveViewAndUpperViews(viewReferenceIndex, moveAmount);
			moveUnderViewsToRight(viewReferenceIndex);
    	}else{//moveLeft
    		lastDirection = LEFT;
    		for (int viewIndex = 0; viewIndex < count; viewIndex++) {
    			moveViewAtIndexToLeft(viewIndex, upperChild, moveAmount);
	    	}
    	}
	}

	private void moveViewAndUpperViews(int viewReferenceIndex, int moveAmount) {
		int count = getChildCount();
		for(int viewIndex=viewReferenceIndex; viewIndex<count; viewIndex++){
			moveViewAtIndexToRight(viewIndex, moveAmount);
		}
	}
	
	private void moveViewAtIndexToRight(int viewIndex, int moveAmount) {
		View viewMoved = getChildAt(viewIndex);
		LayoutParams viewMovedLp = (LayoutParams) viewMoved.getLayoutParams();
		if(!viewMovedLp.fixed){
			viewMovedLp.left -=moveAmount;
			updateViewLayout(viewMoved, viewMovedLp);
		}
	}
	
	private void moveUnderViewsToRight(int touchedChildViewIndex) {
		View currentView = getChildAt(touchedChildViewIndex);
		LayoutParams currentLp = (LayoutParams) currentView.getLayoutParams();
		if(currentLp.fixed)
			return;
		
		for (int i = touchedChildViewIndex-1; i>=0; i--){
			View underView = getChildAt(i);
			LayoutParams underViewLp = (LayoutParams) underView.getLayoutParams();
			if(underViewLp.fixed)
				break;
			
			int underViewWidth = underView.getMeasuredWidth();
			if(currentLp.left>=underViewLp.left+underViewWidth){
				int newUnderLeftPos = currentLp.left - underViewWidth;
				underViewLp.left = newUnderLeftPos;
				updateViewLayout(underView, underViewLp);
			}
			currentLp = underViewLp;
		}
			
	}

	private boolean moveViewAtIndexToLeft(int viewIndex, int upperChild, int moveAmount) {
		View viewMoved = getChildAt(viewIndex);
		LayoutParams viewMovedLp = (LayoutParams) viewMoved.getLayoutParams();
		if(!viewMovedLp.fixed){
			viewMovedLp.left -=moveAmount;
			if(viewMovedLp.left<0 && viewIndex != upperChild)
				viewMovedLp.left=0;
			updateViewLayout(viewMoved, viewMovedLp);
			return true;
		}
		return false;
	}
	
	private boolean isMovingToRight(int moveAmount) {
		if(moveAmount<0)
			return true;
		else
			return false;
	}

	@Override
	public void onEndMove(int velocity) {
		int tooMuchRightDelta = 0;
		int tooMuchLeftDelta = 0;
		int notEnoughtRightDelta = 0;
		int notEnoughtLeftDelta = 0;
		int nbChild = getChildCount();
		for (int i = 0; i < nbChild; i++) {
			View referenceMoveView = getChildAt(i);
			LayoutParams referenceMoveViewLp = (LayoutParams) referenceMoveView.getLayoutParams();
			
			int underLeft = getLeft();
			int underRight = underLeft;
			if(referenceMoveViewLp.underView!=null){
				View underReferenceMoveView = referenceMoveViewLp.underView;
				LayoutParams underReferenceMoveLp = (LayoutParams) underReferenceMoveView.getLayoutParams();

				underRight = underReferenceMoveLp.left + underReferenceMoveView.getMeasuredWidth();
				underLeft = underReferenceMoveLp.left;
			}

			if(referenceMoveViewLp.left > underRight && tooMuchRightDelta==0){
				tooMuchRightDelta += referenceMoveViewLp.left - underRight;
			}
			if(tooMuchRightDelta!=0 && tooMuchLeftDelta==0){
				animViewLayout(i,tooMuchRightDelta, velocity);
			}
			if(referenceMoveViewLp.left < 0 && tooMuchLeftDelta==0){
				tooMuchLeftDelta += referenceMoveViewLp.left ;
			}
			if(tooMuchLeftDelta!=0 && tooMuchRightDelta==0){
				animViewLayout(i,tooMuchLeftDelta, velocity);
			}
			
			
			if(tooMuchLeftDelta==0  && tooMuchRightDelta==0 ){
				if(lastDirection==RIGHT && notEnoughtRightDelta==0 && referenceMoveViewLp.left != underLeft && referenceMoveViewLp.left < underRight){
					notEnoughtRightDelta += referenceMoveViewLp.left - underRight;
				}
				if(notEnoughtRightDelta!=0 && notEnoughtLeftDelta==0){
					animViewLayout(i,notEnoughtRightDelta, velocity);
				}
				if(lastDirection==LEFT && notEnoughtLeftDelta==0
//							&& referenceMoveViewLp.left != underLeft 
						&& referenceMoveViewLp.left > underLeft){
					notEnoughtLeftDelta += referenceMoveViewLp.left - underLeft;
				}
				if(notEnoughtLeftDelta!=0 && notEnoughtRightDelta==0){
					animViewLayout(i,notEnoughtLeftDelta, velocity);
				}
			}
			
		}
		currentInterceptedTouchedView = NO_CHILD_TOUCHED;
	}

	
	private void animViewLayout(final int i, int delta, int velocity) {
		final View view = getChildAt(i);
		final LayoutParams lp = (LayoutParams) view.getLayoutParams();
		final ObjectAnimator animator = ObjectAnimator.ofInt(lp, "left", lp.left-delta);
		if(velocity!=0){
	        velocity = Math.abs(velocity);
	        long duration = Math.round(1000 * Math.abs((float)delta / velocity));
			duration = Math.min(duration, MAX_ANIMATION_DURATION);
			animator.setDuration(duration);
		}
		animator.addUpdateListener(new AnimatorUpdateListener() {
			
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				//part for stoping animation if view is deleted during animation
				int count = getChildCount();
				if(i>count-1){
					animator.cancel();
					return;
				}
				View currentViewAtPosI = getChildAt(i);
				if(currentViewAtPosI!= view){
					animator.cancel();
					return;
				}
				
				updateViewLayout(view, lp);
				
			}
		});
		animator.start();
	}

	//Layout Params part
	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}
	
	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	}
	
	public LayoutParams generateLayoutParams(boolean fixed, boolean bestWidthFromParent) {
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		params.bestWidthFromParent = bestWidthFromParent;
		params.fixed = fixed;
		return params;
	}
	
	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}
	
	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		if(p==null)
			return generateDefaultLayoutParams();
		if (checkLayoutParams(p))
			return (LayoutParams) p;
		else
			return new LayoutParams(p.width, p.height);
	}

	public static class LayoutParams extends ViewGroup.LayoutParams {
		private static final int POS_NOT_SET = Integer.MIN_VALUE;
		int left = POS_NOT_SET;
		private View underView = null;
		boolean bestWidthFromParent = false;
		boolean fixed = false;
		
		public LayoutParams(int w, int h) {
			super(w, h);
			this.left = POS_NOT_SET;
		}

		public void setUnderView(View underView) {
			this.underView = underView;
		}

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StackLayout_LayoutParams);
			try {
				bestWidthFromParent = a.getBoolean(R.styleable.StackLayout_LayoutParams_best_width_from_parent, false);
				fixed = a.getBoolean(R.styleable.StackLayout_LayoutParams_fixed, false);
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
}
