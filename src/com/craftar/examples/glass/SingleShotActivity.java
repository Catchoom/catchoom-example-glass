package com.craftar.examples.glass;

import java.util.ArrayList;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
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



public class SingleShotActivity extends CraftARActivity implements
CraftARResponseHandler, CraftARImageHandler{
	private final String TAG= "CraftARGlassExample";
	
	private String mCollectionToken = "catchoomcooldemo";
	
	private TextView mTextView;
	
	private CraftARCamera mCraftARCamera;
	private CraftARCloudRecognition mCloudRecognition;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
	}
	@Override
	public void onPostCreate() {
		View mainLayout = (View) getLayoutInflater().inflate(R.layout.camera_activity, null);
		
		CraftARCameraView cameraView = (CraftARCameraView) mainLayout.findViewById(R.id.camera_preview);
		super.setCameraView(cameraView);
		setContentView(mainLayout);
		
		mTextView = (TextView) findViewById(R.id.tap_to_scan_textview);
		
		//Initialize the SDK. From this SDK, you will be able to retrieve the necessary modules to use the SDK (camera, tracking, and cloud-recgnition)
		CraftARSDK.init(getApplicationContext(),this);
		
		//Get the camera to be able to do single-shot (if you just use finder-mode, this is not necessary)
		mCraftARCamera = CraftARSDK.getCamera();
		mCraftARCamera.setImageHandler(this); //Tell the camera who will receive the image after takePicture()
		
		//Setup the finder-mode: Note! PRESERVE THE ORDER OF THIS CALLS
		mCloudRecognition= CraftARSDK.getCloudRecognition();//Obtain the cloud recognition module
		mCloudRecognition.setResponseHandler(this); //Tell the cloud recognition who will receive the responses from the cloud
		mCloudRecognition.setCollectionToken(mCollectionToken); //Tell the cloud-recognition which token to use from the finder mode

		mCloudRecognition.connect(mCollectionToken); //This call is optional and it only validates the connectivity with the servers.	
	}

	@Override
	public boolean onKeyDown(int keyCode,KeyEvent event){
		super.onKeyDown(keyCode, event);
		if(keyCode== KeyEvent.KEYCODE_DPAD_CENTER){
			final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
		    tg.startTone(ToneGenerator.TONE_PROP_BEEP);
			
			mCraftARCamera.takePicture();
			return true;
		}
		return false;
	}

	@Override
	public void requestImageReceived(CraftARImage image) {
		mCloudRecognition.searchWithImage(mCollectionToken, image);	
	}

	@Override
	public void searchCompleted(ArrayList<CraftARItem> items) {

		//Restart the preview for future searches
		mTextView.setText("Tap to scan");		
		// Check if at least one result was found
		if(items.size() > 0) {
			Log.d(TAG,"Found item with name: "+items.get(0).getItemName());
			//Pass the results to another activity that will show a card with their content
			Intent showResultIntent = new Intent(getApplicationContext(),ResultActivity.class);
			showResultIntent.putParcelableArrayListExtra("results",items);
			startActivity(showResultIntent);
		}else{
			mCraftARCamera.restartCameraPreview();

			Log.d(TAG,"Nothing found");
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
		mCraftARCamera.restartCameraPreview();

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
		}	}
	
	@Override
	public void requestImageError(String error) {
		//There was an error taking the picture!
		//Restart the camera to allow to take another picture.
		mCraftARCamera.restartCameraPreview();
		mTextView.setText("Tap to scan");		
	}

}
