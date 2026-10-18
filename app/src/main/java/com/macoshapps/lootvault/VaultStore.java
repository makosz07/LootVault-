package com.macoshapps.lootvault;

import android.content.Context;
import android.util.Log;
import com.macoshapps.lootvault.domain.AssetMath;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.UUID;

/**
 * Local JSON vault. Writes are atomic (temp file, then rename). A vault that cannot be read is
 * moved aside, never overwritten, and a vault that cannot be moved is left read-only.
 * Demo entries are flagged and excluded from every total.
 */
public final class VaultStore {
    public static final int SCHEMA = 3;
    private static final String TAG = "LootVault";
    private static final String FILE = "vault.json";
    private static final String TEMP = "vault.tmp";
    private static final int MAX_SNAPSHOTS = 1500;

    private final Context context;
    private JSONObject data = new JSONObject();
    private boolean writable = true;
    private String recoveredFile;

    public VaultStore(Context context) {
        this.context = context;
        load();
    }

    /** Name of the file the unreadable vault was moved to, or null. Clears the value once read. */
    public String consumeRecoveredFile() {
        String name = recoveredFile;
        recoveredFile = null;
        return name;
    }

    private void load() {
        File file = new File(context.getFilesDir(), FILE);
        try {
            if (file.exists()) {
                data = new JSONObject(readAll(file));
            } else {
                String legacy = context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getString("db", "");
                data = legacy.isEmpty() ? new JSONObject() : new JSONObject(legacy);
            }
        } catch (Exception e) {
            Log.e(TAG, "Vault unreadable", e);
            data = new JSONObject();
            if (file.exists()) {
                File aside = new File(context.getFilesDir(), "vault-unreadable-" + System.currentTimeMillis() + ".json");
                if (file.renameTo(aside)) recoveredFile = aside.getName();
                else writable = false;
            }
        }
        normalize();
        seedDemoData();
        save();
    }

    private void normalize() {
        ensureArray("types");
        ensureArray("items");
        ensureArray("snapshots");
        if (!data.has("schema")) set(data, "schema", SCHEMA);
    }

    private void ensureArray(String key) {
        if (data.optJSONArray(key) == null) set(data, key, new JSONArray());
    }

    private void seedDemoData() {
        if (data.optBoolean("starterCreated", false) || types().length() > 0 || items().length() > 0) return;
        String[][] samples = {
            {"Przedmioty", "Laptop", "Słuchawki", "Plecak", "Zegarek", "Aparat fotograficzny"},
            {"Gry i aplikacje", "Minecraft", "Stardew Valley", "Terraria", "Hollow Knight", "Blender"},
            {"Książki", "Sapiens", "Atomowe nawyki", "Medytacje", "Władca Pierścieni", "Zbrodnia i kara"}
        };
        for (String[] group : samples) {
            String typeId = newId();
            types().put(obj("id", typeId, "name", group[0], "demo", true));
            for (int i = 1; i < group.length; i++) {
                items().put(obj("id", newId(), "type", typeId, "name", group[i],
                        "category", "", "location", "", "notes", "",
                        "quantity", "1", "purchase", "0.00", "value", "0.00",
                        "demo", true, "history", new JSONArray()));
            }
        }
        set(data, "starterCreated", true);
    }

    // ---- accessors -------------------------------------------------------------------------

    public String language() { return data.optString("language", "pl"); }

    public boolean setLanguage(String code) {
        set(data, "language", code);
        return save();
    }

    public JSONArray types() { return data.optJSONArray("types"); }
    public JSONArray items() { return data.optJSONArray("items"); }
    public JSONArray snapshots() { return data.optJSONArray("snapshots"); }

    public JSONObject typeById(String id) { return find(types(), id); }
    public JSONObject itemById(String id) { return find(items(), id); }

    public String typeName(String id) {
        JSONObject type = typeById(id);
        return type == null ? "" : type.optString("name");
    }

    /** Count of real (non-demo) assets, optionally within one type. */
    public int realCount() { return countItems(null, false); }
    public int realCount(String typeId) { return countItems(typeId, false); }
    /** Count of all assets in a type, including demo entries. */
    public int itemCount(String typeId) { return countItems(typeId, true); }

    public BigDecimal totalValue() { return total(null, false); }
    public BigDecimal totalCost() { return total(null, true); }
    public BigDecimal totalValue(String typeId) { return total(typeId, false); }
    public BigDecimal totalCost(String typeId) { return total(typeId, true); }

    public boolean hasDemoData() {
        JSONArray all = items();
        for (int i = 0; i < all.length(); i++) {
            JSONObject x = all.optJSONObject(i);
            if (x != null && x.optBoolean("demo", false)) return true;
        }
        return false;
    }

    // ---- money helpers ---------------------------------------------------------------------

    /** Reads a decimal field stored as a string or number. Missing or invalid values become zero. */
    public static BigDecimal decimal(JSONObject x, String key) {
        try {
            return new BigDecimal(String.valueOf(x.opt(key)).trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public static BigDecimal quantity(JSONObject x) {
        BigDecimal q = decimal(x, "quantity");
        return q.signum() < 0 ? BigDecimal.ZERO : q;
    }

    public static BigDecimal unitValue(JSONObject x) {
        BigDecimal v = decimal(x, "value");
        return v.signum() < 0 ? BigDecimal.ZERO : v;
    }

    public static BigDecimal unitCost(JSONObject x) {
        BigDecimal c = decimal(x, "purchase");
        return c.signum() < 0 ? BigDecimal.ZERO : c;
    }

    public static BigDecimal value(JSONObject x) {
        return AssetMath.lineTotal(unitValue(x), quantity(x));
    }

    public static BigDecimal cost(JSONObject x) {
        return AssetMath.lineTotal(unitCost(x), quantity(x));
    }

    /** Adds a valuation to the item's history and makes it the current unit value. */
    public static void appendValuation(JSONObject item, BigDecimal unitValue) {
        JSONArray history = item.optJSONArray("history");
        if (history == null) {
            history = new JSONArray();
            set(item, "history", history);
        }
        history.put(obj("time", System.currentTimeMillis(), "value", unitValue.toPlainString()));
        set(item, "value", unitValue.toPlainString());
    }

    // ---- mutations -------------------------------------------------------------------------

    public boolean addItem(JSONObject item) {
        items().put(item);
        return commit();
    }

    /** Call after changing an item in place. Records a daily snapshot and writes the vault. */
    public boolean commit() {
        recordSnapshot();
        return save();
    }

    public boolean deleteItem(String id) {
        set(data, "items", without(items(), "id", id));
        return commit();
    }

    public boolean addType(String name, String coverData) {
        JSONObject type = obj("id", newId(), "name", name);
        if (!coverData.isEmpty()) set(type, "coverData", coverData);
        types().put(type);
        return commit();
    }

    public boolean deleteType(String typeId) {
        set(data, "items", without(items(), "type", typeId));
        set(data, "types", without(types(), "id", typeId));
        return commit();
    }

    /** Removes sample entries. Types that still hold real assets are kept. */
    public boolean deleteDemoData() {
        JSONArray keptItems = new JSONArray();
        JSONArray all = items();
        for (int i = 0; i < all.length(); i++) {
            JSONObject x = all.optJSONObject(i);
            if (x != null && !x.optBoolean("demo", false)) keptItems.put(x);
        }
        JSONArray keptTypes = new JSONArray();
        JSONArray allTypes = types();
        for (int i = 0; i < allTypes.length(); i++) {
            JSONObject t = allTypes.optJSONObject(i);
            if (t == null) continue;
            if (!t.optBoolean("demo", false) || hasItemsOfType(keptItems, t.optString("id"))) keptTypes.put(t);
        }
        set(data, "items", keptItems);
        set(data, "types", keptTypes);
        set(data, "starterCreated", true);
        return commit();
    }

    // ---- backup ----------------------------------------------------------------------------

    public String exportJson() throws JSONException {
        JSONObject out = new JSONObject(data.toString());
        out.put("schema", SCHEMA);
        out.put("exportedAt", System.currentTimeMillis());
        out.put("app", "LootVault");
        return out.toString(2);
    }

    /** Validates a backup before it is shown to the user. Throws JSONException if it is not usable. */
    public static JSONObject parseBackup(String json) throws JSONException {
        JSONObject backup = new JSONObject(json);
        if (backup.optInt("schema", 2) > SCHEMA) {
            throw new JSONException("Backup was created by a newer version of LootVault");
        }
        JSONArray types = backup.optJSONArray("types");
        JSONArray items = backup.optJSONArray("items");
        if (types == null || items == null) throw new JSONException("Not a LootVault backup");
        for (int i = 0; i < types.length(); i++) {
            JSONObject t = types.optJSONObject(i);
            if (t == null || t.optString("id").isEmpty() || t.optString("name").isEmpty()) {
                throw new JSONException("Invalid asset type at position " + i);
            }
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject x = items.optJSONObject(i);
            if (x == null || x.optString("id").isEmpty() || x.optString("name").isEmpty()
                    || x.optString("type").isEmpty()) {
                throw new JSONException("Invalid asset at position " + i);
            }
        }
        return backup;
    }

    /** Replaces all data with a validated backup. */
    public boolean replaceWith(JSONObject backup) {
        data = backup;
        normalize();
        set(data, "starterCreated", true);
        return save();
    }

    // ---- internals -------------------------------------------------------------------------

    private int countItems(String typeId, boolean includeDemo) {
        int n = 0;
        JSONArray all = items();
        for (int i = 0; i < all.length(); i++) {
            JSONObject x = all.optJSONObject(i);
            if (x == null) continue;
            if (!includeDemo && x.optBoolean("demo", false)) continue;
            if (typeId != null && !typeId.equals(x.optString("type"))) continue;
            n++;
        }
        return n;
    }

    private BigDecimal total(String typeId, boolean useCost) {
        BigDecimal sum = BigDecimal.ZERO;
        JSONArray all = items();
        for (int i = 0; i < all.length(); i++) {
            JSONObject x = all.optJSONObject(i);
            if (x == null || x.optBoolean("demo", false)) continue;
            if (typeId != null && !typeId.equals(x.optString("type"))) continue;
            sum = sum.add(useCost ? cost(x) : value(x));
        }
        return sum.setScale(AssetMath.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** One snapshot per calendar day: today's point is replaced rather than appended again. */
    private void recordSnapshot() {
        long now = System.currentTimeMillis();
        JSONObject point = obj("time", now,
                "value", totalValue().toPlainString(),
                "purchase", totalCost().toPlainString());
        JSONArray old = snapshots();
        JSONArray next = new JSONArray();
        int start = Math.max(0, old.length() - MAX_SNAPSHOTS);
        for (int i = start; i < old.length(); i++) {
            JSONObject p = old.optJSONObject(i);
            if (p == null) continue;
            boolean sameDay = i == old.length() - 1 && dayKey(p.optLong("time")) == dayKey(now);
            if (!sameDay) next.put(p);
        }
        next.put(point);
        set(data, "snapshots", next);
    }

    private boolean save() {
        if (!writable) {
            Log.e(TAG, "Vault is read-only; not saving");
            return false;
        }
        File dir = context.getFilesDir();
        File tmp = new File(dir, TEMP);
        File file = new File(dir, FILE);
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(data.toString().getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        } catch (IOException e) {
            Log.e(TAG, "Save failed", e);
            return false;
        }
        if (!tmp.renameTo(file)) {
            Log.e(TAG, "Rename failed");
            return false;
        }
        return true;
    }

    private static String readAll(File file) throws IOException {
        try (InputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static JSONObject find(JSONArray array, String id) {
        for (int i = 0; i < array.length(); i++) {
            JSONObject x = array.optJSONObject(i);
            if (x != null && id.equals(x.optString("id"))) return x;
        }
        return null;
    }

    private static JSONArray without(JSONArray source, String field, String value) {
        JSONArray out = new JSONArray();
        for (int i = 0; i < source.length(); i++) {
            JSONObject x = source.optJSONObject(i);
            if (x != null && !value.equals(x.optString(field))) out.put(x);
        }
        return out;
    }

    private static boolean hasItemsOfType(JSONArray items, String typeId) {
        for (int i = 0; i < items.length(); i++) {
            JSONObject x = items.optJSONObject(i);
            if (x != null && typeId.equals(x.optString("type"))) return true;
        }
        return false;
    }

    private static int dayKey(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR);
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    private static void set(JSONObject target, String key, Object value) {
        try {
            target.put(key, value);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JSONObject obj(Object... pairs) {
        JSONObject o = new JSONObject();
        for (int i = 0; i < pairs.length; i += 2) set(o, (String) pairs[i], pairs[i + 1]);
        return o;
    }
}
