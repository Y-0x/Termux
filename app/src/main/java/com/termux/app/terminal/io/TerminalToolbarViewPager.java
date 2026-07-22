package com.termux.app.terminal.io;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.terminal.TerminalSession;

public class TerminalToolbarViewPager {

    private static final int VIEW_TYPE_EXTRA_KEYS = 0;
    private static final int VIEW_TYPE_TEXT_INPUT = 1;

    public static class PageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        final TermuxActivity mActivity;
        String mSavedTextInput;

        public PageAdapter(TermuxActivity activity, String savedTextInput) {
            this.mActivity = activity;
            this.mSavedTextInput = savedTextInput;
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? VIEW_TYPE_EXTRA_KEYS : VIEW_TYPE_TEXT_INPUT;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            if (viewType == VIEW_TYPE_EXTRA_KEYS) {
                View view = inflater.inflate(R.layout.view_terminal_toolbar_extra_keys, parent, false);
                return new ExtraKeysViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.view_terminal_toolbar_text_input, parent, false);
                return new TextInputViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ExtraKeysViewHolder) {
                ExtraKeysViewHolder eh = (ExtraKeysViewHolder) holder;
                ExtraKeysView extraKeysView = (ExtraKeysView) eh.itemView;
                extraKeysView.setExtraKeysViewClient(mActivity.getTermuxTerminalExtraKeys());
                extraKeysView.setButtonTextAllCaps(mActivity.getProperties().shouldExtraKeysTextBeAllCaps());
                mActivity.setExtraKeysView(extraKeysView);
                extraKeysView.reload(mActivity.getTermuxTerminalExtraKeys().getExtraKeysInfo(),
                    mActivity.getTerminalToolbarDefaultHeight());

                if (mActivity.getProperties().isUsingFullScreen() && mActivity.getProperties().isUsingFullScreenWorkAround()) {
                    FullScreenWorkAround.apply(mActivity);
                }
            } else if (holder instanceof TextInputViewHolder) {
                TextInputViewHolder th = (TextInputViewHolder) holder;
                final EditText editText = th.editText;

                if (mSavedTextInput != null) {
                    editText.setText(mSavedTextInput);
                    mSavedTextInput = null;
                }

                editText.setOnEditorActionListener((v, actionId, event) -> {
                    TerminalSession session = mActivity.getCurrentSession();
                    if (session != null) {
                        if (session.isRunning()) {
                            String textToSend = editText.getText().toString();
                            if (textToSend.length() == 0) textToSend = "\r";
                            session.write(textToSend);
                        } else {
                            mActivity.getTermuxTerminalSessionClient().removeFinishedSession(session);
                        }
                        editText.setText("");
                    }
                    return true;
                });
            }
        }

        static class ExtraKeysViewHolder extends RecyclerView.ViewHolder {
            ExtraKeysViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

        static class TextInputViewHolder extends RecyclerView.ViewHolder {
            final EditText editText;

            TextInputViewHolder(@NonNull View itemView) {
                super(itemView);
                editText = itemView.findViewById(R.id.terminal_toolbar_text_input);
            }
        }
    }


    public static class OnPageChangeListener extends ViewPager2.OnPageChangeCallback {

        final TermuxActivity mActivity;
        final ViewPager2 mTerminalToolbarViewPager;

        public OnPageChangeListener(TermuxActivity activity, ViewPager2 viewPager) {
            this.mActivity = activity;
            this.mTerminalToolbarViewPager = viewPager;
        }

        @Override
        public void onPageSelected(int position) {
            if (position == 0) {
                mActivity.getTerminalView().requestFocus();
            } else {
                final EditText editText = mTerminalToolbarViewPager.findViewById(R.id.terminal_toolbar_text_input);
                if (editText != null) editText.requestFocus();
            }
        }

    }

}
