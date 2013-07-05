package com.mobilisens.demo.widget.stacklayout;

import com.mobilisens.widget.stacklayout.StackLayout;
import com.mobilisens.widget.stacklayout.StackLayout.StackLayoutParams;
import com.mobilisens.widget.stacklayout.StackViewContainer;

import android.app.Activity;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class StackLayoutDemo extends Activity{

	

	private static final int NB_IN_LIST = 100;
	protected final String LOG_TAG = getClass().getSimpleName();
	private StackLayout stackLayout;
	private final int nbBasePanel = 1;
	private TextView nbChildInfo;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stack_layout_demo);
		stackLayout = (StackLayout)findViewById(R.id.stackLayout);
		initFirstPanel();
	}

	private void initFirstPanel() {
		LinearLayout firstChild = (LinearLayout) findViewById(R.id.firstChild);
		SeekBar nbChild = (SeekBar) firstChild.findViewById(R.id.nbChild);
		nbChild.setOnSeekBarChangeListener(nbChildChangeListener);
		
		nbChildInfo = (TextView) firstChild.findViewById(R.id.nbChildInfo);
		nbChildInfo.setText(getString(R.string.configurator_nbChild, 0));
	}

	private OnSeekBarChangeListener nbChildChangeListener = new OnSeekBarChangeListener() {
		

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			int currentNbChild = stackLayout.getChildCount()-nbBasePanel;
//			Log.i(LOG_TAG, "onProgressChanged currentNbChild "+currentNbChild+" progress "+progress);
			if(progress>currentNbChild){
				for (int i = currentNbChild; i <= progress; i++) {
					addPanelToStackLayout();
				}
			}else if(progress<currentNbChild){
				for (int i = currentNbChild; i > progress; i--) {
					removePanelToStackLayout();
				}
			}
			nbChildInfo.setText(getString(R.string.configurator_nbChild, progress));
		}
	};
	

	private void addPanelToStackLayout() {
		int index = stackLayout.getChildCount();
		String anchors;
		if(index<2){
			anchors =  "1.;1.";
		}else{
			anchors =  "0.;0.5;1.";
		}
		stackLayout.addView(buildSimpleList(), index, stackLayout.generateLayoutParams(false, true, anchors, 1));
	}

	private View buildSimpleList() {
		ListView list = new ListView(getApplicationContext());
		int nbPanel = stackLayout.getChildCount()-1;
		list.setBackgroundColor(Color.GRAY- (0x80808 * nbPanel)%Color.GRAY);

		
		String[] objects = new String[NB_IN_LIST];
		for(int i=0; i<objects.length; i++){
			objects[i] = "element "+i;
		}
		list.setAdapter(new ArrayAdapter<String>(getApplicationContext(), R.layout.standard_row, R.id.textContent, objects));
		
		return list;
	}

	private void removePanelToStackLayout() {
		stackLayout.removeViewAt(stackLayout.getChildCount()-1);
	}
}
