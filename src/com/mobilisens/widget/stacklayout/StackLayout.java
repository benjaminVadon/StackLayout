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
	}
	
	@Override
	public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
		LayoutParams lp  = generateLayoutParams(params);
		int count = getChildCount();
		if(count>0){
			lp.setUnderView(getChildAt(count-1));
		}
		super.addView(child, index, lp);
	}

	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();


        int maxHeight = getSuggestedMinimumHeight();
        int maxWidth = getSuggestedMinimumWidth();


        setMeasuredDimension(getDefaultSize(maxWidth, widthMeasureSpec), getDefaultSize(maxHeight, heightMeasureSpec));
        int cumulatedChildWidth = 0;
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
        		childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidth,
            				MeasureSpec.EXACTLY);
            } else {
                childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,0,
                        lp.width);
            }
            
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(),
                    MeasureSpec.EXACTLY);

            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            
            if(!lp.isPosSet()){
            	if(i==0){
            		lp.left = 0;
            	}else{
//            		View underview = getChildAt(i-1);
            		lp.left = cumulatedChildWidth;
            	}
            	requestLayout();
            }
            cumulatedChildWidth += child.getMeasuredWidth();
            
            
//            childMostRight += child.getWidth();
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
//		Log.i(TAG,"currentInterceptedTouchedView "+currentInterceptedTouchedView );
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
//			int topChildViewMovedIndex = getChildCount()-1;
//			moveAllChildViews(topChildViewMovedIndex, moveAmount);
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

//	private boolean moveUnderViewToRight(int viewReferenceIndex) {
//		View currentView = getChildAt(viewReferenceIndex);
//		LayoutParams currentLp = (LayoutParams) currentView.getLayoutParams();
//		boolean moveUnderView = true;
//		while (moveUnderView){
//			LayoutParams underViewLp = moveViewUnderToRight(currentLp);
//			if(underViewLp.underView == null || ((LayoutParams)underViewLp.underView.getLayoutParams()).underView == null){
//				moveUnderView = false;
//			}else{
//				currentLp = underViewLp;
//			}
//		}
//	}

//	private boolean moveViewAtIndexToRight(int viewIndex, int moveAmount) {
//		if(viewIndex<currentInterceptedTouchedView){
//			View viewToMove = getChildAt(viewIndex);
//			LayoutParams viewToMoveLp = (LayoutParams) viewToMove.getLayoutParams();
//			if(!viewToMoveLp.fixed){
//				int viewToMoveWidth = viewToMove.getMeasuredWidth();
//				if(currentLp.left>=viewToMoveLp.left+viewToMoveWidth){
//					int newUnderLeftPos = currentLp.left - viewToMoveWidth;
//					viewToMoveLp.left = newUnderLeftPos;
//					updateViewLayout(viewToMove, viewToMoveLp);
//				}
//			}
//		}
//		View viewMoved = getChildAt(viewIndex);
//		LayoutParams viewMovedLp = (LayoutParams) viewMoved.getLayoutParams();
//		if(!viewMovedLp.fixed){
//			viewMovedLp.left -=moveAmount;
//			if(viewMovedLp.left<0 && viewIndex != upperChild)
//				viewMovedLp.left=0;
//			updateViewLayout(viewMoved, viewMovedLp);
//		}
//		return false;
//	}

	private void moveViewAtIndexToLeft(int viewIndex, int upperChild, int moveAmount) {
		View viewMoved = getChildAt(viewIndex);
		LayoutParams viewMovedLp = (LayoutParams) viewMoved.getLayoutParams();
		if(!viewMovedLp.fixed){
			viewMovedLp.left -=moveAmount;
			if(viewMovedLp.left<0 && viewIndex != upperChild)
				viewMovedLp.left=0;
			updateViewLayout(viewMoved, viewMovedLp);
		}
	}

//	private void moveAllChildViews(int topChildViewMovedIndex, int moveAmount) {
//		moveView(topChildViewMovedIndex, moveAmount);
//		//if first, no under view
//		if(topChildViewMovedIndex>0){
//			moveViewUnder(topChildViewMovedIndex, moveAmount);
//		}
//	}
	
//	private void moveView(int topChildViewMovedIndex, int moveAmount) {
//		View viewMoved = getChildAt(topChildViewMovedIndex);
//		LayoutParams viewMovedLp = (LayoutParams) viewMoved.getLayoutParams();
//		if(!viewMovedLp.fixed){
//			viewMovedLp.left -=moveAmount;
//			updateViewLayout(viewMoved, viewMovedLp);
//		}
//	}
//
//	private void moveViewUnder(int topChildViewMovedIndex, int moveAmount) {
//		View currentView = getChildAt(topChildViewMovedIndex);
//		LayoutParams currentLp = (LayoutParams) currentView.getLayoutParams();
//		boolean moveUnderView = true;
//		while (moveUnderView){
//			LayoutParams underViewLp = null;		
////			if(isMovingToRight(moveAmount)){
//				underViewLp = moveViewUnderToRight(currentLp);
////			}else{
////				underViewLp = moveViewUnderToLeft(currentLp);
////			}
//			if(underViewLp.underView == null || ((LayoutParams)underViewLp.underView.getLayoutParams()).underView == null){
//				moveUnderView = false;
//			}else{
//				currentLp = underViewLp;
//			}
//		}
//		
//	}
	
	private boolean isMovingToRight(int moveAmount) {
		if(moveAmount<0)
			return true;
		else
			return false;
	}
//
//	private LayoutParams moveViewUnderToRight(LayoutParams currentLp) {
//		View underView = currentLp.underView;
//		int underViewWidth = underView.getMeasuredWidth();
//		LayoutParams underViewLp = (LayoutParams) underView.getLayoutParams();
//		if(currentLp.left>=underViewLp.left+underViewWidth){
//			int newUnderLeftPos = currentLp.left - underViewWidth;
//			underViewLp.left = newUnderLeftPos;
//			updateViewLayout(underView, underViewLp);
//		}
//		
//		return underViewLp;
//	}
//
//	private LayoutParams moveViewUnderToLeft(LayoutParams currentLp) {
//		View underView = currentLp.underView;
//		LayoutParams underViewLp = (LayoutParams) underView.getLayoutParams();
//		if(!underViewLp.fixed){
//			int underViewWidth = underView.getMeasuredWidth();
//			int coverSize = 0;//underViewWidth/2;
//			
//			if (currentLp.left<=(underViewLp.left+underViewWidth-coverSize)){
//				int newUnderLeftPos = currentLp.left -(underViewWidth-coverSize);
//				underViewLp.left = (newUnderLeftPos>0?newUnderLeftPos:0);
//				updateViewLayout(underView, underViewLp);
//			}
//		}
//		return underViewLp;
//	}

	@Override
	public void onEndMove(int velocity) {
		
		
		//check for velocity
		int nbChild = getChildCount();
		
		
		
//		int referenceMoveViewIndex = nbChild-1;
//		if(currentInterceptedTouchedView!=NO_CHILD_TOUCHED)
//			referenceMoveViewIndex = currentInterceptedTouchedView;
//		
//		if(totalDelta>0){
		int NO_DELTA = Integer.MAX_VALUE;
			int tooMuchRightDelta = 0;
			int tooMuchLeftDelta = 0;
			int notEnoughtRightDelta = 0;
			int notEnoughtLeftDelta = 0;
//			int rightDelta = NO_DELTA;
//			int leftDelta = NO_DELTA;
//			boolean accumulate = false;
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
//				else {
//					if(referenceMoveViewLp.left<underRight && (rightDelta==NO_DELTA||accumulate)){
//						if(rightDelta==NO_DELTA) rightDelta=0;
//						rightDelta -= underRight - referenceMoveViewLp.left ;
//						
//					}
//					if(referenceMoveViewLp.left > underLeft && (leftDelta==NO_DELTA||accumulate)){
//						if(leftDelta==NO_DELTA) leftDelta=0;
//						leftDelta += referenceMoveViewLp.left - underLeft;
//					}
//					int moveAmount = 0;
//					if(rightDelta!=NO_DELTA && leftDelta!=NO_DELTA){
//						moveAmount = (Math.abs(rightDelta)>Math.abs(leftDelta))?leftDelta:rightDelta;
//					}else if(rightDelta!=NO_DELTA){
//						moveAmount = rightDelta;
//					}else if(leftDelta!=NO_DELTA){
//						moveAmount = leftDelta;
//					}
//					
//					if(moveAmount!=0){
//						animViewLayout(i,moveAmount);
//						if(Math.abs(totalDelta)>Math.abs(moveAmount)){
////							totalDelta += moveAmount;
////							accumulate = true;
//						}else{
////							accumulate = false;
//						}
//					}
//
//				}
//			}
//		}else{
//			
//		}
		
		
//		
//		
//		//check if lower moveable view is to far from lower (not moveable view)
//		int count = getChildCount();
//		int lowerFixedViewIndex = 0;//TODO put this to global and set adding view
//		int lowerMoveableViewIndex = 1;
//		if(count >= lowerMoveableViewIndex){
//			View lowerFixedView = getChildAt(lowerFixedViewIndex);
//			LayoutParams fixedLp = (LayoutParams) lowerFixedView.getLayoutParams();
//			View lowerMoveableView = getChildAt(lowerMoveableViewIndex);
//			LayoutParams moveableLp = (LayoutParams) lowerMoveableView.getLayoutParams();
//			
//			int fixedViewRight = lowerFixedView.getMeasuredWidth()+fixedLp.left;
//
//			int delta = moveableLp.left - (fixedViewRight);
//			if(delta>0){
//				for(int i=lowerMoveableViewIndex; i<count; i++){
//					final View view = getChildAt(i);
//					final LayoutParams lp = (LayoutParams) view.getLayoutParams();
//					ObjectAnimator bob = ObjectAnimator.ofInt(lp, "left", lp.left-delta);
//					bob.addUpdateListener(new AnimatorUpdateListener() {
//						
//						@Override
//						public void onAnimationUpdate(ValueAnimator animation) {
//							updateViewLayout(view, lp);
//							
//						}
//					});
//					bob.start();
//				}
//			}
//		}
		currentInterceptedTouchedView = NO_CHILD_TOUCHED;
	}

	
	private void animViewLayout(int i, int delta, int velocity) {
		final View view = getChildAt(i);
		final LayoutParams lp = (LayoutParams) view.getLayoutParams();
		ObjectAnimator animator = ObjectAnimator.ofInt(lp, "left", lp.left-delta);
		if(velocity!=0){
	        velocity = Math.abs(velocity);
	        long duration = Math.round(1000 * Math.abs((float)delta / velocity));
			duration = Math.min(duration, MAX_ANIMATION_DURATION);
			animator.setDuration(duration);
		}
		animator.addUpdateListener(new AnimatorUpdateListener() {
			
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
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

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}
	
	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
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
//
//		public LayoutParams(int w, int h, int left) {
//			super(w, h);
//			this.left = left;
//		}

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
