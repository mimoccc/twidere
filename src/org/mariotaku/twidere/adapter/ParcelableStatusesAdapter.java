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

package org.mariotaku.twidere.adapter;

import static android.text.format.DateUtils.getRelativeTimeSpanString;
import static org.mariotaku.twidere.model.ParcelableLocation.isValidLocation;
import static org.mariotaku.twidere.util.Utils.configBaseAdapter;
import static org.mariotaku.twidere.util.Utils.formatSameDayTime;
import static org.mariotaku.twidere.util.Utils.getAccountColor;
import static org.mariotaku.twidere.util.Utils.getAccountScreenName;
import static org.mariotaku.twidere.util.Utils.getNameDisplayOptionInt;
import static org.mariotaku.twidere.util.Utils.getStatusBackground;
import static org.mariotaku.twidere.util.Utils.getUserColor;
import static org.mariotaku.twidere.util.Utils.isFiltered;
import static org.mariotaku.twidere.util.Utils.openImage;
import static org.mariotaku.twidere.util.Utils.openUserProfile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mariotaku.twidere.R;
import org.mariotaku.twidere.adapter.iface.IStatusesAdapter;
import org.mariotaku.twidere.app.TwidereApplication;
import org.mariotaku.twidere.model.ParcelableStatus;
import org.mariotaku.twidere.model.PreviewImage;
import org.mariotaku.twidere.util.ImageLoaderWrapper;
import org.mariotaku.twidere.util.MultiSelectManager;
import org.mariotaku.twidere.util.OnLinkClickHandler;
import org.mariotaku.twidere.util.TwidereLinkify;
import org.mariotaku.twidere.view.holder.StatusViewHolder;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

public class ParcelableStatusesAdapter extends ArrayAdapter<ParcelableStatus> implements
		IStatusesAdapter<List<ParcelableStatus>>, OnClickListener, ImageLoadingListener {

	private final Context mContext;
	private final ImageLoaderWrapper mImageLoader;
	private final MultiSelectManager mMultiSelectManager;
	private final TwidereLinkify mLinkify;
	private final SQLiteDatabase mDatabase;
	private final Map<View, String> mLoadingViewsMap = new HashMap<View, String>();

	private MenuButtonClickListener mListener;
	private boolean mDisplayProfileImage, mDisplayImagePreview, mShowAccountColor, mShowAbsoluteTime, mGapDisallowed,
			mMentionsHighlightDisabled, mDisplaySensitiveContents, mIndicateMyStatusDisabled, mLinkHighlightingEnabled,
			mIsLastItemFiltered, mFiltersEnabled;
	private float mTextSize;
	private int mNameDisplayOption, mLinkHighlightStyle;
	private boolean mFilterIgnoreSource, mFilterIgnoreScreenName, mFilterIgnoreTextHtml, mFilterIgnoreTextPlain;
	private int mMaxAnimationPosition;

	public ParcelableStatusesAdapter(final Context context) {
		super(context, R.layout.status_list_item);
		mContext = context;
		final TwidereApplication app = TwidereApplication.getInstance(context);
		mMultiSelectManager = app.getMultiSelectManager();
		mImageLoader = app.getImageLoaderWrapper();
		mDatabase = app.getSQLiteDatabase();
		mLinkify = new TwidereLinkify(new OnLinkClickHandler(mContext));
		configBaseAdapter(context, this);
		setMaxAnimationPosition(-1);
	}

	@Override
	public long findItemIdByPosition(final int position) {
		if (position >= 0 && position < getCount()) return getItem(position).id;
		return -1;
	}

	@Override
	public int findItemPositionByStatusId(final long status_id) {
		final int count = getCount();
		for (int i = 0; i < count; i++) {
			if (getItem(i).id == status_id) return i;
		}
		return -1;
	}

	@Override
	public int getCount() {
		final int count = super.getCount();
		return mFiltersEnabled && mIsLastItemFiltered && count > 0 ? count - 1 : count;
	}

	@Override
	public long getItemId(final int position) {
		final ParcelableStatus item = getItem(position);
		return item != null ? item.id : -1;
	}

	@Override
	public ParcelableStatus getLastStatus() {
		if (super.getCount() == 0) return null;
		return getItem(super.getCount() - 1);
	}

	@Override
	public ParcelableStatus getStatus(final int position) {
		return getItem(position);
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		final View view = super.getView(position, convertView, parent);
		final Object tag = view.getTag();
		final StatusViewHolder holder;

		if (tag instanceof StatusViewHolder) {
			holder = (StatusViewHolder) tag;
		} else {
			holder = new StatusViewHolder(view);
			holder.profile_image.setOnClickListener(this);
			holder.my_profile_image.setOnClickListener(this);
			holder.image_preview.setOnClickListener(this);
			holder.item_menu.setOnClickListener(this);
			view.setTag(holder);
		}

		// Clear images in prder to prevent images in recycled view shown.
		// holder.profile_image.setImageDrawable(null);
		// holder.my_profile_image.setImageDrawable(null);
		// holder.image_preview.setImageDrawable(null);

		final ParcelableStatus status = getItem(position);

		final boolean show_gap = status.is_gap && !mGapDisallowed && position != getCount() - 1;

		holder.setShowAsGap(show_gap);

		if (!show_gap) {

			holder.setAccountColorEnabled(mShowAccountColor);

			if (mLinkHighlightingEnabled) {
				holder.text.setText(Html.fromHtml(status.text_html));
				mLinkify.applyAllLinks(holder.text, status.account_id, status.is_possibly_sensitive);
				holder.text.setMovementMethod(null);
			} else {
				holder.text.setText(status.text_unescaped);
			}

			if (mShowAccountColor) {
				holder.setAccountColor(getAccountColor(mContext, status.account_id));
			}

			final String account_screen_name = getAccountScreenName(mContext, status.account_id);
			final boolean is_mention = !TextUtils.isEmpty(status.text_plain)
					&& status.text_plain.toLowerCase().contains('@' + account_screen_name.toLowerCase());
			final boolean is_my_status = status.account_id == status.user_id;
			holder.setUserColor(getUserColor(mContext, status.user_id));
			holder.setHighlightColor(getStatusBackground(mMentionsHighlightDisabled ? false : is_mention,
					status.is_favorite, status.is_retweet));
			holder.setTextSize(mTextSize);

			holder.setIsMyStatus(is_my_status && !mIndicateMyStatusDisabled);

			holder.setUserType(status.user_is_verified, status.user_is_protected);
			holder.setNameDisplayOption(mNameDisplayOption);
			holder.setName(status.user_name, status.user_screen_name);
			if (mLinkHighlightingEnabled) {
				mLinkify.applyUserProfileLink(holder.name, status.account_id, status.user_id, status.user_screen_name);
				mLinkify.applyUserProfileLink(holder.screen_name, status.account_id, status.user_id,
						status.user_screen_name);
				holder.name.setMovementMethod(null);
				holder.screen_name.setMovementMethod(null);
			}
			if (mShowAbsoluteTime) {
				holder.time.setText(formatSameDayTime(mContext, status.timestamp));
			} else {
				holder.time.setText(getRelativeTimeSpanString(status.timestamp));
			}
			holder.setStatusType(status.is_favorite, isValidLocation(status.location), status.has_media,
					status.is_possibly_sensitive);
			holder.setIsReplyRetweet(status.in_reply_to_status_id > 0, status.is_retweet);
			if (status.is_retweet) {
				holder.setRetweetedBy(status.retweet_count, status.retweeted_by_name, status.retweeted_by_screen_name);
			} else if (status.in_reply_to_status_id > 0) {
				holder.setReplyTo(status.in_reply_to_screen_name);
			}
			if (mDisplayProfileImage) {
				mImageLoader.displayProfileImage(holder.my_profile_image, status.user_profile_image_url);
				mImageLoader.displayProfileImage(holder.profile_image, status.user_profile_image_url);
				holder.profile_image.setTag(position);
				holder.my_profile_image.setTag(position);
			} else {
				holder.profile_image.setVisibility(View.GONE);
				holder.my_profile_image.setVisibility(View.GONE);
			}
			final boolean has_preview = mDisplayImagePreview && status.has_media && status.image_preview_url != null;
			holder.image_preview_container.setVisibility(has_preview ? View.VISIBLE : View.GONE);
			if (has_preview) {
				if (status.is_possibly_sensitive && !mDisplaySensitiveContents) {
					holder.image_preview.setImageDrawable(null);
					holder.image_preview.setBackgroundResource(R.drawable.image_preview_nsfw);
					holder.image_preview_progress.setVisibility(View.GONE);
				} else if (!status.image_preview_url.equals(mLoadingViewsMap.get(holder.image_preview))) {
					holder.image_preview.setBackgroundResource(0);
					mImageLoader.displayPreviewImage(holder.image_preview, status.image_preview_url, this);
				}
				holder.image_preview.setTag(position);
			}
			holder.item_menu.setTag(position);
		}
		if (position > mMaxAnimationPosition) {
			view.startAnimation(holder.item_animation);
			mMaxAnimationPosition = position;
		}
		return view;
	}

	@Override
	public boolean isLastItemFiltered() {
		return mIsLastItemFiltered;
	}

	@Override
	public void onClick(final View view) {
		if (mMultiSelectManager.isActive()) return;
		final Object tag = view.getTag();
		final int position = tag instanceof Integer ? (Integer) tag : -1;
		if (position == -1) return;
		switch (view.getId()) {
			case R.id.image_preview: {
				final ParcelableStatus status = getStatus(position);
				if (status == null) return;
				final PreviewImage spec = PreviewImage.getAllAvailableImage(status.image_original_url);
				if (spec != null) {
					openImage(mContext, spec.image_full_url, spec.image_original_url, status.is_possibly_sensitive);
				} else {
					openImage(mContext, status.image_original_url, null, status.is_possibly_sensitive);
				}
				break;
			}
			case R.id.my_profile_image:
			case R.id.profile_image: {
				final ParcelableStatus status = getStatus(position);
				if (status == null) return;
				if (mContext instanceof Activity) {
					openUserProfile((Activity) mContext, status.account_id, status.user_id, status.user_screen_name);
				}
				break;
			}
			case R.id.item_menu: {
				if (position == -1 || mListener == null) return;
				mListener.onMenuButtonClick(view, position, getItemId(position));
				break;
			}
		}
	}

	@Override
	public void onLoadingCancelled(final String url, final View view) {
		if (view == null || url == null || url.equals(mLoadingViewsMap.get(view))) return;
		mLoadingViewsMap.remove(view);
		final View parent = (View) view.getParent();
		final View progress = parent.findViewById(R.id.image_preview_progress);
		if (progress != null) {
			progress.setVisibility(View.GONE);
		}
	}

	@Override
	public void onLoadingComplete(final String url, final View view, final Bitmap bitmap) {
		if (view == null) return;
		mLoadingViewsMap.remove(view);
		final View parent = (View) view.getParent();
		final View progress = parent.findViewById(R.id.image_preview_progress);
		if (progress != null) {
			progress.setVisibility(View.GONE);
		}
	}

	@Override
	public void onLoadingFailed(final String url, final View view, final FailReason reason) {
		if (view == null) return;
		mLoadingViewsMap.remove(view);
		final View parent = (View) view.getParent();
		final View progress = parent.findViewById(R.id.image_preview_progress);
		if (progress != null) {
			progress.setVisibility(View.GONE);
		}
	}

	@Override
	public void onLoadingProgressChanged(final String imageUri, final View view, final int current, final int total) {
		if (total == 0) return;
		final View parent = (View) view.getParent();
		final ProgressBar progress = (ProgressBar) parent.findViewById(R.id.image_preview_progress);
		if (progress != null) {
			progress.setIndeterminate(false);
			progress.setProgress(100 * current / total);
		}
	}

	@Override
	public void onLoadingStarted(final String url, final View view) {
		if (view == null || url == null || url.equals(mLoadingViewsMap.get(view))) return;
		mLoadingViewsMap.put(view, url);
		final View parent = (View) view.getParent();
		final ProgressBar progress = (ProgressBar) parent.findViewById(R.id.image_preview_progress);
		if (progress != null) {
			progress.setVisibility(View.VISIBLE);
			progress.setIndeterminate(true);
			progress.setMax(100);
		}
	}

	@Override
	public void setData(final List<ParcelableStatus> data) {
		clear();
		if (data != null && !data.isEmpty()) {
			addAll(data);
			notifyDataSetChanged();
		}
		rebuildFilterInfo();
	}

	@Override
	public void setDisplayImagePreview(final boolean display) {
		if (display == mDisplayImagePreview) return;
		mDisplayImagePreview = display;
		notifyDataSetChanged();
	}

	@Override
	public void setDisplayProfileImage(final boolean display) {
		if (display == mDisplayProfileImage) return;
		mDisplayProfileImage = display;
		notifyDataSetChanged();
	}

	@Override
	public void setDisplaySensitiveContents(final boolean display) {
		if (display == mDisplaySensitiveContents) return;
		mDisplaySensitiveContents = display;
		notifyDataSetChanged();
	}

	@Override
	public void setFiltersEnabled(final boolean enabled) {
		if (mFiltersEnabled == enabled) return;
		mFiltersEnabled = enabled;
		rebuildFilterInfo();
		notifyDataSetChanged();
	}

	@Override
	public void setGapDisallowed(final boolean disallowed) {
		if (mGapDisallowed == disallowed) return;
		mGapDisallowed = disallowed;
		notifyDataSetChanged();
	}

	@Override
	public void setIgnoredFilterFields(final boolean text_plain, final boolean text_html, final boolean screen_name,
			final boolean source) {
		mFilterIgnoreTextPlain = text_plain;
		mFilterIgnoreTextHtml = text_html;
		mFilterIgnoreScreenName = screen_name;
		mFilterIgnoreSource = source;
		rebuildFilterInfo();
		notifyDataSetChanged();
	}

	@Override
	public void setIndicateMyStatusDisabled(final boolean disable) {
		if (mIndicateMyStatusDisabled == disable) return;
		mIndicateMyStatusDisabled = disable;
		notifyDataSetChanged();
	}

	@Override
	public void setLinkHightlightingEnabled(final boolean enable) {
		if (mLinkHighlightingEnabled == enable) return;
		mLinkHighlightingEnabled = enable;
		notifyDataSetChanged();
	}

	@Override
	public void setLinkUnderlineOnly(final boolean underline_only) {
		final int style = underline_only ? TwidereLinkify.HIGHLIGHT_STYLE_UNDERLINE
				: TwidereLinkify.HIGHLIGHT_STYLE_COLOR;
		if (mLinkHighlightStyle == style) return;
		mLinkify.setHighlightStyle(style);
		mLinkHighlightStyle = style;
		notifyDataSetChanged();
	}

	@Override
	public void setMaxAnimationPosition(final int position) {
		mMaxAnimationPosition = position;
	}

	@Override
	public void setMentionsHightlightDisabled(final boolean disable) {
		if (disable == mMentionsHighlightDisabled) return;
		mMentionsHighlightDisabled = disable;
		notifyDataSetChanged();
	}

	@Override
	public void setMenuButtonClickListener(final MenuButtonClickListener listener) {
		mListener = listener;
	}

	@Override
	public void setNameDisplayOption(final String option) {
		final int option_int = getNameDisplayOptionInt(option);
		if (option_int == mNameDisplayOption) return;
		mNameDisplayOption = option_int;
		notifyDataSetChanged();
	}

	@Override
	public void setShowAbsoluteTime(final boolean show) {
		if (show == mShowAbsoluteTime) return;
		mShowAbsoluteTime = show;
		notifyDataSetChanged();
	}

	@Override
	public void setShowAccountColor(final boolean show) {
		if (show == mShowAccountColor) return;
		mShowAccountColor = show;
		notifyDataSetChanged();
	}

	@Override
	public void setTextSize(final float text_size) {
		if (text_size == mTextSize) return;
		mTextSize = text_size;
		notifyDataSetChanged();
	}

	private void rebuildFilterInfo() {
		if (!isEmpty()) {
			final ParcelableStatus last = getItem(super.getCount() - 1);
			final String text_plain = mFilterIgnoreTextPlain ? null : last.text_plain;
			final String text_html = mFilterIgnoreTextHtml ? null : last.text_html;
			final String screen_name = mFilterIgnoreScreenName ? null : last.user_screen_name;
			final String source = mFilterIgnoreSource ? null : last.source;
			mIsLastItemFiltered = isFiltered(mDatabase, text_plain, text_html, screen_name, source);
		} else {
			mIsLastItemFiltered = false;
		}
	}
}
