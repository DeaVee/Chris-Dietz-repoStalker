package com.bypassmobile.octo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bypassmobile.octo.rest.GithubEndpoint;

import retrofit.RestAdapter;

public class BaseActivity extends AppCompatActivity {

    private GithubEndpoint endpoint;

    private Toolbar bar;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RestAdapter adapter = new RestAdapter.Builder()
                .setServer(GithubEndpoint.SERVER)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        endpoint = adapter.create(GithubEndpoint.class);
    }

    public GithubEndpoint getEndpoint() {
        return endpoint;
    }

    /**
     * Convenience method for {@link Activity#findViewById(int)} so things just look cleaner.
     *
     * @param id
     *      ID of the view to find.
     *
     * @return
     *     View found or null if the view is not in the hierarchy yet.
     */
    public <T extends View> T findView(@IdRes int id) {
        //noinspection unchecked  ClassCastException will be thrown if casting to wrong object anyway.
        return (T) findViewById(id);
    }

    /**
     * Convenience method for {@link ActionBar#setDisplayHomeAsUpEnabled(boolean)}
     * in which this will set the up button only for activities that have an actionbar to set.
     */
    public void setDisplayHomeAsUpEnabled(boolean displayHome) {
        final android.support.v7.app.ActionBar supportBar = getSupportActionBar();
        if (supportBar != null) {
            supportBar.setDisplayHomeAsUpEnabled(displayHome);
        }
    }

    @Override
    public void setSupportActionBar(Toolbar bar) {
        super.setSupportActionBar(bar);
        this.bar = bar;
        // If the bar as a custom title, then set the current title to the custom title and erase the current actionbar title.
        if (bar != null) {
            setTitle(getTitle());
        }
    }

    /**
     * Will set the title of the toolbar if there is a custom title view with the ID
     * R.id.txtTitle.
     *
     * @param title
     *      title to set in the actionbar
     */
    @Override
    public void setTitle(CharSequence title) {
        boolean set = false;
        if (bar != null) {
            final TextView titleBar = (TextView) bar.findViewById(R.id.txtTitle);
            if (titleBar != null) {
                titleBar.setText(title);
                //noinspection ConstantConditions It won't be null in this case.
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                set = true;
            }
        }
        if (!set) {
            super.setTitle(title);
        }
    }

    /**
     * Adds a progress indicator to the root view of the application above all other elements.
     * This generally looks cleaner as opposed to a progress dialog window.
     */
    protected void setProgressIndicator() {
        if (progressBar != null) {
            // Already showing.
            progressBar.bringToFront();
            return;
        }
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;

        final ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
        progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(lp);
        progressBar.setIndeterminate(true);
        rootView.addView(progressBar);
    }

    /**
     * Removes the progress bar from the root view.
     */
    protected void removeProgressIndicator() {
        if (progressBar == null) {
            return;
        }

        final ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
        rootView.removeView(progressBar);
        progressBar = null;
    }
}
