
package io.scal.secureshareui.lib;

import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.controller.SiteController.OnEventListener;
import io.scal.secureshareui.model.Account;
import io.scal.secureshareuilibrary.R;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAccountFragment extends Fragment {
    final static String TAG = "ChooseAccountFragment";
        
    private View mView;
    private ViewGroup mContainerConnectedAccountsView;
    private ViewGroup mContainerAvailableAccountsView;
    private OnEventListener mEventListener;
    private Intent mLoginIntent = null;
    private List<Account> mAccounts = new ArrayList<Account>();
    private static boolean mInSelectionMode = false;
    private static boolean mAttemptingLoginRetry = false;
    public static final int ACCOUNT_REQUEST_CODE = 102;
    public static final String EXTRAS_ACCOUNT_KEYS = "accountIds";
    public static final String TOR_PREF_KEY = "pusetor";
    public static final String SM_UPLOAD_PREF_KEY = "psmupload";
    private final static int UPDATE_SM_SWITCH = 0;

    // return the ids of selected items
    ArrayList<String> mSelectedAccountIds = new ArrayList<String>();

    // used for storing state for the callback
    private static ViewGroup mVgAccounts;
    private static Account mAccount;
    private Button mBtnContinue;
    private Switch mSwitchTor;
    private Switch mSwitchStoryMaker;
    private View mDivider;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.choose_account_fragment, container, false);
        mContainerConnectedAccountsView = (ViewGroup) mView.findViewById(R.id.accounts_connected_container);
        mContainerAvailableAccountsView = (ViewGroup) mView.findViewById(R.id.accounts_available_container);

        //get default prefs from settings
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        boolean pUseTor = sharedPref.getBoolean(TOR_PREF_KEY, false);
        boolean pSMUpload = sharedPref.getBoolean(SM_UPLOAD_PREF_KEY, false);
        
        if (getArguments() != null) {
            // if fragment is in connection or selection mode
            mInSelectionMode = getArguments().getBoolean("inSelectionMode", false);
            final boolean isUserLoggedIntoSM = getArguments().getBoolean("isUserLoggedIntoSM", false);
            if (mInSelectionMode) {
                ((TextView) mView.findViewById(R.id.tv_choose_account_header)).setText(this.getString(R.string.select_account));

                mBtnContinue = (Button) mView.findViewById(R.id.btnContinue);
                mBtnContinue.setVisibility(View.VISIBLE);
                mBtnContinue.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                    	//save default prefs in settings
                    	SharedPreferences.Editor editor = sharedPref.edit();
        		        editor.putBoolean(TOR_PREF_KEY, mSwitchTor.isChecked());
        		        editor.putBoolean(SM_UPLOAD_PREF_KEY, mSwitchStoryMaker.isChecked());
        		        editor.apply();
                    	               	
                        Intent data = new Intent();
                        data.putStringArrayListExtra(EXTRAS_ACCOUNT_KEYS, mSelectedAccountIds);
                        data.putExtra(SiteController.VALUE_KEY_USE_TOR, mSwitchTor.isChecked());
                        data.putExtra(SiteController.VALUE_KEY_PUBLISH_TO_STORYMAKER, mSwitchStoryMaker.isChecked());
                        getActivity().setResult(Activity.RESULT_OK, data);
                        getActivity().finish();
                    }
                });

                mSwitchTor = (Switch) mView.findViewById(R.id.switchTor);
                mSwitchTor.setVisibility(View.VISIBLE);
                mSwitchTor.setChecked(pUseTor);

                mSwitchStoryMaker = (Switch) mView.findViewById(R.id.switchStoryMaker);
                mSwitchStoryMaker.setVisibility(View.VISIBLE);
                
                //make sure the user is logged in to SM
                if(isUserLoggedIntoSM) {
                	mSwitchStoryMaker.setChecked(pSMUpload);
                } else {
                	mSwitchStoryMaker.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        	if(isChecked && !isUserLoggedIntoSM) {
                        		showLoginDialogSM();
                        		mSwitchHandler.postDelayed(new Runnable() {
                        			  public void run() {
                        				  mSwitchHandler.sendEmptyMessage(UPDATE_SM_SWITCH);
                        			  }
                        			}, 1);
                        	}
                        }
                    });
                }

                mDivider = (View) mView.findViewById(R.id.divider);
                mDivider.setVisibility(View.VISIBLE);   
            }
        }

        addAccounts();

        return mView;
    }

    public void setAccountsList(List<Account> accounts) {
        this.mAccounts = accounts;
        addAccounts();
    }
    
    public void setLoginIntent(Intent intent) {
        this.mLoginIntent = intent;
    }

    public void setOnEventListener(OnEventListener eventListener) {
        this.mEventListener = eventListener;
    }

    private void addAccounts() {
        // ensure the fragment is attached to a context
        if (getActivity() == null && mAccounts != null)
            return;

        for (Account account : mAccounts) {
            if (account.getIsConnected()) {
                addConnectedAccount(account, false);
            }
            else {
                addAvailableAccount(account);
            }
        }
    }

    private void addConnectedAccount(Account account, boolean isDynamicallyAdded) {
        final ViewGroup vgConnectedAccounts = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.account_item, mContainerConnectedAccountsView, false);
        final CheckBox cbToPublish = (CheckBox) vgConnectedAccounts.findViewById(R.id.cbToPublish);
        final Account currentAccount = account;
        ((TextView) vgConnectedAccounts.findViewById(R.id.tv_account_name)).setText(account.getName());
        ((ImageView) vgConnectedAccounts.findViewById(R.id.iv_account_icon)).setImageResource(SiteController.getAccountIcon(account.getSite(), true, account.getAreCredentialsValid()));

        mContainerConnectedAccountsView.addView(vgConnectedAccounts, 0);
        mView.findViewById(R.id.tv_accounts_connected_empty).setVisibility(View.GONE);

        if (account.getAreCredentialsValid() && mInSelectionMode) {
            cbToPublish.setVisibility(View.VISIBLE);

            // is added after activity load
            if (isDynamicallyAdded) {
            	if(!isAnAccountSelected()) {
            		cbToPublish.setChecked(true);
                    mSelectedAccountIds.add(currentAccount.getSite());
            	} else {
            		Toast.makeText(getActivity(), "Only one account may be selected for upload", Toast.LENGTH_LONG).show();
            	}
                setContinueEnabledState();
            }
        }

        // move Account from Connected to Available
        vgConnectedAccounts.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.dialog_account_message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.lbl_Yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                addAvailableAccount(currentAccount);
                                mContainerConnectedAccountsView.removeView(vgConnectedAccounts);
                                mEventListener.onRemove(currentAccount);
                                // if there are no rows remaining, show the empty view.
                                if (mContainerConnectedAccountsView.getChildCount() == 0) {
                                    mView.findViewById(R.id.tv_accounts_connected_empty).setVisibility(View.VISIBLE);
                                }
                            }
                        })
                        .setNegativeButton(R.string.lbl_Cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();

                return true;
            }
        });

        vgConnectedAccounts.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentAccount.getAreCredentialsValid()) {
                    mAttemptingLoginRetry = true;
                    launchAuthentication(currentAccount, vgConnectedAccounts);
                } else {


                    mAttemptingLoginRetry = true;
                    launchAuthentication(currentAccount, vgConnectedAccounts);

                    /*
                    if (mInSelectionMode) {
                        if (cbToPublish.isChecked()) {
                            cbToPublish.setChecked(false);
                            mSelectedAccountIds.remove(currentAccount.getSite());
                        }
                        else {
                        	if(!isAnAccountSelected()) {
                        		cbToPublish.setChecked(true);
                                mSelectedAccountIds.add(currentAccount.getSite());
                        	} else {
                        		Toast.makeText(getActivity(), "Only one account may be selected for upload", Toast.LENGTH_LONG).show();
                        	}               
                        }
                        setContinueEnabledState();
                    }
                    else {
                        Toast.makeText(getActivity(), currentAccount.getName() + " edit click", Toast.LENGTH_SHORT).show();
                    }
                    */
                }
            }
        });
    }

    private void setContinueEnabledState() {
        mBtnContinue.setEnabled(!mSelectedAccountIds.isEmpty());
    }
    
    //TODO added temporarily to allow only 1 account upload at a time
    private boolean isAnAccountSelected() {
        return (!mSelectedAccountIds.isEmpty());
    }

    private void addAvailableAccount(Account account) {

        final ViewGroup vgAvailableAccounts = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.account_item, mContainerAvailableAccountsView, false);
        final Account currentAccount = account;
        ((TextView) vgAvailableAccounts.findViewById(R.id.tv_account_name)).setText(account.getName());
        ((ImageView) vgAvailableAccounts.findViewById(R.id.iv_account_icon)).setImageResource(SiteController.getAccountIcon(account.getSite(), false, true));

        mContainerAvailableAccountsView.addView(vgAvailableAccounts, 0);
        mView.findViewById(R.id.tv_accounts_available_empty).setVisibility(View.GONE);

        vgAvailableAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAttemptingLoginRetry = false;
                launchAuthentication(currentAccount, vgAvailableAccounts);
            }
        });
    }

    private void launchAuthentication(Account currentAccount, ViewGroup vgAccounts) {
        SiteController siteController = SiteController.getSiteController(currentAccount.getSite(), getActivity(), null, null);

        // ensure controller exists
        if (null == siteController) {
            mEventListener.onFailure(currentAccount, "Error Finding Controller");
            return;
        }

        siteController.setOnEventListener(mEventListener);
        siteController.startAuthentication(currentAccount);

        mAccount = currentAccount;
        mVgAccounts = vgAccounts;
    }
    
    private void showLoginDialogSM(){
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.sign_in_storymaker)
                .setCancelable(false)
                .setPositiveButton(R.string.lbl_Yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    	if(mLoginIntent != null) {
                    		getActivity().startActivity(mLoginIntent);
                    	}
                    }
                })
                .setNegativeButton(R.string.lbl_Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    	dialog.cancel();
                    }
                }).show();     
    }
    
    private void turnSMSwitchOff() {
    	mSwitchStoryMaker.setChecked(false);
    }
    
    @SuppressLint("HandlerLeak")
	private Handler mSwitchHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			final int what = msg.what;
	        switch(what) {
	        	case UPDATE_SM_SWITCH: turnSMSwitchOff(); break;
	        }
		}
	};

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == SiteController.CONTROLLER_REQUEST_CODE) {

            String credentials = intent.getStringExtra(SiteController.EXTRAS_KEY_CREDENTIALS);
            mAccount.setCredentials(credentials != null ? credentials : "");

            String username = intent.getStringExtra(SiteController.EXTRAS_KEY_USERNAME);
            mAccount.setUserName(username != null ? username : "");

            String data = intent.getStringExtra(SiteController.EXTRAS_KEY_DATA);
            mAccount.setData(data != null ? data : null);

            if (resultCode == android.app.Activity.RESULT_OK) {
                mAccount.setAreCredentialsValid(true);
                mEventListener.onSuccess(mAccount);

                if (mAttemptingLoginRetry) {
                    mContainerConnectedAccountsView.removeView(mVgAccounts);
                    addConnectedAccount(mAccount, true);
                }
                else {
                    addConnectedAccount(mAccount, true);
                    mContainerAvailableAccountsView.removeView(mVgAccounts);

                    // If there are no rows remaining, show the empty view.
                    if (mContainerAvailableAccountsView.getChildCount() == 0) {
                        mView.findViewById(R.id.tv_accounts_available_empty).setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }
}
