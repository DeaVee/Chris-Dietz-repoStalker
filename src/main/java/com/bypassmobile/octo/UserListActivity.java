package com.bypassmobile.octo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bypassmobile.octo.image.ImageLoader;
import com.bypassmobile.octo.model.User;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 *  Activity intended to show followers of a specific user.
 */
public class UserListActivity extends BaseActivity implements UserListFragment.UserListListener {

    public static final String ARG_USER_PARCEL = "user";

    private UserListFragment userFragment;
    private User user;
    private TextView errorMsgView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        user = getIntent().getParcelableExtra(ARG_USER_PARCEL);

        if (user == null) {
            throw new IllegalStateException(UserListActivity.class.getCanonicalName() + " currently does not support missing user.");
        }

        final Toolbar bar = findView(R.id.toolBar);

        final ImageView profileImage = (ImageView) bar.findViewById(R.id.imgAvatar);
        if (profileImage != null) {
            ImageLoader.createImageLoader(this)
                    .load(user.getProfileURL())
                    .placeholder(R.drawable.ic_contact_picture)
                    .resizeDimen(R.dimen.avatar_width, R.dimen.avatar_height)
                    .into(profileImage);
        }

        setSupportActionBar(bar);
        setTitle(user.getName());
        setDisplayHomeAsUpEnabled(true);

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
            getFollowers();
        } // Else we're going to assume that this activity is being recreated from a previous state.  Most likely the members status didn't alter in between.
    }

    @Override
    public void onUserClicked(User user) {
        final Intent intent = new Intent(this, UserListActivity.class);
        intent.putExtra(ARG_USER_PARCEL, user);
        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        getSupportFragmentManager().putFragment(out, UserListFragment.TAG, userFragment);
    }

    /**
     * Retrieve the followers of the user.
     */
    private void getFollowers() {
        hideError();
        setProgressIndicator();
        getEndpoint().getFollowingUser(user.getName(), new MembersCallback(userFragment, errorMsgView));
    }

    /**
     * Puts the activity view in a "no current error" state.
     */
    private void hideError() {
        getSupportFragmentManager().beginTransaction().show(userFragment).commit();
        errorMsgView.setVisibility(View.GONE);
    }

    /**
     * Puts the activity view in a "show current error" state.
     * @param message
     *      Message to display to the user.
     */
    private void showError(@StringRes String message) {
        getSupportFragmentManager().beginTransaction().hide(userFragment).commit();
        errorMsgView.setVisibility(View.VISIBLE);
        errorMsgView.setText(message);
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
                showError(getString(R.string.error_empty_follower_list, user.getName()));
            } else {
                hideError();
            }
            removeProgressIndicator();
        }

        @Override
        public void failure(RetrofitError error) {
            Log.e("Octo", "Failure to download members");
            showError(getString(R.string.error_retrieving_follower_list, user.getName()));
            removeProgressIndicator();
        }
    }
}
