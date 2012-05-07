package com.juliasoft.libretto.utils;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class IDContextMenu implements DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {

	/**
	 * menu-like list item with icon
	 */
	protected class IconContextMenuItem {
		public final CharSequence text;
		public final Drawable image;
		public final String actionTag;

		/**
		 * public constructor
		 * 
		 * @param res
		 *            resource handler
		 * @param title
		 *            menu item title
		 * @param imageResourceId
		 *            id of icon in resource
		 * @param actionTag
		 *            indicate action of menu item
		 */
		public IconContextMenuItem(Resources res, CharSequence title, int imageResourceId, String actionTag) {
			text = title;
			image = imageResourceId != -1 ? res.getDrawable(imageResourceId) : null;
			this.actionTag = actionTag;
		}
	}

	/**
	 * IconContextMenu On Click Listener interface
	 */
	public interface IconContextMenuOnClickListener {
		public abstract void onClick(String menuId);
	}

	/**
	 * Menu-like list adapter with icon
	 */
	protected class IconMenuAdapter extends BaseAdapter {
		private final Context context;
		private final ArrayList<IconContextMenuItem> mItems = new ArrayList<IconContextMenuItem>();

		public IconMenuAdapter(Context context) {
			this.context = context;
		}

		/**
		 * add item to adapter
		 * 
		 * @param menuItem
		 */
		public void addItem(IconContextMenuItem menuItem) {
			mItems.add(menuItem);
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public Object getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			IconContextMenuItem item = (IconContextMenuItem) getItem(position);

			TextView textView = (TextView) convertView;
			if (textView == null) {
				Resources res = parentActivity.getResources();
				textView = new TextView(context);
				AbsListView.LayoutParams param = new AbsListView.LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
				textView.setLayoutParams(param);
				textView.setPadding((int) toPixel(res, 15), 0, (int) toPixel(res, 15), 0);
				textView.setGravity(android.view.Gravity.CENTER_VERTICAL);

				TypedValue tv = new TypedValue();
				if (context.getTheme().resolveAttribute(android.R.attr.textAppearanceLargeInverse, tv, true))
					textView.setTextAppearance(context, tv.resourceId);

				textView.setMinHeight(LIST_PREFERED_HEIGHT);
				textView.setCompoundDrawablePadding((int) toPixel(res, 14));
			}

			textView.setTag(item);
			textView.setText(item.text);
			textView.setCompoundDrawablesWithIntrinsicBounds(item.image, null, null, null);

			return textView;
		}

		private float toPixel(Resources res, int dip) {
			return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, res.getDisplayMetrics());
		}
	}

	private static final int LIST_PREFERED_HEIGHT = 65;

	private final IconMenuAdapter menuAdapter;

	private final Activity parentActivity;

	private final int dialogId;

	private final IconContextMenuOnClickListener clickHandler;

	private boolean isShowing = false;
	/**
	 * constructor
	 * 
	 * @param parent
	 * @param id
	 * @param listener the on click listener
	 */

	public IDContextMenu(Activity parent, int id, IconContextMenuOnClickListener listener) {
		this.parentActivity = parent;
		this.dialogId = id;
		this.clickHandler = listener;
		this.menuAdapter = new IconMenuAdapter(parentActivity);
	}

	public void addItem(Resources res, String string, int imageResourceId, String actionTag) {
		menuAdapter.addItem(new IconContextMenuItem(res, string, imageResourceId, actionTag));
	}

	private void cleanup() {
		parentActivity.dismissDialog(dialogId);
		isShowing = false;
	}

	/**
	 * Create menu
	 * 
	 * @return
	 */
	public Dialog createMenu(String menuItitle) {
		AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
		builder.setTitle(menuItitle);
		builder.setAdapter(menuAdapter, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialoginterface, int i) {
				if (clickHandler != null)
					clickHandler.onClick(((IconContextMenuItem) menuAdapter.getItem(i)).actionTag);
				isShowing = false;
			}
		});

		builder.setInverseBackgroundForced(true);

		AlertDialog dialog = builder.create();
		dialog.setOnCancelListener(this);
		dialog.setOnDismissListener(this);
		isShowing = true;
		return dialog;
	}
	
	public boolean isShowing(){
		return isShowing;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		cleanup();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {}
}