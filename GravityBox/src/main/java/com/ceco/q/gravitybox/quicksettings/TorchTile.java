/*
 * Copyright (C) 2018 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.ceco.q.gravitybox.quicksettings;

import com.ceco.q.gravitybox.R;
import com.ceco.q.gravitybox.GravityBoxResultReceiver;
import com.ceco.q.gravitybox.TorchService;
import com.ceco.q.gravitybox.managers.BroadcastMediator;
import com.ceco.q.gravitybox.managers.SysUiManagers;

import de.robv.android.xposed.XSharedPreferences;
import android.content.Intent;
import android.os.Handler;

public class TorchTile extends QsTile {
    public static final class Service extends QsTileServiceBase {
        static final String KEY = TorchTile.class.getSimpleName()+"$Service";
    }

    private int mTorchStatus = TorchService.TORCH_STATUS_OFF;
    private boolean mIsReceiving;
    private GravityBoxResultReceiver mReceiver;

    private BroadcastMediator.Receiver mBroadcastReceiver = (context, intent) -> {
        if (intent.getAction().equals(TorchService.ACTION_TORCH_STATUS_CHANGED) &&
                intent.hasExtra(TorchService.EXTRA_TORCH_STATUS)) {
            mTorchStatus = intent.getIntExtra(TorchService.EXTRA_TORCH_STATUS,
                    TorchService.TORCH_STATUS_OFF);
            refreshState();
        }
    };

    public TorchTile(Object host, String key, Object tile, XSharedPreferences prefs,
            QsTileEventDistributor eventDistributor) throws Throwable {
        super(host, key, tile, prefs, eventDistributor);

        mReceiver = new GravityBoxResultReceiver(new Handler());
        mReceiver.setReceiver((resultCode, resultData) -> {
            final int oldState = mTorchStatus;
            mTorchStatus = resultData.getInt(TorchService.EXTRA_TORCH_STATUS);
            if (mTorchStatus != oldState) {
                refreshState();
                if (DEBUG) log(getKey() + ": onReceiveResult: mTorchStatus=" + mTorchStatus);
            }
        });
    }

    private void registerReceiver() {
        if (!mIsReceiving) {
            SysUiManagers.BroadcastMediator.subscribe(mBroadcastReceiver,
                    TorchService.ACTION_TORCH_STATUS_CHANGED);
            mIsReceiving = true;
            if (DEBUG) log(getKey() + ": receiver registered");
        }
    }

    private void unregisterReceiver() {
        if (mIsReceiving) {
            SysUiManagers.BroadcastMediator.unsubscribe(mBroadcastReceiver);
            mIsReceiving = false;
            if (DEBUG) log(getKey() + ": unreceiver registered");
        }
    }

    @Override
    public String getSettingsKey() {
        return "gb_tile_torch";
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            registerReceiver();
            getTorchState();
        } else {
            unregisterReceiver();
        }
    }

    private void getTorchState() {
        Intent si = new Intent(mGbContext, TorchService.class);
        si.setAction(TorchService.ACTION_TORCH_GET_STATUS);
        si.putExtra("receiver", mReceiver);
        mGbContext.startService(si);
        if (DEBUG) log(getKey() + ": ACTION_TORCH_GET_STATUS sent");
    }

    private void toggleState() {
        Intent si = new Intent(mGbContext, TorchService.class);
        si.setAction(TorchService.ACTION_TOGGLE_TORCH);
        mGbContext.startService(si);
    }

    @Override
    public void handleUpdateState(Object state, Object arg) {
        mState.booleanValue = mTorchStatus == TorchService.TORCH_STATUS_ON;
        if (mTorchStatus == TorchService.TORCH_STATUS_ON) {
            mState.icon = iconFromResId(R.drawable.ic_qs_torch_on);
            mState.label = mGbContext.getString(R.string.quick_settings_torch_on);
        } else {
            mState.icon = iconFromResId(R.drawable.ic_qs_torch_off);
            mState.label = mGbContext.getString(R.string.quick_settings_torch_off);
        }

        super.handleUpdateState(state, arg);
    }

    @Override
    public void handleClick() {
        toggleState();
        super.handleClick();
    }

    @Override
    public void handleDestroy() {
        super.handleDestroy();
        mReceiver = null;
        mBroadcastReceiver = null;
    }
}
