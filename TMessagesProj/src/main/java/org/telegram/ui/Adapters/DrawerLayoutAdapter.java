/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DrawerActionCell;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.DrawerAddCell;
import org.telegram.ui.Cells.DrawerUserCell;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.DrawerProfileCell;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SideMenultItemAnimator;

import java.util.ArrayList;
import java.util.Collections;

import androidx.recyclerview.widget.RecyclerView;

import com.exteragram.messenger.ExteraConfig;

public class DrawerLayoutAdapter extends RecyclerListView.SelectionAdapter {

    private Context mContext;
    private ArrayList<Item> items = new ArrayList<>(11);
    private ArrayList<Integer> accountNumbers = new ArrayList<>();
    private boolean accountsShown;
    private DrawerProfileCell profileCell;
    private SideMenultItemAnimator itemAnimator;
    private boolean hasGps;

    public DrawerLayoutAdapter(Context context, SideMenultItemAnimator animator) {
        mContext = context;
        itemAnimator = animator;
        accountsShown = UserConfig.getActivatedAccountsCount() > 1 && MessagesController.getGlobalMainSettings().getBoolean("accountsShown", true);
        Theme.createCommonDialogResources(context);
        resetItems();
        try {
            hasGps = ApplicationLoader.applicationContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        } catch (Throwable e) {
            hasGps = false;
        }
    }

    private int getAccountRowsCount() {
        int count = accountNumbers.size() + 1;
        if (accountNumbers.size() < UserConfig.MAX_ACCOUNT_COUNT) {
            count++;
        }
        return count;
    }

    @Override
    public int getItemCount() {
        int count = items.size() + 2;
        if (accountsShown) {
            count += getAccountRowsCount();
        }
        return count;
    }

    public void setAccountsShown(boolean value, boolean animated) {
        if (accountsShown == value || itemAnimator.isRunning()) {
            return;
        }
        accountsShown = value;
        if (profileCell != null) {
            profileCell.setAccountsShown(accountsShown, animated);
        }
        MessagesController.getGlobalMainSettings().edit().putBoolean("accountsShown", accountsShown).commit();
        if (animated) {
            itemAnimator.setShouldClipChildren(false);
            if (accountsShown) {
                notifyItemRangeInserted(2, getAccountRowsCount());
            } else {
                notifyItemRangeRemoved(2, getAccountRowsCount());
            }
        } else {
            notifyDataSetChanged();
        }
    }

    public boolean isAccountsShown() {
        return accountsShown;
    }

    @Override
    public void notifyDataSetChanged() {
        resetItems();
        super.notifyDataSetChanged();
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        int itemType = holder.getItemViewType();
        return itemType == 3 || itemType == 4 || itemType == 5 || itemType == 6;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case 0:
                view = profileCell = new DrawerProfileCell(mContext);
                break;
            case 2:
                view = new DividerCell(mContext);
                break;
            case 3:
                view = new DrawerActionCell(mContext);
                break;
            case 4:
                view = new DrawerUserCell(mContext);
                break;
            case 5:
                view = new DrawerAddCell(mContext);
                break;
            case 1:
            default:
                view = new EmptyCell(mContext, AndroidUtilities.dp(8));
                break;
        }
        view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case 0: {
                DrawerProfileCell profileCell = (DrawerProfileCell) holder.itemView;
                profileCell.setUser(MessagesController.getInstance(UserConfig.selectedAccount).getUser(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId()), accountsShown);
                break;
            }
            case 3: {
                DrawerActionCell drawerActionCell = (DrawerActionCell) holder.itemView;
                position -= 2;
                if (accountsShown) {
                    position -= getAccountRowsCount();
                }
                items.get(position).bind(drawerActionCell);
                drawerActionCell.setPadding(0, 0, 0, 0);
                break;
            }
            case 4: {
                DrawerUserCell drawerUserCell = (DrawerUserCell) holder.itemView;
                drawerUserCell.setAccount(accountNumbers.get(position - 2));
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int i) {
        if (i == 0) {
            return 0;
        } else if (i == 1) {
            return 1;
        }
        i -= 2;
        if (accountsShown) {
            if (i < accountNumbers.size()) {
                return 4;
            } else {
                if (accountNumbers.size() < UserConfig.MAX_ACCOUNT_COUNT) {
                    if (i == accountNumbers.size()){
                        return 5;
                    } else if (i == accountNumbers.size() + 1) {
                        return 2;
                    }
                } else {
                    if (i == accountNumbers.size()) {
                        return 2;
                    }
                }
            }
            i -= getAccountRowsCount();
        }
        if (items.get(i) == null) {
            return 2;
        }
        return 3;
    }

    public void swapElements(int fromIndex, int toIndex) {
        int idx1 = fromIndex - 2;
        int idx2 = toIndex - 2;
        if (idx1 < 0 || idx2 < 0 || idx1 >= accountNumbers.size() || idx2 >= accountNumbers.size()) {
            return;
        }
        final UserConfig userConfig1 = UserConfig.getInstance(accountNumbers.get(idx1));
        final UserConfig userConfig2 = UserConfig.getInstance(accountNumbers.get(idx2));
        final int tempLoginTime = userConfig1.loginTime;
        userConfig1.loginTime = userConfig2.loginTime;
        userConfig2.loginTime = tempLoginTime;
        userConfig1.saveConfig(false);
        userConfig2.saveConfig(false);
        Collections.swap(accountNumbers, idx1, idx2);
        notifyItemMoved(fromIndex, toIndex);
    }

    private void resetItems() {
        accountNumbers.clear();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                accountNumbers.add(a);
            }
        }
        Collections.sort(accountNumbers, (o1, o2) -> {
            long l1 = UserConfig.getInstance(o1).loginTime;
            long l2 = UserConfig.getInstance(o2).loginTime;
            if (l1 > l2) {
                return 1;
            } else if (l1 < l2) {
                return -1;
            }
            return 0;
        });

        items.clear();
        if (!UserConfig.getInstance(UserConfig.selectedAccount).isClientActivated()) {
            return;
        }
        int eventType = Theme.getEventType();
        int newGroupIcon;
        int newSecretIcon;
        int newChannelIcon;
        int contactsIcon;
        int callsIcon;
        int savedIcon;
        int settingsIcon;
        int inviteIcon;
        int helpIcon;
        int peopleNearbyIcon;
        if (eventType == 0) {
            newGroupIcon = R.drawable.menu_groups_ny;
            newSecretIcon = R.drawable.menu_secret_ny;
            newChannelIcon = R.drawable.menu_channel_ny;
            contactsIcon = R.drawable.menu_contacts_ny;
            callsIcon = R.drawable.menu_calls_ny;
            savedIcon = R.drawable.menu_bookmarks_ny;
            settingsIcon = R.drawable.menu_settings_ny;
            inviteIcon = R.drawable.menu_invite_ny;
            helpIcon = R.drawable.menu_help_ny;
            peopleNearbyIcon = R.drawable.menu_nearby_ny;
        } else if (eventType == 1) {
            newGroupIcon = R.drawable.menu_groups_14;
            newSecretIcon = R.drawable.menu_secret_14;
            newChannelIcon = R.drawable.menu_broadcast_14;
            contactsIcon = R.drawable.menu_contacts_14;
            callsIcon = R.drawable.menu_calls_14;
            savedIcon = R.drawable.menu_bookmarks_14;
            settingsIcon = R.drawable.menu_settings_14;
            inviteIcon = R.drawable.menu_secret_ny;
            helpIcon = R.drawable.menu_help;
            peopleNearbyIcon = R.drawable.menu_secret_14;
        } else if (eventType == 2) {
            newGroupIcon = R.drawable.menu_groups_hw;
            newSecretIcon = R.drawable.menu_secret_hw;
            newChannelIcon = R.drawable.menu_broadcast_hw;
            contactsIcon = R.drawable.menu_contacts_hw;
            callsIcon = R.drawable.menu_calls_hw;
            savedIcon = R.drawable.menu_bookmarks_hw;
            settingsIcon = R.drawable.menu_settings_hw;
            inviteIcon = R.drawable.menu_invite_hw;
            helpIcon = R.drawable.menu_help_hw;
            peopleNearbyIcon = R.drawable.menu_secret_hw;
        } else {
            newGroupIcon = R.drawable.menu_groups;
            newSecretIcon = R.drawable.menu_secret;
            newChannelIcon = R.drawable.menu_broadcast;
            contactsIcon = R.drawable.menu_contacts;
            callsIcon = R.drawable.menu_calls;
            savedIcon = R.drawable.menu_saved;
            settingsIcon = R.drawable.menu_settings;
            inviteIcon = R.drawable.menu_invite;
            helpIcon = R.drawable.menu_help;
            peopleNearbyIcon = R.drawable.menu_nearby;
        }
        if (ExteraConfig.INSTANCE.getNewGroup()) {
            items.add(new Item(2, LocaleController.getString("NewGroup", R.string.NewGroup), newGroupIcon));
        }
        if (ExteraConfig.INSTANCE.getNewSecretChat()) {
            items.add(new Item(3, LocaleController.getString("NewSecretChat", R.string.NewSecretChat), newSecretIcon));
        }
        if (ExteraConfig.INSTANCE.getNewChannel()) {
            items.add(new Item(4, LocaleController.getString("NewChannel", R.string.NewChannel), newChannelIcon));
        }
        if (ExteraConfig.INSTANCE.getContacts()) {
            items.add(new Item(6, LocaleController.getString("Contacts", R.string.Contacts), contactsIcon));
        }
        if (ExteraConfig.INSTANCE.getCalls()) {
            items.add(new Item(10, LocaleController.getString("Calls", R.string.Calls), callsIcon));
        }
        if (hasGps) {
            if (ExteraConfig.INSTANCE.getPeopleNearby()) {
                items.add(new Item(12, LocaleController.getString("PeopleNearby", R.string.PeopleNearby), peopleNearbyIcon));
            }
        }
        if (ExteraConfig.INSTANCE.getSavedMessages()) {
            items.add(new Item(11, LocaleController.getString("SavedMessages", R.string.SavedMessages), savedIcon));
        }
        items.add(new Item(8, LocaleController.getString("Settings", R.string.Settings), settingsIcon));
        if (ExteraConfig.INSTANCE.getInviteFriends() || ExteraConfig.INSTANCE.getTelegramFeatures()) {
            items.add(null);
        }
        if (ExteraConfig.INSTANCE.getInviteFriends()) {
            items.add(new Item(7, LocaleController.getString("InviteFriends", R.string.InviteFriends), inviteIcon));
        }
        if (ExteraConfig.INSTANCE.getTelegramFeatures()) {
            items.add(new Item(13, LocaleController.getString("TelegramFeatures", R.string.TelegramFeatures), helpIcon));
        }
    }

    public int getId(int position) {
        position -= 2;
        if (accountsShown) {
            position -= getAccountRowsCount();
        }
        if (position < 0 || position >= items.size()) {
            return -1;
        }
        Item item = items.get(position);
        return item != null ? item.id : -1;
    }

    public int getFirstAccountPosition() {
        if (!accountsShown) {
            return RecyclerView.NO_POSITION;
        }
        return 2;
    }

    public int getLastAccountPosition() {
        if (!accountsShown) {
            return RecyclerView.NO_POSITION;
        }
        return 1 + accountNumbers.size();
    }

    private static class Item {
        public int icon;
        public String text;
        public int id;

        public Item(int id, String text, int icon) {
            this.icon = icon;
            this.id = id;
            this.text = text;
        }

        public void bind(DrawerActionCell actionCell) {
            actionCell.setTextAndIcon(id, text, icon);
        }
    }
}
