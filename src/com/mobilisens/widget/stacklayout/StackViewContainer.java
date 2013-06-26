package com.mobilisens.widget.stacklayout;

import com.mobilisens.widget.stacklayout.StackLayout.StackLayoutParams;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;

public class StackViewContainer extends LinearLayout {

	private static final long MAX_ANIMATION_DURATION = 400;
	private static final int MIN_VELOCITY_TO_FLING = 300;
	private final String LOG_TAG = getClass().getSimpleName();
	private final boolean DEBUG = false;
	private boolean isMeasured = false;
	private View contentView;
	private View decorView;
	private int indexInParent;
	private boolean isMoving = false;

	public StackViewContainer(Context context) {
        super(context);
        initStackViewContainer();
    }

	public StackViewContainer(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
        initStackViewContainer();
	}

	private void initStackViewContainer() {
		addGlobalEventCatcher();
	}


	// Need to catch all event to prevent bubbling to other StackViewContainer in StackLayout. 
	// It's because viewgroup are not designed to have view over each other
	private void addGlobalEventCatcher() {
		setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});
	}

	public boolean isMeasured(){
		return isMeasured;
	}

	public void addContentAndDecorViews(View viewToAdd, View decorView) {
		if(viewToAdd!=null){
			addContentView(viewToAdd);
			addDecorView(decorView);
		}
	}

	private void addContentView(View viewToAdd) {
		contentView = viewToAdd;
		LayoutParams contentParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		buildContentId();
		addView(viewToAdd, contentParams);
	}

	private void buildContentId() {
		if(contentView.getId() == View.NO_ID){
			contentView.setId(generateViewId());
		}
	}

	private void addDecorView(View decorViewToAdd) {
		decorView = decorViewToAdd;
		if(decorView==null){
//			StackLayoutParams lp = (StackLayoutParams) getLayoutParams();
//			if(lp.needShadow!=0){
//				decorView = new 
//			}
		}else{
			LayoutParams decorParams = generateDecorLayoutParams(decorView.getLayoutParams());
			decorView.setLayoutParams(decorParams);
			addDecorView(decorView, 0);
		}
		
	}
	
	private void addDecorView(View decorView, int index) {
		addView(decorView, 0);
		addDecorViewEventDenied();
	}

	private void addDecorViewEventDenied() {
		decorView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return false;
			}
		});
	}
	
	protected LayoutParams generateDecorLayoutParams(ViewGroup.LayoutParams p) {
		LayoutParams result;
		if(p==null){
			result = new LayoutParams(15, LayoutParams.MATCH_PARENT);
		}else{
			if (checkLayoutParams(p))
				result =  (LayoutParams) p;
			else
				result = new LayoutParams(p.width, p.height);
		}
		return result;
	}
	
	
	public boolean decorViewIsPresent(){
		return decorView!=null;
	}

	public View getDecorView() {
		return decorView;
	}
	
	public int getDecorViewMeasuredWidth(){
		if(decorView!=null)
			return decorView.getMeasuredWidth();
		else 
			return 0;
	}
	
	public int getMeasuredWidthWithoutDecorView(){
		int decorWidth = 0;
		if(decorView!=null){
			decorWidth = decorView.getMeasuredWidth();
		}
		return getMeasuredWidth()-decorWidth;
	}

	public View getContentView() {
		return contentView;
	}
	
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		contentView.measure(widthMeasureSpec, heightMeasureSpec);

		if(decorViewIsPresent()){
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec)+decorView.getMeasuredWidth(), MeasureSpec.EXACTLY);
		}
		
		setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
		
		setLayoutParamsViewPos();
	}
	
	
	private void setLayoutParamsViewPos() {
		StackLayoutParams params = (StackLayoutParams) getLayoutParams();
			
		if(!params.isViewPosSet()){

			if(DEBUG){Log.i(LOG_TAG, "setLayoutParamsViewPos for view index "+indexInParent);}
    		
			if(decorViewIsPresent()){
				int decorWidth = decorView.getMeasuredWidth();
				params.setDecorViewWidth(decorWidth);
			}

        	if(indexInParent==0){
        		int underPos = ((StackLayout) getParent()).getLeft();
				params.setUnderPos(underPos);
        	}else{
        		StackViewContainer underView = (StackViewContainer) ((StackLayout) getParent()).getChildAt(indexInParent-1);
        		StackLayoutParams underParams = (StackLayoutParams)underView.getLayoutParams();
        		int underViewPos = underParams.getContentViewPos();
        		if(DEBUG){Log.i(LOG_TAG, "underViewPos "+underViewPos);}
				int underWidth = underView.getMeasuredWidth()-underParams.getDecorViewWidth();
				
				params.setUnderPosAndWidth(underViewPos, underWidth);
        	}

			
			
        	requestLayout();
        }
	}

	public void setIndexInParent(int index){
		indexInParent = index;
	}
	
	public void movePanel(int moveAmount){
		if(DEBUG){Log.i(LOG_TAG, "index "+indexInParent +" movePanel");}
		StackLayoutParams params = (StackLayoutParams) getLayoutParams();
		if(!params.fixed){
			boolean hasUpperView = hasUpperView();
			params.changeContentViewPos(moveAmount, hasUpperView);
			((StackLayout) getParent()).updateViewLayout(this, params);
			if(hasUpperView){
				updateUpperPanelAnchors(params);
			}
			isMoving = true;
			moveOtherPanels(moveAmount, params);
			isMoving = false;
		}
		
	}
	
	private void updateUpperPanelAnchors(StackLayoutParams params) {
		if(DEBUG){Log.i(LOG_TAG, "index "+indexInParent +" updateUpperPanelAnchors");}
		((StackLayoutParams)(((StackLayout)getParent()).getChildAt(indexInParent+1)).getLayoutParams()).updateUnderPos(params.getContentViewPos());
	}

	private void moveOtherPanels(int moveAmount, StackLayoutParams params) {
		if(DEBUG){Log.i(LOG_TAG, "index "+indexInParent +"moveOtherPanels");}
		if(hasUpperView()){
			moveUpperPanel(moveAmount);
		}
		if(hasUnderView()){
			moveUnderPanel(moveAmount, params);
		}
	}


	private void moveUpperPanel(int moveAmount) {
		if(DEBUG){Log.i(LOG_TAG, "index "+indexInParent +" moveUpperPanel");}
		StackLayout parent = (StackLayout) getParent();
		StackViewContainer upperPanel = ((StackViewContainer)parent.getChildAt(indexInParent+1));
		if(!upperPanel.isMoving){
			if(upperPanel.shouldMoveFromUnder(moveAmount)){
				upperPanel.movePanel(moveAmount);
			}
		}
	}
	
	private boolean shouldMoveFromUnder(int moveAmount) {
		if(MoveController.isMovingToRight(moveAmount)){
			return true;
		}else{
			return true;
		}
	}

	private void moveUnderPanel(int moveAmount, StackLayoutParams params) {
		if(DEBUG){Log.i(LOG_TAG, "index "+indexInParent +" moveUnderPanel");}
		StackLayout parent = (StackLayout) getParent();
		StackViewContainer underPanel = ((StackViewContainer)parent.getChildAt(indexInParent-1));
		if(!underPanel.isMoving){
			if(DEBUG){Log.i(LOG_TAG, "index "+indexInParent +" moveAmount before "+moveAmount);}
			moveAmount = underPanel.shouldMoveFromUpper(moveAmount, params);
			if(DEBUG){Log.i(LOG_TAG, "\tmoveAmount after "+moveAmount);}
			if(moveAmount!=0){
				underPanel.movePanel(moveAmount);
			}
		}
	}

	private int shouldMoveFromUpper(int moveAmount, StackLayoutParams upperParams) {
		StackLayoutParams params = (StackLayoutParams)getLayoutParams();
		moveAmount = params.howMuchShouldMoveFromUpper(moveAmount, upperParams);
		return moveAmount;
		
	}

	private boolean hasUpperView() {
		ViewParent parent = getParent();
		if(parent==null)
			return false;
		int nbChild = ((StackLayout) parent).getChildCount();
		return (indexInParent<nbChild-1);
	}

	private boolean hasUnderView() {
		return (indexInParent>0);
	}

	@Override
	public void getHitRect(Rect outRect) {
		super.getHitRect(outRect);
		int width = getMeasuredWidthWithoutDecorView();
		outRect.left += (outRect.width()-width);
	}

	public void animToNearestAnchor(int velocity) {
//		Log.i(LOG_TAG, "moveToNearestAnchor");
		StackLayoutParams params = ((StackLayoutParams)getLayoutParams());
		int moveAmount = 0;
		if(Math.abs(velocity)<MIN_VELOCITY_TO_FLING){
			moveAmount = params.getDistanceToNearestAnchor();
		}else{
			moveAmount = params.getDistanceToNearestAnchor(velocity);
		}
		if(moveAmount!=0){
			animPanel(moveAmount, velocity);
			if(hasUpperView()){
				animUpperPanel(moveAmount, velocity);
			}
		}
	}

	private void animUpperPanel(int moveAmount, int velocity) {
		StackLayout parent = ((StackLayout)getParent());
		int nbChild = parent.getChildCount();
		for(int i=indexInParent+1; i<nbChild; i++){
			((StackViewContainer)parent.getChildAt(i)).animPanel(moveAmount, velocity);
		}
	}

	private void animPanel(final int moveAmount, int velocity) {
		final StackLayoutParams params = (StackLayoutParams) getLayoutParams();

		if(params.fixed)
			return;
		
		final ObjectAnimator animator = ObjectAnimator.ofInt(params, "contentViewPos", params.getContentViewPos()-moveAmount);
		if(velocity!=0){
	        velocity = Math.abs(velocity);
	        long duration = Math.round(1000 * Math.abs((float)moveAmount / velocity));
			duration = Math.min(duration, MAX_ANIMATION_DURATION);
			animator.setDuration(duration);
		}
		animator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				//part for stoping animation if view is deleted during animation
				ViewParent parent = getParent();
				if(parent==null){
					animator.cancel();
					return;
				}
				int count = ((StackLayout)parent).getChildCount();
				if(indexInParent>count-1){
					animator.cancel();
					return;
				}
				View currentViewAtPosI = ((StackLayout)parent).getChildAt(indexInParent);
				if(currentViewAtPosI!= StackViewContainer.this){
					animator.cancel();
					return;
				}
				((StackLayout)parent).updateViewLayout(StackViewContainer.this, params);
			}
		});
		animator.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(Animator arg0) {
			}
			@Override
			public void onAnimationRepeat(Animator arg0) {
			}
			@Override
			public void onAnimationEnd(Animator arg0) {
				if(hasUpperView())
					updateUpperPanelAnchors((StackLayoutParams) getLayoutParams());
			}
			@Override
			public void onAnimationCancel(Animator arg0) {
			}
		});
		animator.start();
	}

	public void animToLeftAnchor() {
		StackLayoutParams params = ((StackLayoutParams)getLayoutParams());
		if(params.fixed)
			return;
		int moveAmount = params.getDistanceToLeftAnchor();
		if(moveAmount!=0){
			animPanel(moveAmount, 0);
			if(hasUpperView()){
				animUpperPanel(moveAmount, 0);
			}
		}
	}
	
}
