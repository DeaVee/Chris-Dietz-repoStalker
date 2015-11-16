package com.bypassmobile.octo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bypassmobile.octo.image.ImageLoader;
import com.bypassmobile.octo.model.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *  Simple fragment that is used to display a list of {@link User} objects.
 */
public class UserListFragment extends Fragment implements AdapterView.OnItemClickListener {

    public static final String TAG = UserListFragment.class.getSimpleName();

    /**
     * Activities that use this fragment must implement this interface to listen to events
     * acted upon the fragment.
     */
    public interface UserListListener {
        /**
         * The user has selected a given user in the list.
         * @param user
         *      User that is to be displayed.
         */
        void onUserClicked(User user);
    }

    public static UserListFragment getInstance() {
        final UserListFragment newFrag = new UserListFragment();
        newFrag.setArguments(new Bundle());
        return newFrag;
    }

    private static final String ARG_USERS_ARRAY_LIST_STATE = "savedUsers";

    private UserAdapter mAdapter;
    private UserListListener mListener;

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);
        mAdapter = new UserAdapter(act);
        try {
            mListener = (UserListListener) act;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity " + act.getClass().getCanonicalName() + " must implement " + UserListListener.class.getCanonicalName());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mAdapter.onRestoreState(savedInstanceState);

        final View root = inflater.inflate(R.layout.fragment_user_list, container, false);
        final ListView list = (ListView) root.findViewById(R.id.listView);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(this);

        return root;
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        mAdapter.onSaveInstanceState(out);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        User clickedUser = (User) view.getTag(R.id.itemPayload);
        mListener.onUserClicked(clickedUser);
    }

    /**
     * All all collected users to be displayed on the fragment.
     */
    public void addAll(Collection<User> users) {
        if (mAdapter == null) {
            throw new IllegalStateException("Unable to add users to a detached fragment.");
        }
        mAdapter.addAll(users);
    }

    /**
     * Return if the fragment is currently not displaying any members.
     */
    public boolean isEmpty() {
        return mAdapter == null || mAdapter.isEmpty();
    }

    /**
     * Adapter to display the list of users in their appropriate order.
     */
    private static class UserAdapter extends BaseAdapter {

        private final ArrayList<User> mList;
        private final Picasso mPicasso;

        public UserAdapter(Context ctx) {
            mList = new ArrayList<>();
            mPicasso = ImageLoader.createImageLoader(ctx);
        }

        /**
         * Adds all the user collection to the adapter and restarts the list.
         */
        public void addAll(Collection<User> user) {
            if (user != null) {
                mList.addAll(user);
                notifyDataSetChanged();
            }
        }

        /**
         * Restore the adapter to its previous state.
         * @param in
         *      The state bundle that is passed in to one of the many Fragment callbacks.
         */
        public void onRestoreState(Bundle in) {
            if (in != null) {
                ArrayList<User> savedUsers = in.getParcelableArrayList(ARG_USERS_ARRAY_LIST_STATE);
                addAll(savedUsers);
            }
        }

        /**
         * Saves the state of the adapter to be called in {@link Fragment#onSaveInstanceState(Bundle)}
         */
        public void onSaveInstanceState(Bundle out) {
            out.putParcelableArrayList(ARG_USERS_ARRAY_LIST_STATE, mList);
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View root = convertView;
            ViewHolder holder;
            if (root == null) {
                root = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
                holder = new ViewHolder(root);
                root.setTag(holder);
            } else {
                holder = (ViewHolder) root.getTag();
            }

            final User user = mList.get(position);
            mPicasso.load(user.getProfileURL())
                    .placeholder(R.drawable.ic_contact_picture)
                    .resizeDimen(R.dimen.avatar_width, R.dimen.avatar_height)
                    .into(holder.mAvatar);

            holder.mName.setText(user.getName());
            root.setTag(R.id.itemPayload, user);

            return root;
        }
    }

    private static class ViewHolder {
        final ImageView mAvatar;
        final TextView mName;

        public ViewHolder(View v) {
            mAvatar = (ImageView) v.findViewById(R.id.imgAvatar);
            mName = (TextView) v.findViewById(R.id.txtName);
        }
    }
}
