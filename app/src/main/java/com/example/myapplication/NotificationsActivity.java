package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.data.NotificationRepository;
import com.example.myapplication.data.SessionManager;
import com.example.myapplication.model.AppNotification;

import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private static final String CH_CHAT    = "channel_chat";
    private static final String CH_RANKING = "channel_ranking";
    private static final String CH_REWARDS = "channel_rewards";
    private static final String CH_OTHER   = "channel_other";
    private static final int REQ_NOTIF_PERM = 3001;

    private NotificationRepository notifRepo;
    private SessionManager session;
    private long userId = -1;

    private ListView listView;
    private View emptyState;
    private NotificationAdapter adapter;

    private String pendingChannel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        notifRepo = new NotificationRepository(this);
        session = new SessionManager(this);
        userId = session.getUserId();

        createChannels();

        listView   = findViewById(R.id.notifications_list);
        emptyState = findViewById(R.id.empty_state);

        Button backBtn        = findViewById(R.id.back_button);
        Button markAllReadBtn = findViewById(R.id.mark_all_read_button);
        Button filterAll      = findViewById(R.id.filter_all);
        Button filterChat     = findViewById(R.id.filter_chat);
        Button filterRanking  = findViewById(R.id.filter_ranking);
        Button filterRewards  = findViewById(R.id.filter_rewards);
        Button filterUnread   = findViewById(R.id.filter_unread);
        Button filterRead     = findViewById(R.id.filter_read);
        Button btnTestChat    = findViewById(R.id.btn_test_chat);
        Button btnTestRanking = findViewById(R.id.btn_test_ranking);
        Button btnTestRewards = findViewById(R.id.btn_test_rewards);
        Button btnTestOther   = findViewById(R.id.btn_test_other);

        backBtn.setOnClickListener(v -> finish());

        markAllReadBtn.setOnClickListener(v -> {
            if (userId > 0) {
                notifRepo.markAllRead(userId);
                loadList(notifRepo.getAll(userId));
                Toast.makeText(this, R.string.all_marked_read, Toast.LENGTH_SHORT).show();
            }
        });

        filterAll.setOnClickListener(v -> loadList(notifRepo.getAll(userId)));
        filterChat.setOnClickListener(v -> loadList(notifRepo.getByChannel(userId, AppNotification.CHANNEL_CHAT)));
        filterRanking.setOnClickListener(v -> loadList(notifRepo.getByChannel(userId, AppNotification.CHANNEL_RANKING)));
        filterRewards.setOnClickListener(v -> loadList(notifRepo.getByChannel(userId, AppNotification.CHANNEL_REWARDS)));
        filterUnread.setOnClickListener(v -> loadList(notifRepo.getUnread(userId)));
        filterRead.setOnClickListener(v -> loadList(notifRepo.getReadOnly(userId)));

        btnTestChat.setOnClickListener(v -> sendAndSave(
                AppNotification.CHANNEL_CHAT, CH_CHAT,
                getString(R.string.notification_chat_title),
                getString(R.string.notification_chat_message)));

        btnTestRanking.setOnClickListener(v -> sendAndSave(
                AppNotification.CHANNEL_RANKING, CH_RANKING,
                getString(R.string.notification_ranking_title),
                getString(R.string.notification_ranking_message)));

        btnTestRewards.setOnClickListener(v -> sendAndSave(
                AppNotification.CHANNEL_REWARDS, CH_REWARDS,
                getString(R.string.notification_reward_title),
                getString(R.string.notification_reward_message)));

        btnTestOther.setOnClickListener(v -> sendAndSave(
                AppNotification.CHANNEL_OTHER, CH_OTHER,
                getString(R.string.notification_other_title),
                getString(R.string.notification_other_message)));

        listView.setOnItemClickListener((parent, view, position, id) -> {
            AppNotification n = adapter.getItem(position);
            if (n != null && !n.isRead) {
                notifRepo.markRead(n.id);
                n.isRead = true;
                adapter.notifyDataSetChanged();
            }
        });

        loadList(notifRepo.getAll(userId));
    }

    private void sendAndSave(String modelChannel, String systemChannel, String title, String message) {
        if (userId > 0) {
            notifRepo.insert(userId, modelChannel, title, message);
            loadList(notifRepo.getAll(userId));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                pendingChannel = systemChannel;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF_PERM);
                return;
            }
        }
        postSystemNotification(systemChannel, title, message);
    }

    @SuppressLint("MissingPermission")
    private void postSystemNotification(String channel, String title, String message) {
        Intent intent = new Intent(this, NotificationsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(this, channel.hashCode(), intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channel)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(channel.hashCode(), builder.build());
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        nm.createNotificationChannel(makeChannel(CH_CHAT,    getString(R.string.chat_notifications)));
        nm.createNotificationChannel(makeChannel(CH_RANKING, getString(R.string.ranking_notifications)));
        nm.createNotificationChannel(makeChannel(CH_REWARDS, getString(R.string.reward_notifications)));
        nm.createNotificationChannel(makeChannel(CH_OTHER,   getString(R.string.other_notifications)));
    }

    private NotificationChannel makeChannel(String id, String name) {
        NotificationChannel ch = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription(name);
        return ch;
    }

    private void loadList(List<AppNotification> list) {
        adapter = new NotificationAdapter(list);
        listView.setAdapter(adapter);
        boolean empty = list.isEmpty();
        listView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIF_PERM && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && pendingChannel != null) {
            postSystemNotification(pendingChannel,
                    getString(R.string.test_notification_title),
                    getString(R.string.test_notification_message));
            pendingChannel = null;
        } else {
            Toast.makeText(this, R.string.notif_permission_denied, Toast.LENGTH_SHORT).show();
        }
    }

    private class NotificationAdapter extends ArrayAdapter<AppNotification> {

        NotificationAdapter(List<AppNotification> items) {
            super(NotificationsActivity.this, R.layout.item_notification, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_notification, parent, false);
            }
            AppNotification n = getItem(position);
            if (n == null) return convertView;

            ((TextView) convertView.findViewById(R.id.notif_icon)).setText(n.channelIcon());
            ((TextView) convertView.findViewById(R.id.notif_title)).setText(n.title);
            ((TextView) convertView.findViewById(R.id.notif_message)).setText(n.message);
            ((TextView) convertView.findViewById(R.id.notif_time)).setText(n.formattedTime());

            View dot = convertView.findViewById(R.id.unread_dot);
            dot.setVisibility(n.isRead ? View.INVISIBLE : View.VISIBLE);

            convertView.setAlpha(n.isRead ? 0.6f : 1f);

            return convertView;
        }
    }
}
