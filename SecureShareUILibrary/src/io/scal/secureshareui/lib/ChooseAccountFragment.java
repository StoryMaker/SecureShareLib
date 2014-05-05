package io.scal.secureshareui.lib;

import io.scal.secureshareui.controller.FacebookPublishController;
import io.scal.secureshareui.controller.FlickrPublishController;
import io.scal.secureshareui.controller.PublishController;
import io.scal.secureshareui.controller.PublishController.OnPublishEventListener;
import io.scal.secureshareui.controller.SoundCloudPublishController;
import io.scal.secureshareui.controller.YoutubePublishController;
import io.scal.secureshareui.model.PublishAccount;
import io.scal.secureshareuilibrary.R;

import java.util.ArrayList;
import java.util.List;

import com.facebook.Session;

import android.app.Activity;
import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

public class ChooseAccountFragment extends Fragment {

	private View mView;
	private ViewGroup mContainerConnectedAccountsView;
	private ViewGroup mContainerAvailableAccountsView;
	private OnPublishEventListener mPublishEventListener;
	private List<PublishAccount> mAlPublishAccounts = new ArrayList<PublishAccount>();
	private static boolean mInSelectionMode = false;
	private static boolean mAttemptingLoginRetry = false;
	public static final int ACCOUNT_REQUEST_CODE = 102;
	public static final String EXTRAS_ACCOUNT_KEYS = "accountIds";
	
	//return the ids of selected items
	ArrayList<String> mSelectedAccountIds = new ArrayList<String>();
	
	//used for storing state for the callback
	private static ViewGroup mVgAccounts;
	private static PublishAccount mPublishAccount;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.choose_account_fragment, container, false);		
		mContainerConnectedAccountsView = (ViewGroup) mView.findViewById(R.id.accounts_connected_container);
		mContainerAvailableAccountsView = (ViewGroup) mView.findViewById(R.id.accounts_available_container);	
	
		if (getArguments() != null) {
			//if fragment is in connection or selection mode
			mInSelectionMode = getArguments().getBoolean("inSelectionMode", false);
			if(mInSelectionMode) {
				((TextView) mView.findViewById(R.id.tv_choose_account_header)).setText(this.getString(R.string.select_account));
				
				Button btnContinue = (Button) mView.findViewById(R.id.btnContinue);
				btnContinue.setVisibility(View.VISIBLE);
				btnContinue.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						Intent data = new Intent();
						data.putStringArrayListExtra(EXTRAS_ACCOUNT_KEYS, mSelectedAccountIds);
						getActivity().setResult(Activity.RESULT_OK, data);
						getActivity().finish();
					}
				});
			}
		}

		addPublishAccounts();
		
		return mView;
	}
	
	public void setPublishAccountsList(List<PublishAccount> publishAccounts) {
		this.mAlPublishAccounts = publishAccounts;
		addPublishAccounts();
	}
	
	public void setOnPublishEventListener(OnPublishEventListener publishEventListener) {
		this.mPublishEventListener = publishEventListener;
	}
	
	private void addPublishAccounts() { 
		//ensure the fragment is attached to a context
		if(getActivity() == null && mAlPublishAccounts != null)
			return;
	
		for(PublishAccount account: mAlPublishAccounts) {     	
			if (account.getIsConnected()) {
				addConnectedPublishAccount(account, false);
			} 	
			else {
				addAvailablePublishAccount(account);            
			}
		}
	}
	
	private void addConnectedPublishAccount(PublishAccount account, boolean isDynamicallyAdded) {	
		final ViewGroup vgConnectedAccounts = (ViewGroup) LayoutInflater.from(getActivity())
											   .inflate(R.layout.publish_account_item, mContainerConnectedAccountsView, false);
		final CheckBox cbToPublish = (CheckBox) vgConnectedAccounts.findViewById(R.id.cbToPublish);
		final PublishAccount currentAccount = account;
		((TextView) vgConnectedAccounts.findViewById(R.id.tv_account_name)).setText(account.getName());
		((ImageView) vgConnectedAccounts.findViewById(R.id.iv_account_icon)).setImageResource(getAccountIcon(account.getSite(), true));
			
		mContainerConnectedAccountsView.addView(vgConnectedAccounts, 0);
		mView.findViewById(R.id.tv_accounts_connected_empty).setVisibility(View.GONE);
		
		if(!currentAccount.getAreCredentialsValid()) {
			vgConnectedAccounts.setBackgroundColor(Color.RED);
		}
		else if(mInSelectionMode) {
			cbToPublish.setVisibility(View.VISIBLE);
			
			//is added after activity load
			if(isDynamicallyAdded) {
				cbToPublish.setChecked(true);
				mSelectedAccountIds.add(currentAccount.getSite());
			}
				
		}

		//move PublishAccount from Connected to Available
		vgConnectedAccounts.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {           	
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(R.string.dialog_account_message)
					.setCancelable(false)
					.setPositiveButton(R.string.dialog_account_answer_positive, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id){
							addAvailablePublishAccount(currentAccount);
							mContainerConnectedAccountsView.removeView(vgConnectedAccounts);

							//if there are no rows remaining, show the empty view.
							if(mContainerConnectedAccountsView.getChildCount() == 0){
								mView.findViewById(R.id.tv_accounts_connected_empty).setVisibility(View.VISIBLE);
							}
						}
					})
					.setNegativeButton(R.string.dialog_account_answer_negative, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id){
							dialog.cancel();
						}
					}).show();
				
				return true;
			}
		});

        vgConnectedAccounts.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {			
				if(!currentAccount.getAreCredentialsValid()) {
					mAttemptingLoginRetry = true;
					launchAuthentication(currentAccount, vgConnectedAccounts); 
				} else {
					
					if(mInSelectionMode) {
						if(cbToPublish.isChecked()) {
							cbToPublish.setChecked(false);
							mSelectedAccountIds.remove(currentAccount.getSite());
						}
						else {
							cbToPublish.setChecked(true);
							mSelectedAccountIds.add(currentAccount.getSite());
						}				
					}
					else {
						PublishController publishController = PublishController.getPublishController(currentAccount.getSite());
						publishController.setContext(getActivity());
						publishController.upload("title", "body", "/null/");
						Toast.makeText(getActivity(), currentAccount.getName() + " edit click", Toast.LENGTH_SHORT).show();	
					}		
				}				
			} 
        });
	}
	
	private void addAvailablePublishAccount(PublishAccount account) {
		
		final ViewGroup vgAvailableAccounts = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.publish_account_item, mContainerAvailableAccountsView, false);
		final PublishAccount currentAccount = account;
		((TextView) vgAvailableAccounts.findViewById(R.id.tv_account_name)).setText(account.getName());
		((ImageView) vgAvailableAccounts.findViewById(R.id.iv_account_icon)).setImageResource(getAccountIcon(account.getSite(), false));
		
			
		mContainerAvailableAccountsView.addView(vgAvailableAccounts, 0);		
		mView.findViewById(R.id.tv_accounts_available_empty).setVisibility(View.GONE);
		
		//move PublishAccount from Available to Connected
		vgAvailableAccounts.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mAttemptingLoginRetry = false;
				launchAuthentication(currentAccount, vgAvailableAccounts);     
			}
		});
	}
	
	private void launchAuthentication(PublishAccount currentAccount, ViewGroup vgAccounts) {
		PublishController publishController = PublishController.getPublishController(currentAccount.getSite());
		
		//ensure controller exists
		if(null == publishController) {
			Toast.makeText(getActivity(), "Error Finding Controller (try Facebook!)", Toast.LENGTH_SHORT).show();
			mPublishEventListener.onFailure(currentAccount, "Error Finding Controller" );
			return;
		}
		
		publishController.setOnPublishEventListener(mPublishEventListener);
		publishController.setContext(getActivity());
		publishController.startAuthentication(currentAccount);
		
		mPublishAccount = currentAccount;
		mVgAccounts = vgAccounts; 
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == PublishController.CONTROLLER_REQUEST_CODE) {
			
			String credentials = data.getStringExtra("credentials");
			mPublishAccount.setCredentials(credentials != null ? credentials : "");
			
			if(resultCode == android.app.Activity.RESULT_OK) {
				mPublishAccount.setAreCredentialsValid(true);
				mPublishEventListener.onSuccess(mPublishAccount);
			}
			else {
				mPublishAccount.setAreCredentialsValid(false);
				mPublishEventListener.onFailure(mPublishAccount, "Error Loggging in");		
			}
			
			if(mAttemptingLoginRetry) {
				mContainerConnectedAccountsView.removeView(mVgAccounts);
				addConnectedPublishAccount(mPublishAccount, true);
			}
			else {
				addConnectedPublishAccount(mPublishAccount, true);
				mContainerAvailableAccountsView.removeView(mVgAccounts);
				                                      
				// If there are no rows remaining, show the empty view.
				if (mContainerAvailableAccountsView.getChildCount() == 0) {
					mView.findViewById(R.id.tv_accounts_available_empty).setVisibility(View.VISIBLE);
				}
			}
		}
	}
	
	private int getAccountIcon(String site, boolean isConnected) {	
		if(site.equals(FacebookPublishController.SITE_KEY)) {
			return isConnected ? R.drawable.ic_context_facebook_on : R.drawable.ic_context_facebook; 
		}
		else if(site.equals(YoutubePublishController.SITE_KEY)) {
			return isConnected ? R.drawable.ic_context_youtube_on : R.drawable.ic_context_youtube; 
		}
		else if(site.equals(SoundCloudPublishController.SITE_KEY)) {
			return isConnected ? R.drawable.ic_context_soundcloud_on : R.drawable.ic_context_soundcloud; 
		}
		else if(site.equals(FlickrPublishController.SITE_KEY)) {
			return isConnected ? R.drawable.ic_context_vimeo_on : R.drawable.ic_context_vimeo; 
		}
		
		return R.drawable.snoopy;
	}
}
