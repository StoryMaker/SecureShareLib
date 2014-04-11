package io.scal.secureshareui.lib;

import io.scal.secureshareui.controller.FacebookPublishController;
import io.scal.secureshareui.controller.PublishController.OnPublishEventListener;
import io.scal.secureshareui.model.PublishAccount;
import io.scal.secureshareuilibrary.R;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


public class ChooseAccountFragment extends Fragment {

	private View mView;
	private ViewGroup mContainerConnectedAccountsView;
	private ViewGroup mContainerAvailableAccountsView;
	private FacebookPublishController facebookPublishController = new FacebookPublishController();
	
	private List<PublishAccount> mAlPublishAccounts = new ArrayList<PublishAccount>();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		mView = inflater.inflate(R.layout.choose_account_fragment, container, false);
		
		mContainerConnectedAccountsView = (ViewGroup) mView.findViewById(R.id.accounts_connected_container);
		mContainerAvailableAccountsView = (ViewGroup) mView.findViewById(R.id.accounts_available_container);

		addPublishAccounts();
		
		return mView;
	}
	
	public void setPublishAccountsList(List<PublishAccount> publishAccounts) {
		this.mAlPublishAccounts = publishAccounts;
		addPublishAccounts();
	}
	
	public void setOnPublishEventListener(OnPublishEventListener publishEventListener) {
		facebookPublishController.setOnPublishEventListener(publishEventListener);
	}
	
	private void addPublishAccounts() { 
    	//ensure the fragment is attached to a context
    	if(getActivity() == null && mAlPublishAccounts != null)
    		return;
	
        for(PublishAccount account: mAlPublishAccounts) {
        	
        	if(account.getIsConnected()) {
        		addConnectedPublishAccount(account);
        	} 	
        	else {
        		addAvailablePublishAccount(account);            
        	}
        }      
    }
	
	private void addConnectedPublishAccount(PublishAccount account) {
		
		final ViewGroup vgConnectedAccounts = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.publish_account_item, mContainerConnectedAccountsView, false);	
		((TextView) vgConnectedAccounts.findViewById(R.id.tv_account_name)).setText(account.getName());
		final PublishAccount currentAccount = account;
        		
        mContainerConnectedAccountsView.addView(vgConnectedAccounts, 0);
        mView.findViewById(R.id.tv_accounts_connected_empty).setVisibility(View.GONE);
        
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
            	    	   
            	    	   // If there are no rows remaining, show the empty view.
                           if (mContainerConnectedAccountsView.getChildCount() == 0){
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
        
        //edit connected account
        vgConnectedAccounts.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Toast.makeText(getActivity(), currentAccount.getName() + " edit click", Toast.LENGTH_SHORT).show();		
			} 
        });     
    }
	
	private void addAvailablePublishAccount(PublishAccount account) {
    	
		final ViewGroup vgAvailableAccounts = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.publish_account_item, mContainerAvailableAccountsView, false);
		((TextView) vgAvailableAccounts.findViewById(R.id.tv_account_name)).setText(account.getName());
		final PublishAccount currentAccount = account;
		
		mContainerAvailableAccountsView.addView(vgAvailableAccounts, 0);		
		mView.findViewById(R.id.tv_accounts_available_empty).setVisibility(View.GONE);
		
		//move PublishAccount from Available to Connected
        vgAvailableAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	facebookPublishController.startAuthentication(currentAccount);
            	
            	addConnectedPublishAccount(currentAccount);
            	mContainerAvailableAccountsView.removeView(vgAvailableAccounts);
            	                   	
                // If there are no rows remaining, show the empty view.
                if (mContainerAvailableAccountsView.getChildCount() == 0) {
                    mView.findViewById(R.id.tv_accounts_available_empty).setVisibility(View.VISIBLE);
                }            
            }
        });
    }
}
