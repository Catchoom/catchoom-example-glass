package com.catchoom.examples.glass;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.catchoom.api.Catchoom;
import com.catchoom.api.CatchoomErrorResponseItem;
import com.catchoom.api.CatchoomResponseHandler;
import com.catchoom.api.CatchoomSearchResponseItem;
import com.catchoom.camera.CameraConfig;
import com.catchoom.camera.CatchoomImage;
import com.catchoom.camera.CatchoomImageHandler;
import com.catchoom.camera.CatchoomSingleShotActivity;

public class SingleShotActivity extends CatchoomSingleShotActivity implements
CatchoomResponseHandler, CatchoomImageHandler {
	private final String TAG= "CatchoomGlassExample";
	
	//TODO: modify this token to point to your collection!
	private String mCollectionToken = "catchoomcooldemo";
	
	private FrameLayout mPreview;
	private Context mContext;
	private TextView mTextView;
	private Catchoom mCatchoom;
	private CatchoomImageHandler mCatchoomImageHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_single_shot);
		
		mPreview = (FrameLayout) findViewById(R.id.single_shot_preview);
		mTextView = (TextView) findViewById(R.id.singleshot_textview);
		mContext = getApplicationContext();
		
		//The CatchoomImageHandler is the object that receives the callbacks from the camera.
		mCatchoomImageHandler = (CatchoomImageHandler) this;
		
		// Request the pictures in VGA resolution (640x480) (default is QVGA 320x240).
		// We will later crop the pictures to 320x240 before sending them, in the requestImageReceived() method.
		// This method MUST BE CALLED before setCameraParams(), otherwise it has no effect. 
		CameraConfig.setPictureSize(CameraConfig.PICTURE_SIZE_VGA);
		
		// Setup the camera preview
		setCameraParams(mContext, mPreview);
		// Set the handler that will receive the takePicture() callback
		setImageHandler(mCatchoomImageHandler);
		
		// Create the Catchoom object. The catchoom object is the responsible to do the network calls.
		mCatchoom = new Catchoom();
		// Setup the handler that will receive the network responses.
		mCatchoom.setResponseHandler((CatchoomResponseHandler) this);
		
		//OPTIONAL: Check if the collection token is valid and there's connectivity with the server
		mCatchoom.connect(mCollectionToken);
			
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			//Take picture when tapping on the touchpad
			//Note that the takePicture() call freezes the preview: To take more pictures, you have to call restartPreview()
			mTextView.setText("Searching...");
			takePicture();
			
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}


	@Override
	public void requestImageError(String errorMessage) {
		//There was an error taking the picture!
		//Restart the camera to allow to take another picture.
		restartPreview();
		mTextView.setText("Tap to scan");
	}

	@Override
	public void requestImageReceived(CatchoomImage image) {
		
		Bitmap originalImage= image.toBitmap();
		
		//Crop to half the size (from VGA to QVGA) centered.
 		int newX = (int) (originalImage.getWidth()/4);
		int newY = (int) (originalImage.getHeight()/4);
		int newWidth = (int) (originalImage.getWidth()/2); 
		int newHeight = (int) (originalImage.getHeight()/2);
		Bitmap croppedImage = Bitmap.createBitmap(originalImage, newX, newY, newWidth, newHeight);
		mCatchoom.search(mCollectionToken, croppedImage);
	
	}

	@SuppressWarnings("unchecked")
	@Override
	public void requestCompletedResponse(int requestCode, Object item) {
		
		if (requestCode == Catchoom.Request.CONNECT_REQUEST) {
			// Connect response: Connection accepted
			Log.i("catchoom-example-glass", "Succesfull connection. Token is valid and the server can be reached.");
		} else if (requestCode == Catchoom.Request.SEARCH_REQUEST) {
			ArrayList<CatchoomSearchResponseItem> items = (ArrayList<CatchoomSearchResponseItem>) item;
			
			// Check if at least one result was found
			if(items.size() > 0) {
				//Pass the results to another activity that will show a card with their content
				Intent showResultIntent = new Intent(getApplicationContext(),ResultActivity.class);
				showResultIntent.putParcelableArrayListExtra("results",items);
				startActivity(showResultIntent);
			}
			//Restart the preview for future searches
			restartPreview();
			mTextView.setText("Tap to scan");
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
