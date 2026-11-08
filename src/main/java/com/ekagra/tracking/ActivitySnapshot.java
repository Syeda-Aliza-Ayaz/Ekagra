package com.ekagra.tracking;

import com.ekagra.model.ActivityLog;
import com.ekagra.model.Category;

/**
 * What the tracker saw in one sample.
 *
 * @param window      the active window
 * @param category    its classification
 * @param segment     the current continuous stretch in this app (duration so far)
 * @param switchedApp true if the user just switched to this app/category
 */
public record ActivitySnapshot(WindowInfo window, Category category, ActivityLog segment, boolean switchedApp) {
}
