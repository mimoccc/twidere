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

package org.mariotaku.twidere.activity;

import static org.mariotaku.twidere.util.Utils.cleanDatabasesByItemLimit;
import static org.mariotaku.twidere.util.Utils.getAccountIds;
import static org.mariotaku.twidere.util.Utils.getActivatedAccountIds;
import static org.mariotaku.twidere.util.Utils.getDefaultAccountId;
import static org.mariotaku.twidere.util.Utils.getHomeTabs;
import static org.mariotaku.twidere.util.Utils.openDirectMessagesConversation;
import static org.mariotaku.twidere.util.Utils.openSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.mariotaku.twidere.R;
import org.mariotaku.twidere.adapter.SupportTabsAdapter;
import org.mariotaku.twidere.fragment.APIUpgradeConfirmDialog;
import org.mariotaku.twidere.fragment.BasePullToRefreshListFragment;
import org.mariotaku.twidere.fragment.DirectMessagesFragment;
import org.mariotaku.twidere.fragment.HomeTimelineFragment;
import org.mariotaku.twidere.fragment.MentionsFragment;
import org.mariotaku.twidere.fragment.TrendsFragment;
import org.mariotaku.twidere.fragment.iface.SupportFragmentCallback;
import org.mariotaku.twidere.model.SupportTabSpec;
import org.mariotaku.twidere.provider.RecentSearchProvider;
import org.mariotaku.twidere.util.ArrayUtils;
import org.mariotaku.twidere.util.AsyncTwitterWrapper;
import org.mariotaku.twidere.util.MathUtils;
import org.mariotaku.twidere.util.MultiSelectEventHandler;
import org.mariotaku.twidere.util.ThemeUtils;
import org.mariotaku.twidere.view.ExtendedViewPager;
import org.mariotaku.twidere.view.TabPageIndicator;

import android.app.ActionBar;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManagerTrojan;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ProgressBar;
import edu.ucdavis.earlybird.ProfilingUtil;

public class HomeActivity extends DualPaneActivity implements OnClickListener, OnPageChangeListener,
		SupportFragmentCallback {

	private SharedPreferences mPreferences;
	private AsyncTwitterWrapper mTwitterWrapper;
	private NotificationManager mNotificationManager;
	private MultiSelectEventHandler mMultiSelectHandler;

	private ActionBar mActionBar;
	private SupportTabsAdapter mAdapter;

	private ExtendedViewPager mViewPager;
	private TabPageIndicator mIndicator;
	private DrawerLayout mDrawerLayout;
	private View mLeftDrawerContainer;
	private View mActionsActionView, mActionsButtonLayout;

	private boolean mDisplayAppIcon;
	private boolean mShowHomeTab, mShowMentionsTab, mShowMessagesTab, mShowTrendsTab;
	private boolean mBottomActionsButton;

	private Fragment mCurrentVisibleFragment;

	public static final int TAB_POSITION_HOME = 0;
	public static final int TAB_POSITION_MENTIONS = 1;
	public static final int TAB_POSITION_MESSAGES = 2;
	public static final int TAB_POSITION_TRENDS = 3;

	private final ArrayList<SupportTabSpec> mCustomTabs = new ArrayList<SupportTabSpec>();

	private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
			if (BROADCAST_TASK_STATE_CHANGED.equals(action)) {
				updateActionsButton();
				// updateRefreshingState();
			} else if (BROADCAST_ACCOUNT_LIST_DATABASE_UPDATED.equals(action)) {
				notifyAccountsChanged();
			}
		}

	};

	public void closeAccountsDrawer() {
		if (mDrawerLayout == null) return;
		mDrawerLayout.closeDrawer(Gravity.LEFT);
	}

	@Override
	public Fragment getCurrentVisibleFragment() {
		return mCurrentVisibleFragment;
	}

	public void notifyAccountsChanged() {
		if (mPreferences == null) return;
		final long[] account_ids = getAccountIds(this);
		final long default_id = mPreferences.getLong(PREFERENCE_KEY_DEFAULT_ACCOUNT_ID, -1);
		if (account_ids == null || account_ids.length == 0) {
			finish();
		} else if (account_ids.length > 0 && !ArrayUtils.contains(account_ids, default_id)) {
			mPreferences.edit().putLong(PREFERENCE_KEY_DEFAULT_ACCOUNT_ID, account_ids[0]).commit();
		}
	}

	@Override
	public void onBackStackChanged() {
		super.onBackStackChanged();
		if (!isDualPaneMode()) return;
		final FragmentManager fm = getSupportFragmentManager();
		final Fragment left_pane_fragment = fm.findFragmentById(PANE_LEFT);
		final boolean left_pane_used = left_pane_fragment != null && left_pane_fragment.isAdded();
		setPagingEnabled(!left_pane_used);
		final int count = fm.getBackStackEntryCount();
		if (count == 0) {
			showLeftPane();
		}
		// invalidateOptionsMenu();
		updateActionsButton();
	}

	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
			case R.id.actions_item:
			case R.id.actions_button:
				if (mViewPager == null || mAdapter == null) return;
				final int position = mViewPager.getCurrentItem();
				final SupportTabSpec tab = mAdapter.getTab(position);
				if (tab == null) {
					startActivity(new Intent(INTENT_ACTION_COMPOSE));
				} else {
					switch (tab.position) {
						case TAB_POSITION_MESSAGES:
							openDirectMessagesConversation(this, -1, -1, null);
							break;
						case TAB_POSITION_TRENDS:
							onSearchRequested();
							break;
						default:
							startActivity(new Intent(INTENT_ACTION_COMPOSE));
					}
				}
				break;
		}
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();
		mViewPager = (ExtendedViewPager) findViewById(R.id.main);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mActionsButtonLayout = findViewById(R.id.actions_button);
		mLeftDrawerContainer = findViewById(R.id.left_drawer_container);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.menu_home, menu);
		mActionsActionView = menu.findItem(MENU_ACTIONS).getActionView();
		if (mActionsActionView != null) {
			mActionsActionView.setOnClickListener(this);
			updateActionsButton();
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case MENU_HOME: {
				final FragmentManager fm = getSupportFragmentManager();
				final int count = fm.getBackStackEntryCount();
				if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
					mDrawerLayout.closeDrawer(Gravity.LEFT);
					return true;
				} else if (count == 0 && !mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
					mDrawerLayout.openDrawer(Gravity.LEFT);
					return true;
				}
				if (isDualPaneMode() && !FragmentManagerTrojan.isStateSaved(fm)) {
					for (int i = 0; i < count; i++) {
						fm.popBackStackImmediate();
					}
					updateActionsButton();
				}
				return true;
			}
			case MENU_SEARCH: {
				onSearchRequested();
				return true;
			}
			case MENU_SELECT_ACCOUNT: {
				if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
					mDrawerLayout.closeDrawer(Gravity.LEFT);
				} else {
					mDrawerLayout.openDrawer(Gravity.LEFT);
				}
				return true;
			}
			case MENU_FILTERS: {
				final Intent intent = new Intent(this, FiltersActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
				return true;
			}
			case MENU_SETTINGS: {
				final Intent intent = new Intent(this, SettingsActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
	}

	@Override
	public void onPageScrollStateChanged(final int state) {

	}

	@Override
	public void onPageSelected(final int position) {
		final SupportTabSpec tab = mAdapter.getTab(position);
		switch (tab.position) {
			case TAB_POSITION_HOME: {
				mTwitterWrapper.clearNotification(NOTIFICATION_ID_HOME_TIMELINE);
				break;
			}
			case TAB_POSITION_MENTIONS: {
				mTwitterWrapper.clearNotification(NOTIFICATION_ID_MENTIONS);
				break;
			}
			case TAB_POSITION_MESSAGES: {
				mTwitterWrapper.clearNotification(NOTIFICATION_ID_DIRECT_MESSAGES);
				break;
			}
		}
		if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
			mDrawerLayout.closeDrawer(Gravity.LEFT);
		}
		updateActionsButton();
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		final boolean leftside_compose_button = mPreferences.getBoolean(PREFERENCE_KEY_LEFTSIDE_COMPOSE_BUTTON, false);
		final MenuItem actionsItem = menu.findItem(MENU_ACTIONS);
		if (actionsItem != null) {
			actionsItem.setVisible(!mBottomActionsButton);
		}
		if (mActionsButtonLayout != null) {
			mActionsButtonLayout.setVisibility(mBottomActionsButton ? View.VISIBLE : View.GONE);
			// mComposeButton.setVisibility(mBottomActionsButton &&
			// !isRightPaneUsed() ? View.VISIBLE : View.GONE);
			final FrameLayout.LayoutParams compose_lp = (LayoutParams) mActionsButtonLayout.getLayoutParams();
			compose_lp.gravity = Gravity.BOTTOM | (leftside_compose_button ? Gravity.LEFT : Gravity.RIGHT);
			mActionsButtonLayout.setLayoutParams(compose_lp);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onSetUserVisibleHint(final Fragment fragment, final boolean isVisibleToUser) {
		if (isVisibleToUser) {
			mCurrentVisibleFragment = fragment;
		}
		updateRefreshingState();
	}

	@Override
	protected BasePullToRefreshListFragment getCurrentPullToRefreshFragment() {
		if (mCurrentVisibleFragment instanceof BasePullToRefreshListFragment)
			return (BasePullToRefreshListFragment) mCurrentVisibleFragment;
		else if (mCurrentVisibleFragment instanceof SupportFragmentCallback) {
			final Fragment curr = ((SupportFragmentCallback) mCurrentVisibleFragment).getCurrentVisibleFragment();
			if (curr instanceof BasePullToRefreshListFragment) return (BasePullToRefreshListFragment) curr;
		}
		return null;
	}

	@Override
	protected int getDualPaneLayoutRes() {
		return R.layout.home_dual_pane;
	}

	@Override
	protected int getNormalLayoutRes() {
		return R.layout.home;
	}

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		mTwitterWrapper = getTwitterWrapper();
		mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mMultiSelectHandler = new MultiSelectEventHandler(this);
		mMultiSelectHandler.dispatchOnCreate();
		super.onCreate(savedInstanceState);
		sendBroadcast(new Intent(BROADCAST_HOME_ACTIVITY_ONCREATE));
		final Resources res = getResources();
		mDisplayAppIcon = res.getBoolean(R.bool.home_display_icon);
		final long[] account_ids = getAccountIds(this);
		if (account_ids.length == 0) {
			final Intent intent = new Intent(INTENT_ACTION_TWITTER_LOGIN);
			intent.setClass(this, SignInActivity.class);
			startActivity(intent);
			finish();
			return;
		} else {
			notifyAccountsChanged();
		}
		final boolean refresh_on_start = mPreferences.getBoolean(PREFERENCE_KEY_REFRESH_ON_START, false);
		final int initial_tab = handleIntent(getIntent(), savedInstanceState == null);
		mActionBar = getActionBar();
		mActionBar.setCustomView(R.layout.base_tabs);
		mActionBar.setDisplayShowTitleEnabled(false);
		mActionBar.setDisplayShowCustomEnabled(true);
		mActionBar.setDisplayShowHomeEnabled(mDisplayAppIcon);
		if (mDisplayAppIcon) {
			mActionBar.setHomeButtonEnabled(true);
		}
		final View view = mActionBar.getCustomView();

		mIndicator = (TabPageIndicator) view.findViewById(android.R.id.tabs);
		ThemeUtils.applyBackground(mIndicator);
		final boolean tab_display_label = res.getBoolean(R.bool.tab_display_label);
		mAdapter = new SupportTabsAdapter(this, getSupportFragmentManager(), mIndicator);
		mShowHomeTab = mPreferences.getBoolean(PREFERENCE_KEY_SHOW_HOME_TAB, true);
		mShowMentionsTab = mPreferences.getBoolean(PREFERENCE_KEY_SHOW_MENTIONS_TAB, true);
		mShowMessagesTab = mPreferences.getBoolean(PREFERENCE_KEY_SHOW_MESSAGES_TAB, true);
		mShowTrendsTab = mPreferences.getBoolean(PREFERENCE_KEY_SHOW_TRENDS_TAB, true);
		initTabs(getHomeTabs(this));
		mViewPager.setAdapter(mAdapter);
		mViewPager.setOffscreenPageLimit(3);
		mIndicator.setViewPager(mViewPager);
		mIndicator.setOnPageChangeListener(this);
		mIndicator.setDisplayLabel(tab_display_label);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.LEFT);
		mLeftDrawerContainer.setBackgroundResource(getPaneBackground());
		mActionsButtonLayout.setOnClickListener(this);
		// getSupportFragmentManager().addOnBackStackChangedListener(this);

		final boolean remember_position = mPreferences.getBoolean(PREFERENCE_KEY_REMEMBER_POSITION, true);
		final long[] activated_ids = getActivatedAccountIds(this);
		if (activated_ids.length <= 0) {
			// TODO set activated account automatically
			startActivityForResult(new Intent(INTENT_ACTION_SELECT_ACCOUNT), REQUEST_SELECT_ACCOUNT);
		} else if (initial_tab >= 0) {
			mViewPager.setCurrentItem(MathUtils.clamp(initial_tab, mAdapter.getCount(), 0));
		} else if (remember_position) {
			final int position = mPreferences.getInt(PREFERENCE_KEY_SAVED_TAB_POSITION, TAB_POSITION_HOME);
			mViewPager.setCurrentItem(MathUtils.clamp(position, mAdapter.getCount(), 0));
		}
		if (refresh_on_start && savedInstanceState == null) {
			mTwitterWrapper.refreshAll();
		}
		showAPIUpgradeNotice();
		showDataProfilingRequest();
	}

	@Override
	protected void onDestroy() {
		// Delete unused items in databases.
		cleanDatabasesByItemLimit(this);
		sendBroadcast(new Intent(BROADCAST_HOME_ACTIVITY_ONDESTROY));
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		handleIntent(intent, false);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mViewPager.setPagingEnabled(!mPreferences.getBoolean(PREFERENCE_KEY_DISABLE_TAB_SWIPE, false));
		mBottomActionsButton = mPreferences.getBoolean(PREFERENCE_KEY_BOTTOM_COMPOSE_BUTTON, false);
		invalidateOptionsMenu();
		updateActionsButton();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mMultiSelectHandler.dispatchOnStart();
		sendBroadcast(new Intent(BROADCAST_HOME_ACTIVITY_ONSTART));
		final IntentFilter filter = new IntentFilter(BROADCAST_TASK_STATE_CHANGED);
		registerReceiver(mStateReceiver, filter);
		final boolean show_home_tab = mPreferences.getBoolean(PREFERENCE_KEY_SHOW_HOME_TAB, true);
		final boolean show_mentions_tab = mPreferences.getBoolean(PREFERENCE_KEY_SHOW_MENTIONS_TAB, true);
		final boolean show_messages_tab = mPreferences.getBoolean(PREFERENCE_KEY_SHOW_MESSAGES_TAB, true);
		final boolean show_trends_tab = mPreferences.getBoolean(PREFERENCE_KEY_SHOW_TRENDS_TAB, true);

		final List<SupportTabSpec> tabs = getHomeTabs(this);
		if (isTabsChanged(tabs) || show_home_tab != mShowHomeTab || show_mentions_tab != mShowMentionsTab
				|| show_messages_tab != mShowMessagesTab || show_trends_tab != mShowTrendsTab) {
			restart();
		}
		// UCD
		ProfilingUtil.profile(this, ProfilingUtil.FILE_NAME_APP, "App onStart");
	}

	@Override
	protected void onStop() {
		mMultiSelectHandler.dispatchOnStop();
		unregisterReceiver(mStateReceiver);
		mPreferences.edit().putInt(PREFERENCE_KEY_SAVED_TAB_POSITION, mViewPager.getCurrentItem()).commit();
		sendBroadcast(new Intent(BROADCAST_HOME_ACTIVITY_ONSTOP));

		// UCD
		ProfilingUtil.profile(this, ProfilingUtil.FILE_NAME_APP, "App onStop");
		super.onStop();
	}

	protected void setPagingEnabled(final boolean enabled) {
		if (mIndicator != null && mViewPager != null) {
			mViewPager.setPagingEnabled(!mPreferences.getBoolean(PREFERENCE_KEY_DISABLE_TAB_SWIPE, false));
			mIndicator.setSwitchingEnabled(enabled);
			mIndicator.setEnabled(enabled);
		}
	}

	private int handleIntent(final Intent intent, final boolean first_create) {
		Log.d(LOGTAG, String.format("Intent: %s", intent));
		// Reset intent
		setIntent(new Intent(this, HomeActivity.class));
		final String action = intent.getAction();
		if (Intent.ACTION_SEARCH.equals(action)) {
			final String query = intent.getStringExtra(SearchManager.QUERY);
			if (first_create) {
				final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
						RecentSearchProvider.AUTHORITY, RecentSearchProvider.MODE);
				suggestions.saveRecentQuery(query, null);
			}
			final long account_id = getDefaultAccountId(this);
			openSearch(this, account_id, query);
			return -1;
		}
		final Bundle extras = intent.getExtras();
		final boolean refresh_on_start = mPreferences.getBoolean(PREFERENCE_KEY_REFRESH_ON_START, false);
		final long[] refreshed_ids = extras != null ? extras.getLongArray(INTENT_KEY_IDS) : null;
		if (refreshed_ids != null) {
			mTwitterWrapper.refreshAll(refreshed_ids);
		} else if (first_create && refresh_on_start) {
			mTwitterWrapper.refreshAll();
		}

		int initial_tab = -1;
		if (extras != null) {
			initial_tab = extras.getInt(INTENT_KEY_INITIAL_TAB, -1);
			if (initial_tab != -1 && mViewPager != null) {
				switch (initial_tab) {
					case TAB_POSITION_HOME: {
						if (mShowHomeTab) {
							mTwitterWrapper.clearNotification(NOTIFICATION_ID_HOME_TIMELINE);
						}
						break;
					}
					case TAB_POSITION_MENTIONS: {
						mTwitterWrapper.clearNotification(NOTIFICATION_ID_MENTIONS);
						break;
					}
					case TAB_POSITION_MESSAGES: {
						mTwitterWrapper.clearNotification(NOTIFICATION_ID_DIRECT_MESSAGES);
						break;
					}
				}
			}
			final Intent extra_intent = extras.getParcelable(INTENT_KEY_EXTRA_INTENT);
			if (extra_intent != null) {
				startActivity(extra_intent);
			}
		}
		return initial_tab;
	}

	private boolean hasActivatedTask() {
		if (mTwitterWrapper == null) return false;
		return mTwitterWrapper.hasActivatedTask();
	}

	private void initTabs(final Collection<? extends SupportTabSpec> tabs) {
		mCustomTabs.clear();
		mCustomTabs.addAll(tabs);
		mAdapter.clear();
		if (mShowHomeTab) {
			mAdapter.addTab(HomeTimelineFragment.class, null, getString(R.string.home), R.drawable.ic_tab_home,
					TAB_POSITION_HOME);
		}
		if (mShowMentionsTab) {
			mAdapter.addTab(MentionsFragment.class, null, getString(R.string.mentions), R.drawable.ic_tab_mention,
					TAB_POSITION_MENTIONS);
		}
		if (mShowMessagesTab) {
			mAdapter.addTab(DirectMessagesFragment.class, null, getString(R.string.direct_messages),
					R.drawable.ic_tab_message, TAB_POSITION_MESSAGES);
		}
		if (mShowTrendsTab) {
			mAdapter.addTab(TrendsFragment.class, null, getString(R.string.trends), R.drawable.ic_tab_trends,
					TAB_POSITION_TRENDS);
		}
		mAdapter.addTabs(tabs);
	}

	private boolean isTabsChanged(final List<SupportTabSpec> tabs) {
		if (mCustomTabs.size() == 0 && tabs == null) return false;
		if (mCustomTabs.size() != tabs.size()) return true;
		final int size = mCustomTabs.size();
		for (int i = 0; i < size; i++) {
			if (!mCustomTabs.get(i).equals(tabs.get(i))) return true;
		}
		return false;
	}

	private void showAPIUpgradeNotice() {
		if (!mPreferences.getBoolean(PREFERENCE_KEY_API_UPGRADE_CONFIRMED, false)) {
			final FragmentManager fm = getSupportFragmentManager();
			if (fm.findFragmentByTag(FRAGMENT_TAG_API_UPGRADE_NOTICE) == null
					|| !fm.findFragmentByTag(FRAGMENT_TAG_API_UPGRADE_NOTICE).isAdded()) {
				new APIUpgradeConfirmDialog().show(getSupportFragmentManager(), "api_upgrade_notice");
			}
		}
	}

	private void showDataProfilingRequest() {
		if (mPreferences.getBoolean(PREFERENCE_KEY_SHOW_UCD_DATA_PROFILING_REQUEST, true)) {
			final Intent intent = new Intent(this, DataProfilingSettingsActivity.class);
			final PendingIntent content_intent = PendingIntent.getActivity(this, 0, intent, 0);
			final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
			builder.setAutoCancel(true);
			builder.setSmallIcon(R.drawable.ic_stat_question_mark);
			builder.setTicker(getString(R.string.data_profiling_notification_ticker));
			builder.setContentTitle(getString(R.string.data_profiling_notification_title));
			builder.setContentText(getString(R.string.data_profiling_notification_desc));
			builder.setContentIntent(content_intent);
			mNotificationManager.notify(NOTIFICATION_ID_DATA_PROFILING, builder.build());
		}
	}

	private void updateActionsButton() {
		if (mViewPager == null || mAdapter == null) return;
		final int action_icon, button_icon, title;
		final int position = mViewPager.getCurrentItem();
		final SupportTabSpec tab = mAdapter.getTab(position);
		final boolean light_action_bar = ThemeUtils.isLightActionBar(getCurrentThemeResource());
		if (tab == null) {
			title = R.string.compose;
			action_icon = light_action_bar ? R.drawable.ic_action_status_compose_light
					: R.drawable.ic_action_status_compose_dark;
			button_icon = R.drawable.ic_menu_status_compose;
		} else {
			switch (tab.position) {
				case TAB_POSITION_MESSAGES:
					action_icon = light_action_bar ? R.drawable.ic_action_compose_light
							: R.drawable.ic_action_compose_dark;
					button_icon = R.drawable.ic_menu_compose;
					title = R.string.compose;
					break;
				case TAB_POSITION_TRENDS:
					action_icon = light_action_bar ? R.drawable.ic_action_search_light
							: R.drawable.ic_action_search_dark;
					button_icon = android.R.drawable.ic_menu_search;
					title = android.R.string.search_go;
					break;
				default:
					action_icon = light_action_bar ? R.drawable.ic_action_status_compose_light
							: R.drawable.ic_action_status_compose_dark;
					button_icon = R.drawable.ic_menu_status_compose;
					title = R.string.compose;
			}
		}
		final View view = mBottomActionsButton ? mActionsButtonLayout : mActionsActionView;
		if (view == null) return;
		final boolean has_task = hasActivatedTask();
		final ImageView actions_icon = (ImageView) view.findViewById(R.id.actions_icon);
		final ProgressBar progress = (ProgressBar) view.findViewById(R.id.progress);
		actions_icon.setImageResource(mBottomActionsButton ? button_icon : action_icon);
		actions_icon.setContentDescription(getString(title));
		actions_icon.setVisibility(has_task ? View.GONE : View.VISIBLE);
		progress.setVisibility(has_task ? View.VISIBLE : View.GONE);
	}
}
