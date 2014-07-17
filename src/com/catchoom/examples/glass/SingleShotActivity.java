package com.catchoom.examples.glass;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.catchoom.CatchoomActivity;
import com.catchoom.CatchoomCamera;
import com.catchoom.CatchoomCameraView;
import com.catchoom.CatchoomCloudRecognition;
import com.catchoom.CatchoomCloudRecognitionError;
import com.catchoom.CatchoomCloudRecognitionItem;
import com.catchoom.CatchoomImage;
import com.catchoom.CatchoomImageHandler;
import com.catchoom.CatchoomResponseHandler;
import com.catchoom.CatchoomSDK;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;



public class SingleShotActivity extends CatchoomActivity implements
CatchoomResponseHandler, CatchoomImageHandler {
	private final String TAG= "CatchoomGlassExample";
	
	//TODO: modify this token to point to your collection!
	private String mCollectionToken = "catchoomcooldemo";
	
	private TextView mTextView;
	
	private CatchoomCamera mCatchoomCamera;
	private CatchoomCloudRecognition mCloudRecognition;
	
    private GestureDetector mGestureDetector;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
	}
	@Override
	public void onPostCreate() {
		View mainLayout = (View) getLayoutInflater().inflate(R.layout.activity_single_shot, null);
		CatchoomCameraView cameraView = (CatchoomCameraView) mainLayout.findViewById(R.id.camera_preview);
		super.setCameraView(cameraView);
		setContentView(mainLayout);
		
        mGestureDetector = createGestureDetector(this);

		mTextView = (TextView) findViewById(R.id.singleshot_textview);
		
		//Initialize the SDK. From this SDK, you will be able to retrieve the necessary modules to use the SDK (camera, tracking, and cloud-recgnition)
		CatchoomSDK.init(getApplicationContext(),this);
		
		//Get the camera to be able to do single-shot (if you just use finder-mode, this is not necessary)
		mCatchoomCamera = CatchoomSDK.getCamera();
		mCatchoomCamera.setImageHandler(this); //Tell the camera who will receive the image after takePicture()
		
		//Setup the finder-mode: Note! PRESERVE THE ORDER OF THIS CALLS
		mCloudRecognition= CatchoomSDK.getCloudRecognition();//Obtain the cloud recognition module
		mCloudRecognition.setResponseHandler(this); //Tell the cloud recognition who will receive the responses from the cloud
		mCloudRecognition.setCollectionToken(mCollectionToken); //Tell the cloud-recognition which token to use from the finder mode

		mCloudRecognition.connect(mCollectionToken);	
	}

	private GestureDetector createGestureDetector(Context context) {
		GestureDetector gestureDetector = new GestureDetector(context);
		// Create a base listener for generic gestures
		gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
			@Override
			public boolean onGesture(Gesture gesture) {
				// 1 tap, Single shot mode
				if (gesture == Gesture.TAP) {
					Log.i(TAG,"take picture");
					mTextView.setText("Searching...");
					mCatchoomCamera.takePicture();
					return true;
				// 1 long tap, Finder mode  
				} else if (gesture == Gesture.LONG_PRESS) {
					Log.i(TAG,"start finding");
					mTextView.setText("Scanning");
					mCloudRecognition.startFinding();
					return true;
				}
				return false;
			}
		});
		return gestureDetector;
	}
	
	/*
	 * Send generic motion events to the gesture detector
	 */
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (mGestureDetector != null) {
			return mGestureDetector.onMotionEvent(event);
		}
		return false;
	}

	@Override
	public void requestImageReceived(CatchoomImage image) {
		Bitmap originalImage = image.toBitmap();
		
		//Crop to half the size (from VGA to QVGA) centered.
 		int newX = (int) (originalImage.getWidth()/4);
		int newY = (int) (originalImage.getHeight()/4);
		int newWidth = (int) (originalImage.getWidth()/2); 
		int newHeight = (int) (originalImage.getHeight()/2);
		Bitmap croppedImage = Bitmap.createBitmap(originalImage, newX, newY, newWidth, newHeight);
		mCloudRecognition.searchWithImage(mCollectionToken, image);	
	}

	@Override
	public void searchCompleted(ArrayList<CatchoomCloudRecognitionItem> items) {
			
		// Check if at least one result was found
		if(items.size() > 0) {
			mCloudRecognition.stopFinding();
			//Pass the results to another activity that will show a card with their content
			Intent showResultIntent = new Intent(getApplicationContext(),ResultActivity.class);
			showResultIntent.putParcelableArrayListExtra("results",items);
			startActivity(showResultIntent);
		
			//Restart the preview for future searches
			mTextView.setText("Tap to scan");	
		}
	}

	@Override
	public void connectCompleted() {
		// Connect response: Connection accepted
		Log.i(TAG, "Succesfull connection. Token is valid and the server can be reached.");		
	}

	@Override
	public void requestFailedResponse(int requestCode,
			CatchoomCloudRecognitionError responseError) {
		//Something went wrong. Either there's no connectivity, the collection token is invalid, the image has not enough details, etc.
		
		if (null == responseError) {
			Log.e(TAG,"Check your internet connection");
		} else {
			Log.d(TAG, responseError.getErrorCode() + ": " + responseError.getErrorMessage());
			switch (responseError.getErrorCode()) {
				case CatchoomCloudRecognitionError.ErrorCodes.TOKEN_INVALID:
					Log.e(TAG,"The collection token is invalid");
					break;
				case CatchoomCloudRecognitionError.ErrorCodes.TOKEN_WRONG:
					Log.e(TAG,"Wrong collection token. Note that a collection token must have 16 characters");
					break;
				case CatchoomCloudRecognitionError.ErrorCodes.IMAGE_NO_DETAILS:
					Log.e(TAG,"The requested image has not enough details");
					break;
				default:
					Log.e(TAG,"Unknown error occurred");
			}
		}
		mCloudRecognition.stopFinding();
	}
	
	@Override
	public void requestImageError(String error) {
		//There was an error taking the picture!
		//Restart the camera to allow to take another picture.
		mCatchoomCamera.restartCameraPreview();
		mTextView.setText("Tap to scan");		
	}
}
