package org.uibk.iis.robotprojectapp;

import java.util.List;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

public class PreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	/**
	 * Populate the activity with the top-level headers.
	 */
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
		Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.activity_preferences, root, false);
		root.addView(bar, 0); // insert at top
		bar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		if(CalibrationFragment.class.getName().equals(fragmentName))
			return true;
		else if(CommonFragment.class.getName().equals(fragmentName))
			return true;
		return false;
	}

	public static class CalibrationFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.preferences_calibration);

		}
	}
	public static class CommonFragment extends PreferenceFragment {
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.preferences_common);
			
		}
	}
}