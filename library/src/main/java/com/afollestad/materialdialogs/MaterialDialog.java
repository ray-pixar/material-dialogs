package com.afollestad.materialdialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.ArrayRes;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.internal.MDButton;
import com.afollestad.materialdialogs.internal.MDRootLayout;
import com.afollestad.materialdialogs.util.DialogUtils;
import com.afollestad.materialdialogs.util.TypefaceHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MaterialDialog extends DialogBase implements
        View.OnClickListener, AdapterView.OnItemClickListener {

    protected final MDRootLayout view;
    protected final Builder mBuilder;
    protected ListView listView;
    protected ImageView icon;
    protected TextView title;
    protected View titleFrame;
    protected FrameLayout customViewFrame;
    protected ProgressBar mProgress;
    protected TextView mProgressLabel;
    protected TextView mProgressMinMax;
    protected TextView content;
    protected EditText input;

    protected MDButton positiveButton;
    protected MDButton neutralButton;
    protected MDButton negativeButton;
    protected ListType listType;
    protected List<Integer> selectedIndicesList;

    public final Builder getBuilder() {
        return mBuilder;
            }

    @SuppressLint("InflateParams")
    protected MaterialDialog(Builder builder) {
        super(builder.context, DialogInit.getTheme(builder));
        mBuilder = builder;
        final LayoutInflater inflater = LayoutInflater.from(builder.context);
        view = (MDRootLayout) inflater.inflate(DialogInit.getInflateLayout(builder), null);
        DialogInit.init(this);
/*
        if (!mBuilder.useCustomFonts) {
            if (mBuilder.mediumFont == null)
                mBuilder.mediumFont = TypefaceHelper.get(getContext(), "NotoNaskhArabic-Bold");
            if (mBuilder.regularFont == null)
//                mBuilder.regularFont = TypefaceHelper.get(getContext(), "Roboto-Regular");
                mBuilder.regularFont = TypefaceHelper.get(getContext(), "NotoNaskhArabic-Regular");
        }
*/
    public final void setTypeface(TextView target, Typeface t) {
        if (t == null) return;
        int flags = target.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG;
        target.setPaintFlags(flags);
        target.setTypeface(t);
                }

    protected final void checkIfListInitScroll() {
        if (listView == null)
            return;
        listView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    //noinspection deprecation
                    listView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
        } else {
                    listView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }

                if (listType == ListType.SINGLE || listType == ListType.MULTI) {
                    int selectedIndex;
                    if (listType == ListType.SINGLE) {
                        if (mBuilder.selectedIndex < 0)
                            return;
                        selectedIndex = mBuilder.selectedIndex;
        } else {
                        if (mBuilder.selectedIndices == null || mBuilder.selectedIndices.length == 0)
                            return;
                        List<Integer> indicesList = Arrays.asList(mBuilder.selectedIndices);
                        Collections.sort(indicesList);
                        selectedIndex = indicesList.get(0);
        }
                    if (listView.getLastVisiblePosition() < selectedIndex) {
                        final int totalVisible = listView.getLastVisiblePosition() - listView.getFirstVisiblePosition();
                        // Scroll so that the selected index appears in the middle (vertically) of the ListView
                        int scrollIndex = selectedIndex - (totalVisible / 2);
                        if (scrollIndex < 0) scrollIndex = 0;
                        final int fScrollIndex = scrollIndex;
                        listView.post(new Runnable() {
                    @Override
                            public void run() {
                                listView.requestFocus();
                                listView.setSelection(fScrollIndex);
                        }
                });
        }
    }
        }
        });
    }

    /**
     * Sets the dialog ListView's adapter and it's item click listener.
     */
    protected final void invalidateList() {
        if ((mBuilder.items == null || mBuilder.items.length == 0) && mBuilder.adapter == null)
            return;
        // Set up list with adapter
        listView.setAdapter(mBuilder.adapter);
        if (listType != null || mBuilder.listCallbackCustom != null)
            listView.setOnItemClickListener(this);
    }

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mBuilder.listCallbackCustom != null) {
            // Custom adapter
            CharSequence text = null;
            if (view instanceof TextView)
                text = ((TextView) view).getText();
            mBuilder.listCallbackCustom.onSelection(this, view, position, text);
        } else if (listType == null || listType == ListType.REGULAR) {
            // Default adapter, non choice mode
            if (mBuilder.autoDismiss) {
                // If auto dismiss is enabled, dismiss the dialog when a list item is selected
                dismiss();
            }
            mBuilder.listCallback.onSelection(this, view, position, mBuilder.items[position]);
        } else {
            // Default adapter, choice mode
                    if (listType == ListType.MULTI) {
                final boolean shouldBeChecked = !selectedIndicesList.contains(Integer.valueOf(position));
                final CheckBox cb = (CheckBox) ((LinearLayout) view).getChildAt(0);
                if (shouldBeChecked) {
                    // Add the selection to the states first so the callback includes it (when alwaysCallMultiChoiceCallback)
                                selectedIndicesList.add(position);
                    if (mBuilder.alwaysCallMultiChoiceCallback) {
                        // If the checkbox wasn't previously selected, and the callback returns true, add it to the states and check it
                        if (sendMultichoiceCallback()) {
                            cb.setChecked(true);
                        } else {
                            // The callback cancelled selection, remove it from the states
                            selectedIndicesList.remove(Integer.valueOf(position));
                            }
                    } else {
                        // The callback was not used to check if selection is allowed, just select it
                        cb.setChecked(true);
                    }
                } else {
                    // The checkbox was unchecked
                            selectedIndicesList.remove(Integer.valueOf(position));
                    cb.setChecked(false);
                    if (mBuilder.alwaysCallMultiChoiceCallback)
                        sendMultichoiceCallback();
                        }
                    } else if (listType == ListType.SINGLE) {
                boolean allowSelection = true;
                if (mBuilder.autoDismiss && mBuilder.positiveText == null) {
                    // If auto dismiss is enabled, and no action button is visible to approve the selection, dismiss the dialog
                    dismiss();
                    // Don't allow the selection to be updated since the dialog is being dismissed anyways
                    allowSelection = false;
                    // Update selected index and send callback
                            mBuilder.selectedIndex = position;
                    sendSingleChoiceCallback(view);
                } else if (mBuilder.alwaysCallSingleChoiceCallback) {
                    int oldSelected = mBuilder.selectedIndex;
                    // Temporarily set the new index so the callback uses the right one
                    mBuilder.selectedIndex = position;
                    // Only allow the radio button to be checked if the callback returns true
                    allowSelection = sendSingleChoiceCallback(view);
                    // Restore the old selected index, so the state is updated below
                    mBuilder.selectedIndex = oldSelected;
                }
                // Update the checked states
                if (allowSelection && mBuilder.selectedIndex != position) {
                    mBuilder.selectedIndex = position;
                            ((MaterialDialogAdapter) mBuilder.adapter).notifyDataSetChanged();
                        }
                    }

                }
        }

    public static class NotImplementedException extends Error {
        public NotImplementedException(@SuppressWarnings("SameParameterValue") String message) {
            super(message);
        }
    }

    public static class DialogException extends WindowManager.BadTokenException {
        public DialogException(@SuppressWarnings("SameParameterValue") String message) {
            super(message);
        }
    }

    protected final Drawable getListSelector() {
        if (mBuilder.listSelector != 0)
            return ResourcesCompat.getDrawable(mBuilder.context.getResources(), mBuilder.listSelector, null);
        final Drawable d = DialogUtils.resolveDrawable(mBuilder.context, R.attr.md_list_selector);
        if (d != null) return d;
        return DialogUtils.resolveDrawable(getContext(), R.attr.md_list_selector);
    }

    /* package */ Drawable getButtonSelector(DialogAction which, boolean isStacked) {
        if (isStacked) {
            if (mBuilder.btnSelectorStacked != 0)
                return ResourcesCompat.getDrawable(mBuilder.context.getResources(), mBuilder.btnSelectorStacked, null);
            final Drawable d = DialogUtils.resolveDrawable(mBuilder.context, R.attr.md_btn_stacked_selector);
            if (d != null) return d;
            return DialogUtils.resolveDrawable(getContext(), R.attr.md_btn_stacked_selector);
        } else {
            switch (which) {
                default: {
                    if (mBuilder.btnSelectorPositive != 0)
                        return ResourcesCompat.getDrawable(mBuilder.context.getResources(), mBuilder.btnSelectorPositive, null);
                    final Drawable d = DialogUtils.resolveDrawable(mBuilder.context, R.attr.md_btn_positive_selector);
                    if (d != null) return d;
                    return DialogUtils.resolveDrawable(getContext(), R.attr.md_btn_positive_selector);
                }
                case NEUTRAL: {
                    if (mBuilder.btnSelectorNeutral != 0)
                        return ResourcesCompat.getDrawable(mBuilder.context.getResources(), mBuilder.btnSelectorNeutral, null);
                    final Drawable d = DialogUtils.resolveDrawable(mBuilder.context, R.attr.md_btn_neutral_selector);
                    if (d != null) return d;
                    return DialogUtils.resolveDrawable(getContext(), R.attr.md_btn_neutral_selector);
                }
                case NEGATIVE: {
                    if (mBuilder.btnSelectorNegative != 0)
                        return ResourcesCompat.getDrawable(mBuilder.context.getResources(), mBuilder.btnSelectorNegative, null);
                    final Drawable d = DialogUtils.resolveDrawable(mBuilder.context, R.attr.md_btn_negative_selector);
                    if (d != null) return d;
                    return DialogUtils.resolveDrawable(getContext(), R.attr.md_btn_negative_selector);
                }
            }
        }
    }

    private boolean sendSingleChoiceCallback(View v) {
        CharSequence text = null;
        if (mBuilder.selectedIndex >= 0) {
            text = mBuilder.items[mBuilder.selectedIndex];
        }
        return mBuilder.listCallbackSingleChoice.onSelection(this, v, mBuilder.selectedIndex, text);
    }

    private boolean sendMultichoiceCallback() {
        Collections.sort(selectedIndicesList); // make sure the indicies are in order
        List<CharSequence> selectedTitles = new ArrayList<>();
        for (Integer i : selectedIndicesList) {
            selectedTitles.add(mBuilder.items[i]);
        }
        return mBuilder.listCallbackMultiChoice.onSelection(this,
                selectedIndicesList.toArray(new Integer[selectedIndicesList.size()]),
                selectedTitles.toArray(new CharSequence[selectedTitles.size()]));
    }

    @Override
    public final void onClick(View v) {
        DialogAction tag = (DialogAction) v.getTag();
        switch (tag) {
            case POSITIVE: {
                if (mBuilder.callback != null)
                    mBuilder.callback.onPositive(this);
                if (mBuilder.listCallbackSingleChoice != null)
                    sendSingleChoiceCallback(v);
                if (mBuilder.listCallbackMultiChoice != null)
                    sendMultichoiceCallback();
                if (mBuilder.inputCallback != null && input != null && !mBuilder.alwaysCallInputCallback)
                    mBuilder.inputCallback.onInput(this, input.getText());
                if (mBuilder.autoDismiss) dismiss();
                break;
            }
            case NEGATIVE: {
                if (mBuilder.callback != null)
                    mBuilder.callback.onNegative(this);
                if (mBuilder.autoDismiss) dismiss();
                break;
            }
            case NEUTRAL: {
                if (mBuilder.callback != null)
                    mBuilder.callback.onNeutral(this);
                if (mBuilder.autoDismiss) dismiss();
                break;
            }
                    }
                    }

    /**
     * The class used to construct a MaterialDialog.
     */
    public static class Builder {

        protected final Context context;
        protected CharSequence title;
        protected GravityEnum titleGravity = GravityEnum.START;
        protected GravityEnum contentGravity = GravityEnum.START;
        protected GravityEnum btnStackedGravity = GravityEnum.END;
        protected GravityEnum itemsGravity = GravityEnum.START;
        protected GravityEnum buttonsGravity = GravityEnum.START;
        protected int titleColor = -1;
        protected int contentColor = -1;
        protected CharSequence content;
        protected CharSequence[] items;
        protected CharSequence positiveText;
        protected CharSequence neutralText;
        protected CharSequence negativeText;
        protected View customView;
        protected int widgetColor;
        protected int positiveColor;
        protected int negativeColor;
        protected int neutralColor;
        protected ButtonCallback callback;
        protected ListCallback listCallback;
        protected ListCallbackSingleChoice listCallbackSingleChoice;
        protected ListCallbackMultiChoice listCallbackMultiChoice;
        protected ListCallback listCallbackCustom;
        protected boolean alwaysCallMultiChoiceCallback = false;
        protected boolean alwaysCallSingleChoiceCallback = false;
        protected Theme theme = Theme.LIGHT;
        protected boolean cancelable = true;
        protected float contentLineSpacingMultiplier = 1.2f;
        protected int selectedIndex = -1;
        protected Integer[] selectedIndices = null;
        protected boolean autoDismiss = true;
        protected Typeface regularFont;
        protected Typeface mediumFont;
        protected boolean useCustomFonts;
        protected Drawable icon;
        protected boolean limitIconToDefaultSize;
        protected int maxIconSize = -1;
        protected ListAdapter adapter;
        protected OnDismissListener dismissListener;
        protected OnCancelListener cancelListener;
        protected OnKeyListener keyListener;
        protected OnShowListener showListener;
        protected boolean forceStacking;
        protected boolean wrapCustomViewInScroll;
        protected int dividerColor;
        protected int backgroundColor;
        protected int itemColor;
        protected boolean indeterminateProgress;
        protected boolean showMinMax;
        protected int progress = -2;
        protected int progressMax = 0;
        protected CharSequence inputPrefill;
        protected CharSequence inputHint;
        protected InputCallback inputCallback;
        protected boolean inputAllowEmpty;
        protected int inputType = -1;
        protected boolean alwaysCallInputCallback;

        protected boolean titleColorSet = false;
        protected boolean contentColorSet = false;
        protected boolean itemColorSet = false;

        @DrawableRes
        protected int listSelector;
        @DrawableRes
        protected int btnSelectorStacked;
        @DrawableRes
        protected int btnSelectorPositive;
        @DrawableRes
        protected int btnSelectorNeutral;
        @DrawableRes
        protected int btnSelectorNegative;

        public final Context getContext() {
            return context;
        }

        public final GravityEnum getItemsGravity() {
            return itemsGravity;
        }

        public final int getTitleColor() {
            return titleColor;
        }

        public final Typeface getRegularFont() {
            return regularFont;
        }

        public Builder(@NonNull Context context) {
            this.context = context;
            final int materialBlue = context.getResources().getColor(R.color.md_material_blue_600);

            // Retrieve default accent colors, which are used on the action buttons and progress bars
            this.widgetColor = DialogUtils.resolveColor(context, R.attr.colorAccent, materialBlue);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.widgetColor = DialogUtils.resolveColor(context, android.R.attr.colorAccent, this.widgetColor);
                }

            this.positiveColor = this.widgetColor;
            this.negativeColor = this.widgetColor;
            this.neutralColor = this.widgetColor;

            // Set the default theme based on the Activity theme's primary color darkness (more white or more black)
            final int primaryTextColor = DialogUtils.resolveColor(context, android.R.attr.textColorPrimary);
            this.theme = DialogUtils.isColorDark(primaryTextColor) ? Theme.LIGHT : Theme.DARK;

            // Load theme values from the ThemeSingleton if needed
            checkSingleton();

            // Retrieve gravity settings from global theme attributes if needed
            this.titleGravity = DialogUtils.resolveGravityEnum(context, R.attr.md_title_gravity, this.titleGravity);
            this.contentGravity = DialogUtils.resolveGravityEnum(context, R.attr.md_content_gravity, this.contentGravity);
            this.btnStackedGravity = DialogUtils.resolveGravityEnum(context, R.attr.md_btnstacked_gravity, this.btnStackedGravity);
            this.itemsGravity = DialogUtils.resolveGravityEnum(context, R.attr.md_items_gravity, this.itemsGravity);
            this.buttonsGravity = DialogUtils.resolveGravityEnum(context, R.attr.md_buttons_gravity, this.buttonsGravity);
        }

        private void checkSingleton() {
            if (ThemeSingleton.get(false) == null) return;
            ThemeSingleton s = ThemeSingleton.get();
            if (s.darkTheme)
                this.theme = Theme.DARK;
            if (s.titleColor != 0)
                this.titleColor = s.titleColor;
            if (s.contentColor != 0)
                this.contentColor = s.contentColor;
            if (s.positiveColor != 0)
                this.positiveColor = s.positiveColor;
            if (s.neutralColor != 0)
                this.neutralColor = s.neutralColor;
            if (s.negativeColor != 0)
                this.negativeColor = s.negativeColor;
            if (s.itemColor != 0)
                this.itemColor = s.itemColor;
            if (s.icon != null)
                this.icon = s.icon;
            if (s.backgroundColor != 0)
                this.backgroundColor = s.backgroundColor;
            if (s.dividerColor != 0)
                this.dividerColor = s.dividerColor;
            if (s.btnSelectorStacked != 0)
                this.btnSelectorStacked = s.btnSelectorStacked;
            if (s.listSelector != 0)
                this.listSelector = s.listSelector;
            if (s.btnSelectorPositive != 0)
                this.btnSelectorPositive = s.btnSelectorPositive;
            if (s.btnSelectorNeutral != 0)
                this.btnSelectorNeutral = s.btnSelectorNeutral;
            if (s.btnSelectorNegative != 0)
                this.btnSelectorNegative = s.btnSelectorNegative;
            if (s.widgetColor != 0)
                this.widgetColor = s.widgetColor;
            this.titleGravity = s.titleGravity;
            this.contentGravity = s.contentGravity;
            this.btnStackedGravity = s.btnStackedGravity;
            this.itemsGravity = s.itemsGravity;
            this.buttonsGravity = s.buttonsGravity;
        }

        public Builder title(@StringRes int titleRes) {
            title(this.context.getText(titleRes));
            return this;
        }

        public Builder title(@NonNull CharSequence title) {
            this.title = title;
            return this;
        }

        public Builder titleGravity(@NonNull GravityEnum gravity) {
            this.titleGravity = gravity;
            return this;
        }

        public Builder titleColor(int color) {
            this.titleColor = color;
            this.titleColorSet = true;
            return this;
        }

        public Builder titleColorRes(@ColorRes int colorRes) {
            titleColor(this.context.getResources().getColor(colorRes));
            return this;
        }

        public Builder titleColorAttr(@AttrRes int colorAttr) {
            titleColor(DialogUtils.resolveColor(this.context, colorAttr));
            return this;
        }

        /**
         * Disable usage of the default fonts. This is automatically set by
         * {@link #typeface(String, String)} and {@link #typeface(Typeface, Typeface)}.
         *
         * @return The Builder instance so you can chain calls to it.
         */
        public Builder disableDefaultFonts() {
            this.useCustomFonts = true;
            return this;
        }

        /**
         * Sets the fonts used in the dialog. It's recommended that you use {@link #typeface(String, String)} instead,
         * to avoid duplicate Typeface allocations and high memory usage.
         *
         * @param medium  The font used on titles and action buttons. Null uses device default.
         * @param regular The font used everywhere else, like on the content and list items. Null uses device default.
         * @return The Builder instance so you can chain calls to it.
         */
        public Builder typeface(Typeface medium, Typeface regular) {
            this.mediumFont = medium;
            this.regularFont = regular;
            this.useCustomFonts = true;
            return this;
        }

        /**
         * Sets the fonts used in the dialog, by file names. This also uses TypefaceHelper in order
         * to avoid any un-needed allocations (it recycles typefaces for you).
         *
         * @param medium  The name of font in assets/fonts, minus the extension (null uses device default). E.g. [your-project]/app/main/assets/fonts/[medium].ttf
         * @param regular The name of font in assets/fonts, minus the extension (null uses device default). E.g. [your-project]/app/main/assets/fonts/[regular].ttf
         * @return The Builder instance so you can chain calls to it.
         */
        public Builder typeface(String medium, String regular) {
            if (medium != null) {
                this.mediumFont = TypefaceHelper.get(this.context, medium);
                if (this.mediumFont == null)
                    throw new RuntimeException("No font asset found for " + medium);
            }
            if (regular != null) {
                this.regularFont = TypefaceHelper.get(this.context, regular);
                if (this.regularFont == null)
                    throw new RuntimeException("No font asset found for " + regular);
            }
            this.useCustomFonts = this.mediumFont != null || this.regularFont != null;
            return this;
        }

        public Builder icon(@NonNull Drawable icon) {
            this.icon = icon;
            return this;
        }

        public Builder iconRes(@DrawableRes int icon) {
            this.icon = ResourcesCompat.getDrawable(context.getResources(), icon, null);
            return this;
        }

        public Builder iconAttr(@AttrRes int iconAttr) {
            this.icon = DialogUtils.resolveDrawable(context, iconAttr);
            return this;
        }

        public Builder content(@StringRes int contentRes) {
            content(this.context.getText(contentRes));
            return this;
        }

        public Builder content(@NonNull CharSequence content) {
            this.content = content;
            return this;
        }

        public Builder content(@StringRes int contentRes, Object... formatArgs) {
            content(this.context.getString(contentRes, formatArgs));
            return this;
        }

        public Builder contentColor(int color) {
            this.contentColor = color;
            this.contentColorSet = true;
            return this;
        }

        public Builder contentColorRes(@ColorRes int colorRes) {
            contentColor(this.context.getResources().getColor(colorRes));
            return this;
        }

        public Builder contentColorAttr(@AttrRes int colorAttr) {
            contentColor(DialogUtils.resolveColor(this.context, colorAttr));
            return this;
        }

        public Builder contentGravity(@NonNull GravityEnum gravity) {
            this.contentGravity = gravity;
            return this;
        }

        public Builder contentLineSpacing(float multiplier) {
            this.contentLineSpacingMultiplier = multiplier;
            return this;
        }

        public Builder items(@ArrayRes int itemsRes) {
            items(this.context.getResources().getTextArray(itemsRes));
            return this;
        }

        public Builder items(@NonNull CharSequence[] items) {
            this.items = items;
            return this;
        }

        public Builder itemsCallback(@NonNull ListCallback callback) {
            this.listCallback = callback;
            this.listCallbackSingleChoice = null;
            this.listCallbackMultiChoice = null;
            return this;
        }

        public Builder itemColor(int color) {
            this.itemColor = color;
            this.itemColorSet = true;
            return this;
        }

        public Builder itemColorRes(@ColorRes int colorRes) {
            return itemColor(this.context.getResources().getColor(colorRes));
        }

        public Builder itemColorAttr(@AttrRes int colorAttr) {
            return itemColor(DialogUtils.resolveColor(this.context, colorAttr));
        }

        public Builder itemsGravity(@NonNull GravityEnum gravity) {
            this.itemsGravity = gravity;
            return this;
        }

        public Builder buttonsGravity(@NonNull GravityEnum gravity) {
            this.buttonsGravity = gravity;
            return this;
        }

        /**
         * Pass anything below 0 (such as -1) for the selected index to leave all options unselected initially.
         * Otherwise pass the index of an item that will be selected initially.
         *
         * @param selectedIndex The checkbox index that will be selected initially.
         * @param callback      The callback that will be called when the presses the positive button.
         * @return The Builder instance so you can chain calls to it.
         */
        public Builder itemsCallbackSingleChoice(int selectedIndex, @NonNull ListCallbackSingleChoice callback) {
            this.selectedIndex = selectedIndex;
            this.listCallback = null;
            this.listCallbackSingleChoice = callback;
            this.listCallbackMultiChoice = null;
            return this;
        }

        /**
         * By default, the single choice callback is only called when the user clicks the positive button
         * or if there are no buttons. Call this to force it to always call on item clicks even if the
         * positive button exists.
         *
         * @return The Builder instance so you can chain calls to it.
         */
        public Builder alwaysCallSingleChoiceCallback() {
            this.alwaysCallSingleChoiceCallback = true;
            return this;
        }

        /**
         * Pass null for the selected indices to leave all options unselected initially. Otherwise pass
         * an array of indices that will be selected initially.
         *
         * @param selectedIndices The radio button indices that will be selected initially.
         * @param callback        The callback that will be called when the presses the positive button.
         * @return The Builder instance so you can chain calls to it.
         */
        public Builder itemsCallbackMultiChoice(Integer[] selectedIndices, @NonNull ListCallbackMultiChoice callback) {
            this.selectedIndices = selectedIndices;
            this.listCallback = null;
            this.listCallbackSingleChoice = null;
            this.listCallbackMultiChoice = callback;
            return this;
        }

        /**
         * By default, the multi choice callback is only called when the user clicks the positive button
         * or if there are no buttons. Call this to force it to always call on item clicks even if the
         * positive button exists.
         *
         * @return The Builder instance so you can chain calls to it.
         */
        public Builder alwaysCallMultiChoiceCallback() {
            this.alwaysCallMultiChoiceCallback = true;
            return this;
        }

        public Builder positiveText(@StringRes int postiveRes) {
            positiveText(this.context.getText(postiveRes));
            return this;
        }

        public Builder positiveText(@NonNull CharSequence message) {
            this.positiveText = message;
            return this;
        }

        public Builder positiveColor(int color) {
            this.positiveColor = color;
            return this;
        }

        public Builder positiveColorRes(@ColorRes int colorRes) {
            return positiveColor(this.context.getResources().getColor(colorRes));
        }

        public Builder positiveColorAttr(@AttrRes int colorAttr) {
            return positiveColor(DialogUtils.resolveColor(this.context, colorAttr));
        }

        public Builder neutralText(@StringRes int neutralRes) {
            return neutralText(this.context.getText(neutralRes));
        }

        public Builder neutralText(@NonNull CharSequence message) {
            this.neutralText = message;
            return this;
        }

        public Builder negativeColor(int color) {
            this.negativeColor = color;
            return this;
        }

        public Builder negativeColorRes(@ColorRes int colorRes) {
            return negativeColor(this.context.getResources().getColor(colorRes));
        }

        public Builder negativeColorAttr(@AttrRes int colorAttr) {
            return negativeColor(DialogUtils.resolveColor(this.context, colorAttr));
        }

        public Builder negativeText(@StringRes int negativeRes) {
            return negativeText(this.context.getText(negativeRes));
        }

        public Builder negativeText(@NonNull CharSequence message) {
            this.negativeText = message;
            return this;
        }

        public Builder neutralColor(int color) {
            this.neutralColor = color;
            return this;
        }

        public Builder neutralColorRes(@ColorRes int colorRes) {
            return neutralColor(this.context.getResources().getColor(colorRes));
        }

        public Builder neutralColorAttr(@AttrRes int colorAttr) {
            return neutralColor(DialogUtils.resolveColor(this.context, colorAttr));
        }

        public Builder listSelector(@DrawableRes int selectorRes) {
            this.listSelector = selectorRes;
            return this;
        }

        public Builder btnSelectorStacked(@DrawableRes int selectorRes) {
            this.btnSelectorStacked = selectorRes;
            return this;
        }

        public Builder btnSelector(@DrawableRes int selectorRes) {
            this.btnSelectorPositive = selectorRes;
            this.btnSelectorNeutral = selectorRes;
            this.btnSelectorNegative = selectorRes;
            return this;
        }

        public Builder btnSelector(@DrawableRes int selectorRes, @NonNull DialogAction which) {
            switch (which) {
                default:
                    this.btnSelectorPositive = selectorRes;
                    break;
                case NEUTRAL:
                    this.btnSelectorNeutral = selectorRes;
                    break;
                case NEGATIVE:
                    this.btnSelectorNegative = selectorRes;
                    break;
            }
            return this;
        }

        /**
         * Sets the gravity used for the text in stacked action buttons. By default, it's #{@link GravityEnum#END}.
         *
         * @param gravity The gravity to use.
         * @return The Builder instance so calls can be chained.
         */
        public Builder btnStackedGravity(@NonNull GravityEnum gravity) {
            this.btnStackedGravity = gravity;
            return this;
        }

        public Builder customView(@LayoutRes int layoutRes, boolean wrapInScrollView) {
            LayoutInflater li = LayoutInflater.from(this.context);
            return customView(li.inflate(layoutRes, null), wrapInScrollView);
        }

        public Builder customView(@NonNull View view, boolean wrapInScrollView) {
            this.customView = view;
            this.wrapCustomViewInScroll = wrapInScrollView;
            return this;
        }

        /**
         * Makes this dialog a progress dialog.
         *
         * @param indeterminate If true, an infinite circular spinner is shown. If false, a horizontal progress bar is shown that is incremented or set via the built MaterialDialog instance.
         * @param max           When indeterminate is false, the max value the horizontal progress bar can get to.
         * @return An instance of the Builder so calls can be chained.
         */
        public Builder progress(boolean indeterminate, int max) {
            if (indeterminate) {
                this.indeterminateProgress = true;
                this.progress = -2;
            } else {
                this.indeterminateProgress = false;
                this.progress = -1;
                this.progressMax = max;
            }
            return this;
        }

        /**
         * Makes this dialog a progress dialog.
         *
         * @param indeterminate If true, an infinite circular spinner is shown. If false, a horizontal progress bar is shown that is incremented or set via the built MaterialDialog instance.
         * @param max           When indeterminate is false, the max value the horizontal progress bar can get to.
         * @param showMinMax    For determinate dialogs, the min and max will be displayed to the left (start) of the progress bar, e.g. 50/100.
         * @return An instance of the Builder so calls can be chained.
         */
        public Builder progress(boolean indeterminate, int max, boolean showMinMax) {
            this.showMinMax = showMinMax;
            return progress(indeterminate, max);
        }

        public Builder widgetColor(int color) {
            this.widgetColor = color;
            return this;
        }

        public Builder widgetColorRes(@ColorRes int colorRes) {
            return widgetColor(this.context.getResources().getColor(colorRes));
        }

        public Builder widgetColorAttr(@AttrRes int colorAttr) {
            return widgetColorRes(DialogUtils.resolveColor(this.context, colorAttr));
        }

        public Builder dividerColor(int color) {
            this.dividerColor = color;
            return this;
        }

        public Builder dividerColorRes(@ColorRes int colorRes) {
            return dividerColor(this.context.getResources().getColor(colorRes));
        }

        public Builder dividerColorAttr(@AttrRes int colorAttr) {
            return dividerColor(DialogUtils.resolveColor(this.context, colorAttr));
        }

        public Builder backgroundColor(int color) {
            this.backgroundColor = color;
            return this;
        }

        public Builder backgroundColorRes(@ColorRes int colorRes) {
            return backgroundColor(this.context.getResources().getColor(colorRes));
        }

        public Builder backgroundColorAttr(@AttrRes int colorAttr) {
            return backgroundColor(DialogUtils.resolveColor(this.context, colorAttr));
        }

        public Builder callback(@NonNull ButtonCallback callback) {
            this.callback = callback;
            return this;
        }

        public Builder theme(@NonNull Theme theme) {
            this.theme = theme;
            return this;
        }

        public Builder cancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        /**
         * This defaults to true. If set to false, the dialog will not automatically be dismissed
         * when an action button is pressed, and not automatically dismissed when the user selects
         * a list item.
         *
         * @param dismiss Whether or not to dismiss the dialog automatically.
         * @return The Builder instance so you can chain calls to it.
         */
        public Builder autoDismiss(boolean dismiss) {
            this.autoDismiss = dismiss;
            return this;
        }

        /**
         * Sets a custom {@link android.widget.ListAdapter} for the dialog's list
         *
         * @param adapter  The adapter to set to the list.
         * @param callback The callback invoked when an item in the list is selected.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder adapter(@NonNull ListAdapter adapter, ListCallback callback) {
            this.adapter = adapter;
            this.listCallbackCustom = callback;
            return this;
        }

        /**
         * Limits the display size of a set icon to 48dp.
         */
        public Builder limitIconToDefaultSize() {
            this.limitIconToDefaultSize = true;
            return this;
        }

        public Builder maxIconSize(int maxIconSize) {
            this.maxIconSize = maxIconSize;
            return this;
        }

        public Builder maxIconSizeRes(@DimenRes int maxIconSizeRes) {
            return maxIconSize((int) this.context.getResources().getDimension(maxIconSizeRes));
        }

        public Builder showListener(@NonNull OnShowListener listener) {
            this.showListener = listener;
            return this;
        }

        public Builder dismissListener(@NonNull OnDismissListener listener) {
            this.dismissListener = listener;
            return this;
        }

        public Builder cancelListener(@NonNull OnCancelListener listener) {
            this.cancelListener = listener;
            return this;
        }

        public Builder keyListener(@NonNull OnKeyListener listener) {
            this.keyListener = listener;
            return this;
        }

        public Builder forceStacking(boolean stacked) {
            this.forceStacking = stacked;
            return this;
        }

        public Builder input(CharSequence hint, CharSequence prefill, boolean allowEmptyInput, @NonNull InputCallback callback) {
            this.inputCallback = callback;
            this.inputHint = hint;
            this.inputPrefill = prefill;
            this.inputAllowEmpty = allowEmptyInput;
            return this;
        }

        public Builder input(CharSequence hint, CharSequence prefill, @NonNull InputCallback callback) {
            return input(hint, prefill, true, callback);
        }

        public Builder input(@StringRes int hint, @StringRes int prefill, boolean allowEmptyInput, @NonNull InputCallback callback) {
            return input(hint == 0 ? null : context.getText(hint), prefill == 0 ? null : context.getText(prefill), allowEmptyInput, callback);
        }

        public Builder input(@StringRes int hint, @StringRes int prefill, @NonNull InputCallback callback) {
            return input(hint, prefill, true, callback);
        }

        public Builder inputType(int type) {
            this.inputType = type;
            return this;
        }

        public Builder alwaysCallInputCallback() {
            this.alwaysCallInputCallback = true;
            return this;
        }

        public MaterialDialog build() {
            return new MaterialDialog(this);
        }

        public MaterialDialog show() {
            MaterialDialog dialog = build();
            dialog.show();
            return dialog;
        }
    }

    @Override
    public void show() {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("Dialogs can only be shown from the UI thread.");

        try {
            super.show();
        } catch (WindowManager.BadTokenException e) {
            throw new DialogException("Bad window token, you cannot show a dialog before an Activity is created or after it's hidden.");
        }
    }

    /**
     * Retrieves the view of an action button, allowing you to modify properties such as whether or not it's enabled.
     * Use {@link #setActionButton(DialogAction, int)} to change text, since the view returned here is not
     * the view that displays text.
     *
     * @param which The action button of which to get the view for.
     * @return The view from the dialog's layout representing this action button.
     */
    public final View getActionButton(@NonNull DialogAction which) {
            switch (which) {
                default:
                    return view.findViewById(R.id.buttonDefaultPositive);
                case NEUTRAL:
                    return view.findViewById(R.id.buttonDefaultNeutral);
                case NEGATIVE:
                    return view.findViewById(R.id.buttonDefaultNegative);
            }
        }
/*
    *//**
     * This will not return buttons that are actually in the layout itself, since the layout doesn't
     * contain buttons. This is only implemented to avoid crashing issues on Huawei devices. Huawei's
     * stock OS requires this method in order to detect visible buttons.
     *
     * @deprecated Use getActionButton(com.afollestad.materialdialogs.DialogAction)} instead.
     *//*
    @Deprecated
    @Override
    public Button getButton(int whichButton) {
        Log.w("MaterialDialog", "Warning: getButton() is a deprecated method that does not return valid references to action buttons.");
        if (whichButton == AlertDialog.BUTTON_POSITIVE) {
            return mBuilder.positiveText != null ? new Button(getContext()) : null;
        } else if (whichButton == AlertDialog.BUTTON_NEUTRAL) {
            return mBuilder.neutralText != null ? new Button(getContext()) : null;
        } else {
            return mBuilder.negativeText != null ? new Button(getContext()) : null;
        }
    }*/

    /**
     * Retrieves the view representing the dialog as a whole. Be careful with this.
     */
    public final View getView() {
        return view;
    }

    @Nullable
    public final ListView getListView() {
        return listView;
    }

    @Nullable
    public final EditText getInputEditText() {
        return input;
    }

    /**
     * Retrieves the TextView that contains the dialog title. If you want to update the
     * title, use #{@link #setTitle(CharSequence)} instead.
     */
    public final TextView getTitleView() {
        return title;
    }

    /**
     * Retrieves the TextView that contains the dialog content. If you want to update the
     * content (message), use #{@link #setContent(CharSequence)} instead.
     */
    @Nullable
    public final TextView getContentView() {
        return content;
    }

    /**
     * Retrieves the custom view that was inflated or set to the MaterialDialog during building.
     *
     * @return The custom view that was passed into the Builder.
     */
    @Nullable
    public final View getCustomView() {
        return mBuilder.customView;
    }

    /**
     * Updates an action button's title, causing invalidation to check if the action buttons should be stacked.
     * Setting an action button's text to null is a shortcut for hiding it, too.
     *
     * @param which The action button to update.
     * @param title The new title of the action button.
     */
    public final void setActionButton(@NonNull DialogAction which, CharSequence title) {
        switch (which) {
            default:
                mBuilder.positiveText = title;
                positiveButton.setText(title);
                positiveButton.setVisibility(title == null ? View.GONE : View.VISIBLE);
                break;
            case NEUTRAL:
                mBuilder.neutralText = title;
                neutralButton.setText(title);
                neutralButton.setVisibility(title == null ? View.GONE : View.VISIBLE);
                break;
            case NEGATIVE:
                mBuilder.negativeText = title;
                negativeButton.setText(title);
                negativeButton.setVisibility(title == null ? View.GONE : View.VISIBLE);
                break;
        }
    }

    /**
     * Updates an action button's title, causing invalidation to check if the action buttons should be stacked.
     *
     * @param which    The action button to update.
     * @param titleRes The string resource of the new title of the action button.
     */
    public final void setActionButton(DialogAction which, @StringRes int titleRes) {
        setActionButton(which, getContext().getText(titleRes));
    }

    /**
     * Gets whether or not the positive, neutral, or negative action button is visible.
     *
     * @return Whether or not 1 or more action buttons is visible.
     */
    public final boolean hasActionButtons() {
        return numberOfActionButtons() > 0;
    }

    /**
     * Gets the number of visible action buttons.
     *
     * @return 0 through 3, depending on how many should be or are visible.
     */
    public final int numberOfActionButtons() {
        int number = 0;
        if (mBuilder.positiveText != null && positiveButton.getVisibility() == View.VISIBLE)
            number++;
        if (mBuilder.neutralText != null && neutralButton.getVisibility() == View.VISIBLE)
            number++;
        if (mBuilder.negativeText != null && negativeButton.getVisibility() == View.VISIBLE)
            number++;
        return number;
    }

    /**
     * Updates the dialog's title.
     */
    public final void setTitle(@NonNull CharSequence title) {
        this.title.setText(title);
    }

    public void setIcon(@DrawableRes int resId) {
        icon.setImageResource(resId);
        icon.setVisibility(resId != 0 ? View.VISIBLE : View.GONE);
    }

    public void setIcon(Drawable d) {
        icon.setImageDrawable(d);
        icon.setVisibility(d != null ? View.VISIBLE : View.GONE);
    }

    public void setIconAttribute(@AttrRes int attrId) {
        Drawable d = DialogUtils.resolveDrawable(mBuilder.context, attrId);
        icon.setImageDrawable(d);
        icon.setVisibility(d != null ? View.VISIBLE : View.GONE);
    }

    public final void setContent(CharSequence content) {
        this.content.setText(content);
        this.content.setVisibility(TextUtils.isEmpty(content) ? View.GONE : View.VISIBLE);
    }

    /**
     * @deprecated Use setContent() instead.
     */
    @Deprecated
    public void setMessage(CharSequence message) {
        setContent(message);
    }

    public final void setItems(CharSequence[] items) {
        if (mBuilder.adapter == null)
            throw new IllegalStateException("This MaterialDialog instance does not yet have an adapter set to it. You cannot use setItems().");
        if (mBuilder.adapter instanceof MaterialDialogAdapter) {
            mBuilder.adapter = new MaterialDialogAdapter(this,
                    ListType.getLayoutForType(listType), R.id.title, items);
        } else {
            throw new IllegalStateException("When using a custom adapter, setItems() cannot be used. Set items through the adapter instead.");
        }
        mBuilder.items = items;
        listView.setAdapter(mBuilder.adapter);
    }

    public final int getCurrentProgress() {
        if (mProgress == null) return -1;
        return mProgress.getProgress();
    }

    public final void incrementProgress(int by) {
        if (mBuilder.progress <= -2)
            throw new IllegalStateException("Cannot use incrementProgress() on this dialog.");
        setProgress(getCurrentProgress() + by);
    }

    public final void setProgress(int progress) {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("You can only set the dialog's progress from the UI thread.");
        else if (mBuilder.progress <= -2)
            throw new IllegalStateException("Cannot use setProgress() on this dialog.");
        mProgress.setProgress(progress);
        int percentage = (int) (((float) getCurrentProgress() / (float) getMaxProgress()) * 100f);
        mProgressLabel.setText(percentage + "%");
        if (mProgressMinMax != null)
            mProgressMinMax.setText(getCurrentProgress() + "/" + getMaxProgress());
    }

    public final void setMaxProgress(int max) {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("You can only set the dialog's progress from the UI thread.");
        else if (mBuilder.progress <= -2)
            throw new IllegalStateException("Cannot use setMaxProgress() on this dialog.");
        mProgress.setMax(max);
    }

    public final boolean isIndeterminateProgress() {
        return mBuilder.indeterminateProgress;
    }

    public final int getMaxProgress() {
        if (mProgress == null) return -1;
        return mProgress.getMax();
    }

    public final boolean isCancelled() {
        return !isShowing();
    }

    /**
     * Convenience method for getting the currently selected index of a single choice list.
     *
     * @return Currently selected index of a single choice list, or -1 if not showing a single choice list
     */
    public int getSelectedIndex() {
        if (mBuilder.listCallbackSingleChoice != null) {
            return mBuilder.selectedIndex;
        } else {
            return -1;
        }
    }

    /**
     * Convenience method for getting the currently selected indices of a multi choice list
     *
     * @return Currently selected index of a multi choice list, or null if not showing a multi choice list
     */
    @Nullable
    public Integer[] getSelectedIndices() {
        if (mBuilder.listCallbackMultiChoice != null) {
            return selectedIndicesList.toArray(new Integer[selectedIndicesList.size()]);
        } else {
            return null;
        }
    }

    /**
     * Convenience method for setting the currently selected index of a single choice list.
     * This only works if you are not using a custom adapter; if you're using a custom adapter,
     * an IllegalStateException is thrown. Note that this does not call the respective single choice callback.
     *
     * @param index The index of the list item to check.
     */
    public void setSelectedIndex(int index) {
        mBuilder.selectedIndex = index;
        if (mBuilder.adapter != null && mBuilder.adapter instanceof MaterialDialogAdapter) {
            ((MaterialDialogAdapter) mBuilder.adapter).notifyDataSetChanged();
        } else {
            throw new IllegalStateException("You can only use setSelectedIndex() with the default adapter implementation.");
        }
    }

    /**
     * Convenience method for setting the currently selected indices of a multi choice list.
     * This only works if you are not using a custom adapter; if you're using a custom adapter,
     * an IllegalStateException is thrown. Note that this does not call the respective multi choice callback.
     *
     * @param indices The indices of the list items to check.
     */
    public void setSelectedIndices(@NonNull Integer[] indices) {
        mBuilder.selectedIndices = indices;
        selectedIndicesList = new ArrayList<>(Arrays.asList(indices));
        if (mBuilder.adapter != null && mBuilder.adapter instanceof MaterialDialogAdapter) {
            ((MaterialDialogAdapter) mBuilder.adapter).notifyDataSetChanged();
        } else {
            throw new IllegalStateException("You can only use setSelectedIndices() with the default adapter implementation.");
        }
    }

    @Override
    public final void onShow(DialogInterface dialog) {
        super.onShow(dialog);
        if (input != null) {
            DialogUtils.showKeyboard(this, mBuilder);
            if (input.getText().length() > 0)
                input.setSelection(input.getText().length());
        }
    }

    protected void setInternalInputCallback() {
        if (input == null) return;
        input.addTextChangedListener(new TextWatcher() {
        @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mBuilder.alwaysCallInputCallback)
                    mBuilder.inputCallback.onInput(MaterialDialog.this, s);
                if (!mBuilder.inputAllowEmpty) {
                    final View positiveAb = getActionButton(DialogAction.POSITIVE);
                    positiveAb.setEnabled(s.toString().trim().length() > 0);
        }
            }

        @Override
            public void afterTextChanged(Editable s) {
                }
        });
                }

    @Override
    protected void onStop() {
        super.onStop();
        if (input != null)
            DialogUtils.hideKeyboard(this, mBuilder);
            }

    protected enum ListType {
        REGULAR, SINGLE, MULTI;

        public static int getLayoutForType(ListType type) {
            switch (type) {
                case REGULAR:
                    return R.layout.md_listitem;
                case SINGLE:
                    return R.layout.md_listitem_singlechoice;
                case MULTI:
                    return R.layout.md_listitem_multichoice;
                default:
                    throw new IllegalArgumentException("Not a valid list type");
            }
        }
    }

    /**
     * A callback used for regular list dialogs.
     */
    public interface ListCallback {
        void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text);
    }

    /**
     * A callback used for multi choice (check box) list dialogs.
     */
    public interface ListCallbackSingleChoice {
        /**
         * Return true to allow the radio button to be checked, if the alwaysCallSingleChoice() option is used.
         *
         * @param dialog The dialog of which a list item was selected.
         * @param which  The index of the item that was selected.
         * @param text   The text of the  item that was selected.
         * @return True to allow the radio button to be selected.
         */
        boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text);
    }

    /**
     * A callback used for multi choice (check box) list dialogs.
     */
    public interface ListCallbackMultiChoice {
        /**
         * Return true to allow the check box to be checked, if the alwaysCallSingleChoice() option is used.
         *
         * @param dialog The dialog of which a list item was selected.
         * @param which  The indices of the items that were selected.
         * @param text   The text of the items that were selected.
         * @return True to allow the checkbox to be selected.
         */
        boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text);
    }

    /**
     * Override these as needed, so no needing to sub empty methods from an interface
     */
    public static abstract class ButtonCallback {

        public void onPositive(MaterialDialog dialog) {
        }

        public void onNegative(MaterialDialog dialog) {
        }

        public void onNeutral(MaterialDialog dialog) {
        }

        public ButtonCallback() {
            super();
        }

        @Override
        protected final Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public final boolean equals(Object o) {
            return super.equals(o);
        }

        @Override
        protected final void finalize() throws Throwable {
            super.finalize();
        }

        @Override
        public final int hashCode() {
            return super.hashCode();
        }

        @Override
        public final String toString() {
            return super.toString();
        }
    }

    public interface InputCallback {

        void onInput(MaterialDialog dialog, CharSequence input);
    }
}