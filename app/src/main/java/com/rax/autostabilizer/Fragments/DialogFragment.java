package com.rax.autostabilizer.Fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.rax.autostabilizer.R;
import com.rax.autostabilizer.databinding.DialogFullscreenBinding;


public class DialogFragment extends androidx.fragment.app.DialogFragment {
    private Fragment fragment;
    private DialogFullscreenBinding binding;
    private String title = "";
    private boolean showAppBar = true;
    OnDismissListener onDismissListener;

    public DialogFragment(OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    public DialogFragment(Fragment fragment, String title) {
        this.fragment = fragment;
        this.title = title;
    }

    public void hideAppbar() {
        showAppBar = false;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FullScreenDialog);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        onDismissListener.OnDismiss();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_fullscreen, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.toolbar.setTitle(title);
        binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment.this.dismiss();
            }
        });
        if (showAppBar) {
            binding.appBar.setVisibility(View.VISIBLE);
        } else {
            binding.appBar.setVisibility(View.GONE);
        }
        if (fragment != null) {
            FragmentManager manager = getChildFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.full_screen_container, fragment);
            transaction.commit();
        }
    }

    public void setFragment(Fragment fragment, String title) {
        this.fragment = fragment;
        this.title = title;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

    public interface OnDismissListener {
        void OnDismiss();
    }

}
