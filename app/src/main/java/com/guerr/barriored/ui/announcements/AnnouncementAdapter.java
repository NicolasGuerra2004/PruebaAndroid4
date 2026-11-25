package com.guerr.barriored.ui.announcements;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.guerr.barriored.R;
import com.guerr.barriored.databinding.ItemAnnouncementBinding;
import com.guerr.barriored.model.Announcement;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder> {

    public interface OnAnnouncementDeleteListener {
        void onDeleteAnnouncement(Announcement announcement);
    }

    private final List<Announcement> list = new ArrayList<>();
    private boolean isAdmin = false;
    private final OnAnnouncementDeleteListener deleteListener;

    public AnnouncementAdapter(OnAnnouncementDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setAnnouncements(List<Announcement> announcements) {
        list.clear();
        if (announcements != null) {
            list.addAll(announcements);
        }
        notifyDataSetChanged();
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAnnouncementBinding binding = ItemAnnouncementBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AnnouncementViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementViewHolder holder, int position) {
        holder.bind(list.get(position), isAdmin, deleteListener);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        private final ItemAnnouncementBinding binding;

        public AnnouncementViewHolder(ItemAnnouncementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Announcement announcement, boolean isAdmin, OnAnnouncementDeleteListener deleteListener) {
            Context context = itemView.getContext();

            binding.tvAnnouncementTitle.setText(announcement.getTitulo());
            binding.tvAnnouncementDesc.setText(announcement.getDescripcion());
            binding.tvAnnouncementAuthor.setText(announcement.getAutorNombre() != null ? announcement.getAutorNombre() : "Directiva BarrioRed");

            if (announcement.getFecha() > 0) {
                String formattedDate = DateFormat.format("dd MMM · HH:mm", new Date(announcement.getFecha())).toString();
                binding.tvAnnouncementDate.setText(formattedDate);
            } else {
                binding.tvAnnouncementDate.setText("Reciente");
            }

            if (announcement.isImportante()) {
                binding.tvAnnouncementBadge.setText(R.string.badge_urgent);
                binding.tvAnnouncementBadge.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.badge_urgent_bg));
                binding.tvAnnouncementBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_urgent));
            } else {
                binding.tvAnnouncementBadge.setText(R.string.badge_official);
                binding.tvAnnouncementBadge.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.badge_official_bg));
                binding.tvAnnouncementBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_official));
            }

            // Delete action (Admin only)
            if (isAdmin) {
                binding.btnDeleteAnnouncement.setVisibility(View.VISIBLE);
                binding.btnDeleteAnnouncement.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onDeleteAnnouncement(announcement);
                    }
                });
            } else {
                binding.btnDeleteAnnouncement.setVisibility(View.GONE);
                binding.btnDeleteAnnouncement.setOnClickListener(null);
            }
        }
    }
}
