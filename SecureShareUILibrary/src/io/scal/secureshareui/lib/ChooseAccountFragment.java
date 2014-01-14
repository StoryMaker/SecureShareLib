package io.scal.secureshareui.lib;

import io.scal.secureshareui.models.AccountItem;
import io.scal.secureshareuilibrary.R;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Fragment;
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

	private ViewGroup mContainerConnectedAccountsView;
	private ViewGroup mContainerAvailableAccountsView;
	private View mView;
	
	List<AccountItem> mAlAccountItems = new ArrayList<AccountItem>();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		mView = inflater.inflate(R.layout.choose_account_fragment, container, false);
		
		mContainerConnectedAccountsView = (ViewGroup) mView.findViewById(R.id.accounts_connected_container);
		mContainerAvailableAccountsView = (ViewGroup) mView.findViewById(R.id.accounts_available_container);
		
		getAccountItems();
		addAccountItems();
		
		return mView;
	}
	
	private void getAccountItems() {
		
		int i = 0;
		
		mAlAccountItems.add(new AccountItem(i++, "Facebook", "snoopy", true));
		mAlAccountItems.add(new AccountItem(i++, "Youtube", "snoopy", false));
		mAlAccountItems.add(new AccountItem(i++, "SoundCloud", "ic_launcher", false));
		mAlAccountItems.add(new AccountItem(i++, "Flickr", "ic_launcher", false));
		mAlAccountItems.add(new AccountItem(i++, "Wordpress", "ic_launcher", false));
		mAlAccountItems.add(new AccountItem(i++, "GlobalLeaks", "ic_launcher", false));
		mAlAccountItems.add(new AccountItem(i++, "SSH", "ic_launcher", false));
	}
	
	private void addAccountItems(){ 
    	//ensure the fragment is attached to a context
    	if(getActivity() == null)
    		return;
	
        for(AccountItem account: mAlAccountItems) {
        	
        	if(account.getIsConnected()) {
        		addConnectedAccountItem(account);
        	} 	
        	else {
        		addAvailableAccountItem(account);            
        	}
        }      
    }
	
	private void addConnectedAccountItem(AccountItem account) {
		
		final ViewGroup vgConnectedAccounts = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.account_list_item, mContainerConnectedAccountsView, false);	
		((TextView) vgConnectedAccounts.findViewById(R.id.tv_account_name)).setText(account.getName());
		final AccountItem currentAccount = account;
        		
        mContainerConnectedAccountsView.addView(vgConnectedAccounts, 0);
        mView.findViewById(R.id.tv_accounts_connected_empty).setVisibility(View.GONE);
        
        //move AccountItem from Connected to Available
        vgConnectedAccounts.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
            	
            	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            	builder.setMessage(R.string.dialog_account_message)
            	   .setCancelable(false)
            	   .setPositiveButton(R.string.dialog_account_answer_positive, new DialogInterface.OnClickListener() {
            	       public void onClick(DialogInterface dialog, int id){
            	    	   addAvailableAccountItem(currentAccount);
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
        
        //move AccountItem from Connected to Available
        vgConnectedAccounts.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Toast.makeText(getActivity(), currentAccount.getName() + " edit click", Toast.LENGTH_SHORT).show();		
			} 
        });     
    }
	
	private void addAvailableAccountItem(AccountItem account) {
    	
		final ViewGroup vgAvailableAccounts = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.account_list_item, mContainerAvailableAccountsView, false);
		((TextView) vgAvailableAccounts.findViewById(R.id.tv_account_name)).setText(account.getName());
		final AccountItem currentAccount = account;
		
		mContainerAvailableAccountsView.addView(vgAvailableAccounts, 0);		
		mView.findViewById(R.id.tv_accounts_available_empty).setVisibility(View.GONE);
		
		//move AccountItem from Available to Connected
        vgAvailableAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	
            	addConnectedAccountItem(currentAccount);
            	mContainerAvailableAccountsView.removeView(vgAvailableAccounts);        	
            	                   	
                // If there are no rows remaining, show the empty view.
                if (mContainerAvailableAccountsView.getChildCount() == 0) {
                    mView.findViewById(R.id.tv_accounts_available_empty).setVisibility(View.VISIBLE);
                }            
            }
        });
    }
}
