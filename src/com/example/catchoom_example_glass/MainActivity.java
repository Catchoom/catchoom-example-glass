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
	private final String TAG= "CatchoomGlassExample";
	
	private String mCollectionToken = "catchoomcooldemo";
	
	private FrameLayout mPreview;
	private Context mContext;
	private Catchoom mCatchoom;
	private CatchoomImageHandler mCatchoomImageHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mPreview = (FrameLayout) findViewById(R.id.camera_preview);
		
		mContext = getApplicationContext();
		//The CatchoomImageHandler is the object that receives the callbacks from the camera.
		mCatchoomImageHandler = (CatchoomImageHandler) this;
		
		// Setup the camera preview
		setCameraParams(mContext, mPreview);
		// Tell the parent activity who will receive the takePicture() callback
		setImageHandler(mCatchoomImageHandler);
		
		// Create the Catchoom object. The catchoom object is the responsible to do the network calls.
		mCatchoom = new Catchoom();
		// Tell the catchoom object who will receive the network responses.
		mCatchoom.setResponseHandler((CatchoomResponseHandler) this);
		//Optional call: Check if the collection token is valid and there's connectivity with the server
		mCatchoom.connect(mCollectionToken);
		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			//Take picture when tapping on the touchpad
			takePicture();
			//Note that this call freezes the preview. To take more pictures, you have to call restartPreview()
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}


	@Override
	public void requestImageError(String errorMessage) {
		//There was an error taking the picture!
		//Restart the camera to allow to take another picture.
		restartPreview();
	}

	@Override
	public void requestImageReceived(CatchoomImage item) {
		mCatchoom.search(mCollectionToken, item);	
	}

	@SuppressWarnings("unchecked")
	@Override
	public void requestCompletedResponse(int requestCode, Object item) {
		
		if (requestCode == Catchoom.Request.CONNECT_REQUEST) {
			// Connect response: Connection accepted
			Log.i("catchoom-example-glass", "Connection established");
		} else if (requestCode == Catchoom.Request.SEARCH_REQUEST) {
			ArrayList<CatchoomSearchResponseItem> items = (ArrayList<CatchoomSearchResponseItem>) item;
			
			// Check if at least one item was found
			if(items.size() > 0) {
				//In this example we just consider the best match.
				//Note that the API can return more than one match, and they are sorted by the confidence score.
				CatchoomSearchResponseItem bestMatch = items.get(0);
				
				//Create a card with the content of the matched image.
				Card card = new Card(this);
				card.setText(bestMatch.getItemName());
				card.setFootnote(bestMatch.getItemId());
	
				View cardView = card.getView();
				setContentView(cardView);
			}else{
				//Nothing was found. Restart the preview to allow to take more pictures.
				restartPreview();
			}
		}

	}

	@Override
	public void requestFailedResponse(int requestCode, CatchoomErrorResponseItem responseError) {
		//Something went wrong. Either there's no connectivity, the collection token is invalid, the image has not enough details, etc.
		
		if (null == responseError) {
			Log.e(TAG,"Check your internet connection");
		} else {
			Log.d(TAG, responseError.getErrorCode() + ": " + responseError.getErrorMessage());
			switch (responseError.getErrorCode()) {
				case CatchoomErrorResponseItem.ErrorCodes.TOKEN_INVALID:
					Log.e(TAG,"The collection token is invalid");
					break;
				case CatchoomErrorResponseItem.ErrorCodes.TOKEN_WRONG:
					Log.e(TAG,"Wrong collection token. Note that a collection token must have 16 characters");
					break;
				case CatchoomErrorResponseItem.ErrorCodes.IMAGE_NO_DETAILS:
					Log.e(TAG,"The requested image has not enough details");
					break;
				default:
					Log.e(TAG,"Unknown error occurred");
			}
		}
	}
}
