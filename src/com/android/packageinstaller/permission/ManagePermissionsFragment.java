/*
* Copyright (C) 2015 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.packageinstaller.permission;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.packageinstaller.R;

public final class ManagePermissionsFragment extends SettingsWithHeader
        implements OnPreferenceChangeListener {
    private static final String LOG_TAG = "ManagePermissionsFragment";

    private AppPermissions mAppPermissions;

    public static ManagePermissionsFragment newInstance(String packageName) {
        ManagePermissionsFragment instance = new ManagePermissionsFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Intent.EXTRA_PACKAGE_NAME, packageName);
        instance.setArguments(arguments);
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        bindUi();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void bindUi() {
        String packageName = getArguments().getString(Intent.EXTRA_PACKAGE_NAME);

        final Activity activity = getActivity();
        PackageInfo packageInfo = getPackageInfo(packageName);
        if (packageInfo == null) {
            Toast.makeText(activity, R.string.app_not_found_dlg_title, Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return;
        }
        final PackageManager pm = activity.getPackageManager();
        ApplicationInfo appInfo = packageInfo.applicationInfo;
        setHeader(appInfo.loadIcon(pm), appInfo.loadLabel(pm), null);

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(activity);
        mAppPermissions = new AppPermissions(activity, packageInfo, null);

        for (AppPermissions.PermissionGroup group : mAppPermissions.getPermissionGroups()) {
            if (group.hasRuntimePermissions()) {
                SwitchPreference preference = new SwitchPreference(activity);
                preference.setOnPreferenceChangeListener(this);
                preference.setKey(group.getName());
                preference.setIcon(AppPermissions.loadDrawable(pm, group.getIconPkg(),
                        group.getIconResId()));
                preference.setTitle(group.getLabel());
                preference.setPersistent(false);
                screen.addPreference(preference);
            }
        }

        setPreferenceScreen(screen);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String groupName = preference.getKey();
        AppPermissions.PermissionGroup group = mAppPermissions.getPermissionGroup(groupName);

        if (group == null) {
            return false;
        }

        if (newValue == Boolean.TRUE) {
            group.grantRuntimePermissions();
        } else {
            group.revokeRuntimePermissions();
        }

        return true;
    }

    private void updateUi() {
        mAppPermissions.refresh();

        final int preferenceCount = getPreferenceScreen().getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            SwitchPreference preference = (SwitchPreference)
                    getPreferenceScreen().getPreference(i);
            AppPermissions.PermissionGroup group = mAppPermissions
                    .getPermissionGroup(preference.getKey());
            if (group != null) {
                preference.setChecked(group.areRuntimePermissionsGranted());
            }
        }
    }

    private PackageInfo getPackageInfo(String packageName) {
        try {
            return getActivity().getPackageManager().getPackageInfo(
                    packageName, PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(LOG_TAG, "No package:" + getActivity().getCallingPackage(), e);
            return null;
        }
    }
}