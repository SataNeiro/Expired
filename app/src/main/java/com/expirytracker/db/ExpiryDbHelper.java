package com.expirytracker.db;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.expirytracker.models.BaseProduct;
import com.expirytracker.models.FoodProduct;
import com.expirytracker.models.Product;
import java.util.ArrayList;
import java.util.List;
public class ExpiryDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "expiry_tracker.db";
    private static final int DB_VERSION = 10;
    public static final String TABLE_PRODUCTS = "products";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_QTY = "qty";
    public static final String COL_BARCODE = "barcode";
    public static final String COL_EXPIRY = "expiry";
    public static final String COL_IMAGE = "image";
    public static final String COL_IMAGE_ORIGINAL = "image_original";
    public static final String COL_STATUS = "status";
    public static final String COL_ARCHIVED = "archived";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_SYNCED = "synced";
    public static final String TABLE_BASE = "base_products";
    public static final String COL_BASE_ID = "base_id";
    public static final String COL_BASE_NAME = "base_name";
    public static final String COL_BASE_BARCODE = "base_barcode";
    public static final String COL_BASE_IMAGE = "base_image";
    public static final String COL_BASE_IMAGE_ORIG = "base_image_orig";
    public static final String TABLE_FOOD = "foodbase";
    public static final String COL_FOOD_ID = "food_id";
    public static final String COL_FOOD_NAME = "food_name";
    public static final String COL_FOOD_BARCODE = "food_barcode";
    public static final String COL_FOOD_IMAGE = "food_image";
    public static final String COL_FOOD_IMAGE_ORIG = "food_image_orig";
    public ExpiryDbHelper(Context context) { super(context, DB_NAME, null, DB_VERSION); }
    @Override public void onCreate(SQLiteDatabase db) { createTables(db); }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BASE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FOOD);
        createTables(db);
    }
    private void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PRODUCTS + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_NAME + " TEXT NOT NULL, "
                + COL_QTY + " INTEGER, "
                + COL_BARCODE + " TEXT, "
                + COL_EXPIRY + " TEXT, "
                + COL_IMAGE + " TEXT, "
                + COL_IMAGE_ORIGINAL + " TEXT, "
                + COL_STATUS + " TEXT, "
                + COL_ARCHIVED + " INTEGER DEFAULT 0, "
                + COL_CREATED_AT + " TEXT, "
                + COL_SYNCED + " INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE " + TABLE_BASE + " ("
                + COL_BASE_ID + " TEXT PRIMARY KEY, "
                + COL_BASE_NAME + " TEXT NOT NULL, "
                + COL_BASE_BARCODE + " TEXT NOT NULL, "
                + COL_BASE_IMAGE + " TEXT, "
                + COL_BASE_IMAGE_ORIG + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_FOOD + " ("
                + COL_FOOD_ID + " TEXT PRIMARY KEY, "
                + COL_FOOD_NAME + " TEXT NOT NULL, "
                + COL_FOOD_BARCODE + " TEXT NOT NULL, "
                + COL_FOOD_IMAGE + " TEXT, "
                + COL_FOOD_IMAGE_ORIG + " TEXT)");
        db.execSQL("CREATE INDEX idx_products_barcode ON " + TABLE_PRODUCTS + "(" + COL_BARCODE + ")");
        db.execSQL("CREATE INDEX idx_products_archived ON " + TABLE_PRODUCTS + "(" + COL_ARCHIVED + ")");
        db.execSQL("CREATE INDEX idx_base_barcode ON " + TABLE_BASE + "(" + COL_BASE_BARCODE + ")");
        db.execSQL("CREATE INDEX idx_food_barcode ON " + TABLE_FOOD + "(" + COL_FOOD_BARCODE + ")");
    }
    // ---- Products ----
    public void insertOrUpdate(Product p) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_ID + " FROM " + TABLE_PRODUCTS + " WHERE " + COL_ID + " = ?", new String[]{p.id});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        if (exists) {
            db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET "
                    + COL_NAME + "=?, " + COL_QTY + "=?, " + COL_BARCODE + "=?, " + COL_EXPIRY + "=?, "
                    + COL_IMAGE + "=?, " + COL_IMAGE_ORIGINAL + "=?, "
                    + COL_STATUS + "=?, " + COL_ARCHIVED + "=?, " + COL_CREATED_AT + "=?, " + COL_SYNCED + "=? "
                    + "WHERE " + COL_ID + "=?", new Object[]{p.name, p.qty, p.barcode, p.expiry, p.image, p.imageOriginal,
                    p.status, p.archived ? 1 : 0, p.createdAt, p.synced, p.id});
        } else {
            db.execSQL("INSERT INTO " + TABLE_PRODUCTS + " ("
                    + COL_ID + "," + COL_NAME + "," + COL_QTY + "," + COL_BARCODE + ","
                    + COL_EXPIRY + "," + COL_IMAGE + "," + COL_IMAGE_ORIGINAL + ","
                    + COL_STATUS + "," + COL_ARCHIVED + "," + COL_CREATED_AT + "," + COL_SYNCED + ") VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    new Object[]{p.id, p.name, p.qty, p.barcode, p.expiry, p.image, p.imageOriginal,
                            p.status, p.archived ? 1 : 0, p.createdAt, p.synced});
        }
    }
    public void deleteProduct(String id) { getWritableDatabase().execSQL("DELETE FROM " + TABLE_PRODUCTS + " WHERE " + COL_ID + "=?", new String[]{id}); }
    public void clearAllProducts() { getWritableDatabase().execSQL("DELETE FROM " + TABLE_PRODUCTS); }
    public List<Product> getAllActive() {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COL_ARCHIVED + "=0 ORDER BY " + COL_CREATED_AT + " DESC", null);
        if (c.moveToFirst()) {
            do {
                Product p = new Product();
                p.id = c.getString(c.getColumnIndex(COL_ID));
                p.name = c.getString(c.getColumnIndex(COL_NAME));
                p.qty = c.getInt(c.getColumnIndex(COL_QTY));
                p.barcode = c.getString(c.getColumnIndex(COL_BARCODE));
                p.expiry = c.getString(c.getColumnIndex(COL_EXPIRY));
                p.image = c.getString(c.getColumnIndex(COL_IMAGE));
                p.imageOriginal = c.getString(c.getColumnIndex(COL_IMAGE_ORIGINAL));
                p.status = c.getString(c.getColumnIndex(COL_STATUS));
                p.archived = c.getInt(c.getColumnIndex(COL_ARCHIVED)) == 1;
                p.createdAt = c.getString(c.getColumnIndex(COL_CREATED_AT));
                p.synced = c.getInt(c.getColumnIndex(COL_SYNCED));
                list.add(p);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }
    public Product getActiveByBarcode(String barcode) {
        if (barcode == null) return null;
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COL_BARCODE + " = ? AND " + COL_ARCHIVED + "=0 LIMIT 1", new String[]{barcode});
        if (c.moveToFirst()) {
            Product p = new Product();
            p.id = c.getString(c.getColumnIndex(COL_ID));
            p.name = c.getString(c.getColumnIndex(COL_NAME));
            p.qty = c.getInt(c.getColumnIndex(COL_QTY));
            p.barcode = c.getString(c.getColumnIndex(COL_BARCODE));
            p.expiry = c.getString(c.getColumnIndex(COL_EXPIRY));
            p.image = c.getString(c.getColumnIndex(COL_IMAGE));
            p.imageOriginal = c.getString(c.getColumnIndex(COL_IMAGE_ORIGINAL));
            p.status = c.getString(c.getColumnIndex(COL_STATUS));
            p.archived = c.getInt(c.getColumnIndex(COL_ARCHIVED)) == 1;
            p.createdAt = c.getString(c.getColumnIndex(COL_CREATED_AT));
            p.synced = c.getInt(c.getColumnIndex(COL_SYNCED));
            c.close();
            return p;
        }
        c.close();
        return null;
    }
    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " ORDER BY " + COL_CREATED_AT + " DESC", null);
        if (c.moveToFirst()) {
            do {
                Product p = new Product();
                p.id = c.getString(c.getColumnIndex(COL_ID));
                p.name = c.getString(c.getColumnIndex(COL_NAME));
                p.qty = c.getInt(c.getColumnIndex(COL_QTY));
                p.barcode = c.getString(c.getColumnIndex(COL_BARCODE));
                p.expiry = c.getString(c.getColumnIndex(COL_EXPIRY));
                p.image = c.getString(c.getColumnIndex(COL_IMAGE));
                p.imageOriginal = c.getString(c.getColumnIndex(COL_IMAGE_ORIGINAL));
                p.status = c.getString(c.getColumnIndex(COL_STATUS));
                p.archived = c.getInt(c.getColumnIndex(COL_ARCHIVED)) == 1;
                p.createdAt = c.getString(c.getColumnIndex(COL_CREATED_AT));
                p.synced = c.getInt(c.getColumnIndex(COL_SYNCED));
                list.add(p);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }
    public void markSynced(String id) { getWritableDatabase().execSQL("UPDATE " + TABLE_PRODUCTS + " SET " + COL_SYNCED + "=1 WHERE " + COL_ID + "=?", new String[]{id}); }
    public void replaceAllProducts(List<Product> newProducts) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM " + TABLE_PRODUCTS);
            for (Product p : newProducts) {
                db.execSQL("INSERT INTO " + TABLE_PRODUCTS + " ("
                        + COL_ID + "," + COL_NAME + "," + COL_QTY + "," + COL_BARCODE + ","
                        + COL_EXPIRY + "," + COL_IMAGE + "," + COL_IMAGE_ORIGINAL + ","
                        + COL_STATUS + "," + COL_ARCHIVED + "," + COL_CREATED_AT + "," + COL_SYNCED + ") VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                        new Object[]{p.id, p.name, p.qty, p.barcode, p.expiry, p.image, p.imageOriginal,
                                p.status, p.archived ? 1 : 0, p.createdAt, 1});
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    // ---- Base ----
    public void insertOrUpdateBase(BaseProduct bp) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_BASE_ID + " FROM " + TABLE_BASE + " WHERE " + COL_BASE_ID + " = ?", new String[]{bp.baseId});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        if (exists) {
            db.execSQL("UPDATE " + TABLE_BASE + " SET "
                    + COL_BASE_NAME + "=?, " + COL_BASE_BARCODE + "=?, " + COL_BASE_IMAGE + "=?, " + COL_BASE_IMAGE_ORIG + "=? "
                    + "WHERE " + COL_BASE_ID + "=?", new Object[]{bp.name, bp.barcode, bp.image, bp.imageOriginal, bp.baseId});
        } else {
            db.execSQL("INSERT INTO " + TABLE_BASE + " ("
                    + COL_BASE_ID + "," + COL_BASE_NAME + "," + COL_BASE_BARCODE + "," + COL_BASE_IMAGE + "," + COL_BASE_IMAGE_ORIG + ") VALUES (?,?,?,?,?)",
                    new Object[]{bp.baseId, bp.name, bp.barcode, bp.image, bp.imageOriginal});
        }
    }
    public BaseProduct getBaseByBarcode(String barcode) {
        if (barcode == null) return null;
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_BASE + " WHERE " + COL_BASE_BARCODE + " = ?", new String[]{barcode});
        if (c.moveToFirst()) {
            BaseProduct bp = new BaseProduct();
            bp.baseId = c.getString(c.getColumnIndex(COL_BASE_ID));
            bp.name = c.getString(c.getColumnIndex(COL_BASE_NAME));
            bp.barcode = c.getString(c.getColumnIndex(COL_BASE_BARCODE));
            bp.image = c.getString(c.getColumnIndex(COL_BASE_IMAGE));
            bp.imageOriginal = c.getString(c.getColumnIndex(COL_BASE_IMAGE_ORIG));
            c.close();
            return bp;
        }
        c.close();
        return null;
    }
    public List<BaseProduct> getAllBase() {
        List<BaseProduct> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_BASE + " ORDER BY " + COL_BASE_NAME, null);
        if (c.moveToFirst()) {
            do {
                BaseProduct bp = new BaseProduct();
                bp.baseId = c.getString(c.getColumnIndex(COL_BASE_ID));
                bp.name = c.getString(c.getColumnIndex(COL_BASE_NAME));
                bp.barcode = c.getString(c.getColumnIndex(COL_BASE_BARCODE));
                bp.image = c.getString(c.getColumnIndex(COL_BASE_IMAGE));
                bp.imageOriginal = c.getString(c.getColumnIndex(COL_BASE_IMAGE_ORIG));
                list.add(bp);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }
    public void clearBase() { getWritableDatabase().execSQL("DELETE FROM " + TABLE_BASE); }
    public void replaceAllBase(List<BaseProduct> newBase) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM " + TABLE_BASE);
            for (BaseProduct bp : newBase) {
                db.execSQL("INSERT INTO " + TABLE_BASE + " ("
                        + COL_BASE_ID + "," + COL_BASE_NAME + "," + COL_BASE_BARCODE + "," + COL_BASE_IMAGE + "," + COL_BASE_IMAGE_ORIG + ") VALUES (?,?,?,?,?)",
                        new Object[]{bp.baseId, bp.name, bp.barcode, bp.image, bp.imageOriginal});
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    // ---- Food ----
    public void insertOrUpdateFood(FoodProduct fp) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_FOOD_ID + " FROM " + TABLE_FOOD + " WHERE " + COL_FOOD_ID + " = ?", new String[]{fp.foodId});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        if (exists) {
            db.execSQL("UPDATE " + TABLE_FOOD + " SET "
                    + COL_FOOD_NAME + "=?, " + COL_FOOD_BARCODE + "=?, " + COL_FOOD_IMAGE + "=?, " + COL_FOOD_IMAGE_ORIG + "=? "
                    + "WHERE " + COL_FOOD_ID + "=?", new Object[]{fp.name, fp.barcode, fp.image, fp.imageOriginal, fp.foodId});
        } else {
            db.execSQL("INSERT INTO " + TABLE_FOOD + " ("
                    + COL_FOOD_ID + "," + COL_FOOD_NAME + "," + COL_FOOD_BARCODE + "," + COL_FOOD_IMAGE + "," + COL_FOOD_IMAGE_ORIG + ") VALUES (?,?,?,?,?)",
                    new Object[]{fp.foodId, fp.name, fp.barcode, fp.image, fp.imageOriginal});
        }
    }
    public FoodProduct getFoodByBarcode(String barcode) {
        if (barcode == null) return null;
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_FOOD + " WHERE " + COL_FOOD_BARCODE + " = ?", new String[]{barcode});
        if (c.moveToFirst()) {
            FoodProduct fp = new FoodProduct();
            fp.foodId = c.getString(c.getColumnIndex(COL_FOOD_ID));
            fp.name = c.getString(c.getColumnIndex(COL_FOOD_NAME));
            fp.barcode = c.getString(c.getColumnIndex(COL_FOOD_BARCODE));
            fp.image = c.getString(c.getColumnIndex(COL_FOOD_IMAGE));
            fp.imageOriginal = c.getString(c.getColumnIndex(COL_FOOD_IMAGE_ORIG));
            c.close();
            return fp;
        }
        c.close();
        return null;
    }
    public List<FoodProduct> getAllFood() {
        List<FoodProduct> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_FOOD + " ORDER BY " + COL_FOOD_NAME, null);
        if (c.moveToFirst()) {
            do {
                FoodProduct fp = new FoodProduct();
                fp.foodId = c.getString(c.getColumnIndex(COL_FOOD_ID));
                fp.name = c.getString(c.getColumnIndex(COL_FOOD_NAME));
                fp.barcode = c.getString(c.getColumnIndex(COL_FOOD_BARCODE));
                fp.image = c.getString(c.getColumnIndex(COL_FOOD_IMAGE));
                fp.imageOriginal = c.getString(c.getColumnIndex(COL_FOOD_IMAGE_ORIG));
                list.add(fp);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }
    public void clearFood() { getWritableDatabase().execSQL("DELETE FROM " + TABLE_FOOD); }
    public void replaceAllFood(List<FoodProduct> newFood) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM " + TABLE_FOOD);
            for (FoodProduct fp : newFood) {
                db.execSQL("INSERT INTO " + TABLE_FOOD + " ("
                        + COL_FOOD_ID + "," + COL_FOOD_NAME + "," + COL_FOOD_BARCODE + "," + COL_FOOD_IMAGE + "," + COL_FOOD_IMAGE_ORIG + ") VALUES (?,?,?,?,?)",
                        new Object[]{fp.foodId, fp.name, fp.barcode, fp.image, fp.imageOriginal});
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    public void clearAll() { clearAllProducts(); clearBase(); clearFood(); }
}
