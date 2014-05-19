package com.example.catchoom_example_glass;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.catchoom.api.Catchoom;
import com.catchoom.api.CatchoomErrorResponseItem;
import com.catchoom.api.CatchoomResponseHandler;
import com.catchoom.api.CatchoomSearchResponseItem;
import com.catchoom.camera.CatchoomImage;
import com.catchoom.camera.CatchoomImageHandler;
import com.catchoom.camera.CatchoomSingleShotActivity;
import com.catchoom.glass.R;
import com.google.android.glass.app.Card;

public class MainActivity extends CatchoomSingleShotActivity implements
CatchoomResponseHandler, CatchoomImageHandler {
	String token = "catchoomcooldemo";
	
	private FrameLayout mPreview;
	private Context mContext;
	private Catchoom mCatchoom;
	private CatchoomImageHandler mCatchoomImageHandler;
	private CatchoomSearchResponseItem mResult;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mPreview = (FrameLayout) findViewById(R.id.parent_layout);
		
		mContext = getApplicationContext();
		mCatchoomImageHandler = (CatchoomImageHandler) this;
		
		setCameraParams(mContext, mPreview);
		// Tell the parent activity who will receive the takePicture() callback
		setImageHandler(mCatchoomImageHandler);
		
		// Create the Catchoom object
		mCatchoom = new Catchoom();
		mCatchoom.setResponseHandler((CatchoomResponseHandler) this);
		mCatchoom.connect(token);
		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			// tap on touchpad
			takePicture();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}


	@Override
	public void requestImageError(String arg0) {
	}

	@Override
	public void requestImageReceived(CatchoomImage item) {
		mCatchoom.search(token, item);	
	}

	@Override
	public void requestCompletedResponse(int requestCode, Object item) {
		
		if (requestCode == Catchoom.Request.CONNECT_REQUEST) {
			// Connect response: Connection accepted
			Log.i("catchoom-example-glass", "Connection established");
			return;
		} else if (requestCode == Catchoom.Request.SEARCH_REQUEST) {
			ArrayList<CatchoomSearchResponseItem> array = (ArrayList<CatchoomSearchResponseItem>) item;
			
			// have content
			if(array.size() > 0) {
				
				Card card2 = new Card(this);
				card2.setText(array.get(0).getItemName());
				card2.setFootnote(array.get(0).getItemId());
				
				View card2View = card2.getView();
				setContentView(card2View);
			}
		}
		restartPreview();
	}

	@Override
	public void requestFailedResponse(int arg0, CatchoomErrorResponseItem arg1) {
		//
		
	}
}
