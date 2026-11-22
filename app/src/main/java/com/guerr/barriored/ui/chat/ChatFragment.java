package com.guerr.barriored.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.guerr.barriored.databinding.FragmentChatBinding;
import com.guerr.barriored.model.ChatMessage;
import com.guerr.barriored.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatAdapter adapter;
    private DatabaseReference chatRef;
    private DatabaseReference userRef;
    private ValueEventListener chatListener;
    private User currentUser;
    private String currentUid = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser != null) {
            currentUid = fUser.getUid();
        }

        chatRef = FirebaseDatabase.getInstance().getReference("chat");

        setupRecyclerView();
        loadCurrentUserData();
        loadChatMessages();
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(currentUid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        binding.rvChat.setLayoutManager(layoutManager);
        binding.rvChat.setAdapter(adapter);

        binding.btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void loadCurrentUserData() {
        if (currentUid.isEmpty()) return;
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentUser = snapshot.getValue(User.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadChatMessages() {
        binding.progressChat.setVisibility(View.VISIBLE);
        chatListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) return;
                binding.progressChat.setVisibility(View.GONE);

                List<ChatMessage> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ChatMessage msg = child.getValue(ChatMessage.class);
                    if (msg != null) {
                        list.add(msg);
                    }
                }

                // Sort by timestamp ascending (oldest first, newest at the bottom)
                Collections.sort(list, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));

                adapter.setMessages(list);
                binding.tvEmptyChat.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

                if (!list.isEmpty()) {
                    binding.rvChat.smoothScrollToPosition(list.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded() || binding == null) return;
                binding.progressChat.setVisibility(View.GONE);
            }
        };

        chatRef.limitToLast(50).addValueEventListener(chatListener);
    }

    private void sendMessage() {
        String text = binding.etChatMessage.getText() != null ? binding.etChatMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) {
            return;
        }

        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        // Identidad exacta requerida: Nombre · #311A
        String senderName = (currentUser != null)
                ? currentUser.getIdentidad()
                : (fUser != null && fUser.getEmail() != null ? fUser.getEmail() + " · #S/N" : "Vecino · #S/N");

        String msgId = chatRef.push().getKey();
        if (msgId == null) {
            msgId = "msg_" + System.currentTimeMillis();
        }

        ChatMessage message = new ChatMessage(
                msgId,
                currentUid,
                senderName,
                text,
                System.currentTimeMillis()
        );

        binding.etChatMessage.setText("");

        chatRef.child(msgId).setValue(message)
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error al enviar mensaje: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatRef != null && chatListener != null) {
            chatRef.removeEventListener(chatListener);
        }
        binding = null;
    }
}
