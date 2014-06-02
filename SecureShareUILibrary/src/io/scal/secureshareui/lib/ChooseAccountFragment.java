
package io.scal.secureshareui.lib;

import io.scal.secureshareui.controller.FacebookSiteController;
import io.scal.secureshareui.controller.FlickrSiteController;
import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.controller.SiteController.OnEventListener;
import io.scal.secureshareui.controller.SoundCloudSiteController;
import io.scal.secureshareui.controller.YoutubeSiteController;
import io.scal.secureshareui.model.Account;
import io.scal.secureshareuilibrary.R;

import java.util.ArrayList;
import java.util.List;

import org.holoeverywhere.widget.Switch;

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
    final static String TAG = "ChooseAccountFragment";
        
    private View mView;
    private ViewGroup mContainerConnectedAccountsView;
    private ViewGroup mContainerAvailableAccountsView;
    private OnEventListener mEventListener;
    private List<Account> mAccounts = new ArrayList<Account>();
    private static boolean mInSelectionMode = false;
    private static boolean mAttemptingLoginRetry = false;
    public static final int ACCOUNT_REQUEST_CODE = 102;
    public static final String EXTRAS_ACCOUNT_KEYS = "accountIds";
    public static final String EXTRAS_USE_TOR = "use_tor";
    public static final String EXTRAS_PUBLISH_TO_STORYMAKER = "publish_to_storymaker"; // TODO generalize this, allow clie

    // return the ids of selected items
    ArrayList<String> mSelectedAccountIds = new ArrayList<String>();

    // used for storing state for the callback
    private static ViewGroup mVgAccounts;
    private static Account mAccount;
    private Button mBtnContinue;
    private Switch mSwitchTor;
    private TextView mTextViewTor;
    private Switch mSwitchStoryMaker;
    private TextView mTextViewStoryMaker;
    private View mDivider;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.choose_account_fragment, container, false);
        mContainerConnectedAccountsView = (ViewGroup) mView.findViewById(R.id.accounts_connected_container);
        mContainerAvailableAccountsView = (ViewGroup) mView.findViewById(R.id.accounts_available_container);

        if (getArguments() != null) {
            // if fragment is in connection or selection mode
            mInSelectionMode = getArguments().getBoolean("inSelectionMode", false);
            if (mInSelectionMode) {
                ((TextView) mView.findViewById(R.id.tv_choose_account_header)).setText(this.getString(R.string.select_account));

                mBtnContinue = (Button) mView.findViewById(R.id.btnContinue);
                mBtnContinue.setVisibility(View.VISIBLE);
                mBtnContinue.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // TODO only do this if the user has selected something?
                        // maybe button shouldn't be enabled until you select an
                        // item
                        Intent data = new Intent();
                        data.putStringArrayListExtra(EXTRAS_ACCOUNT_KEYS, mSelectedAccountIds);
                        data.putExtra(EXTRAS_USE_TOR, mSwitchTor.isChecked());
                        data.putExtra(EXTRAS_PUBLISH_TO_STORYMAKER, mSwitchStoryMaker.isChecked());
                        getActivity().setResult(Activity.RESULT_OK, data);
                        getActivity().finish();
                    }
                });

                mSwitchTor = (Switch) mView.findViewById(R.id.switchTor);
                mSwitchTor.setVisibility(View.VISIBLE);TextView

                mTextViewTor = (TextView) mView.findViewById(R.id.textViewTor);
                mTextViewTor.setVisibility(View.VISIBLE);

                mSwitchStoryMaker = (Switch) mView.findViewById(R.id.switchStoryMaker);
                mSwitchStoryMaker.setVisibility(View.VISIBLE);

                mTextViewStoryMaker = (TextView) mView.findViewById(R.id.textStoryMaker);
                mTextViewStoryMaker.setVisibility(View.VISIBLE);

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
        ((ImageView) vgConnectedAccounts.findViewById(R.id.iv_account_icon)).setImageResource(getAccountIcon(account.getSite(), true));

        mContainerConnectedAccountsView.addView(vgConnectedAccounts, 0);
        mView.findViewById(R.id.tv_accounts_connected_empty).setVisibility(View.GONE);

        if (!currentAccount.getAreCredentialsValid()) {
            vgConnectedAccounts.setBackgroundColor(Color.RED);
        }
        else if (mInSelectionMode) {
            cbToPublish.setVisibility(View.VISIBLE);

            // is added after activity load
            if (isDynamicallyAdded) {
                cbToPublish.setChecked(true);
                mSelectedAccountIds.add(currentAccount.getSite());
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
                        .setPositiveButton(R.string.dialog_account_answer_positive, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                addAvailableAccount(currentAccount);
                                mContainerConnectedAccountsView.removeView(vgConnectedAccounts);
                                mEventListener.onRemove(currentAccount);
                                // if there are no rows remaining, show the
                                // empty view.
                                if (mContainerConnectedAccountsView.getChildCount() == 0) {
                                    mView.findViewById(R.id.tv_accounts_connected_empty).setVisibility(View.VISIBLE);
                                }
                            }
                        })
                        .setNegativeButton(R.string.dialog_account_answer_negative, new DialogInterface.OnClickListener() {
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

                    if (mInSelectionMode) {
                        if (cbToPublish.isChecked()) {
                            cbToPublish.setChecked(false);
                            mSelectedAccountIds.remove(currentAccount.getSite());
                        }
                        else {
                            cbToPublish.setChecked(true);
                            mSelectedAccountIds.add(currentAccount.getSite());
                        }
                        setContinueEnabledState();
                    }
                    else {
                        Toast.makeText(getActivity(), currentAccount.getName() + " edit click", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void setContinueEnabledState() {
        mBtnContinue.setEnabled(!mSelectedAccountIds.isEmpty());
    }

    private void addAvailableAccount(Account account) {

        final ViewGroup vgAvailableAccounts = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.account_item, mContainerAvailableAccountsView, false);
        final Account currentAccount = account;
        ((TextView) vgAvailableAccounts.findViewById(R.id.tv_account_name)).setText(account.getName());
        ((ImageView) vgAvailableAccounts.findViewById(R.id.iv_account_icon)).setImageResource(getAccountIcon(account.getSite(), false));

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
            Toast.makeText(getActivity(), "Error Finding Controller (try Facebook!)", Toast.LENGTH_SHORT).show();
            mEventListener.onFailure(currentAccount, "Error Finding Controller");
            return;
        }

        siteController.setOnEventListener(mEventListener);
        siteController.startAuthentication(currentAccount);

        mAccount = currentAccount;
        mVgAccounts = vgAccounts;
    }

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

    private int getAccountIcon(String site, boolean isConnected) {
        if (site.equals(FacebookSiteController.SITE_KEY)) {
            return isConnected ? R.drawable.ic_context_facebook_on : R.drawable.ic_context_facebook;
        }
        else if (site.equals(YoutubeSiteController.SITE_KEY)) {
            return isConnected ? R.drawable.ic_context_youtube_on : R.drawable.ic_context_youtube;
        }
        else if (site.equals(SoundCloudSiteController.SITE_KEY)) {
            return isConnected ? R.drawable.ic_context_soundcloud_on : R.drawable.ic_context_soundcloud;
        }
        else if (site.equals(FlickrSiteController.SITE_KEY)) {
            return isConnected ? R.drawable.ic_context_vimeo_on : R.drawable.ic_context_vimeo;
        }

        return R.drawable.ssh;
    }
}
