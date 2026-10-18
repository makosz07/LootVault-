package com.macoshapps.lootvault;

import static com.macoshapps.lootvault.Theme.ACCENT;
import static com.macoshapps.lootvault.Theme.ACCENT_ON;
import static com.macoshapps.lootvault.Theme.BG;
import static com.macoshapps.lootvault.Theme.MUTED;
import static com.macoshapps.lootvault.Theme.NAV;
import static com.macoshapps.lootvault.Theme.NEGATIVE;
import static com.macoshapps.lootvault.Theme.POSITIVE;
import static com.macoshapps.lootvault.Theme.STROKE;
import static com.macoshapps.lootvault.Theme.SURFACE;
import static com.macoshapps.lootvault.Theme.SURFACE_HI;
import static com.macoshapps.lootvault.Theme.TEXT;
import static com.macoshapps.lootvault.Theme.box;
import static com.macoshapps.lootvault.Theme.dp;
import static com.macoshapps.lootvault.Theme.gradient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.macoshapps.lootvault.domain.AssetMath;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class MainActivity extends Activity {
    private static final int REQ_IMAGE = 13;
    private static final int REQ_EXPORT = 11;
    private static final int REQ_IMPORT = 12;
    private static final String[] LANG_CODES = {"pl", "en", "es"};

    private VaultStore store;
    private String lang = "pl";
    private String tab = "home";
    private String returnTo = "assets";
    private String typeFilter = "";
    private String searchText = "";
    private int sortMode = 0;
    private String detailId = "";
    private Consumer<String> imageCallback;
    private int imageMaxSide = 1024;
    private String pendingExport;

    private LinearLayout root;
    private ScrollView scroll;
    private LinearLayout body;
    private LinearLayout navPill;
    private LinearLayout listHost;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            Theme.density = getResources().getDisplayMetrics().density;
            store = new VaultStore(this);
            lang = store.language();
            configureWindow();
            buildShell();
            String recovered = store.consumeRecoveredFile();
            open("home");
            if (recovered != null) {
                toast(tr("Nie udało się odczytać danych. Oryginał zapisano jako " + recovered + ".",
                        "The saved data could not be read. The original was kept as " + recovered + ".",
                        "No se pudieron leer los datos. El original se guardó como " + recovered + "."));
            }
        } catch (Exception startupError) {
            android.util.Log.e("LootVault", "Startup failed; user data left intact", startupError);
            showStartupFailure(startupError);
        }
    }

    /** Show the actual startup exception without deleting or resetting the user's vault. */
    private void showStartupFailure(Exception error) {
        LinearLayout diagnostic = new LinearLayout(this);
        diagnostic.setOrientation(LinearLayout.VERTICAL);
        diagnostic.setPadding(32, 40, 32, 32);
        diagnostic.setBackgroundColor(0xFF0B1220);
        TextView title = new TextView(this);
        title.setText("LootVault — błąd uruchamiania");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(21);
        diagnostic.addView(title);
        TextView message = new TextView(this);
        StringBuilder details = new StringBuilder(error.toString());
        for (StackTraceElement frame : error.getStackTrace()) {
            if (details.length() > 3500) break;
            details.append("\\n").append(frame.toString());
        }
        message.setText(details.toString());
        message.setTextColor(0xFFB5C8E8);
        message.setTextSize(12);
        message.setTextIsSelectable(true);
        diagnostic.addView(message);
        android.widget.Button copy = new android.widget.Button(this);
        copy.setText("Kopiuj szczegóły błędu");
        copy.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText("LootVault startup error", details.toString()));
            Toast.makeText(this, "Skopiowano", Toast.LENGTH_SHORT).show();
        });
        diagnostic.addView(copy);
        setContentView(diagnostic);
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 30) {
            // The decor view is not attached during Activity.onCreate. Calling
            // Window.getInsetsController() here crashes on some Android 15/16 devices.
            window.setDecorFitsSystemWindows(false);
            View decor = window.getDecorView();
            decor.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                WindowInsetsController controller = decor.getWindowInsetsController();
                if (controller != null) {
                    controller.setSystemBarsAppearance(0,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                }
            });
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    /** Builds the fixed shell once: scrollable body above a floating bottom bar, both inside system insets. */
    private void buildShell() {
        root = column();
        root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            root.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        body = column();
        body.setPadding(dp(20), dp(14), dp(20), dp(24));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        navPill = row();
        navPill.setPadding(dp(6), dp(6), dp(6), dp(6));
        navPill.setBackground(box(NAV, 28, STROKE));
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(-1, -2);
        navParams.setMargins(dp(14), dp(8), dp(14), dp(12));
        root.addView(navPill, navParams);

        setContentView(root);
        root.requestApplyInsets();
    }

    private void open(String page) {
        if (!page.equals(tab)) scroll.scrollTo(0, 0);
        tab = page;
        body.removeAllViews();
        switch (page) {
            case "assets": renderAssets(); break;
            case "list": renderList(); break;
            case "detail": renderDetail(); break;
            case "stats": renderStats(); break;
            case "settings": renderSettings(); break;
            default: renderHome(); break;
        }
        renderNav();
    }

    private void renderNav() {
        navPill.removeAllViews();
        String active = (tab.equals("detail") || tab.equals("list")) ? "assets" : tab;
        String[] keys = {"home", "assets", "stats", "settings"};
        String[] labels = {
            tr("Pulpit", "Home", "Inicio"),
            tr("Zasoby", "Assets", "Recursos"),
            tr("Statystyki", "Stats", "Estadísticas"),
            tr("Ustawienia", "Settings", "Ajustes")
        };
        String[] icons = {"⌂", "▦", "▥", "⚙"};
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            boolean on = key.equals(active);
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, dp(8), 0, dp(8));
            item.setBackground(box(on ? 0x337AA6FF : 0x00000000, 20, 0));
            TextView icon = text(icons[i], 20, on ? ACCENT : MUTED, true);
            icon.setGravity(Gravity.CENTER);
            item.addView(icon);
            TextView label = text(labels[i], 11, on ? TEXT : MUTED, on);
            label.setSingleLine(true);
            label.setGravity(Gravity.CENTER);
            item.addView(label);
            item.setOnClickListener(v -> open(key));
            navPill.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
        }
    }

    // ---- screens ---------------------------------------------------------------------------

    private void renderHome() {
        header(tr("Twój majątek", "Your wealth", "Tu patrimonio"), tr("Mój skarbiec", "My vault", "Mi tesoro"));
        summaryCard(tr("Łączna wartość majątku", "Total asset value", "Valor total del patrimonio"),
                store.totalValue(), store.totalCost());

        LinearLayout stats = row();
        stats.addView(statTile(tr("Zasobów", "Assets", "Recursos"), String.valueOf(store.realCount())), slot(0, 2, 12));
        stats.addView(statTile(tr("Typów", "Types", "Tipos"), String.valueOf(store.types().length())), slot(1, 2, 12));
        add(body, stats);
        gap(body, 14);
        add(body, primaryButton(tr("+ Dodaj zasób", "+ Add asset", "+ Añadir recurso"), this::chooseType));

        section(tr("Historia wartości", "Value history", "Historial de valor"));
        chartCard(tr("Wykres pojawi się po kolejnych zmianach wartości.",
                "The chart appears after the next value change.",
                "El gráfico aparecerá tras el próximo cambio de valor."));

        section(tr("Ostatnio dodane", "Recently added", "Añadidos recientemente"));
        JSONArray items = store.items();
        int shown = 0;
        for (int i = items.length() - 1; i >= 0 && shown < 4; i--) {
            JSONObject x = items.optJSONObject(i);
            if (x == null || x.optBoolean("demo", false)) continue;
            add(body, assetRow(x, "home"));
            gap(body, 10);
            shown++;
        }
        if (shown == 0) {
            add(body, emptyState(tr("Dodaj pierwszy zasób, aby zacząć śledzić majątek.",
                    "Add your first asset to start tracking your wealth.",
                    "Añade tu primer recurso para empezar a seguir tu patrimonio.")));
        }
    }

    private void renderAssets() {
        header(tr("Twoja kolekcja", "Your collection", "Tu colección"), tr("Zasoby", "Assets", "Recursos"));

        LinearLayout all = row();
        all.setPadding(dp(20), dp(20), dp(20), dp(20));
        all.setBackground(gradient(0xFF3D4FA8, 0xFF1C2A5E, 24));
        LinearLayout left = column();
        left.addView(text(tr("Wszystkie zasoby", "All assets", "Todos los recursos"), 20, TEXT, true));
        gap(left, 6);
        left.addView(text(store.realCount() + " · " + money(store.totalValue()), 14, 0xCCFFFFFF, false));
        all.addView(left, new LinearLayout.LayoutParams(0, -2, 1f));
        all.addView(text("→", 26, TEXT, true));
        all.setOnClickListener(v -> {
            typeFilter = "";
            searchText = "";
            open("list");
        });
        add(body, all);
        gap(body, 18);

        JSONArray types = store.types();
        LinearLayout current = null;
        for (int i = 0; i <= types.length(); i++) {
            if (i % 2 == 0) {
                current = row();
                add(body, current);
                gap(body, 12);
            }
            JSONObject type = i < types.length() ? types.optJSONObject(i) : null;
            current.addView(typeCell(type), slot(i % 2, 2, 12));
        }
        gap(body, 6);
        add(body, text(tr("Przytrzymaj kafelek, aby zmienić nazwę lub okładkę.",
                "Long-press a tile to change its name or cover.",
                "Mantén pulsada una tarjeta para cambiar su nombre o portada."), 12, MUTED, false));
    }

    private void renderList() {
        if (!typeFilter.isEmpty() && store.typeById(typeFilter) == null) typeFilter = "";
        String title = typeFilter.isEmpty()
                ? tr("Wszystkie zasoby", "All assets", "Todos los recursos")
                : store.typeName(typeFilter);
        header(tr("Kolekcja", "Collection", "Colección"), title);

        add(body, primaryButton(tr("+ Dodaj zasób", "+ Add asset", "+ Añadir recurso"), () -> {
            if (typeFilter.isEmpty()) chooseType();
            else assetForm(typeFilter, null);
        }));
        gap(body, 14);

        EditText search = input(tr("Szukaj po nazwie, kategorii lub miejscu",
                "Search by name, category or location",
                "Buscar por nombre, categoría o lugar"), searchText);
        search.addTextChangedListener(watch(() -> {
            searchText = search.getText().toString();
            renderListItems();
        }));
        add(body, search);
        gap(body, 12);

        String[] labels = {
            tr("Najnowsze", "Newest", "Recientes"),
            tr("Nazwa", "Name", "Nombre"),
            tr("Wartość", "Value", "Valor"),
            tr("Zysk %", "Return %", "Rentab. %")
        };
        LinearLayout sorts = row();
        for (int i = 0; i < labels.length; i++) {
            final int mode = i;
            boolean on = mode == sortMode;
            TextView sortChip = text(labels[i], 13, on ? ACCENT_ON : MUTED, true);
            sortChip.setGravity(Gravity.CENTER);
            sortChip.setSingleLine(true);
            sortChip.setPadding(dp(6), dp(9), dp(6), dp(9));
            sortChip.setBackground(box(on ? ACCENT : SURFACE, 14, on ? 0 : STROKE));
            sortChip.setOnClickListener(v -> {
                sortMode = mode;
                open("list");
            });
            sorts.addView(sortChip, slot(i, labels.length, 8));
        }
        add(body, sorts);
        gap(body, 16);

        listHost = column();
        add(body, listHost);
        renderListItems();
    }

    private void renderListItems() {
        listHost.removeAllViews();
        List<JSONObject> rows = filteredItems();
        if (rows.isEmpty()) {
            add(listHost, emptyState(tr("Brak pasujących zasobów", "No matching assets", "Sin recursos coincidentes")));
            return;
        }
        for (JSONObject x : rows) {
            add(listHost, assetRow(x, "list"));
            gap(listHost, 10);
        }
    }

    private List<JSONObject> filteredItems() {
        String query = searchText.trim().toLowerCase(Locale.ROOT);
        List<JSONObject> rows = new ArrayList<>();
        JSONArray items = store.items();
        for (int i = items.length() - 1; i >= 0; i--) {
            JSONObject x = items.optJSONObject(i);
            if (x == null) continue;
            if (!typeFilter.isEmpty() && !typeFilter.equals(x.optString("type"))) continue;
            String haystack = (x.optString("name") + " " + x.optString("category") + " " + x.optString("location"))
                    .toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !haystack.contains(query)) continue;
            rows.add(x);
        }
        Comparator<JSONObject> order = null;
        if (sortMode == 1) {
            order = (a, b) -> a.optString("name").compareToIgnoreCase(b.optString("name"));
        } else if (sortMode == 2) {
            order = (a, b) -> VaultStore.value(b).compareTo(VaultStore.value(a));
        } else if (sortMode == 3) {
            order = (a, b) -> {
                BigDecimal pa = AssetMath.returnPercent(VaultStore.cost(a), VaultStore.value(a));
                BigDecimal pb = AssetMath.returnPercent(VaultStore.cost(b), VaultStore.value(b));
                if (pa == null && pb == null) return 0;
                if (pa == null) return 1;
                if (pb == null) return -1;
                return pb.compareTo(pa);
            };
        }
        if (order != null) Collections.sort(rows, order);
        return rows;
    }

    private void renderDetail() {
        final JSONObject x = store.itemById(detailId);
        if (x == null) {
            open("assets");
            return;
        }
        final String id = x.optString("id");
        String typeId = x.optString("type");
        header(store.typeName(typeId), x.optString("name"));

        ImageView hero = new ImageView(this);
        hero.setScaleType(ImageView.ScaleType.CENTER_CROP);
        hero.setBackground(box(SURFACE, 26, 0));
        hero.setClipToOutline(true);
        Bitmap photo = decodeOrNull(x.optString("photoData"), 1024);
        if (photo != null) hero.setImageBitmap(photo);
        else hero.setImageDrawable(new CoverArt(store.typeName(typeId), typeId.hashCode()));
        body.addView(hero, new LinearLayout.LayoutParams(-1, dp(230)));
        gap(body, 16);

        BigDecimal value = VaultStore.value(x);
        BigDecimal cost = VaultStore.cost(x);
        BigDecimal gain = AssetMath.gain(cost, value);
        BigDecimal pct = AssetMath.returnPercent(cost, value);

        LinearLayout valueCard = column();
        valueCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        valueCard.setBackground(box(SURFACE, 22, STROKE));
        valueCard.addView(text(tr("Aktualna wartość", "Current value", "Valor actual"), 13, MUTED, false));
        gap(valueCard, 6);
        valueCard.addView(text(money(value), 30, TEXT, true));
        gap(valueCard, 10);
        valueCard.addView(chip(signedMoney(gain) + "  " + percent(pct), gainColor(gain)));
        add(body, valueCard);
        gap(body, 12);

        LinearLayout r1 = row();
        r1.addView(infoTile(tr("Koszt zakupu", "Purchase cost", "Coste de compra"), money(cost)), slot(0, 2, 12));
        r1.addView(infoTile(tr("Ilość", "Quantity", "Cantidad"),
                VaultStore.quantity(x).stripTrailingZeros().toPlainString()), slot(1, 2, 12));
        add(body, r1);
        gap(body, 12);

        LinearLayout r2 = row();
        r2.addView(infoTile(tr("Wartość za sztukę", "Value per unit", "Valor por unidad"),
                money(VaultStore.unitValue(x))), slot(0, 2, 12));
        String location = x.optString("location");
        r2.addView(infoTile(tr("Miejsce", "Location", "Ubicación"), location.isEmpty() ? "—" : location), slot(1, 2, 12));
        add(body, r2);
        gap(body, 12);

        String category = x.optString("category");
        String notes = x.optString("notes");
        if (!category.isEmpty() || !notes.isEmpty()) {
            LinearLayout info = column();
            info.setPadding(dp(18), dp(16), dp(18), dp(16));
            info.setBackground(box(SURFACE, 20, STROKE));
            if (!category.isEmpty()) {
                info.addView(text(tr("Kategoria", "Category", "Categoría"), 12, MUTED, false));
                info.addView(text(category, 15, TEXT, false));
            }
            if (!notes.isEmpty()) {
                if (!category.isEmpty()) gap(info, 12);
                info.addView(text(tr("Notatki", "Notes", "Notas"), 12, MUTED, false));
                info.addView(text(notes, 15, TEXT, false));
            }
            add(body, info);
            gap(body, 12);
        }

        section(tr("Historia wycen", "Valuation history", "Historial de valoraciones"));
        JSONArray history = x.optJSONArray("history");
        if (history == null || history.length() == 0) {
            add(body, emptyState(tr("Brak wycen.", "No valuations yet.", "Sin valoraciones.")));
        } else {
            SimpleDateFormat dateTime = dateFormat("dd.MM.yyyy HH:mm");
            for (int i = history.length() - 1; i >= 0; i--) {
                JSONObject entry = history.optJSONObject(i);
                if (entry == null) continue;
                BigDecimal entryValue = VaultStore.decimal(entry, "value");
                LinearLayout r = row();
                r.setPadding(dp(16), dp(14), dp(16), dp(14));
                r.setBackground(box(SURFACE, 18, STROKE));
                LinearLayout when = column();
                when.addView(text(dateTime.format(new Date(entry.optLong("time"))), 12, MUTED, false));
                when.addView(text(money(entryValue), 17, TEXT, true));
                r.addView(when, new LinearLayout.LayoutParams(0, -2, 1f));
                if (i > 0) {
                    JSONObject previous = history.optJSONObject(i - 1);
                    if (previous != null) {
                        BigDecimal delta = AssetMath.gain(VaultStore.decimal(previous, "value"), entryValue);
                        r.addView(chip(signedMoney(delta), gainColor(delta)));
                    }
                }
                add(body, r);
                gap(body, 8);
            }
        }

        gap(body, 18);
        add(body, primaryButton(tr("+ Nowa wycena", "+ New valuation", "+ Nueva valoración"),
                () -> valuationDialog(x)));
        gap(body, 10);
        LinearLayout actions = row();
        actions.addView(secondaryButton(tr("Edytuj", "Edit", "Editar"),
                () -> assetForm(x.optString("type"), x)), slot(0, 2, 10));
        actions.addView(secondaryButton(tr("Zdjęcie", "Photo", "Foto"),
                () -> pickImage(encoded -> updatePhoto(x, encoded), 1024)), slot(1, 2, 10));
        add(body, actions);
        gap(body, 12);
        add(body, dangerButton(tr("Usuń zasób", "Delete asset", "Eliminar recurso"), () ->
                confirm(tr("Usunąć ten zasób?", "Delete this asset?", "¿Eliminar este recurso?"),
                        tr("Usuń", "Delete", "Eliminar"), () -> {
                            persist(store.deleteItem(id));
                            open(returnTo);
                        })));
    }

    private void renderStats() {
        header(tr("Analiza majątku", "Wealth analytics", "Análisis patrimonial"),
                tr("Statystyki", "Statistics", "Estadísticas"));
        BigDecimal value = store.totalValue();
        BigDecimal cost = store.totalCost();
        summaryCard(tr("Łączna wartość", "Total value", "Valor total"), value, cost);

        section(tr("Historia wartości", "Value history", "Historial del patrimonio"));
        chartCard(tr("Wykres pojawi się po kolejnych zmianach wartości.",
                "The chart appears after the next value change.",
                "El gráfico aparecerá tras el próximo cambio de valor."));

        section(tr("Podział majątku", "Asset breakdown", "Desglose del patrimonio"));
        JSONArray types = store.types();
        boolean any = false;
        for (int i = 0; i < types.length(); i++) {
            JSONObject t = types.optJSONObject(i);
            if (t == null) continue;
            final String id = t.optString("id");
            BigDecimal typeValue = store.totalValue(id);
            BigDecimal typeCost = store.totalCost(id);
            BigDecimal typeGain = AssetMath.gain(typeCost, typeValue);
            BigDecimal share = AssetMath.share(typeValue, value);
            if (typeValue.signum() != 0 || typeCost.signum() != 0) any = true;

            LinearLayout card = column();
            card.setPadding(dp(18), dp(16), dp(18), dp(16));
            card.setBackground(box(SURFACE, 20, STROKE));
            card.setOnClickListener(v -> {
                typeFilter = id;
                searchText = "";
                open("list");
            });
            LinearLayout top = row();
            LinearLayout names = column();
            names.addView(text(t.optString("name"), 16, TEXT, true));
            names.addView(text(store.realCount(id) + " " + tr("zasobów", "assets", "recursos"), 12, MUTED, false));
            top.addView(names, new LinearLayout.LayoutParams(0, -2, 1f));
            top.addView(text(money(typeValue), 15, TEXT, true));
            card.addView(top);
            gap(card, 12);
            card.addView(bar(share), new LinearLayout.LayoutParams(-1, dp(10)));
            gap(card, 12);
            LinearLayout bottom = row();
            bottom.addView(chip(share == null ? "—" : percentText(share), ACCENT));
            View spacer = new View(this);
            bottom.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
            bottom.addView(chip(signedMoney(typeGain) + "  " + percent(AssetMath.returnPercent(typeCost, typeValue)),
                    gainColor(typeGain)));
            card.addView(bottom);
            add(body, card);
            gap(body, 10);
        }
        if (!any) {
            add(body, emptyState(tr("Dodaj zasoby z wartością, aby zobaczyć podział.",
                    "Add valued assets to see the breakdown.",
                    "Añade recursos con valor para ver el desglose.")));
        }
    }

    private void renderSettings() {
        header(tr("Personalizacja", "Preferences", "Preferencias"), tr("Ustawienia", "Settings", "Ajustes"));

        section(tr("Język", "Language", "Idioma"));
        String[] names = {"Polski", "English", "Español"};
        LinearLayout langCard = column();
        langCard.setBackground(box(SURFACE, 20, STROKE));
        for (int i = 0; i < LANG_CODES.length; i++) {
            final String code = LANG_CODES[i];
            boolean on = code.equals(lang);
            LinearLayout r = row();
            r.setPadding(dp(18), dp(16), dp(18), dp(16));
            r.addView(text(names[i], 16, TEXT, on), new LinearLayout.LayoutParams(0, -2, 1f));
            r.addView(text(on ? "✓" : "", 18, ACCENT, true));
            r.setOnClickListener(v -> {
                if (!code.equals(lang)) {
                    lang = code;
                    persist(store.setLanguage(code));
                }
                open("settings");
            });
            add(langCard, r);
            if (i < LANG_CODES.length - 1) divider(langCard);
        }
        add(body, langCard);

        section(tr("Kopia zapasowa", "Backup", "Copia de seguridad"));
        LinearLayout backupCard = column();
        backupCard.setBackground(box(SURFACE, 20, STROKE));
        add(backupCard, settingsRow(tr("Eksportuj JSON", "Export JSON", "Exportar JSON"),
                tr("Zapisuje zasoby, wyceny, historię i zdjęcia. Plik może być duży.",
                        "Saves assets, valuations, history and photos. The file can be large.",
                        "Guarda recursos, valoraciones, historial y fotos. El archivo puede ser grande."),
                this::exportBackup));
        divider(backupCard);
        add(backupCard, settingsRow(tr("Importuj JSON", "Import JSON", "Importar JSON"),
                tr("Najpierw pokażemy podgląd. Obecne dane zostaną zastąpione.",
                        "You will see a preview first. Current data will be replaced.",
                        "Verás una vista previa antes. Los datos actuales se reemplazarán."),
                this::pickImport));
        add(body, backupCard);

        if (store.hasDemoData()) {
            section(tr("Dane", "Data", "Datos"));
            LinearLayout dataCard = column();
            dataCard.setBackground(box(SURFACE, 20, STROKE));
            add(dataCard, settingsRow(tr("Usuń dane przykładowe", "Remove sample data", "Eliminar datos de ejemplo"),
                    tr("Usuwa oznaczone przykłady. Twoje zasoby zostaną.",
                            "Removes the marked samples. Your assets stay.",
                            "Elimina los ejemplos marcados. Tus recursos se mantienen."),
                    () -> confirm(tr("Usunąć dane przykładowe?", "Remove sample data?", "¿Eliminar datos de ejemplo?"),
                            tr("Usuń", "Remove", "Eliminar"), () -> {
                                persist(store.deleteDemoData());
                                open("settings");
                            })));
            add(body, dataCard);
        }

        section(tr("O aplikacji", "About", "Acerca de"));
        LinearLayout about = column();
        about.setPadding(dp(18), dp(16), dp(18), dp(16));
        about.setBackground(box(SURFACE, 20, STROKE));
        about.addView(text("LootVault", 18, TEXT, true));
        gap(about, 4);
        about.addView(text("Macosh Apps · " + tr("wersja ", "version ", "versión ") + appVersion(), 13, MUTED, false));
        add(body, about);
    }

    // ---- components ------------------------------------------------------------------------

    private void header(String eyebrow, String title) {
        TextView small = text(eyebrow.toUpperCase(Locale.ROOT), 11, ACCENT, true);
        small.setLetterSpacing(0.12f);
        add(body, small);
        gap(body, 6);
        add(body, text(title, 30, TEXT, true));
        gap(body, 18);
    }

    private void section(String title) {
        gap(body, 18);
        add(body, text(title, 18, TEXT, true));
        gap(body, 10);
    }

    private void summaryCard(String title, BigDecimal value, BigDecimal cost) {
        BigDecimal gain = AssetMath.gain(cost, value);
        LinearLayout hero = column();
        hero.setPadding(dp(22), dp(22), dp(22), dp(22));
        hero.setBackground(gradient(0xFF2F5FC4, 0xFF18264F, 26));
        hero.addView(text(title, 14, 0xCCFFFFFF, false));
        gap(hero, 6);
        hero.addView(text(money(value), 34, TEXT, true));
        gap(hero, 14);
        hero.addView(chip(signedMoney(gain) + "  ·  " + percent(AssetMath.returnPercent(cost, value)), gainColor(gain)));
        gap(hero, 10);
        hero.addView(text(tr("Koszt zakupu: ", "Cost: ", "Coste: ") + money(cost), 13, 0xB3FFFFFF, false));
        add(body, hero);
        gap(body, 14);
    }

    private void chartCard(String emptyMessage) {
        List<WealthChart.Point> points = snapshotPoints();
        if (points.size() < 2) {
            add(body, emptyState(emptyMessage));
            return;
        }
        LinearLayout card = column();
        card.setPadding(dp(10), dp(14), dp(10), dp(10));
        card.setBackground(box(SURFACE, 22, STROKE));
        card.addView(new WealthChart(this, points, moneyFormat(), dateFormat("d MMM")),
                new LinearLayout.LayoutParams(-1, dp(200)));
        add(body, card);
    }

    private List<WealthChart.Point> snapshotPoints() {
        List<WealthChart.Point> out = new ArrayList<>();
        JSONArray snapshots = store.snapshots();
        for (int i = 0; i < snapshots.length(); i++) {
            JSONObject p = snapshots.optJSONObject(i);
            if (p != null) out.add(new WealthChart.Point(p.optLong("time"), VaultStore.decimal(p, "value").doubleValue()));
        }
        return out;
    }

    private LinearLayout statTile(String label, String value) {
        LinearLayout tile = column();
        tile.setPadding(dp(16), dp(14), dp(16), dp(14));
        tile.setBackground(box(SURFACE, 18, STROKE));
        tile.addView(text(label, 12, MUTED, false));
        gap(tile, 6);
        tile.addView(text(value, 22, TEXT, true));
        return tile;
    }

    private View typeCell(JSONObject type) {
        LinearLayout card = column();
        card.setBackground(box(SURFACE, 22, STROKE));
        card.setClipToOutline(true);
        if (type == null) {
            LinearLayout empty = column();
            empty.setGravity(Gravity.CENTER);
            empty.setMinimumHeight(dp(190));
            TextView plus = text("+", 36, ACCENT, true);
            plus.setGravity(Gravity.CENTER);
            empty.addView(plus);
            TextView label = text(tr("Nowy typ", "New type", "Nuevo tipo"), 15, TEXT, true);
            label.setGravity(Gravity.CENTER);
            empty.addView(label);
            card.addView(empty);
            card.setOnClickListener(v -> newType());
            return card;
        }
        final String id = type.optString("id");
        final String name = type.optString("name");
        ImageView cover = new ImageView(this);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap photo = decodeOrNull(type.optString("coverData"), 512);
        if (photo != null) cover.setImageBitmap(photo);
        else cover.setImageDrawable(new CoverArt(name, id.hashCode()));
        card.addView(cover, new LinearLayout.LayoutParams(-1, dp(138)));

        LinearLayout details = column();
        details.setPadding(dp(14), dp(12), dp(14), dp(14));
        details.addView(text(name, 16, TEXT, true));
        gap(details, 4);
        details.addView(text(store.realCount(id) + " " + tr("zasobów", "assets", "recursos")
                + " · " + money(store.totalValue(id)), 12, MUTED, false));
        card.addView(details);

        card.setOnClickListener(v -> {
            typeFilter = id;
            searchText = "";
            open("list");
        });
        card.setOnLongClickListener(v -> {
            typeDialog(type);
            return true;
        });
        return card;
    }

    private LinearLayout assetRow(JSONObject x, String from) {
        final String id = x.optString("id");
        BigDecimal value = VaultStore.value(x);
        BigDecimal cost = VaultStore.cost(x);
        BigDecimal gain = AssetMath.gain(cost, value);
        BigDecimal pct = AssetMath.returnPercent(cost, value);

        LinearLayout row = row();
        row.setPadding(dp(10), dp(10), dp(14), dp(10));
        row.setBackground(box(SURFACE, 20, STROKE));
        row.addView(thumb(x), new LinearLayout.LayoutParams(dp(76), dp(76)));

        LinearLayout info = column();
        info.setPadding(dp(14), 0, dp(8), 0);
        TextView name = text(x.optString("name"), 16, TEXT, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(name);
        gap(info, 3);
        String subtitle = store.typeName(x.optString("type"));
        String location = x.optString("location");
        if (!location.isEmpty()) subtitle += " · " + location;
        TextView sub = text(subtitle, 12, MUTED, false);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(sub);
        gap(info, 8);
        info.addView(chip(signedMoney(gain) + "  " + percent(pct), gainColor(gain)));
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

        LinearLayout price = column();
        price.setGravity(Gravity.END);
        price.addView(text(money(value), 15, TEXT, true));
        if (x.optBoolean("demo", false)) price.addView(text("DEMO", 10, ACCENT, true));
        row.addView(price, new LinearLayout.LayoutParams(-2, -2));

        row.setOnClickListener(v -> openDetail(id, from));
        return row;
    }

    private ImageView thumb(JSONObject x) {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(box(SURFACE_HI, 16, 0));
        image.setClipToOutline(true);
        String typeId = x.optString("type");
        Bitmap photo = decodeOrNull(x.optString("photoData"), 256);
        if (photo != null) image.setImageBitmap(photo);
        else image.setImageDrawable(new CoverArt(store.typeName(typeId), typeId.hashCode()));
        return image;
    }

    private View bar(BigDecimal share) {
        LinearLayout track = row();
        track.setBackground(box(SURFACE_HI, 6, 0));
        float fraction = share == null ? 0f : Math.min(1f, Math.max(0f, share.floatValue() / 100f));
        View fill = new View(this);
        fill.setBackground(box(ACCENT, 6, 0));
        track.addView(fill, new LinearLayout.LayoutParams(0, -1, fraction));
        track.addView(new View(this), new LinearLayout.LayoutParams(0, -1, 1f - fraction));
        return track;
    }

    private LinearLayout infoTile(String label, String value) {
        LinearLayout tile = column();
        tile.setPadding(dp(14), dp(14), dp(14), dp(14));
        tile.setBackground(box(SURFACE, 18, STROKE));
        tile.addView(text(label, 12, MUTED, false));
        gap(tile, 6);
        TextView v = text(value, 16, TEXT, true);
        v.setSingleLine(true);
        v.setEllipsize(TextUtils.TruncateAt.END);
        tile.addView(v);
        return tile;
    }

    private View emptyState(String message) {
        TextView t = text(message, 14, MUTED, false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(20), dp(28), dp(20), dp(28));
        t.setBackground(box(SURFACE, 20, STROKE));
        return t;
    }

    private LinearLayout settingsRow(String title, String subtitle, Runnable action) {
        LinearLayout r = column();
        r.setPadding(dp(18), dp(16), dp(18), dp(16));
        r.addView(text(title, 16, TEXT, true));
        gap(r, 4);
        r.addView(text(subtitle, 12, MUTED, false));
        if (action != null) r.setOnClickListener(v -> action.run());
        return r;
    }

    private TextView chip(String label, int color) {
        TextView c = text(label, 13, color, true);
        c.setSingleLine(true);
        c.setPadding(dp(10), dp(5), dp(10), dp(5));
        c.setBackground(box((color & 0x00FFFFFF) | 0x26000000, 12, 0));
        return c;
    }

    private TextView primaryButton(String label, Runnable action) {
        TextView b = text(label, 16, ACCENT_ON, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(16), dp(16), dp(16));
        b.setBackground(box(ACCENT, 18, 0));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private TextView secondaryButton(String label, Runnable action) {
        TextView b = text(label, 15, TEXT, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(15), dp(16), dp(15));
        b.setBackground(box(SURFACE_HI, 18, STROKE));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private TextView dangerButton(String label, Runnable action) {
        TextView b = text(label, 15, NEGATIVE, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(15), dp(16), dp(15));
        b.setBackground(box((NEGATIVE & 0x00FFFFFF) | 0x1F000000, 18, 0));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private EditText input(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setBackground(box(SURFACE_HI, 14, STROKE));
        e.setPadding(dp(14), dp(14), dp(14), dp(14));
        return e;
    }

    private void field(LinearLayout form, String label, View input) {
        form.addView(text(label, 12, MUTED, true));
        gap(form, 6);
        form.addView(input, new LinearLayout.LayoutParams(-1, -2));
        gap(form, 12);
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(color);
        v.setTextSize(sp);
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return v;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void add(LinearLayout parent, View child) {
        parent.addView(child, new LinearLayout.LayoutParams(-1, -2));
    }

    private void gap(LinearLayout parent, int heightDp) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(-1, dp(heightDp)));
    }

    private void divider(LinearLayout parent) {
        View line = new View(this);
        line.setBackgroundColor(STROKE);
        parent.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    /** Equal-width slot inside a horizontal row, with spacing split between neighbours. */
    private LinearLayout.LayoutParams slot(int index, int count, int spacingDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        int half = dp(spacingDp) / 2;
        p.setMargins(index == 0 ? 0 : half, 0, index == count - 1 ? 0 : half, 0);
        return p;
    }

    private static TextWatcher watch(final Runnable onChange) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onChange.run(); }
            @Override public void afterTextChanged(Editable e) {}
        };
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void persist(boolean ok) {
        if (!ok) toast(tr("Nie udało się zapisać danych", "Could not save data", "No se pudieron guardar los datos"));
    }

    private void confirm(String message, String positive, Runnable action) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(positive, (d, w) -> action.run())
                .setNegativeButton(tr("Anuluj", "Cancel", "Cancelar"), null)
                .show();
    }

    // ---- formatting ------------------------------------------------------------------------

    private Locale locale() {
        if (lang.equals("en")) return Locale.forLanguageTag("en-GB");
        if (lang.equals("es")) return Locale.forLanguageTag("es-ES");
        return Locale.forLanguageTag("pl-PL");
    }

    private NumberFormat moneyFormat() {
        NumberFormat f = NumberFormat.getNumberInstance(locale());
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return f;
    }

    private SimpleDateFormat dateFormat(String pattern) {
        return new SimpleDateFormat(pattern, locale());
    }

    private String money(BigDecimal amount) {
        return moneyFormat().format(amount) + " zł";
    }

    private String signedMoney(BigDecimal amount) {
        return (amount.signum() > 0 ? "+" : "") + money(amount);
    }

    private String percentText(BigDecimal p) {
        NumberFormat f = NumberFormat.getNumberInstance(locale());
        f.setMinimumFractionDigits(1);
        f.setMaximumFractionDigits(1);
        return f.format(p) + "%";
    }

    private String percent(BigDecimal p) {
        if (p == null) return "—";
        return (p.signum() > 0 ? "+" : "") + percentText(p);
    }

    private int gainColor(BigDecimal gain) {
        return gain.signum() >= 0 ? POSITIVE : NEGATIVE;
    }

    private String tr(String pl, String en, String es) {
        if (lang.equals("en")) return en;
        if (lang.equals("es")) return es;
        return pl;
    }

    private Bitmap decodeOrNull(String encoded, int maxSide) {
        return encoded == null || encoded.isEmpty() ? null : Images.decode(encoded, maxSide);
    }

    private String appVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }

    // ---- actions and dialogs ---------------------------------------------------------------

    private void openDetail(String id, String from) {
        detailId = id;
        returnTo = from;
        open("detail");
    }

    private void chooseType() {
        JSONArray types = store.types();
        if (types.length() == 0) {
            newType();
            return;
        }
        final String[] names = new String[types.length()];
        final String[] ids = new String[types.length()];
        for (int i = 0; i < types.length(); i++) {
            JSONObject t = types.optJSONObject(i);
            names[i] = t == null ? "" : t.optString("name");
            ids[i] = t == null ? "" : t.optString("id");
        }
        new AlertDialog.Builder(this)
                .setTitle(tr("Wybierz typ", "Choose type", "Elige tipo"))
                .setItems(names, (d, which) -> assetForm(ids[which], null))
                .setNeutralButton(tr("Nowy typ", "New type", "Nuevo tipo"), (d, w) -> newType())
                .show();
    }

    private void newType() {
        typeDialog(null);
    }

    private void typeDialog(final JSONObject type) {
        final EditText name = input(tr("Nazwa typu", "Type name", "Nombre del tipo"),
                type == null ? "" : type.optString("name"));
        final String[] cover = {type == null ? "" : type.optString("coverData")};

        final ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setClipToOutline(true);
        preview.setBackground(box(SURFACE_HI, 18, 0));
        Bitmap current = decodeOrNull(cover[0], 512);
        if (current != null) preview.setImageBitmap(current);
        else preview.setImageDrawable(new CoverArt(name.getText().toString(), 0));

        LinearLayout form = column();
        form.setPadding(dp(20), dp(12), dp(20), dp(8));
        field(form, tr("Nazwa", "Name", "Nombre"), name);
        form.addView(preview, new LinearLayout.LayoutParams(-1, dp(150)));
        gap(form, 12);
        form.addView(secondaryButton(tr("Wybierz okładkę", "Choose cover", "Elegir portada"), () ->
                pickImage(encoded -> {
                    cover[0] = encoded;
                    Bitmap b = decodeOrNull(encoded, 512);
                    if (b != null) preview.setImageBitmap(b);
                }, 512)), new LinearLayout.LayoutParams(-1, -2));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(type == null ? tr("Nowy typ", "New type", "Nuevo tipo")
                        : tr("Edytuj typ", "Edit type", "Editar tipo"))
                .setView(form)
                .setNegativeButton(tr("Anuluj", "Cancel", "Cancelar"), null)
                .setPositiveButton(tr("Zapisz", "Save", "Guardar"), null);
        if (type != null) {
            builder.setNeutralButton(tr("Usuń", "Delete", "Eliminar"), (d, w) -> confirmDeleteType(type));
        }
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            if (n.isEmpty()) {
                name.setError(tr("Wpisz nazwę", "Enter a name", "Introduce un nombre"));
                return;
            }
            boolean ok;
            try {
                if (type == null) {
                    ok = store.addType(n, cover[0]);
                } else {
                    type.put("name", n);
                    if (cover[0].isEmpty()) type.remove("coverData");
                    else type.put("coverData", cover[0]);
                    ok = store.commit();
                }
            } catch (JSONException e) {
                ok = false;
            }
            persist(ok);
            dialog.dismiss();
            open("assets");
        }));
        dialog.show();
    }

    private void confirmDeleteType(final JSONObject type) {
        final String id = type.optString("id");
        final String name = type.optString("name");
        int count = store.itemCount(id);
        String message = tr("Usunąć typ «" + name + "» i " + count + " zasobów?",
                "Delete type «" + name + "» and its " + count + " assets?",
                "¿Eliminar el tipo «" + name + "» y sus " + count + " recursos?");
        confirm(message, tr("Usuń", "Delete", "Eliminar"), () -> {
            persist(store.deleteType(id));
            if (typeFilter.equals(id)) typeFilter = "";
            open("assets");
        });
    }

    private BigDecimal parseMoneyField(EditText field) {
        String raw = field.getText().toString();
        if (raw.trim().isEmpty()) return BigDecimal.ZERO.setScale(2);
        return AssetMath.parseMoney(raw);
    }

    private void assetForm(final String typeId, final JSONObject existing) {
        final boolean isNew = existing == null;
        ScrollView sc = new ScrollView(this);
        LinearLayout form = column();
        form.setPadding(dp(20), dp(12), dp(20), dp(8));
        sc.addView(form);

        final EditText name = input("", isNew ? "" : existing.optString("name"));
        final EditText category = input("", isNew ? "" : existing.optString("category"));
        final EditText quantity = input("1", isNew ? "1" : VaultStore.quantity(existing).stripTrailingZeros().toPlainString());
        final EditText unitCost = input("0,00", isNew ? "" : VaultStore.unitCost(existing).toPlainString());
        final EditText unitValue = input("0,00", isNew ? "" : VaultStore.unitValue(existing).toPlainString());
        final EditText location = input("", isNew ? "" : existing.optString("location"));
        final EditText notes = input("", isNew ? "" : existing.optString("notes"));
        notes.setSingleLine(false);
        notes.setMinLines(2);
        int decimal = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
        quantity.setInputType(decimal);
        unitCost.setInputType(decimal);
        unitValue.setInputType(decimal);

        field(form, tr("Nazwa", "Name", "Nombre"), name);
        field(form, tr("Kategoria", "Category", "Categoría"), category);
        field(form, tr("Ilość", "Quantity", "Cantidad"), quantity);
        field(form, tr("Cena zakupu za sztukę (zł)", "Purchase price per unit (PLN)", "Precio de compra por unidad (PLN)"), unitCost);
        field(form, tr("Wartość za sztukę (zł)", "Current value per unit (PLN)", "Valor por unidad (PLN)"), unitValue);
        field(form, tr("Miejsce przechowywania", "Storage location", "Ubicación"), location);
        field(form, tr("Notatki", "Notes", "Notas"), notes);

        final TextView total = text("", 16, ACCENT, true);
        form.addView(total);
        gap(form, 12);
        final String[] photo = {isNew ? "" : existing.optString("photoData")};
        final ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setClipToOutline(true);
        preview.setBackground(box(SURFACE_HI, 18, 0));
        Bitmap current = decodeOrNull(photo[0], 512);
        if (current != null) preview.setImageBitmap(current);
        else preview.setImageDrawable(new CoverArt(store.typeName(typeId), typeId.hashCode()));
        form.addView(preview, new LinearLayout.LayoutParams(-1, dp(160)));
        gap(form, 10);
        form.addView(secondaryButton(tr("Wybierz zdjęcie", "Choose photo", "Elegir foto"), () ->
                pickImage(encoded -> {
                    photo[0] = encoded;
                    Bitmap b = decodeOrNull(encoded, 512);
                    if (b != null) preview.setImageBitmap(b);
                }, 1024)), new LinearLayout.LayoutParams(-1, -2));

        Runnable refresh = () -> {
            BigDecimal q = AssetMath.parseQuantity(quantity.getText().toString());
            BigDecimal v = parseMoneyField(unitValue);
            if (q == null || v == null) {
                total.setText(tr("Razem: —", "Total: —", "Total: —"));
            } else {
                total.setText(tr("Razem: ", "Total: ", "Total: ") + money(AssetMath.lineTotal(v, q)));
            }
        };
        quantity.addTextChangedListener(watch(refresh));
        unitValue.addTextChangedListener(watch(refresh));
        refresh.run();

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isNew ? tr("Nowy zasób", "New asset", "Nuevo recurso")
                        : tr("Edytuj zasób", "Edit asset", "Editar recurso"))
                .setView(sc)
                .setPositiveButton(tr("Zapisz", "Save", "Guardar"), null)
                .setNegativeButton(tr("Anuluj", "Cancel", "Cancelar"), null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            BigDecimal q = AssetMath.parseQuantity(quantity.getText().toString());
            BigDecimal cost = parseMoneyField(unitCost);
            BigDecimal value = parseMoneyField(unitValue);
            if (n.isEmpty()) {
                name.setError(tr("Wpisz nazwę", "Enter a name", "Introduce un nombre"));
                return;
            }
            if (q == null) {
                quantity.setError(tr("Nieprawidłowa ilość", "Invalid quantity", "Cantidad no válida"));
                return;
            }
            if (cost == null) {
                unitCost.setError(tr("Nieprawidłowa kwota", "Invalid amount", "Importe no válido"));
                return;
            }
            if (value == null) {
                unitValue.setError(tr("Nieprawidłowa kwota", "Invalid amount", "Importe no válido"));
                return;
            }

            boolean ok;
            String itemId;
            try {
                JSONObject x = isNew ? new JSONObject() : existing;
                boolean valueChanged = isNew || value.compareTo(VaultStore.unitValue(existing)) != 0;
                if (isNew) {
                    x.put("id", VaultStore.newId());
                    x.put("type", typeId);
                    x.put("history", new JSONArray());
                }
                x.put("name", n);
                x.put("category", category.getText().toString().trim());
                x.put("location", location.getText().toString().trim());
                x.put("notes", notes.getText().toString().trim());
                x.put("quantity", q.toPlainString());
                x.put("purchase", cost.toPlainString());
                x.put("demo", false);
                if (photo[0].isEmpty()) x.remove("photoData");
                else x.put("photoData", photo[0]);
                if (valueChanged) VaultStore.appendValuation(x, value);
                else x.put("value", value.toPlainString());
                ok = isNew ? store.addItem(x) : store.commit();
                itemId = x.optString("id");
            } catch (JSONException e) {
                ok = false;
                itemId = "";
            }
            persist(ok);
            dialog.dismiss();
            if (isNew) {
                typeFilter = typeId;
                searchText = "";
                open("list");
            } else {
                detailId = itemId;
                open("detail");
            }
        }));
        dialog.show();
    }

    private void valuationDialog(final JSONObject item) {
        final EditText input = input(tr("Nowa wartość za sztukę", "New value per unit", "Nuevo valor por unidad"),
                VaultStore.unitValue(item).toPlainString());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout form = column();
        form.setPadding(dp(20), dp(12), dp(20), dp(8));
        form.addView(input, new LinearLayout.LayoutParams(-1, -2));
        gap(form, 8);
        form.addView(text(tr("Obecna wycena: ", "Current value: ", "Valor actual: ")
                + money(VaultStore.unitValue(item)), 12, MUTED, false));

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(tr("Nowa wycena", "New valuation", "Nueva valoración"))
                .setView(form)
                .setPositiveButton(tr("Zapisz", "Save", "Guardar"), null)
                .setNegativeButton(tr("Anuluj", "Cancel", "Cancelar"), null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            BigDecimal value = parseMoneyField(input);
            if (value == null) {
                input.setError(tr("Nieprawidłowa kwota", "Invalid amount", "Importe no válido"));
                return;
            }
            boolean ok = true;
            if (value.compareTo(VaultStore.unitValue(item)) != 0) {
                VaultStore.appendValuation(item, value);
                ok = store.commit();
            }
            persist(ok);
            dialog.dismiss();
            detailId = item.optString("id");
            open("detail");
        }));
        dialog.show();
    }

    private void updatePhoto(JSONObject item, String encoded) {
        try {
            item.put("photoData", encoded);
            persist(store.commit());
        } catch (JSONException e) {
            persist(false);
        }
        open("detail");
    }

    private void pickImage(Consumer<String> callback, int maxSide) {
        imageCallback = callback;
        imageMaxSide = maxSide;
        Intent in = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        in.setType("image/*");
        in.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(in, REQ_IMAGE);
    }

    private void exportBackup() {
        try {
            pendingExport = store.exportJson();
        } catch (JSONException e) {
            toast(tr("Nie udało się przygotować kopii", "Could not prepare the backup", "No se pudo preparar la copia"));
            return;
        }
        Intent in = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        in.setType("application/json");
        in.addCategory(Intent.CATEGORY_OPENABLE);
        in.putExtra(Intent.EXTRA_TITLE,
                "lootvault-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(new Date()) + ".json");
        startActivityForResult(in, REQ_EXPORT);
    }

    private void pickImport() {
        Intent in = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        in.setType("application/json");
        in.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(in, REQ_IMPORT);
    }

    private void importPreview(String json) throws JSONException {
        final JSONObject backup = VaultStore.parseBackup(json);
        JSONArray types = backup.optJSONArray("types");
        JSONArray items = backup.optJSONArray("items");
        int typeCount = types == null ? 0 : types.length();
        int itemCount = items == null ? 0 : items.length();
        new AlertDialog.Builder(this)
                .setTitle(tr("Przywrócić kopię?", "Restore backup?", "¿Restaurar la copia?"))
                .setMessage(tr("Kopia zawiera " + typeCount + " typów i " + itemCount + " zasobów. Obecne dane zostaną zastąpione.",
                        "The backup contains " + typeCount + " types and " + itemCount + " assets. Current data will be replaced.",
                        "La copia contiene " + typeCount + " tipos y " + itemCount + " recursos. Los datos actuales se reemplazarán."))
                .setPositiveButton(tr("Przywróć", "Restore", "Restaurar"), (d, w) -> {
                    persist(store.replaceWith(backup));
                    open("home");
                })
                .setNegativeButton(tr("Anuluj", "Cancel", "Cancelar"), null)
                .show();
    }

    private String readText(Uri uri) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot open file");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (request == REQ_IMAGE && imageCallback != null) {
                Consumer<String> callback = imageCallback;
                imageCallback = null;
                callback.accept(Images.encodeScaled(getContentResolver(), uri, imageMaxSide, 82));
            } else if (request == REQ_EXPORT && pendingExport != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out == null) throw new IOException("Cannot write file");
                    out.write(pendingExport.getBytes(StandardCharsets.UTF_8));
                }
                pendingExport = null;
                toast(tr("Kopia zapisana", "Backup saved", "Copia guardada"));
            } else if (request == REQ_IMPORT) {
                importPreview(readText(uri));
            }
        } catch (Exception e) {
            toast(tr("Nie udało się wykonać operacji", "The operation failed", "La operación falló"));
        }
    }

    @Override
    public void onBackPressed() {
        if (tab.equals("detail")) open(returnTo);
        else if (tab.equals("list")) open("assets");
        else if (!tab.equals("home")) open("home");
        else super.onBackPressed();
    }
}
