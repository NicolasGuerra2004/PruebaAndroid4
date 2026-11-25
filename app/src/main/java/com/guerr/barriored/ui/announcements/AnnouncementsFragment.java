package com.guerr.barriored.ui.announcements;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.guerr.barriored.R;
import com.guerr.barriored.databinding.FragmentAnnouncementsBinding;
import com.guerr.barriored.model.Announcement;
import com.guerr.barriored.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnnouncementsFragment extends Fragment implements AnnouncementAdapter.OnAnnouncementDeleteListener {

    private FragmentAnnouncementsBinding binding;
    private AnnouncementAdapter adapter;
    private DatabaseReference announcementsRef;
    private DatabaseReference userRef;
    private ValueEventListener announcementsListener;
    private ValueEventListener userRoleListener;
    private User currentUser;
    private boolean isAdmin = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAnnouncementsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        announcementsRef = FirebaseDatabase.getInstance().getReference("announcements");

        setupRecyclerView();
        checkUserRole();
        loadAnnouncements();
    }

    private void setupRecyclerView() {
        adapter = new AnnouncementAdapter(this);
        binding.rvAnnouncements.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAnnouncements.setAdapter(adapter);

        binding.fabAddAnnouncement.setOnClickListener(v -> showCreateAnnouncementDialog());
    }

    private void checkUserRole() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;

        userRef = FirebaseDatabase.getInstance().getReference("users").child(fUser.getUid());
        userRoleListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) return;
                currentUser = snapshot.getValue(User.class);
                isAdmin = currentUser != null && currentUser.isAdmin();

                binding.fabAddAnnouncement.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                binding.tvAnnouncementsRoleBadge.setText(isAdmin ? "ADMINISTRADOR" : "VECINO");
                adapter.setIsAdmin(isAdmin);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        userRef.addValueEventListener(userRoleListener);
    }

    private void loadAnnouncements() {
        binding.progressAnnouncements.setVisibility(View.VISIBLE);
        announcementsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) return;
                binding.progressAnnouncements.setVisibility(View.GONE);

                List<Announcement> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Announcement announcement = child.getValue(Announcement.class);
                    if (announcement != null) {
                        list.add(announcement);
                    }
                }

                // Sort descending by date (newest first)
                Collections.sort(list, (a1, a2) -> Long.compare(a2.getFecha(), a1.getFecha()));

                adapter.setAnnouncements(list);
                binding.tvEmptyAnnouncements.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded() || binding == null) return;
                binding.progressAnnouncements.setVisibility(View.GONE);
            }
        };

        announcementsRef.addValueEventListener(announcementsListener);
    }

    private void showCreateAnnouncementDialog() {
        Context context = getContext();
        if (context == null) return;

        BottomSheetDialog sheetDialog = new BottomSheetDialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_announcement, null);
        sheetDialog.setContentView(dialogView);

        EditText etTitle = dialogView.findViewById(R.id.etAnnouncementTitle);
        EditText etDesc = dialogView.findViewById(R.id.etAnnouncementDesc);
        MaterialSwitch switchUrgent = dialogView.findViewById(R.id.switchIsUrgent);

        dialogView.findViewById(R.id.btnCloseAnnouncementModal).setOnClickListener(v -> sheetDialog.dismiss());
        dialogView.findViewById(R.id.btnCancelAnnouncementModal).setOnClickListener(v -> sheetDialog.dismiss());
        dialogView.findViewById(R.id.btnPublishAnnouncementModal).setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
            boolean isUrgent = switchUrgent.isChecked();

            if (TextUtils.isEmpty(title)) {
                Toast.makeText(context, "El título del aviso no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }

            publishAnnouncement(title, desc, isUrgent);
            sheetDialog.dismiss();
        });

        sheetDialog.show();
    }

    private void publishAnnouncement(String title, String desc, boolean isUrgent) {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        String authorUid = fUser != null ? fUser.getUid() : "";
        String authorName = (currentUser != null && currentUser.getNombre() != null)
                ? currentUser.getNombre()
                : "Directiva BarrioRed";

        String id = announcementsRef.push().getKey();
        if (id == null) {
            id = "announcement_" + System.currentTimeMillis();
        }

        Announcement announcement = new Announcement(
                id,
                title,
                desc,
                System.currentTimeMillis(),
                authorName,
                authorUid,
                isUrgent
        );

        announcementsRef.child(id).setValue(announcement)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Aviso oficial publicado correctamente.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error al publicar aviso: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onDeleteAnnouncement(Announcement announcement) {
        Context context = getContext();
        if (context == null || announcement == null || announcement.getId() == null) return;

        new MaterialAlertDialogBuilder(context)
                .setTitle("Eliminar Aviso")
                .setMessage("¿Estás seguro de que deseas eliminar este aviso oficial? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    announcementsRef.child(announcement.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "Aviso eliminado", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (announcementsRef != null && announcementsListener != null) {
            announcementsRef.removeEventListener(announcementsListener);
        }
        if (userRef != null && userRoleListener != null) {
            userRef.removeEventListener(userRoleListener);
        }
        binding = null;
    }
}
