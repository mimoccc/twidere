/*
 *				Twidere - Twitter client for Android
 * 
 * Copyright (C) 2012 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.fragment;

import static org.mariotaku.twidere.util.Utils.addIntentToMenu;
import static org.mariotaku.twidere.util.Utils.getActivatedAccountIds;
import static org.mariotaku.twidere.util.Utils.getDefaultTextSize;
import static org.mariotaku.twidere.util.Utils.openUserProfile;

import java.util.Collections;
import java.util.List;

import org.mariotaku.popupmenu.PopupMenu;
import org.mariotaku.popupmenu.PopupMenu.OnMenuItemClickListener;
import org.mariotaku.twidere.R;
import org.mariotaku.twidere.adapter.ParcelableUsersAdapter;
import org.mariotaku.twidere.adapter.iface.IBaseAdapter.MenuButtonClickListener;
import org.mariotaku.twidere.loader.DummyParcelableUsersLoader;
import org.mariotaku.twidere.model.Panes;
import org.mariotaku.twidere.model.ParcelableUser;
import org.mariotaku.twidere.util.MultiSelectManager;
import org.mariotaku.twidere.util.NoDuplicatesArrayList;
import org.mariotaku.twidere.util.Utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

abstract class BaseUsersListFragment extends BasePullToRefreshListFragment implements
		LoaderCallbacks<List<ParcelableUser>>, OnItemLongClickListener, Panes.Left, OnMenuItemClickListener,
		MultiSelectManager.Callback, MenuButtonClickListener {

	private static final long TICKER_DURATION = 5000L;

	private SharedPreferences mPreferences;
	private PopupMenu mPopupMenu;
	private MultiSelectManager mMultiSelectManager;

	private ParcelableUsersAdapter mAdapter;

	private Handler mHandler;
	private Runnable mTicker;

	private boolean mLoadMoreAutomatically;
	private ListView mListView;
	private long mAccountId;
	private final List<ParcelableUser> mData = Collections
			.synchronizedList(new NoDuplicatesArrayList<ParcelableUser>());
	private boolean mTickerStopped, mBusy;

	private ParcelableUser mSelectedUser;

	public long getAccountId() {
		return mAccountId;
	}

	public final List<ParcelableUser> getData() {
		return mData;
	}

	@Override
	public ParcelableUsersAdapter getListAdapter() {
		return mAdapter;
	}

	public SharedPreferences getSharedPreferences() {
		return mPreferences;
	}

	public void loadMoreUsers() {
		if (isRefreshing()) return;
		final int count = mAdapter.getCount();
		if (count - 1 > 0) {
			final Bundle args = getArguments();
			if (args != null) {
				args.putLong(INTENT_KEY_MAX_ID, mAdapter.getItem(count - 1).id);
			}
			if (!getLoaderManager().hasRunningLoaders()) {
				getLoaderManager().restartLoader(0, args, this);
			}
		}
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		mAdapter = new ParcelableUsersAdapter(getActivity());
		mAdapter.setMenuButtonClickListener(this);
		mMultiSelectManager = getMultiSelectManager();
		mListView = getListView();
		mListView.setFastScrollEnabled(mPreferences.getBoolean(PREFERENCE_KEY_FAST_SCROLL_THUMB, false));
		final Bundle args = getArguments() != null ? getArguments() : new Bundle();
		final long account_id = args.getLong(INTENT_KEY_ACCOUNT_ID, -1);
		if (mAccountId != account_id) {
			mAdapter.clear();
			mData.clear();
		}
		mAccountId = account_id;
		mListView.setDivider(null);
		mListView.setOnItemLongClickListener(this);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, getArguments(), this);
		setListShown(false);
	}

	@Override
	public Loader<List<ParcelableUser>> onCreateLoader(final int id, final Bundle args) {
		setProgressBarIndeterminateVisibility(true);
		final Loader<List<ParcelableUser>> loader = newLoaderInstance(getActivity(), args);
		return loader != null ? loader : new DummyParcelableUsersLoader(getActivity());
	}

	@Override
	public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		final ParcelableUsersAdapter adapter = getListAdapter();
		final ParcelableUser user = adapter.findItem(id);
		if (user == null) return false;
		setItemSelected(user, position, !mMultiSelectManager.isSelected(user));
		return true;
	}

	@Override
	public void onItemsCleared() {
		mListView.clearChoices();
		mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
		// Workaround for Android bug
		// http://stackoverflow.com/questions/9754170/listview-selection-remains-persistent-after-exiting-choice-mode
		final int position = mListView.getFirstVisiblePosition(), offset = Utils.getFirstChildOffset(mListView);
		mListView.setAdapter(mAdapter);
		Utils.scrollListToPosition(mListView, position, offset);
	}

	@Override
	public void onItemSelected(final Object item) {
		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	}

	@Override
	public void onItemUnselected(final Object item) {
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
		final ParcelableUser user = mAdapter.findItem(id);
		if (user == null) return;
		if (mMultiSelectManager.isActive()) {
			setItemSelected(user, position, !mMultiSelectManager.isSelected(user));
			return;
		}
		openUserProfile(getActivity(), user);
	}

	@Override
	public void onLoaderReset(final Loader<List<ParcelableUser>> loader) {
		setProgressBarIndeterminateVisibility(false);
	}

	@Override
	public void onLoadFinished(final Loader<List<ParcelableUser>> loader, final List<ParcelableUser> data) {
		setProgressBarIndeterminateVisibility(false);
		mAdapter.setData(data);
		mAdapter.setShowAccountColor(getActivatedAccountIds(getActivity()).length > 1);
		setRefreshComplete();
		setListShown(true);
	}

	@Override
	public void onMenuButtonClick(final View button, final int position, final long id) {
		final ParcelableUser user = mAdapter.findItem(id);
		if (user == null) return;
		showMenu(button, user);
	}

	@Override
	public boolean onMenuItemClick(final MenuItem item) {
		if (mSelectedUser == null) return false;
		final ParcelableUser user = mSelectedUser;
		switch (item.getItemId()) {
			case MENU_VIEW_PROFILE: {
				openUserProfile(getActivity(), user);
				break;
			}
			default: {
				if (item.getIntent() != null) {
					try {
						startActivity(item.getIntent());
					} catch (final ActivityNotFoundException e) {
						Log.w(LOGTAG, e);
						return false;
					}
				}
				break;
			}
		}
		return true;
	}

	@Override
	public void onReachedBottom() {
		if (!mLoadMoreAutomatically) return;
		loadMoreUsers();
	}

	@Override
	public void onRefreshStarted() {
		super.onRefreshStarted();
		getLoaderManager().restartLoader(0, getArguments(), this);
	}

	@Override
	public void onResume() {
		super.onResume();
		mLoadMoreAutomatically = mPreferences.getBoolean(PREFERENCE_KEY_LOAD_MORE_AUTOMATICALLY, false);
		final float text_size = mPreferences.getInt(PREFERENCE_KEY_TEXT_SIZE, getDefaultTextSize(getActivity()));
		final boolean display_profile_image = mPreferences.getBoolean(PREFERENCE_KEY_DISPLAY_PROFILE_IMAGE, true);
		final String name_display_option = mPreferences.getString(PREFERENCE_KEY_NAME_DISPLAY_OPTION,
				NAME_DISPLAY_OPTION_BOTH);
		mAdapter.setDisplayProfileImage(display_profile_image);
		mAdapter.setTextSize(text_size);
		mAdapter.setNameDisplayOption(name_display_option);
	}

	@Override
	public void onScrollStateChanged(final AbsListView view, final int scrollState) {
		switch (scrollState) {
			case SCROLL_STATE_FLING:
			case SCROLL_STATE_TOUCH_SCROLL:
				mBusy = true;
				break;
			case SCROLL_STATE_IDLE:
				mBusy = false;
				break;
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		mTickerStopped = false;
		mHandler = new Handler();

		mTicker = new Runnable() {

			@Override
			public void run() {
				if (mTickerStopped) return;
				if (mListView != null && !mBusy) {
					mAdapter.notifyDataSetChanged();
				}
				final long now = SystemClock.uptimeMillis();
				final long next = now + TICKER_DURATION - now % TICKER_DURATION;
				mHandler.postAtTime(mTicker, next);
			}
		};
		mTicker.run();
		mMultiSelectManager.registerCallback(this);
	}

	@Override
	public void onStop() {
		mMultiSelectManager.unregisterCallback(this);
		mTickerStopped = true;
		if (mPopupMenu != null) {
			mPopupMenu.dismiss();
		}
		super.onStop();
	}

	protected abstract Loader<List<ParcelableUser>> newLoaderInstance(Context context, Bundle args);

	@Override
	protected void onPullUp() {
		if (mLoadMoreAutomatically) return;
		loadMoreUsers();
	}

	protected final void removeUsers(final long... user_ids) {
		if (user_ids == null || user_ids.length == 0) return;
		for (final long user_id : user_ids) {
			mData.remove(mAdapter.findItem(user_id));
		}
		mAdapter.setData(mData, true);
	}

	protected void setItemSelected(final ParcelableUser user, final int position, final boolean selected) {
		if (selected) {
			mMultiSelectManager.selectItem(user);
		} else {
			mMultiSelectManager.unselectItem(user);
		}
		mListView.setItemChecked(position, selected);
	}

	private void showMenu(final View view, final ParcelableUser user) {
		mPopupMenu = PopupMenu.getInstance(getActivity(), view);
		mPopupMenu.inflate(R.menu.action_user);
		final Menu menu = mPopupMenu.getMenu();
		final Intent extensions_intent = new Intent(INTENT_ACTION_EXTENSION_OPEN_USER);
		final Bundle extensions_extras = new Bundle();
		extensions_extras.putParcelable(INTENT_KEY_USER, user);
		extensions_intent.putExtras(extensions_extras);
		addIntentToMenu(getActivity(), menu, extensions_intent);
		mPopupMenu.setOnMenuItemClickListener(this);
		mPopupMenu.show();
	}
}
