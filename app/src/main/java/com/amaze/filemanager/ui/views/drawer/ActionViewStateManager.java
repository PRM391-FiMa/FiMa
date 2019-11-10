package com.amaze.filemanager.ui.views.drawer;

import androidx.annotation.ColorInt;

import android.view.MenuItem;
import android.widget.ImageButton;

/**
 * Quản lý màu các ActionView đã chọn
 * và bỏ màu các ActionView không được chọn
 */
public class ActionViewStateManager {

    private ImageButton lastItemSelected = null;
    private @ColorInt int idleIconColor;
    private @ColorInt int selectedIconColor;

    public ActionViewStateManager(@ColorInt int idleColor, @ColorInt int accentColor) {
        idleIconColor = idleColor;
        selectedIconColor = accentColor;
    }

    public void deselectCurrentActionView() {
        if(lastItemSelected != null) {
            lastItemSelected.setColorFilter(idleIconColor);
            lastItemSelected = null;
        }
    }

    public void selectActionView(MenuItem item) {
        if(lastItemSelected != null) {
            lastItemSelected.setColorFilter(idleIconColor);
        }
        if(item.getActionView() != null) {
            lastItemSelected = (ImageButton) item.getActionView();
            lastItemSelected.setColorFilter(selectedIconColor);
        }
    }

}
