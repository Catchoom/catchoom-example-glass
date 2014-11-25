package com.craftar.examples.glass;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.craftar.CraftARItem;
import com.google.android.glass.app.Card;


public class ResultActivity extends Activity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras= getIntent().getExtras();
		ArrayList<CraftARItem> results= extras.getParcelableArrayList("results");
		
		//In this example we just consider the best match.
		//Note that the API can return more than one match, and they are sorted by the confidence score.
		CraftARItem bestMatch= results.get(0);
		
		//Create a card with the content of the matched image.
		Card card = new Card(this);
		card.setText(bestMatch.getItemName());
		card.setFootnote(bestMatch.getItemId());

		View cardView = card.getView();
		setContentView(cardView);
		
	}
}
