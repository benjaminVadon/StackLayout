package com.mobilisens.widget.stacklayout;

import com.mobilisens.widget.stacklayout.StackLayout.StackLayoutParams;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class StackViewContainer extends LinearLayout {

	private final String LOG_TAG = getClass().getSimpleName();
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
//		((StackLayoutParams)getLayoutParams()).setViewPos();
		
		setLayoutParamsViewPos();
	}
	
	
	private void setLayoutParamsViewPos() {
		StackLayoutParams params = (StackLayoutParams) getLayoutParams();
			
		if(!params.isViewPosSet()){

    		Log.i(LOG_TAG, "setLayoutParamsViewPos for view index "+indexInParent);
    		
			if(decorViewIsPresent()){
				int decorWidth = decorView.getMeasuredWidth();
				params.setDecorViewWidth(decorWidth);
			}

        	if(indexInParent==0){
        		int underPos = ((StackLayout) getParent()).getLeft();
        		params.setViewPos(underPos);
        		params.initAnchors(new int[]{underPos});
        	}else{
        		StackViewContainer underView = (StackViewContainer) ((StackLayout) getParent()).getChildAt(indexInParent-1);
        		StackLayoutParams underParams = (StackLayoutParams)underView.getLayoutParams();
        		int underViewPos = underParams.getContentViewPos();
        		Log.i(LOG_TAG, "underViewPos "+underViewPos);
				int underWidth = underView.getMeasuredWidth()-underParams.getDecorViewWidth();
				params.setViewPos((int) (underViewPos+(underWidth*1.7f/3.f)));
				params.initAnchors(new int[]{underViewPos,underWidth});
        	}

			
			
        	requestLayout();
        }
	}

	public void setIndexInParent(int index){
		indexInParent = index;
	}
	
	public void movePanel(int moveAmount){
		StackLayoutParams params = (StackLayoutParams) getLayoutParams();
		if(!params.fixed){
			params.changeContentViewPos(moveAmount);
			((StackLayout) getParent()).updateViewLayout(this, params);
			isMoving = true;
			moveOtherPanels(moveAmount, params);
			isMoving = false;
		}
		
	}

	private void moveOtherPanels(int moveAmount, StackLayoutParams params) {
		int nbChild = ((StackLayout) getParent()).getChildCount();
		if(hasUpperView(nbChild)){
			updateUpperPanelAnchors(moveAmount);
			moveUpperPanel(moveAmount);
		}
		if(hasUnderView()){
			moveUnderPanel(moveAmount, params);
		}
	}

	private void updateUpperPanelAnchors(int moveAmount) {
		((StackLayoutParams)(((StackLayout)getParent()).getChildAt(indexInParent+1)).getLayoutParams()).updateAnchors(moveAmount);
	}

	private void moveUpperPanel(int moveAmount) {
		StackLayout parent = (StackLayout) getParent();
		StackViewContainer upperPanel = ((StackViewContainer)parent.getChildAt(indexInParent+1));
		if(!upperPanel.isMoving){
			if(upperPanel.shouldMoveFromUnder(moveAmount)){
				upperPanel.movePanel(moveAmount);
			}
		}
	}
	
	private boolean shouldMoveFromUnder(int moveAmount) {
		if(isMovingToRight(moveAmount)){
			return true;
		}else{
			return true;
		}
	}

	private void moveUnderPanel(int moveAmount, StackLayoutParams params) {
		StackLayout parent = (StackLayout) getParent();
		StackViewContainer underPanel = ((StackViewContainer)parent.getChildAt(indexInParent-1));
		if(!underPanel.isMoving){
			moveAmount = underPanel.shouldMoveFromUpper(moveAmount, params);
			if(moveAmount!=0){
				underPanel.movePanel(moveAmount);
			}
		}
	}

	private int shouldMoveFromUpper(int moveAmount, StackLayoutParams upperParams) {
		if(isMovingToRight(moveAmount)){
			return moveAmount;
		}else{
			StackLayoutParams params = (StackLayoutParams)getLayoutParams();
			moveAmount = params.shouldMove(moveAmount);
			return moveAmount;
		}
	}

	private boolean isMovingToRight(int moveAmount) {
		if(moveAmount<0)
			return true;
		else
			return false;
	}

	private boolean hasUpperView(int nbChild) {
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
	
}
