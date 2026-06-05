package ru.mogcommunity.rbr_project.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ru.mogcommunity.rbr_project.data.model.ChatMessage;
import ru.mogcommunity.rbr_project.databinding.ItemChatMessageBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ChatViewHolder> {
    private final List<ChatMessage> messages;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatMessageAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatMessageBinding binding = ItemChatMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ChatViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateList(List<ChatMessage> newMessages) {
        messages.clear();
        if (newMessages != null) {
            messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatMessageBinding binding;

        ChatViewHolder(ItemChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage message) {
            String timeText = timeFormat.format(new Date(message.getTimestamp()));

            if ("user".equals(message.getSender())) {
                binding.layoutUserMessage.setVisibility(View.VISIBLE);
                binding.layoutAiMessage.setVisibility(View.GONE);
                binding.textUserMessage.setText(message.getText());
                binding.textUserTime.setText(timeText);
            } else {
                binding.layoutUserMessage.setVisibility(View.GONE);
                binding.layoutAiMessage.setVisibility(View.VISIBLE);
                binding.textAiMessage.setText(message.getText());
                binding.textAiTime.setText(timeText);
            }
        }
    }
}
