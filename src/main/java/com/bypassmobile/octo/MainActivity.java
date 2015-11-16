package com.bypassmobile.octo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.util.Log;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.bypassmobile.octo.model.User;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Main entry point of the applications.  Activity will display the members of an organization.
 */
public class MainActivity extends BaseActivity implements UserListFragment.UserListListener {

    private UserListFragment userFragment;
    private TextView errorMsgView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Toolbar bar = findView(R.id.toolBar);
        setSupportActionBar(bar);
        setTitle("bypasslane");

        errorMsgView = findView(R.id.txtError);

        if (savedInstanceState != null) {
            // Restore the fragment from the previous state.
            userFragment = (UserListFragment) getSupportFragmentManager().getFragment(savedInstanceState, UserListFragment.TAG);
        } else {
            userFragment = UserListFragment.getInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.userListContainer, userFragment, UserListFragment.TAG)
                    .commit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (userFragment.isEmpty()) {
            getMembers("bypasslane");
        } // Else we're going to assume that this activity is being recreated from a previous state.  Most likely the members status didn't alter in between.
    }

    @Override
    public void onUserClicked(User user) {
        final Intent intent = new Intent(this, UserListActivity.class);
        intent.putExtra(UserListActivity.ARG_USER_PARCEL, user);
        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        getSupportFragmentManager().putFragment(out, UserListFragment.TAG, userFragment);
    }

    /**
     * Retrieve the members of the given organization
     */
    private void getMembers(String orgnization) {
        hideError();
        setProgressIndicator();
        getEndpoint().getOrganizationMember(orgnization, new MembersCallback(userFragment, errorMsgView));
    }

    /**
     * Puts the view state in a "no current error" state.
     */
    private void hideError() {
        errorMsgView.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction().show(userFragment).commit();
    }

    /**
     * Puts the view state in a "show error" state.
     * @param resId
     *      Resource ID of the error or message to display.
     */
    private void showError(@StringRes int resId) {
        errorMsgView.setVisibility(View.VISIBLE);
        errorMsgView.setText(resId);
        getSupportFragmentManager().beginTransaction().show(userFragment).commit();
    }

    /**
     * Internal callback that can handle operations for retrieving the members.
     */
    private class MembersCallback implements Callback<List<User>> {
        final TextView errorMsgView;
        final UserListFragment userListFragment;

        /**
         *
         * @param frag
         *      The user fragment that will contain the list of users that are to show.
         * @param errorMsgView
         *      Error message textview that will display the appropriate error message if the view is
         *      either empty or full.
         */
        public MembersCallback(UserListFragment frag, TextView errorMsgView) {
            this.userListFragment = frag;
            this.errorMsgView = errorMsgView;
        }

        @Override
        public void success(List<User> users, Response response) {
            userListFragment.addAll(users);
            if (userListFragment.isEmpty()) {
                showError(R.string.error_empty_member_list);
            }
            removeProgressIndicator();
        }

        @Override
        public void failure(RetrofitError error) {
            Log.e("Octo", "Failure to download members");
            showError(R.string.error_retrieving_member_list);
            removeProgressIndicator();
        }
    }
}
