package com.amaze.filemanager.ui.views.drawer;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.navigation.NavigationView;

import androidx.legacy.app.ActionBarDrawerToggle;
import androidx.fragment.app.FragmentTransaction;
import androidx.drawerlayout.widget.DrawerLayout;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.activities.PreferencesActivity;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.usb.SingletonUsbOtg;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.utils.BookSorter;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.theme.AppTheme;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_FOLDERS;

public class Drawer implements NavigationView.OnNavigationItemSelectedListener {

    public static final int image_selector_request_code = 31;

    public static final int STORAGES_GROUP = 0, SERVERS_GROUP = 1, CLOUDS_GROUP = 2, FOLDERS_GROUP = 3,
            QUICKACCESSES_GROUP = 4, LASTGROUP = 5;
    public static final int[] GROUPS = {STORAGES_GROUP, SERVERS_GROUP, CLOUDS_GROUP, FOLDERS_GROUP,
            QUICKACCESSES_GROUP, LASTGROUP};


    private MainActivity mainActivity;
    private Resources resources;
    private DataUtils dataUtils;

    private ActionViewStateManager actionViewStateManager;
    private boolean isSomethingSelected;
    private volatile int phoneStorageCount = 0; // number of storage available (internal/external/otg etc)
    private boolean isDrawerLocked = false;
    private FragmentTransaction pending_fragmentTransaction;
    private String pendingPath;
    private String firstPath = null, secondPath = null;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private CustomNavigationView navView;

    public Drawer(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        resources = mainActivity.getResources();
        dataUtils = DataUtils.getInstance();



        navView = mainActivity.findViewById(R.id.navigation);
        navView.setNavigationItemSelectedListener(this);

        int accentColor = mainActivity.getAccent(), idleColor; // màu khi tác động đến item

        if (mainActivity.getAppTheme().equals(AppTheme.LIGHT)) {
            idleColor = mainActivity.getResources().getColor(R.color.item_light_theme);
        } else {
            idleColor = Color.WHITE;
        }

        actionViewStateManager = new ActionViewStateManager(idleColor, accentColor); // màu khi tác động hoặc không tác động

        ColorStateList drawerColors = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{android.R.attr.state_enabled},
                        new int[]{android.R.attr.state_pressed},
                        new int[]{android.R.attr.state_focused},
                        new int[]{android.R.attr.state_pressed}
                },
                new int[]{accentColor, idleColor, idleColor, idleColor, idleColor}
        );

        navView.setItemTextColor(drawerColors);
        navView.setItemIconTintList(drawerColors);

        if (mainActivity.getAppTheme().equals(AppTheme.DARK)) {
            navView.setBackgroundColor(Utils.getColor(mainActivity, R.color.holo_dark_background));
        } else if (mainActivity.getAppTheme().equals(AppTheme.BLACK)) {
            navView.setBackgroundColor(Utils.getColor(mainActivity, android.R.color.black));
        } else {
            navView.setBackgroundColor(Color.WHITE);
        }

        mDrawerLayout = mainActivity.findViewById(R.id.drawer_layout);

        // Navigation toggle
        if (!isDrawerLocked) {
            mDrawerToggle = new ActionBarDrawerToggle(
                    mainActivity,                  /* host Activity */
                    mDrawerLayout,         /* DrawerLayout object */
                    R.drawable.ic_drawer_l,  /* nav drawer image to replace 'Up' caret */
                    R.string.drawer_open,  /* "open drawer" description for accessibility */
                    R.string.drawer_close  /* "close drawer" description for accessibility */
            ) {
                public void onDrawerClosed(View view) {
                    Drawer.this.onDrawerClosed();
                }

                public void onDrawerOpened(View drawerView) {
                }
            };
            mDrawerLayout.setDrawerListener(mDrawerToggle);
            mainActivity.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer_l);
            mainActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mainActivity.getSupportActionBar().setHomeButtonEnabled(true);
            mDrawerToggle.syncState();
        }

    }

    // refresh navigation
    public void refreshDrawer() {
        Menu menu = navView.getMenu();
        menu.clear();
        actionViewStateManager.deselectCurrentActionView();

        int order = 0;
        ArrayList<String> storageDirectories = mainActivity.getStorageDirectories(); // trả về danh sách các thư mục được lưu trữ
        phoneStorageCount = 0;
        for (String file : storageDirectories) {
            if (file.contains(OTGUtil.PREFIX_OTG)) {
                addNewItem(menu, STORAGES_GROUP, order++, "OTG", new MenuMetadata(file),
                        R.drawable.ic_usb_white_24dp, R.drawable.ic_show_chart_black_24dp);
                continue;
            }

            File f = new File(file);
            String name;
            @DrawableRes int icon1;
            if ("/storage/emulated/legacy".equals(file) || "/storage/emulated/0".equals(file) || "/mnt/sdcard".equals(file)) {
                name = resources.getString(R.string.internalstorage);
                icon1 = R.drawable.ic_phone_android_white_24dp;
            } else if ("/storage/sdcard1".equals(file)) {
                name = resources.getString(R.string.extstorage);
                icon1 = R.drawable.ic_sd_storage_white_24dp;
            } else if ("/".equals(file)) {
                name = resources.getString(R.string.root_directory);
                icon1 = R.drawable.ic_drawer_root_white;
            } else {
                name = f.getName();
                icon1 = R.drawable.ic_sd_storage_white_24dp;
            }

            if (f.isDirectory() || f.canExecute()) {
                addNewItem(menu, STORAGES_GROUP, order++, name, new MenuMetadata(file), icon1,
                        R.drawable.ic_show_chart_black_24dp);
                if (phoneStorageCount == 0) firstPath = file;
                else if (phoneStorageCount == 1) secondPath = file;

                phoneStorageCount++;
            }
        }
        dataUtils.setStorages(storageDirectories);

        if (dataUtils.getServers().size() > 0) {
            Collections.sort(dataUtils.getServers(), new BookSorter());
            synchronized (dataUtils.getServers()) {
                for (String[] file : dataUtils.getServers()) {
                    addNewItem(menu, SERVERS_GROUP, order++, file[0],
                            new MenuMetadata(file[1]), R.drawable.ic_settings_remote_white_24dp,
                            R.drawable.ic_edit_24dp);
                }
            }
        }

        if (mainActivity.getBoolean(PREFERENCE_SHOW_SIDEBAR_FOLDERS)) {
            if (dataUtils.getBooks().size() > 0) {

                Collections.sort(dataUtils.getBooks(), new BookSorter());

                synchronized (dataUtils.getBooks()) {
                    for (String[] file : dataUtils.getBooks()) {
                        addNewItem(menu, FOLDERS_GROUP, order++, file[0],
                                new MenuMetadata(file[1]), R.drawable.ic_folder_white_24dp,null);
                    }
                }
            }
        }

        addNewItem(menu, LASTGROUP, order++, R.string.setting,
                new MenuMetadata(() -> {
                    Intent in = new Intent(mainActivity, PreferencesActivity.class);
                    mainActivity.startActivity(in);
                    mainActivity.finish();
                }),
                R.drawable.ic_settings_white_24dp, null);

        for (int i = 0; i < navView.getMenu().size(); i++) {
            navView.getMenu().getItem(i).setEnabled(true);
        }

        for (int group : GROUPS) {
            menu.setGroupCheckable(group, true, true);
        }

        MenuItem item = navView.getSelected();
        if (item != null) {
            item.setChecked(true);
            actionViewStateManager.selectActionView(item);
            isSomethingSelected = true;
        }
    }

    private void addNewItem(Menu menu, int group, int order, @StringRes int text, MenuMetadata meta,
                            @DrawableRes int icon, @DrawableRes Integer actionViewIcon) {
        if (BuildConfig.DEBUG && menu.findItem(order) != null)
            throw new IllegalStateException("Item already id exists: " + order);

        MenuItem item = menu.add(group, order, order, text).setIcon(icon);
        dataUtils.putDrawerMetadata(item, meta);
        if (actionViewIcon != null) {
            item.setActionView(R.layout.layout_draweractionview);

            ImageView imageView = item.getActionView().findViewById(R.id.imageButton);
            imageView.setImageResource(actionViewIcon);
            if (!mainActivity.getAppTheme().equals(AppTheme.LIGHT)) {
                imageView.setColorFilter(Color.WHITE);
            }

            item.getActionView().setOnClickListener((view) -> onNavigationItemActionClick(item));
        }
    }

    private void addNewItem(Menu menu, int group, int order, String text, MenuMetadata meta,
                            @DrawableRes int icon, @DrawableRes Integer actionViewIcon) {
        if (BuildConfig.DEBUG && menu.findItem(order) != null)
            throw new IllegalStateException("Item already id exists: " + order);

        MenuItem item = menu.add(group, order, order, text).setIcon(icon);
        dataUtils.putDrawerMetadata(item, meta);

        if (actionViewIcon != null) {
            item.setActionView(R.layout.layout_draweractionview);

            ImageView imageView = item.getActionView().findViewById(R.id.imageButton);
            imageView.setImageResource(actionViewIcon);
            if (!mainActivity.getAppTheme().equals(AppTheme.LIGHT)) {
                imageView.setColorFilter(Color.WHITE);
            }

            item.getActionView().setOnClickListener((view) -> onNavigationItemActionClick(item));
        }
    }

    public void closeIfNotLocked() {
        if (!isLocked()) {
            close();
        }
    }

    public boolean isLocked() {
        return isDrawerLocked;
    }

    public boolean isOpen() {
        return mDrawerLayout.isDrawerOpen(navView);
    }

    public void open() {
        mDrawerLayout.openDrawer(navView);
    }

    public void close() {
        mDrawerLayout.closeDrawer(navView);
    }

    public void onDrawerClosed() {
        if (pending_fragmentTransaction != null) {
            pending_fragmentTransaction.commit();
            pending_fragmentTransaction = null;
        }

        if (pendingPath != null) {
            HybridFile hFile = new HybridFile(OpenMode.UNKNOWN, pendingPath);
            hFile.generateMode(mainActivity);
            if (hFile.isSimpleFile()) {
                FileUtils.openFile(new File(pendingPath), mainActivity, mainActivity.getPrefs());
                pendingPath = null;
                return;
            }

            MainFragment mainFrag = mainActivity.getCurrentMainFragment();
            if (mainFrag != null) {
                mainFrag.loadlist(pendingPath, false, OpenMode.UNKNOWN);
            } else {
                mainActivity.goToMain(pendingPath);
                return;
            }
            pendingPath = null;
        }
        mainActivity.supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        actionViewStateManager.deselectCurrentActionView();
        actionViewStateManager.selectActionView(item);
        isSomethingSelected = true;

        String title = item.getTitle().toString();
        MenuMetadata meta = dataUtils.getDrawerMetadata(item);

        switch (meta.type) {
            case MenuMetadata.ITEM_ENTRY:
                if (dataUtils.containsBooks(new String[]{title, meta.path}) != -1) {
                    FileUtils.checkForPath(mainActivity, meta.path, mainActivity.isRootExplorer());
                }

                if (dataUtils.getAccounts().size() > 0 && (meta.path.startsWith(CloudHandler.CLOUD_PREFIX_BOX) ||
                        meta.path.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX) ||
                        meta.path.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE) ||
                        meta.path.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE))) {
                    // we have cloud accounts, try see if token is expired or not
                    CloudUtil.checkToken(meta.path, mainActivity);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                        && meta.path.contains(OTGUtil.PREFIX_OTG)
                        && SingletonUsbOtg.getInstance().getUsbOtgRoot() == null) {
                    MaterialDialog dialog = GeneralDialogCreation.showOtgSafExplanationDialog(mainActivity);
                    dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener((v) -> {
                        Intent safIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        mainActivity.startActivityForResult(safIntent, MainActivity.REQUEST_CODE_SAF);
                        dialog.dismiss();
                    });
                } else {
                    pendingPath = meta.path;
                    closeIfNotLocked();
                    if (isLocked()) {
                        onDrawerClosed();
                    }
                }

                break;
            case MenuMetadata.ITEM_INTENT:
                meta.onClickListener.onClick();
                break;
        }

        return true;
    }

    public void onNavigationItemActionClick(MenuItem item) {
        String title = item.getTitle().toString();
        MenuMetadata meta = dataUtils.getDrawerMetadata(item);
        String path = meta.path;

        switch (item.getGroupId()) {
            case STORAGES_GROUP:
                if (!path.equals("/")) {
                    GeneralDialogCreation.showPropertiesDialogForStorage(
                            RootHelper.generateBaseFile(new File(path), true),
                            mainActivity, mainActivity.getAppTheme());
                }
                break;
            // not to remove the first bookmark (storage) and permanent bookmarks
            case SERVERS_GROUP:
        }
    }

    public boolean isSomethingSelected() {
        return isSomethingSelected;
    }

    public void setSomethingSelected(boolean isSelected) {
        isSomethingSelected = isSelected;
    }

    public int getPhoneStorageCount() {
        return phoneStorageCount;
    }

    public void selectCorrectDrawerItemForPath(final String path) {
        Integer id = dataUtils.findLongestContainingDrawerItem(path);

        if (id == null) deselectEverything();
        else {
            MenuItem item = navView.getMenu().findItem(id);
            navView.setCheckedItem(item);
            actionViewStateManager.selectActionView(item);
        }
    }


    public void resetPendingPath() {
        pendingPath = null;
    }

    public void syncState() {
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (mDrawerToggle != null) mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item);
    }

    public void setDrawerIndicatorEnabled() {
        if (mDrawerToggle != null) {
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            mDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_drawer_l);
        }
    }

    public void deselectEverything() {
        actionViewStateManager.deselectCurrentActionView();//If you set the item as checked the listener doesn't trigger
        if (!isSomethingSelected) {
            return;
        }

        navView.deselectItems();

        for (int i = 0; i < navView.getMenu().size(); i++) {
            navView.getMenu().getItem(i).setChecked(false);
        }

        isSomethingSelected = false;
    }

    public String getFirstPath() {
        return firstPath;
    }

    public String getSecondPath() {
        return secondPath;
    }
}
