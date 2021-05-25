/*
 * Copyright (c) 2016 Applivery
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.applivery.applvsdklib.ui.views.update;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.applivery.applvsdklib.AppliverySdk;
import com.applivery.applvsdklib.R;
import com.applivery.applvsdklib.domain.exceptions.NotForegroundActivityAvailable;
import com.applivery.applvsdklib.ui.model.UpdateInfo;
import com.applivery.updates.ProgressListener;
import com.applivery.updates.domain.DownloadInfo;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static com.applivery.applvsdklib.AppliverySdk.getSilentAccept;

/**
 * Created by Sergio Martinez Rodriguez
 * Date 3/1/16.
 */

public class MustUpdateViewImpl extends DialogFragment implements UpdateView {

    private Button update;
    private UpdateInfo updateInfo;
    private ProgressBar progressBar;
    private TextView updateMessage;
    private TextView permissionsDenied;
    private static UpdateListener updateListener;

    /**
     * * Using DialogFragment instead of Dialog because DialogFragment is not dismissed in rotation.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    /**
     * Overrided in order to get fullScreen dialog
     */
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        final RelativeLayout root = new RelativeLayout(getActivity());
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.YELLOW));
        dialog.getWindow()
                .setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstState) {
        View view = inflater.inflate(R.layout.applivery_must_update, container);
        setCancelable(false);
        initViewElements(view);
        initViewElementsData(updateInfo);
        return view;
    }

    public MustUpdateViewImpl() {
    }

    public void setUpdateInfo(UpdateInfo updateInfo) {
        this.updateInfo = updateInfo;
    }

    public void setUpdateListener(UpdateListener updateListener) {
        MustUpdateViewImpl.updateListener = updateListener;
    }

    private void initViewElements(View view) {
        this.updateMessage = view.findViewById(R.id.must_update_message);
        this.permissionsDenied = view.findViewById(R.id.permissions_denied_message);
        this.update = view.findViewById(R.id.must_update_button);
        this.progressBar = view.findViewById(R.id.must_update_progress_bar);

        LayerDrawable layerDrawable = (LayerDrawable) progressBar.getProgressDrawable();
        Drawable progressDrawable = layerDrawable.findDrawableByLayerId(android.R.id.progress);
    }

    private void initViewElementsData(UpdateInfo updateInfo) {

        String mustUpdateAppLockedText = getString(R.string.appliveryMustUpdateAppLocked);

        if (!mustUpdateAppLockedText.isEmpty()) {
            updateMessage.setText(mustUpdateAppLockedText);
        } else {
            if (updateInfo != null) {
                updateMessage.setText(updateInfo.getAppUpdateMessage());
            }
        }

        permissionsDenied.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        if (updateListener != null) {
            update.setVisibility(View.VISIBLE);
            update.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateListener.onUpdateButtonClick();
                    showDownloadInProgress();
                    initListener();
                }
            });
            if(getSilentAccept()){
                // auto-click accept if we allow a forced accept
                AppliverySdk.Logger.log("Proceeding because of silent accept.");
                update.callOnClick();
            }
        } else {
            update.setVisibility(View.GONE);
        }
    }

    private void initListener() {
        ProgressListener.INSTANCE.setOnUpdate(new Function1<DownloadInfo, Unit>() {
            @Override
            public Unit invoke(DownloadInfo downloadInfo) {
                showDownloadInProgress();
                updateProgress(downloadInfo.component1());
                return null;
            }
        });

        ProgressListener.INSTANCE.setOnFinish(new Function0<Unit>() {
            @Override
            public Unit invoke() {
                dismiss();
                hideDownloadInProgress();
                return null;
            }
        });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        ProgressListener.INSTANCE.clearListener();
    }

    @Override
    public void showUpdateDialog() {
        try {
            if (!AppliverySdk.isUpdating()) {
                show(AppliverySdk.getCurrentActivity().getFragmentManager(), "");
            }
        } catch (NotForegroundActivityAvailable notForegroundActivityAvailable) {
            AppliverySdk.Logger.log("Unable to show dialog again");
        }
    }

    public void lockRotationOnParentScreen() {
        AppliverySdk.lockRotation();
    }

    @Override
    public void hideDownloadInProgress() {
        Handler handler = new Handler(getLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                AppliverySdk.isUpdating(false);
                permissionsDenied.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                update.setVisibility(View.VISIBLE);
            }
        };
        handler.post(myRunnable);
    }

    @Override
    public void showDownloadInProgress() {
        Handler handler = new Handler(getLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                AppliverySdk.isUpdating(true);
                update.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                permissionsDenied.setVisibility(View.GONE);
            }
        };
        handler.post(myRunnable);
    }

    @Override
    public void updateProgress(double percent) {
        updatProcessTextView(percent, new Handler(getLooper()));
        AppliverySdk.Logger.log("Updated progress to " + percent);
    }

    @Override
    public void downloadNotStartedPermissionDenied() {
        permissionsDenied.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        update.setVisibility(View.VISIBLE);
    }

    @NonNull
    @Override
    public Boolean isActive() {
        return false;
    }

    private void updatProcessTextView(final double percent, Handler handler) {
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(Double.valueOf(percent).intValue());
            }
        };
        handler.post(myRunnable);
    }

    public Looper getLooper() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return Looper.myLooper();
        } else {
            return Looper.getMainLooper();
        }
    }
}
