package lb.listviewvariants;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import lb.library.PinnedHeaderListView;
import lb.library.SearchablePinnedHeaderListViewAdapter;
import lb.library.StringArrayAlphabetIndexer;
import lb.listviewvariants.utils.CircularContactView;
import lb.listviewvariants.utils.ContactImageUtil;
import lb.listviewvariants.utils.ContactsQuery;
import lb.listviewvariants.utils.ImageCache;
import lb.listviewvariants.utils.async_task_thread_pool.AsyncTaskEx;
import lb.listviewvariants.utils.async_task_thread_pool.AsyncTaskThreadPool;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements OnQueryTextListener{
	private LayoutInflater mInflater;
	private PinnedHeaderListView mListView;
	private ContactsAdapter mAdapter;
	private SupportMenuItem mSearchMenuItem;
	private SearchView mSearchView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mInflater = LayoutInflater.from(MainActivity.this);
		setContentView(R.layout.activity_main);
		final ArrayList<Contact> contacts = getContacts();
		Collections.sort(contacts, new Comparator<Contact>() {
			private final Collator sCollator = Collator.getInstance();
			@Override
			public int compare(Contact lhs, Contact rhs) {
//				char lhsFirstLetter = TextUtils.isEmpty(lhs.displayName) ? ' '
//						: lhs.displayName.charAt(0);
//				char rhsFirstLetter = TextUtils.isEmpty(rhs.displayName) ? ' '
//						: rhs.displayName.charAt(0);
//				int firstLetterComparison = Character
//						.toUpperCase(lhsFirstLetter)
//						- Character.toUpperCase(rhsFirstLetter);
//				if (firstLetterComparison == 0)
//					return lhs.displayName.compareTo(rhs.displayName);
//				return firstLetterComparison;
				return sCollator.compare(lhs.section, rhs.section);
			}
		});
		mListView = (PinnedHeaderListView) findViewById(android.R.id.list);
		mAdapter = new ContactsAdapter(contacts);

		int pinnedHeaderBackgroundColor = getResources().getColor(
				getResIdFromAttribute(this, android.R.attr.colorBackground));
		mAdapter.setPinnedHeaderBackgroundColor(pinnedHeaderBackgroundColor);
		mAdapter.setPinnedHeaderTextColor(getResources().getColor(
				R.color.pinned_header_text));
		mListView.setPinnedHeaderView(mInflater.inflate(
				R.layout.pinned_header_listview_side_header, mListView, false));
		mListView.setAdapter(mAdapter);
		mListView.setOnScrollListener(mAdapter);
		mListView.setEnableHeaderTransparencyChanges(false);
		// mAdapter.getFilter().filter(mQueryText,new FilterListener() ...
		// You can also perform operations on selected item by using :
		// mListView.setOnItemClickListener(new
		// AdapterView.OnItemClickListener() ...
	}
	
	public static int getResIdFromAttribute(final Activity activity,
			final int attr) {
		if (attr == 0)
			return 0;
		final TypedValue typedValue = new TypedValue();
		activity.getTheme().resolveAttribute(attr, typedValue, true);
		return typedValue.resourceId;
	}

	private ArrayList<Contact> getContacts() {
		if (checkContactsReadPermission()) {
			Uri uri = ContactsQuery.CONTENT_URI;
			final Cursor cursor = managedQuery(uri, ContactsQuery.PROJECTION,
					ContactsQuery.SELECTION, null, ContactsQuery.SORT_ORDER);
			if (cursor == null)
				return null;
			ArrayList<Contact> result = new ArrayList<Contact>();
			while (cursor.moveToNext()) {
				Contact contact = new Contact();
				contact.contactUri = ContactsContract.Contacts.getLookupUri(
						cursor.getLong(ContactsQuery.ID),
						cursor.getString(ContactsQuery.LOOKUP_KEY));
				contact.displayName = cursor
						.getString(ContactsQuery.DISPLAY_NAME);
				contact.photoId = cursor
						.getString(ContactsQuery.PHOTO_THUMBNAIL_DATA);
				contact.section = getSection(contact.displayName);
				result.add(contact);
			}

			return result;
		}
		ArrayList<Contact> result = new ArrayList<Contact>();
		Random r = new Random();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 1000; ++i) {
			Contact contact = new Contact();
			sb.delete(0, sb.length());
			int strLength = r.nextInt(10) + 1;
			for (int j = 0; j < strLength; ++j)
				switch (r.nextInt(3)) {
				case 0:
					sb.append((char) ('a' + r.nextInt('z' - 'a')));
					break;
				case 1:
					sb.append((char) ('A' + r.nextInt('Z' - 'A')));
					break;
				case 2:
					sb.append((char) ('0' + r.nextInt('9' - '0')));
					break;
				}

			contact.displayName = sb.toString();
			result.add(contact);
		}
		return result;
	}
	private String getSection(CharSequence label) {
		if (label == null || label.length() == 0)
			return SECTION_BEFORE_A;
		 char c = Character.toUpperCase(label.charAt(0));
		 String locale = getResources().getConfiguration().locale.toString();
		 if (TextUtils.equals(locale,
					Locale.CHINA.toString())) {// 中文
				String pinyin = HanziToPinyin.getPinYin(String.valueOf(c));
				c = TextUtils.isEmpty(pinyin) ? c : pinyin.charAt(0);
			}
		if (c < 'A')
			return SECTION_BEFORE_A;
		if (c > 'Z')
			return SECTION_AFTER_Z;
		return Character.toString(c);
	}
	private static final String SECTION_BEFORE_A = "#";
	private static final String SECTION_AFTER_Z = "#";
	
	private boolean checkContactsReadPermission() {
		String permission = "android.permission.READ_CONTACTS";
		int res = checkCallingOrSelfPermission(permission);
		return (res == PackageManager.PERMISSION_GRANTED);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mAdapter.mAsyncTaskThreadPool.cancelAllTasks(true);
	}

	private static class Contact {
		long contactId;
		Uri contactUri;
		String displayName;
		String photoId;
		public String section;
	}

	@Override
	public void onBackPressed() {
		if (mSearchMenuItem.isActionViewExpanded()) {
			mSearchMenuItem.collapseActionView();
			return;
		}
		super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Search view
		getMenuInflater().inflate(R.menu.search, menu);
		// Filter the list the user is looking it via SearchView
		mSearchMenuItem = (SupportMenuItem) menu.findItem(R.id.menu_search);
		mSearchView = (SearchView) MenuItemCompat
				.getActionView(mSearchMenuItem);
		if (mSearchMenuItem == null || mSearchView == null) {
			return false;
		}
		mSearchView.setOnQueryTextListener(this);
		mSearchView.setQueryHint(getString(R.string.searchHint));

		return super.onCreateOptionsMenu(menu);
	}

	// ////////////////////////////////////////////////////////////
	// ContactsAdapter //
	// //////////////////
	private class ContactsAdapter extends
			SearchablePinnedHeaderListViewAdapter<Contact> {
		private ArrayList<Contact> mContacts;
		private final int CONTACT_PHOTO_IMAGE_SIZE;
		private final int[] PHOTO_TEXT_BACKGROUND_COLORS;
		private final AsyncTaskThreadPool mAsyncTaskThreadPool = new AsyncTaskThreadPool(
				1, 2, 10);

		@Override
		public CharSequence getSectionTitle(int sectionIndex) {
			return ((StringArrayAlphabetIndexer.AlphaBetSection) getSections()[sectionIndex])
					.getName();
		}

		public ContactsAdapter(final ArrayList<Contact> contacts) {
			setData(contacts);
			PHOTO_TEXT_BACKGROUND_COLORS = getResources().getIntArray(
					R.array.contacts_text_background_colors);
			CONTACT_PHOTO_IMAGE_SIZE = getResources().getDimensionPixelSize(
					R.dimen.list_item__contact_imageview_size);
		}

		public void setData(final ArrayList<Contact> contacts) {
			this.mContacts = contacts;
			final String[] generatedContactNames = generateContactNames(contacts);
			setSectionIndexer(new StringArrayAlphabetIndexer(
					generatedContactNames, true));
		}

		private String[] generateContactNames(final List<Contact> contacts) {
			final ArrayList<String> contactNames = new ArrayList<String>();
			if (contacts != null)
				for (final Contact contactEntity : contacts)
					contactNames.add(contactEntity.section);
			return contactNames.toArray(new String[contactNames.size()]);
		}

		@Override
		public View getView(final int position, final View convertView,
				final ViewGroup parent) {
			final ViewHolder holder;
			final View rootView;
			if (convertView == null) {
				holder = new ViewHolder();
				rootView = mInflater.inflate(R.layout.listview_item, parent,
						false);
				holder.friendProfileCircularContactView = (CircularContactView) rootView
						.findViewById(R.id.listview_item__friendPhotoImageView);
				holder.friendProfileCircularContactView.getTextView()
						.setTextColor(0xFFffffff);
				holder.friendName = (TextView) rootView
						.findViewById(R.id.listview_item__friendNameTextView);
				holder.headerView = (TextView) rootView
						.findViewById(R.id.header_text);
				rootView.setTag(holder);
			} else {
				rootView = convertView;
				holder = (ViewHolder) rootView.getTag();
			}
			final Contact contact = getItem(position);
			final String displayName = contact.displayName;
			final String section = contact.section;
			holder.friendName.setText(displayName);
			boolean hasPhoto = !TextUtils.isEmpty(contact.photoId);
			if (holder.updateTask != null && !holder.updateTask.isCancelled())
				holder.updateTask.cancel(true);
			final Bitmap cachedBitmap = hasPhoto ? ImageCache.INSTANCE
					.getBitmapFromMemCache(contact.photoId) : null;
			if (cachedBitmap != null)
				holder.friendProfileCircularContactView
						.setImageBitmap(cachedBitmap);
			else {
				final int backgroundColorToUse = PHOTO_TEXT_BACKGROUND_COLORS[position
						% PHOTO_TEXT_BACKGROUND_COLORS.length];
				if (TextUtils.isEmpty(section))
					holder.friendProfileCircularContactView.setImageResource(
							R.drawable.ic_person_white_120dp,
							backgroundColorToUse);
				else {
					final String characterToShow = TextUtils
							.isEmpty(section) ? "" : section;
					holder.friendProfileCircularContactView
							.setTextAndBackgroundColor(characterToShow,
									backgroundColorToUse);
				}
				if (hasPhoto) {
					holder.updateTask = new AsyncTaskEx<Void, Void, Bitmap>() {

						@Override
						public Bitmap doInBackground(final Void... params) {
							if (isCancelled())
								return null;
							final Bitmap b = ContactImageUtil
									.loadContactPhotoThumbnail(
											MainActivity.this, contact.photoId,
											CONTACT_PHOTO_IMAGE_SIZE);
							if (b != null)
								return ThumbnailUtils.extractThumbnail(b,
										CONTACT_PHOTO_IMAGE_SIZE,
										CONTACT_PHOTO_IMAGE_SIZE);
							return null;
						}

						@Override
						public void onPostExecute(final Bitmap result) {
							super.onPostExecute(result);
							if (result == null)
								return;
							ImageCache.INSTANCE.addBitmapToCache(
									contact.photoId, result);
							holder.friendProfileCircularContactView
									.setImageBitmap(result);
						}
					};
					mAsyncTaskThreadPool.executeAsyncTask(holder.updateTask);
				}
			}
			bindSectionHeader(holder.headerView, null, position);
			return rootView;
		}

		@Override
		public boolean doFilter(final Contact item,
				final CharSequence constraint) {
			if (TextUtils.isEmpty(constraint))
				return true;
			final String displayName = item.displayName;
			return !TextUtils.isEmpty(displayName)
					&& displayName.toLowerCase(Locale.getDefault()).contains(
							constraint.toString().toLowerCase(
									Locale.getDefault()));
		}

		@Override
		public ArrayList<Contact> getOriginalList() {
			return mContacts;
		}

	}

	// /////////////////////////////////////////////////////////////////////////////////////
	// ViewHolder //
	// /////////////
	private static class ViewHolder {
		public CircularContactView friendProfileCircularContactView;
		TextView friendName, headerView;
		public AsyncTaskEx<Void, Void, Bitmap> updateTask;
	}

	@Override
	public boolean onQueryTextChange(String query) {
		if (mAdapter != null)
			mAdapter.getFilter().filter(query);
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		if (TextUtils.isEmpty(query)) {
			return false;
		}
		if (mSearchView != null) {
			final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null) {
				imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
			}
			mSearchView.clearFocus();
		}
		return true;
	}
}
