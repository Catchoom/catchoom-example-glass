package com.craftar.examples.glass;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.craftar.CraftARActivity;
import com.craftar.CraftARCamera;
import com.craftar.CraftARCameraView;
import com.craftar.CraftARCloudRecognition;
import com.craftar.CraftARCloudRecognitionError;
import com.craftar.CraftARImage;
import com.craftar.CraftARImageHandler;
import com.craftar.CraftARItem;
import com.craftar.CraftARResponseHandler;
import com.craftar.CraftARSDK;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;



public class SingleShotActivity extends CraftARActivity implements
CraftARResponseHandler, CraftARImageHandler {
	private final String TAG= "CraftARGlassExample";
	
	//TODO: modify this token to point to your collection!
	private String mCollectionToken = "catchoomcooldemo";
	
	private TextView mTextView;
	private boolean mHaveContent = false;
	
	private CraftARCamera mCraftARCamera;
	private CraftARCloudRecognition mCloudRecognition;
	
    private GestureDetector mGestureDetector;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
	}
	@Override
	public void onPostCreate() {
		View mainLayout = (View) getLayoutInflater().inflate(R.layout.activity_single_shot, null);
		CraftARCameraView cameraView = (CraftARCameraView) mainLayout.findViewById(R.id.camera_preview);
		super.setCameraView(cameraView);
		setContentView(mainLayout);
		
        mGestureDetector = createGestureDetector(this);

		mTextView = (TextView) findViewById(R.id.singleshot_textview);
		
		//Initialize the SDK. From this SDK, you will be able to retrieve the necessary modules to use the SDK (camera, tracking, and cloud-recgnition)
		CraftARSDK.init(getApplicationContext(),this);
		
		//Get the camera to be able to do single-shot (if you just use finder-mode, this is not necessary)
		mCraftARCamera = CraftARSDK.getCamera();
		mCraftARCamera.setImageHandler(this); //Tell the camera who will receive the image after takePicture()
		
		//Setup the finder-mode: Note! PRESERVE THE ORDER OF THIS CALLS
		mCloudRecognition= CraftARSDK.getCloudRecognition();//Obtain the cloud recognition module
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
					mCraftARCamera.takePicture();
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
	public void requestImageReceived(CraftARImage image) {
		mCloudRecognition.searchWithImage(mCollectionToken, image);	
	}

	@Override
	public void searchCompleted(ArrayList<CraftARItem> items) {
			
		// Check if at least one result was found
		if(items.size() > 0) {
			mHaveContent = true;
			mCloudRecognition.stopFinding();
			//Pass the results to another activity that will show a card with their content
			Intent showResultIntent = new Intent(getApplicationContext(),ResultActivity.class);
			showResultIntent.putParcelableArrayListExtra("results",items);
			startActivity(showResultIntent);
		}
		
		if (mHaveContent) {
			//Restart the preview for future searches
			mCraftARCamera.restartCameraPreview();
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
			CraftARCloudRecognitionError responseError) {
		//Something went wrong. Either there's no connectivity, the collection token is invalid, the image has not enough details, etc.
		
		if (null == responseError) {
			Log.e(TAG,"Check your internet connection");
		} else {
			Log.d(TAG, responseError.getErrorCode() + ": " + responseError.getErrorMessage());
			switch (responseError.getErrorCode()) {
				case CraftARCloudRecognitionError.ErrorCodes.TOKEN_INVALID:
					Log.e(TAG,"The collection token is invalid");
					break;
				case CraftARCloudRecognitionError.ErrorCodes.TOKEN_WRONG:
					Log.e(TAG,"Wrong collection token. Note that a collection token must have 16 characters");
					break;
				case CraftARCloudRecognitionError.ErrorCodes.IMAGE_NO_DETAILS:
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
		mCraftARCamera.restartCameraPreview();
		mTextView.setText("Tap to scan");		
	}
}
