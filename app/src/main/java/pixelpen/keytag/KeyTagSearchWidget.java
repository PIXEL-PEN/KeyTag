package pixelpen.keytag;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class KeyTagSearchWidget extends AppWidgetProvider {

    public static final String ACTION_SEARCH = "pixelpen.keytag.WIDGET_SEARCH";
    public static final String EXTRA_KEYWORD = "widget_keyword";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_search);

        // Tap search button — open KeyTag search
        Intent searchIntent = new Intent(context, WidgetSearchActivity.class);
        PendingIntent searchPending = PendingIntent.getActivity(
                context, widgetId, searchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.widgetSearchButton, searchPending);
        views.setOnClickPendingIntent(R.id.widgetSearchHint, searchPending);
        appWidgetManager.updateAppWidget(widgetId, views);
    }
}