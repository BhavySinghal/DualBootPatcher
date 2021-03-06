/*
 * Copyright (C) 2014  Andrew Gunnerson <andrewgunnerson@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.chenxiaolong.dualbootpatcher.switcher;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.github.chenxiaolong.dualbootpatcher.MainActivity;
import com.github.chenxiaolong.dualbootpatcher.R;
import com.github.chenxiaolong.dualbootpatcher.RomUtils;
import com.github.chenxiaolong.dualbootpatcher.RomUtils.RomInformation;
import com.github.chenxiaolong.dualbootpatcher.switcher.ExperimentalInAppWipeDialog
        .ExperimentalInAppWipeDialogListener;
import com.github.chenxiaolong.dualbootpatcher.switcher.RomNameInputDialog
        .RomNameInputDialogListener;
import com.github.chenxiaolong.dualbootpatcher.switcher.SetKernelConfirmDialog
        .SetKernelConfirmDialogListener;
import com.github.chenxiaolong.dualbootpatcher.switcher.SwitcherEventCollector.SetKernelEvent;
import com.github.chenxiaolong.dualbootpatcher.switcher.SwitcherEventCollector.SwitchedRomEvent;
import com.github.chenxiaolong.dualbootpatcher.switcher.SwitcherEventCollector.WipedRomEvent;
import com.github.chenxiaolong.dualbootpatcher.switcher.SwitcherListFragment.LoaderResult;
import com.github.chenxiaolong.dualbootpatcher.switcher.WipeTargetsSelectionDialog
        .WipeTargetsSelectionDialogListener;
import com.github.chenxiaolong.multibootpatcher.EventCollector.BaseEvent;
import com.github.chenxiaolong.multibootpatcher.EventCollector.EventCollectorListener;
import com.github.chenxiaolong.multibootpatcher.adapters.RomCardAdapter;
import com.github.chenxiaolong.multibootpatcher.adapters.RomCardAdapter.RomCardActionListener;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

import mbtool.daemon.v2.WipeTarget;

public class SwitcherListFragment extends Fragment implements
        EventCollectorListener, RomCardActionListener,
        ExperimentalInAppWipeDialogListener,
        SetKernelConfirmDialogListener,
        WipeTargetsSelectionDialogListener,
        RomNameInputDialogListener,
        LoaderManager.LoaderCallbacks<LoaderResult> {
    public static final String TAG = SwitcherListFragment.class.getSimpleName();

    private static final String EXTRA_PERFORMING_ACTION = "performingAction";
    private static final String EXTRA_SELECTED_ROM = "selectedRom";

    private static final int PROGRESS_DIALOG_SWITCH_ROM = 1;
    private static final int PROGRESS_DIALOG_SET_KERNEL = 2;
    private static final int PROGRESS_DIALOG_WIPE_ROM = 3;

    private static final int REQUEST_IMAGE = 1234;
    private static final int REQUEST_FLASH_ZIP = 2345;

    private boolean mPerformingAction;

    private SwitcherEventCollector mEventCollector;

    private CardView mErrorCardView;
    private RomCardAdapter mRomCardAdapter;
    private RecyclerView mCardListView;
    private ProgressBar mProgressBar;
    private FloatingActionButton mFabFlashZip;

    private ArrayList<RomInformation> mRoms;
    private RomInformation mCurrentRom;
    private RomInformation mSelectedRom;

    public static SwitcherListFragment newInstance() {
        return new SwitcherListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getFragmentManager();
        mEventCollector = (SwitcherEventCollector) fm.findFragmentByTag(SwitcherEventCollector.TAG);

        if (mEventCollector == null) {
            mEventCollector = new SwitcherEventCollector();
            fm.beginTransaction().add(mEventCollector, SwitcherEventCollector.TAG).commit();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mPerformingAction = savedInstanceState.getBoolean(EXTRA_PERFORMING_ACTION);

            mSelectedRom = savedInstanceState.getParcelable(EXTRA_SELECTED_ROM);
        }

        mProgressBar = (ProgressBar) getActivity().findViewById(R.id.card_list_loading);

        mFabFlashZip = (FloatingActionButton) getActivity()
                .findViewById(R.id.fab_flash_zip);
        mFabFlashZip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ZipFlashingActivity.class);
                startActivityForResult(intent, REQUEST_FLASH_ZIP);
            }
        });

        initErrorCard();
        initCardList();
        refreshErrorVisibility(false);

        // Show progress bar on initial load, not on rotation
        if (savedInstanceState != null) {
            refreshProgressVisibility(false);
        }

        getActivity().getLoaderManager().initLoader(0, null, this);
    }

    private void reloadFragment() {
        refreshErrorVisibility(false);
        refreshRomListVisibility(false);
        refreshFabVisibility(false);
        refreshProgressVisibility(true);
        getActivity().getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_switcher, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        mEventCollector.attachListener(TAG, this);
    }

    @Override
    public void onPause() {
        super.onPause();

        mEventCollector.detachListener(TAG);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(EXTRA_PERFORMING_ACTION, mPerformingAction);
        if (mSelectedRom != null) {
            outState.putParcelable(EXTRA_SELECTED_ROM, mSelectedRom);
        }
    }

    /**
     * Create error card on fragment startup
     */
    private void initErrorCard() {
        mErrorCardView = (CardView) getActivity().findViewById(R.id.card_error);
        mErrorCardView.setClickable(true);
        mErrorCardView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                reloadFragment();
            }
        });
    }

    /**
     * Create CardListView on fragment startup
     */
    private void initCardList() {
        mRoms = new ArrayList<>();
        mRomCardAdapter = new RomCardAdapter(getActivity(), mRoms, this);

        mCardListView = (RecyclerView) getActivity().findViewById(R.id.card_list);
        mCardListView.setHasFixedSize(true);
        mCardListView.setAdapter(mRomCardAdapter);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mCardListView.setLayoutManager(llm);

        refreshRomListVisibility(false);
        refreshFabVisibility(false);
    }

    private void refreshProgressVisibility(boolean visible) {
        mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void refreshRomListVisibility(boolean visible) {
        mCardListView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void refreshErrorVisibility(boolean visible) {
        mErrorCardView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void refreshFabVisibility(boolean visible) {
        mFabFlashZip.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateCardUI() {
        if (mPerformingAction) {
            // Keep screen on
            getActivity().getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            // Show progress spinner in navigation bar
            ((MainActivity) getActivity()).showProgress(MainActivity.FRAGMENT_ROMS, true);
        } else {
            // Don't keep screen on
            getActivity().getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            // Hide progress spinner in navigation bar
            ((MainActivity) getActivity()).showProgress(MainActivity.FRAGMENT_ROMS, false);
        }
    }

    @Override
    public void onActivityResult(int request, int result, Intent data) {
        switch (request) {
        case REQUEST_IMAGE:
            if (data != null && result == Activity.RESULT_OK) {
                new ResizeAndCacheImageTask(getActivity().getApplicationContext(),
                        data.getData()).execute();
            }
            break;
        case REQUEST_FLASH_ZIP:
            reloadFragment();
            break;
        }

        super.onActivityResult(request, result, data);
    }

    @Override
    public void onEventReceived(BaseEvent bEvent) {
        if (bEvent instanceof SwitchedRomEvent) {
            SwitchedRomEvent event = (SwitchedRomEvent) bEvent;
            mPerformingAction = false;
            updateCardUI();

            GenericProgressDialog d = (GenericProgressDialog) getFragmentManager()
                    .findFragmentByTag(GenericProgressDialog.TAG + PROGRESS_DIALOG_SWITCH_ROM);
            if (d != null) {
                d.dismiss();
            }

            switch (event.result) {
            case SUCCEEDED:
                Toast.makeText(getActivity(), R.string.choose_rom_success,
                        Toast.LENGTH_SHORT).show();
                break;
            case FAILED:
                Toast.makeText(getActivity(), R.string.choose_rom_failure,
                        Toast.LENGTH_SHORT).show();
                break;
            case UNKNOWN_BOOT_PARTITION:
                showUnknownBootPartitionDialog();
                break;
            }
        } else if (bEvent instanceof SetKernelEvent) {
            SetKernelEvent event = (SetKernelEvent) bEvent;
            mPerformingAction = false;
            updateCardUI();

            GenericProgressDialog d = (GenericProgressDialog) getFragmentManager()
                    .findFragmentByTag(GenericProgressDialog.TAG + PROGRESS_DIALOG_SET_KERNEL);
            if (d != null) {
                d.dismiss();
            }

            switch (event.result) {
            case SUCCEEDED:
                Toast.makeText(getActivity(), R.string.set_kernel_success,
                        Toast.LENGTH_SHORT).show();
                break;
            case FAILED:
                Toast.makeText(getActivity(), R.string.set_kernel_failure,
                        Toast.LENGTH_SHORT).show();
                break;
            case UNKNOWN_BOOT_PARTITION:
                showUnknownBootPartitionDialog();
                break;
            }
        } else if (bEvent instanceof WipedRomEvent) {
            WipedRomEvent event = (WipedRomEvent) bEvent;
            mPerformingAction = false;
            updateCardUI();

            GenericProgressDialog d = (GenericProgressDialog) getFragmentManager()
                    .findFragmentByTag(GenericProgressDialog.TAG + PROGRESS_DIALOG_WIPE_ROM);
            if (d != null) {
                d.dismiss();
            }

            if (event.succeeded == null || event.failed == null) {
                Toast.makeText(getActivity(), R.string.wipe_rom_initiate_fail, Toast.LENGTH_LONG)
                        .show();
            } else {
                StringBuilder sb = new StringBuilder();

                if (event.succeeded.length > 0) {
                    sb.append(String.format(getString(R.string.wipe_rom_successfully_wiped),
                            targetsToString(event.succeeded)));
                    if (event.failed.length > 0) {
                        sb.append("\n");
                    }
                }
                if (event.failed.length > 0) {
                    sb.append(String.format(getString(R.string.wipe_rom_failed_to_wipe),
                            targetsToString(event.failed)));
                }

                Toast.makeText(getActivity(), sb.toString(), Toast.LENGTH_LONG).show();

                // We don't want deleted ROMs to still show up
                reloadFragment();
            }
        }
    }

    private void showUnknownBootPartitionDialog() {
        String message = String.format(getString(R.string.unknown_boot_partition), Build.DEVICE);

        GenericConfirmDialog gcd = GenericConfirmDialog.newInstance(null, message);
        gcd.show(getFragmentManager(), GenericConfirmDialog.TAG);
    }

    private String targetsToString(short[] targets) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < targets.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }

            if (targets[i] == WipeTarget.SYSTEM) {
                sb.append(getString(R.string.wipe_target_system));
            } else if (targets[i] == WipeTarget.CACHE) {
                sb.append(getString(R.string.wipe_target_cache));
            } else if (targets[i] == WipeTarget.DATA) {
                sb.append(getString(R.string.wipe_target_data));
            } else if (targets[i] == WipeTarget.DALVIK_CACHE) {
                sb.append(getString(R.string.wipe_target_dalvik_cache));
            } else if (targets[i] == WipeTarget.MULTIBOOT) {
                sb.append(getString(R.string.wipe_target_multiboot_files));
            }
        }

        return sb.toString();
    }

    @Override
    public void onSelectedRom(RomInformation info) {
        mSelectedRom = info;
        mPerformingAction = true;
        updateCardUI();

        GenericProgressDialog d = GenericProgressDialog.newInstance(
                R.string.switching_rom, R.string.please_wait);
        d.show(getFragmentManager(), GenericProgressDialog.TAG + PROGRESS_DIALOG_SWITCH_ROM);

        mEventCollector.chooseRom(info.getId());
    }

    @Override
    public void onSelectedSetKernel(RomInformation info) {
        mSelectedRom = info;

        // Ask for confirmation
        SetKernelConfirmDialog d = SetKernelConfirmDialog.newInstance(this, mSelectedRom);
        d.show(getFragmentManager(), SetKernelConfirmDialog.TAG);
    }

    private void setKernel(RomInformation info) {
        mPerformingAction = true;
        updateCardUI();

        GenericProgressDialog d = GenericProgressDialog.newInstance(
                R.string.setting_kernel, R.string.please_wait);
        d.show(getFragmentManager(), GenericProgressDialog.TAG + PROGRESS_DIALOG_SET_KERNEL);

        mEventCollector.setKernel(info.getId());
    }

    @Override
    public void onSelectedEditName(RomInformation info) {
        mSelectedRom = info;

        RomNameInputDialog d = RomNameInputDialog.newInstance(this, mSelectedRom);
        d.show(getFragmentManager(), RomNameInputDialog.TAG);
    }

    @Override
    public void onSelectedChangeImage(RomInformation info) {
        mSelectedRom = info;

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    @Override
    public void onSelectedResetImage(RomInformation info) {
        mSelectedRom = info;

        File f = new File(info.getThumbnailPath());
        if (f.isFile()) {
            f.delete();
        }

        mRomCardAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSelectedWipeRom(RomInformation info) {
        mSelectedRom = info;

        if (mCurrentRom != null && mCurrentRom.getId().equals(info.getId())) {
            Toast.makeText(getActivity(), R.string.wipe_rom_no_wipe_current_rom,
                    Toast.LENGTH_LONG).show();
        } else {
            ExperimentalInAppWipeDialog d = ExperimentalInAppWipeDialog.newInstance(this);
            d.show(getFragmentManager(), ExperimentalInAppWipeDialog.TAG);
        }
    }

    private void wipeRom(RomInformation info, short[] targets) {
        mPerformingAction = true;
        updateCardUI();

        GenericProgressDialog d = GenericProgressDialog.newInstance(
                R.string.wiping_targets, R.string.please_wait);
        d.show(getFragmentManager(), GenericProgressDialog.TAG + PROGRESS_DIALOG_WIPE_ROM);

        mEventCollector.wipeRom(info.getId(), targets);
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        return new RomsLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult> loader, LoaderResult result) {
        mRoms.clear();

        if (result.roms != null) {
            Collections.addAll(mRoms, result.roms);
        } else {
            refreshErrorVisibility(true);
        }

        mCurrentRom = result.currentRom;

        mRomCardAdapter.notifyDataSetChanged();
        updateCardUI();

        refreshProgressVisibility(false);
        refreshRomListVisibility(true);
        refreshFabVisibility(true);
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader) {
    }

    @Override
    public void onConfirmInAppRomWipeWarning() {
        WipeTargetsSelectionDialog d = WipeTargetsSelectionDialog.newInstance(this);
        d.show(getFragmentManager(), WipeTargetsSelectionDialog.TAG);
    }

    @Override
    public void onConfirmSetKernel() {
        setKernel(mSelectedRom);
    }

    @Override
    public void onSelectedWipeTargets(short[] targets) {
        if (targets.length == 0) {
            Toast.makeText(getActivity(), R.string.wipe_rom_none_selected,
                    Toast.LENGTH_LONG).show();
            return;
        }

        wipeRom(mSelectedRom, targets);
    }

    @Override
    public void onRomNameChanged(String newName) {
        mSelectedRom.setName(newName);

        new Thread() {
            @Override
            public void run() {
                RomUtils.saveConfig(mSelectedRom);
            }
        }.start();

        mRomCardAdapter.notifyDataSetChanged();
    }

    protected class ResizeAndCacheImageTask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        private final Uri mUri;

        public ResizeAndCacheImageTask(Context context, Uri uri) {
            mContext = context;
            mUri = uri;
        }

        private Bitmap getThumbnail(Uri uri) {
            try {
                InputStream input = mContext.getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();

                if (bitmap == null) {
                    return null;
                }

                return ThumbnailUtils.extractThumbnail(bitmap, 500, 500);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Bitmap thumbnail = getThumbnail(mUri);

            // Write the image to a temporary file. If the user selects it,
            // the move it to the appropriate location.
            File f = new File(mSelectedRom.getThumbnailPath());
            f.getParentFile().mkdirs();

            FileOutputStream out = null;

            try {
                out = new FileOutputStream(f);
                thumbnail.compress(Bitmap.CompressFormat.WEBP, 100, out);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(out);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (getActivity() == null) {
                return;
            }

            mRomCardAdapter.notifyDataSetChanged();
        }
    }

    private static class RomsLoader extends AsyncTaskLoader<LoaderResult> {
        private LoaderResult mResult;

        public RomsLoader(Context context) {
            super(context);
            onContentChanged();
        }

        @Override
        protected void onStartLoading() {
            if (mResult != null) {
                deliverResult(mResult);
            } else if (takeContentChanged()) {
                forceLoad();
            }
        }

        @Override
        public LoaderResult loadInBackground() {
            mResult = new LoaderResult();
            mResult.roms = RomUtils.getRoms(getContext());
            mResult.currentRom = RomUtils.getCurrentRom(getContext());
            return mResult;
        }
    }

    protected static class LoaderResult {
        RomInformation[] roms;
        RomInformation currentRom;
    }
}
