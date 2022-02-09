/*
 * UNISOC: CTA Feature:
 * Copyright (C) 2019 The Unisoc Open Source Project
 * This interface is used for cta permissions.
 */

package com.android.packageinstaller.permission.cta;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;
import android.cta.CtaManifest;
import android.cta.PermissionUtils;
import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.packageinstaller.permission.model.AppPermissions;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.Permission;
import com.android.packageinstaller.permission.utils.ArrayUtils;
import com.android.packageinstaller.permission.utils.Utils;
import com.android.permissioncontroller.R;

import java.util.List;
import java.util.HashMap;

public final class CtaPermissionPlus {

    private static final String CTA_TAG = "CtaSecurity";

    private static HashMap<String, PermissionState> mPermStateMap = new HashMap<>();

    private static final String[] ADD_SWITCH_PERMISSIONS = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.SEND_SMS,
            CtaManifest.Permission.CTA_SEND_MMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_WAP_PUSH,
            Manifest.permission.READ_CELL_BROADCASTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ADD_VOICEMAIL,
            Manifest.permission.USE_SIP,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.ACCEPT_HANDOVER
    };

    public static final int TYPE_GROUP = 0;
    public static final int TYPE_PERMISSION = 1;
    public static final int TYPE_MAX = 2;

    private CtaPermissionPlus() { /* cannot be instantiated */ }

    public static boolean isCtaPermissionIndividuallyControlled(String permission) {
        return ArrayUtils.contains(ADD_SWITCH_PERMISSIONS, permission);
    }

    public static boolean filterGroupPermission(String group) {
        return Manifest.permission_group.CAMERA.equals(group)
                || Manifest.permission_group.LOCATION.equals(group)
                || Manifest.permission_group.MICROPHONE.equals(group)
                || Manifest.permission_group.STORAGE.equals(group)
                || Manifest.permission_group.SENSORS.equals(group)
                || Manifest.permission_group.ACTIVITY_RECOGNITION.equals(group);
    }

    public static void updateDisplayRevokePermissions(Context context, String[] permissions, TextView textView) {
        PermissionInfo permInfo = null;
        PackageManager pm = context.getPackageManager();
        StringBuilder permissionDetail = new StringBuilder();
        permissionDetail.append(context.getString(R.string.display_revoke_permissions));

        for (int i = 0; i < permissions.length; i++) {
            try {
                permInfo = pm.getPermissionInfo(permissions[i], 0);
            } catch (NameNotFoundException e) {
                Log.e(CTA_TAG, "Can't get permission info for " + permissions[i], e);
                continue;
            }
            if (permInfo != null) {
                permissionDetail.append("\n").append(permInfo.loadLabel(pm));
            }
        }
        textView.setVisibility(View.VISIBLE);
        textView.setText(permissionDetail.toString());
    }

    public static void addCtaPermissionsToArrayMap(ArrayMap<String, String> permissions) {
        permissions.put(CtaManifest.Permission.CTA_SEND_MMS, Manifest.permission_group.SMS);
    }

    public static void initCtaPermState(Context context, AppPermissions appPermissions,
                                          List<String> revokedPerms) {
        mPermStateMap.clear();
        for (AppPermissionGroup group : appPermissions.getPermissionGroups()) {
            if (!Utils.shouldShowPermission(context, group)) {
                continue;
            }
            for (Permission permission : group.getPermissions()) {
                int state = revokedPerms != null && revokedPerms.contains(permission.getName())
                        ? PermissionState.STATE_DENIED
                        : PermissionState.STATE_ALLOWED;
                mPermStateMap.put(permission.getName(),
                        new PermissionState(group, permission, state));
            }
            AppPermissionGroup backgroundGroup = group.getBackgroundPermissions();
            if (backgroundGroup != null) {
                for (Permission permission : backgroundGroup.getPermissions()) {
                    int state = revokedPerms != null && revokedPerms.contains(permission.getName())
                            ? PermissionState.STATE_DENIED
                            : PermissionState.STATE_ALLOWED;
                    mPermStateMap.put(permission.getName(),
                            new PermissionState(group, permission, state));
                }
            }
        }
    }

    public static boolean isPermGrantedForReviewUI(Permission permission) {
        if (mPermStateMap.get(permission.getName()) == null) {
            Log.w(CTA_TAG, "can not find permission: " + permission.getName());
            return false;
        }

        return mPermStateMap.get(permission.getName())
                .getState() == PermissionState.STATE_ALLOWED;
    }

    public static void setPermStateForReviewUI(Permission permission, int state) {
        if (mPermStateMap.get(permission.getName()) == null) {
            return;
        }
        mPermStateMap.get(permission.getName()).setState(state);
    }

    public static void setPermGroupStateForReviewUI(AppPermissionGroup group, boolean grant) {
        for (Permission permission : group.getPermissions()) {
            setPermStateForReviewUI(permission,
                    grant ? PermissionState.STATE_ALLOWED : PermissionState.STATE_DENIED);
        }
        AppPermissionGroup backgroundGroup = group.getBackgroundPermissions();
        if (backgroundGroup != null) {
            for (Permission permission : backgroundGroup.getPermissions()) {
                setPermStateForReviewUI(permission,
                        grant ? PermissionState.STATE_ALLOWED : PermissionState.STATE_DENIED);
            }
        }
    }


    public static boolean isPermGroupGrantedForReviewUI(AppPermissionGroup group) {
        boolean grant = false;
        for (Permission permission : group.getPermissions()) {
            grant |= isPermGrantedForReviewUI(permission);
        }
        AppPermissionGroup backgroundGroup = group.getBackgroundPermissions();
        if (backgroundGroup != null) {
            for (Permission permission : backgroundGroup.getPermissions()) {
                grant |= isPermGrantedForReviewUI(permission);
            }
        }
        return grant;
    }
}