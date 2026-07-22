package com.termux.app.terminal;

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.theme.ThemeUtils;
import com.termux.terminal.TerminalSession;

import java.util.List;

public class TermuxSessionsListViewController extends RecyclerView.Adapter<TermuxSessionsListViewController.SessionViewHolder> {

    final TermuxActivity mActivity;
    final List<TermuxSession> mSessionList;

    final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
    final StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);

    public TermuxSessionsListViewController(TermuxActivity activity, List<TermuxSession> sessionList) {
        this.mActivity = activity;
        this.mSessionList = sessionList;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.item_terminal_sessions_list, parent, false);
        return new SessionViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        TextView sessionTitleView = holder.sessionTitleView;

        TerminalSession sessionAtRow = mSessionList.get(position).getTerminalSession();
        if (sessionAtRow == null) {
            sessionTitleView.setText("null session");
            return;
        }

        String name = sessionAtRow.mSessionName;
        String sessionTitle = sessionAtRow.getTitle();

        String numberPart = "[" + (position + 1) + "] ";
        String sessionNamePart = (TextUtils.isEmpty(name) ? "" : name);
        String sessionTitlePart = (TextUtils.isEmpty(sessionTitle) ? "" : ((sessionNamePart.isEmpty() ? "" : "\n") + sessionTitle));

        String fullSessionTitle = numberPart + sessionNamePart + sessionTitlePart;
        SpannableString fullSessionTitleStyled = new SpannableString(fullSessionTitle);
        fullSessionTitleStyled.setSpan(boldSpan, 0, numberPart.length() + sessionNamePart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        fullSessionTitleStyled.setSpan(italicSpan, numberPart.length() + sessionNamePart.length(), fullSessionTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        sessionTitleView.setText(fullSessionTitleStyled);

        boolean sessionRunning = sessionAtRow.isRunning();

        if (sessionRunning) {
            sessionTitleView.setPaintFlags(sessionTitleView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            sessionTitleView.setPaintFlags(sessionTitleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        int defaultColor = ThemeUtils.getTextColorPrimary(mActivity);
        int color = sessionRunning || sessionAtRow.getExitStatus() == 0 ? defaultColor : ThemeUtils.getSystemAttrColor(mActivity, android.R.attr.colorError);
        sessionTitleView.setTextColor(color);

        holder.itemView.setOnClickListener(v -> {
            TermuxSession clickedSession = mSessionList.get(holder.getAdapterPosition());
            mActivity.getTermuxTerminalSessionClient().setCurrentSession(clickedSession.getTerminalSession());
            mActivity.getDrawer().closeDrawers();
        });

        holder.itemView.setOnLongClickListener(v -> {
            final TermuxSession selectedSession = mSessionList.get(holder.getAdapterPosition());
            mActivity.getTermuxTerminalSessionClient().renameSession(selectedSession.getTerminalSession());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return mSessionList.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        final TextView sessionTitleView;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            sessionTitleView = itemView.findViewById(R.id.session_title);
        }
    }
}
