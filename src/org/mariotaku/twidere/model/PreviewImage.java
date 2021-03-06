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

package org.mariotaku.twidere.model;

import static android.text.TextUtils.isEmpty;
import static org.mariotaku.twidere.util.Utils.matcherGroup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mariotaku.twidere.util.HtmlLinkExtractor;
import org.mariotaku.twidere.util.HtmlLinkExtractor.HtmlLink;

public class PreviewImage {

	public static final String AVAILABLE_URL_SCHEME_PREFIX = "(https?:\\/\\/)?";
	public static final String AVAILABLE_IMAGE_SHUFFIX = "(png|jpeg|jpg|gif|bmp)";
	public static final String SINA_WEIBO_IMAGES_AVAILABLE_SIZES = "(woriginal|large|thumbnail|bmiddle|mw[\\d]+)";
	public static final String GOOGLE_IMAGES_AVAILABLE_SIZES = "((([whs]\\d+|no)\\-?)+)";

	private static final String STRING_PATTERN_TWITTER_IMAGES_DOMAIN = "(p|pbs)\\.twimg\\.com";
	private static final String STRING_PATTERN_SINA_WEIBO_IMAGES_DOMAIN = "[\\w\\d]+\\.sinaimg\\.cn|[\\w\\d]+\\.sina\\.cn";
	private static final String STRING_PATTERN_LOCKERZ_DOMAIN = "lockerz\\.com";
	private static final String STRING_PATTERN_PLIXI_DOMAIN = "plixi\\.com";
	private static final String STRING_PATTERN_INSTAGRAM_DOMAIN = "instagr\\.am|instagram\\.com";
	private static final String STRING_PATTERN_TWITPIC_DOMAIN = "twitpic\\.com";
	private static final String STRING_PATTERN_IMGLY_DOMAIN = "img\\.ly";
	private static final String STRING_PATTERN_YFROG_DOMAIN = "yfrog\\.com";
	private static final String STRING_PATTERN_TWITGOO_DOMAIN = "twitgoo\\.com";
	private static final String STRING_PATTERN_MOBYPICTURE_DOMAIN = "moby\\.to";
	private static final String STRING_PATTERN_IMGUR_DOMAIN = "imgur\\.com|i\\.imgur\\.com";
	private static final String STRING_PATTERN_PHOTOZOU_DOMAIN = "photozou\\.jp";
	private static final String STRING_PATTERN_GOOGLE_IMAGES_DOMAIN = "(lh|gp|s)(\\d+)?\\.(ggpht|googleusercontent)\\.com";

	private static final String STRING_PATTERN_IMAGES_NO_SCHEME = "[^:\\/\\/].+?\\." + AVAILABLE_IMAGE_SHUFFIX;
	private static final String STRING_PATTERN_TWITTER_IMAGES_NO_SCHEME = STRING_PATTERN_TWITTER_IMAGES_DOMAIN
			+ "(\\/media)?\\/([\\d\\w\\-_]+)\\." + AVAILABLE_IMAGE_SHUFFIX;
	private static final String STRING_PATTERN_SINA_WEIBO_IMAGES_NO_SCHEME = "("
			+ STRING_PATTERN_SINA_WEIBO_IMAGES_DOMAIN + ")" + "\\/" + SINA_WEIBO_IMAGES_AVAILABLE_SIZES
			+ "\\/(([\\d\\w]+)\\." + AVAILABLE_IMAGE_SHUFFIX + ")";
	private static final String STRING_PATTERN_LOCKERZ_NO_SCHEME = "(" + STRING_PATTERN_LOCKERZ_DOMAIN + ")"
			+ "\\/s\\/(\\w+)\\/?";
	private static final String STRING_PATTERN_PLIXI_NO_SCHEME = "(" + STRING_PATTERN_PLIXI_DOMAIN + ")"
			+ "\\/p\\/(\\w+)\\/?";
	private static final String STRING_PATTERN_INSTAGRAM_NO_SCHEME = "(" + STRING_PATTERN_INSTAGRAM_DOMAIN + ")"
			+ "\\/p\\/([_\\-\\d\\w]+)\\/?";
	private static final String STRING_PATTERN_TWITPIC_NO_SCHEME = STRING_PATTERN_TWITPIC_DOMAIN + "\\/([\\d\\w]+)\\/?";
	private static final String STRING_PATTERN_IMGLY_NO_SCHEME = STRING_PATTERN_IMGLY_DOMAIN + "\\/([\\w\\d]+)\\/?";
	private static final String STRING_PATTERN_YFROG_NO_SCHEME = STRING_PATTERN_YFROG_DOMAIN + "\\/([\\w\\d]+)\\/?";
	private static final String STRING_PATTERN_TWITGOO_NO_SCHEME = STRING_PATTERN_TWITGOO_DOMAIN + "\\/([\\d\\w]+)\\/?";
	private static final String STRING_PATTERN_MOBYPICTURE_NO_SCHEME = STRING_PATTERN_MOBYPICTURE_DOMAIN
			+ "\\/([\\d\\w]+)\\/?";
	private static final String STRING_PATTERN_IMGUR_NO_SCHEME = "(" + STRING_PATTERN_IMGUR_DOMAIN + ")"
			+ "\\/([\\d\\w]+)((?-i)s|(?-i)l)?(\\." + AVAILABLE_IMAGE_SHUFFIX + ")?";
	private static final String STRING_PATTERN_PHOTOZOU_NO_SCHEME = STRING_PATTERN_PHOTOZOU_DOMAIN
			+ "\\/photo\\/show\\/([\\d]+)\\/([\\d]+)\\/?";
	private static final String STRING_PATTERN_GOOGLE_IMAGES_NO_SCHEME = "(" + STRING_PATTERN_GOOGLE_IMAGES_DOMAIN
			+ ")" + "((\\/[\\w\\d\\-\\_]+)+)\\/" + GOOGLE_IMAGES_AVAILABLE_SIZES + "\\/.+";
	private static final String STRING_PATTERN_GOOGLE_PROXY_IMAGES_NO_SCHEME = "("
			+ STRING_PATTERN_GOOGLE_IMAGES_DOMAIN + ")" + "\\/proxy\\/([\\w\\d\\-\\_]+)="
			+ GOOGLE_IMAGES_AVAILABLE_SIZES;

	private static final String STRING_PATTERN_IMAGES = AVAILABLE_URL_SCHEME_PREFIX + STRING_PATTERN_IMAGES_NO_SCHEME;
	private static final String STRING_PATTERN_TWITTER_IMAGES = AVAILABLE_URL_SCHEME_PREFIX
			+ STRING_PATTERN_TWITTER_IMAGES_NO_SCHEME;
	private static final String STRING_PATTERN_SINA_WEIBO_IMAGES = AVAILABLE_URL_SCHEME_PREFIX
			+ STRING_PATTERN_SINA_WEIBO_IMAGES_NO_SCHEME;
	private static final String STRING_PATTERN_LOCKERZ = AVAILABLE_URL_SCHEME_PREFIX + STRING_PATTERN_LOCKERZ_NO_SCHEME;
	private static final String STRING_PATTERN_PLIXI = AVAILABLE_URL_SCHEME_PREFIX + STRING_PATTERN_PLIXI_NO_SCHEME;
	private static final String STRING_PATTERN_INSTAGRAM = AVAILABLE_URL_SCHEME_PREFIX
			+ STRING_PATTERN_INSTAGRAM_NO_SCHEME;
	private static final String STRING_PATTERN_TWITPIC = AVAILABLE_URL_SCHEME_PREFIX + STRING_PATTERN_TWITPIC_NO_SCHEME;
	private static final String STRING_PATTERN_IMGLY = AVAILABLE_URL_SCHEME_PREFIX + STRING_PATTERN_IMGLY_NO_SCHEME;
	private static final String STRING_PATTERN_YFROG = AVAILABLE_URL_SCHEME_PREFIX + STRING_PATTERN_YFROG_NO_SCHEME;
	private static final String STRING_PATTERN_TWITGOO = AVAILABLE_URL_SCHEME_PREFIX + STRING_PATTERN_TWITGOO_NO_SCHEME;
	private static final String STRING_PATTERN_MOBYPICTURE = AVAILABLE_URL_SCHEME_PREFIX
			+ STRING_PATTERN_MOBYPICTURE_NO_SCHEME;
	private static final String STRING_PATTERN_IMGUR = AVAILABLE_URL_SCHEME_PREFIX + STRING_PATTERN_IMGUR_NO_SCHEME;
	private static final String STRING_PATTERN_PHOTOZOU = AVAILABLE_URL_SCHEME_PREFIX
			+ STRING_PATTERN_PHOTOZOU_NO_SCHEME;
	private static final String STRING_PATTERN_GOOGLE_IMAGES = AVAILABLE_URL_SCHEME_PREFIX
			+ STRING_PATTERN_GOOGLE_IMAGES_NO_SCHEME;
	private static final String STRING_PATTERN_GOOGLE_PROXY_IMAGES = AVAILABLE_URL_SCHEME_PREFIX
			+ STRING_PATTERN_GOOGLE_PROXY_IMAGES_NO_SCHEME;

	public static final Pattern PATTERN_ALL_AVAILABLE_IMAGES = Pattern.compile(AVAILABLE_URL_SCHEME_PREFIX + "("
			+ STRING_PATTERN_IMAGES_NO_SCHEME + "|" + STRING_PATTERN_TWITTER_IMAGES_NO_SCHEME + "|"
			+ STRING_PATTERN_INSTAGRAM_NO_SCHEME + "|" + STRING_PATTERN_GOOGLE_IMAGES_NO_SCHEME + "|"
			+ STRING_PATTERN_SINA_WEIBO_IMAGES_NO_SCHEME + "|" + STRING_PATTERN_LOCKERZ_NO_SCHEME + "|"
			+ STRING_PATTERN_PLIXI_NO_SCHEME + "|" + STRING_PATTERN_TWITPIC_NO_SCHEME + "|"
			+ STRING_PATTERN_IMGLY_NO_SCHEME + "|" + STRING_PATTERN_YFROG_NO_SCHEME + "|"
			+ STRING_PATTERN_TWITGOO_NO_SCHEME + "|" + STRING_PATTERN_MOBYPICTURE_NO_SCHEME + "|"
			+ STRING_PATTERN_IMGUR_NO_SCHEME + "|" + STRING_PATTERN_PHOTOZOU_NO_SCHEME + ")", Pattern.CASE_INSENSITIVE);

	public static final Pattern PATTERN_PREVIEW_AVAILABLE_IMAGES_MATCH_ONLY = Pattern.compile(
			AVAILABLE_URL_SCHEME_PREFIX + "(" + STRING_PATTERN_IMAGES_NO_SCHEME + "|"
					+ STRING_PATTERN_TWITTER_IMAGES_DOMAIN + "|" + STRING_PATTERN_INSTAGRAM_DOMAIN + "|"
					+ STRING_PATTERN_GOOGLE_IMAGES_DOMAIN + "|" + STRING_PATTERN_SINA_WEIBO_IMAGES_DOMAIN + "|"
					+ STRING_PATTERN_LOCKERZ_DOMAIN + "|" + STRING_PATTERN_PLIXI_DOMAIN + "|"
					+ STRING_PATTERN_TWITPIC_DOMAIN + "|" + STRING_PATTERN_IMGLY_DOMAIN + "|"
					+ STRING_PATTERN_YFROG_DOMAIN + "|" + STRING_PATTERN_TWITGOO_DOMAIN + "|"
					+ STRING_PATTERN_MOBYPICTURE_DOMAIN + "|" + STRING_PATTERN_IMGUR_DOMAIN + "|"
					+ STRING_PATTERN_PHOTOZOU_DOMAIN + ")", Pattern.CASE_INSENSITIVE);

	public static final Pattern PATTERN_IMAGES = Pattern.compile(STRING_PATTERN_IMAGES, Pattern.CASE_INSENSITIVE);
	public static final Pattern PATTERN_TWITTER_IMAGES = Pattern.compile(STRING_PATTERN_TWITTER_IMAGES,
			Pattern.CASE_INSENSITIVE);
	public static final Pattern PATTERN_SINA_WEIBO_IMAGES = Pattern.compile(STRING_PATTERN_SINA_WEIBO_IMAGES,
			Pattern.CASE_INSENSITIVE);
	public static final Pattern PATTERN_LOCKERZ = Pattern.compile(STRING_PATTERN_LOCKERZ, Pattern.CASE_INSENSITIVE);
	public static final Pattern PATTERN_PLIXI = Pattern.compile(STRING_PATTERN_PLIXI, Pattern.CASE_INSENSITIVE);

	public static final Pattern PATTERN_INSTAGRAM = Pattern.compile(STRING_PATTERN_INSTAGRAM, Pattern.CASE_INSENSITIVE);
	public static final int INSTAGRAM_GROUP_ID = 3;

	public static final Pattern PATTERN_TWITPIC = Pattern.compile(STRING_PATTERN_TWITPIC, Pattern.CASE_INSENSITIVE);
	public static final int TWITPIC_GROUP_ID = 2;

	public static final Pattern PATTERN_IMGLY = Pattern.compile(STRING_PATTERN_IMGLY, Pattern.CASE_INSENSITIVE);
	public static final int IMGLY_GROUP_ID = 2;

	public static final Pattern PATTERN_YFROG = Pattern.compile(STRING_PATTERN_YFROG, Pattern.CASE_INSENSITIVE);
	public static final int YFROG_GROUP_ID = 2;

	public static final Pattern PATTERN_TWITGOO = Pattern.compile(STRING_PATTERN_TWITGOO, Pattern.CASE_INSENSITIVE);
	public static final int TWITGOO_GROUP_ID = 2;

	public static final Pattern PATTERN_MOBYPICTURE = Pattern.compile(STRING_PATTERN_MOBYPICTURE,
			Pattern.CASE_INSENSITIVE);
	public static final int MOBYPICTURE_GROUP_ID = 2;

	public static final Pattern PATTERN_IMGUR = Pattern.compile(STRING_PATTERN_IMGUR, Pattern.CASE_INSENSITIVE);
	public static final int IMGUR_GROUP_ID = 3;

	public static final Pattern PATTERN_PHOTOZOU = Pattern.compile(STRING_PATTERN_PHOTOZOU, Pattern.CASE_INSENSITIVE);
	public static final int PHOTOZOU_GROUP_ID = 3;

	public static final Pattern PATTERN_GOOGLE_IMAGES = Pattern.compile(STRING_PATTERN_GOOGLE_IMAGES,
			Pattern.CASE_INSENSITIVE);
	public static final int GOOGLE_IMAGES_GROUP_SERVER = 2;
	public static final int GOOGLE_IMAGES_GROUP_ID = 6;

	public static final Pattern PATTERN_GOOGLE_PROXY_IMAGES = Pattern.compile(STRING_PATTERN_GOOGLE_PROXY_IMAGES,
			Pattern.CASE_INSENSITIVE);
	public static final int GOOGLE_PROXY_IMAGES_GROUP_SERVER = 2;
	public static final int GOOGLE_PROXY_IMAGES_GROUP_ID = 6;

	private static final PreviewImage EMPTY_INSTANCE = new PreviewImage(null, null, null);

	public final String image_preview_url, image_full_url, image_original_url;

	public PreviewImage(final String preview_image_link, final String full_image_link, final String orig_link) {
		image_preview_url = preview_image_link;
		image_full_url = full_image_link;
		image_original_url = orig_link;
	}

	@Override
	public String toString() {
		return "PreviewImage{image_preview_url=" + image_preview_url + ", image_full_url=" + image_full_url
				+ ", image_original_url=" + image_original_url + "}";
	}

	public static PreviewImage getAllAvailableImage(final String link) {
		if (link == null) return null;
		Matcher m;
		m = PATTERN_TWITTER_IMAGES.matcher(link);
		if (m.matches()) return getTwitterImage(link);
		m = PATTERN_INSTAGRAM.matcher(link);
		if (m.matches()) return getInstagramImage(matcherGroup(m, INSTAGRAM_GROUP_ID), link);
		m = PATTERN_GOOGLE_IMAGES.matcher(link);
		if (m.matches()) return getGoogleImage(m);
		m = PATTERN_GOOGLE_PROXY_IMAGES.matcher(link);
		if (m.matches()) return getGoogleProxyImage(m);
		m = PATTERN_SINA_WEIBO_IMAGES.matcher(link);
		if (m.matches()) return getSinaWeiboImage(link);
		m = PATTERN_TWITPIC.matcher(link);
		if (m.matches()) return getTwitpicImage(matcherGroup(m, TWITPIC_GROUP_ID), link);
		m = PATTERN_IMGUR.matcher(link);
		if (m.matches()) return getImgurImage(matcherGroup(m, IMGUR_GROUP_ID), link);
		m = PATTERN_IMGLY.matcher(link);
		if (m.matches()) return getImglyImage(matcherGroup(m, IMGLY_GROUP_ID), link);
		m = PATTERN_YFROG.matcher(link);
		if (m.matches()) return getYfrogImage(matcherGroup(m, YFROG_GROUP_ID), link);
		m = PATTERN_LOCKERZ.matcher(link);
		if (m.matches()) return getLockerzAndPlixiImage(link);
		m = PATTERN_PLIXI.matcher(link);
		if (m.matches()) return getLockerzAndPlixiImage(link);
		m = PATTERN_TWITGOO.matcher(link);
		if (m.matches()) return getTwitgooImage(matcherGroup(m, TWITGOO_GROUP_ID), link);
		m = PATTERN_MOBYPICTURE.matcher(link);
		if (m.matches()) return getMobyPictureImage(matcherGroup(m, MOBYPICTURE_GROUP_ID), link);
		m = PATTERN_PHOTOZOU.matcher(link);
		if (m.matches()) return getPhotozouImage(matcherGroup(m, PHOTOZOU_GROUP_ID), link);
		return null;
	}

	public static PreviewImage getEmpty() {
		return EMPTY_INSTANCE;
	}

	public static PreviewImage getGoogleImage(final Matcher m) {
		final String server = matcherGroup(m, GOOGLE_IMAGES_GROUP_SERVER), id = matcherGroup(m, GOOGLE_IMAGES_GROUP_ID);
		if (isEmpty(server) || isEmpty(id)) return null;
		final String full = "https://" + server + id + "/s0/full";
		return new PreviewImage(full, full, full);
	}

	public static PreviewImage getGoogleProxyImage(final Matcher m) {
		final String server = matcherGroup(m, GOOGLE_PROXY_IMAGES_GROUP_SERVER);
		final String id = matcherGroup(m, GOOGLE_PROXY_IMAGES_GROUP_ID);
		if (isEmpty(server) || isEmpty(id)) return null;
		final String full = "https://" + server + "/proxy/" + id + "=s0";
		return new PreviewImage(full, full, full);
	}

	public static PreviewImage getImglyImage(final String id, final String orig) {
		if (isEmpty(id)) return null;
		final String full = "https://img.ly/show/full/" + id;
		return new PreviewImage(full, full, orig);

	}

	public static PreviewImage getImgurImage(final String id, final String orig) {
		if (isEmpty(id)) return null;
		final String full = "http://i.imgur.com/" + id + ".jpg";
		return new PreviewImage(full, full, orig);
	}

	public static PreviewImage getInstagramImage(final String id, final String orig) {
		if (isEmpty(id)) return null;
		final String full = "https://instagr.am/p/" + id + "/media/?size=l";
		return new PreviewImage(full, full, orig);
	}

	public static PreviewImage getLockerzAndPlixiImage(final String url) {
		if (isEmpty(url)) return null;
		final String full = "https://api.plixi.com/api/tpapi.svc/imagefromurl?url=" + url + "&size=big";
		return new PreviewImage(full, full, url);

	}

	public static PreviewImage getMobyPictureImage(final String id, final String orig) {
		if (isEmpty(id)) return null;
		final String full = "https://moby.to/" + id + ":full";
		return new PreviewImage(full, full, orig);
	}

	public static PreviewImage getPhotozouImage(final String id, final String orig) {
		if (isEmpty(id)) return null;
		final String full = "http://photozou.jp/p/img/" + id;
		return new PreviewImage(full, full, orig);
	}

	public static PreviewImage getPreviewImage(final String html, final boolean include_image) {
		if (html == null) return null;
		if (!include_image
				&& (html.contains(".twimg.com/") || html.contains("://instagr.am/")
						|| html.contains("://instagram.com/") || html.contains("://imgur.com/")
						|| html.contains("://i.imgur.com/") || html.contains("://twitpic.com/")
						|| html.contains("://img.ly/") || html.contains("://yfrog.com/")
						|| html.contains("://twitgoo.com/") || html.contains("://moby.to/")
						|| html.contains("://plixi.com/p/") || html.contains("://lockerz.com/s/")
						|| html.contains(".sinaimg.cn/") || html.contains("://photozou.jp/")
						|| html.contains(".googleusercontent.com/") || html.contains(".ggpht.com/")))
			return getEmpty();
		final HtmlLinkExtractor extractor = new HtmlLinkExtractor();
		for (final HtmlLink link : extractor.grabLinks(html)) {
			final PreviewImage image = getAllAvailableImage(link.getLink());
			if (image != null) return image;
		}
		return null;
	}

	public static PreviewImage getSinaWeiboImage(final String url) {
		if (isEmpty(url)) return null;
		final String full = url.replaceAll("\\/" + SINA_WEIBO_IMAGES_AVAILABLE_SIZES + "\\/", "/large/");
		return new PreviewImage(full, full, full);
	}

	public static PreviewImage getTwitgooImage(final String id, final String orig) {
		if (isEmpty(id)) return null;
		final String full = "https://twitgoo.com/show/img/" + id;
		return new PreviewImage(full, full, orig);
	}

	public static PreviewImage getTwitpicImage(final String id, final String orig) {
		if (isEmpty(id)) return null;
		final String full = "https://twitpic.com/show/large/" + id;
		return new PreviewImage(full, full, orig);
	}

	public static PreviewImage getTwitterImage(final String url) {
		if (isEmpty(url)) return null;
		final String full = (url + ":large").replaceFirst("https?://", "https://");
		return new PreviewImage(full, full, full);
	}

	public static PreviewImage getYfrogImage(final String id, final String orig) {
		if (isEmpty(id)) return null;
		final String full = "https://yfrog.com/" + id + ":medium";
		return new PreviewImage(full, full, orig);

	}
}
