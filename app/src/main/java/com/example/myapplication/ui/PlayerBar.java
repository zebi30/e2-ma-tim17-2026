package com.example.myapplication.ui;

import android.app.Activity;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.logic.LeagueService;
import com.example.myapplication.model.User;

import java.util.Locale;

/**
 * Popunjava traku sa tokenima, zvezdama i ligom (view_player_bar.xml).
 * Spec. zahteva da ovi podaci budu vidljivi u svakom trenutku, i tokom partije.
 */
public final class PlayerBar {

    private static final String[] LEAGUE_ICONS = {"⚪", "🥉", "🥈", "🥇", "🔷", "💎"};
    private static final int[] LEAGUE_NAMES = {
            R.string.league_0, R.string.league_1, R.string.league_2,
            R.string.league_3, R.string.league_4, R.string.league_5
    };

    private PlayerBar() {
    }

    public static void bind(Activity activity, User user) {
        ((TextView) activity.findViewById(R.id.player_bar_tokens))
                .setText(activity.getString(R.string.player_bar_tokens_fmt, user.tokens));
        ((TextView) activity.findViewById(R.id.player_bar_stars))
                .setText(activity.getString(R.string.player_bar_stars_fmt, user.stars));

        int leagueIndex = LeagueService.leagueIndexForStars(user.stars);
        ((TextView) activity.findViewById(R.id.player_bar_league)).setText(String.format(
                Locale.getDefault(), "%s %s",
                LEAGUE_ICONS[leagueIndex], activity.getString(LEAGUE_NAMES[leagueIndex])));
    }
}
