package com.android.packageinstaller.permission.cta;

import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.Permission;

/**
 *  @hide
 */
public final class PermissionState {
    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_ALLOWED = 1;
    public static final int STATE_DENIED = 2;

    private final AppPermissionGroup mGroup;
    private final Permission mPermission;
    private int mState = STATE_UNKNOWN;

    public PermissionState(AppPermissionGroup group, Permission permission) {
        mGroup = group;
        mPermission = permission;
    }

    public PermissionState(AppPermissionGroup group, Permission permission, int state) {
        this(group, permission);
        mState = state;
    }

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        mState = state;
    }

    public Permission getPermission() {
        return mPermission;
    }

    public AppPermissionGroup getAppPermissionGroup() {
        return mGroup;
    }
}
