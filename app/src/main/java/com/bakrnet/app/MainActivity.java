package com.bakrnet.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView listView;
    private SubscriberAdapter adapter;
    private List<Subscriber> subscriberList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);

        // --- 1. بناء الواجهة الرئيسية برمجياً (Main Layout) ---
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 32);
        mainLayout.setBackgroundColor(Color.parseColor("#F4F6F9"));

        // الشعار العلوي (Logo)
        ImageView imgLogo = new ImageView(this);
        imgLogo.setImageResource(R.drawable.logo); // التأكد من وجود صورة باسم logo في مجلد drawable
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(240, 240);
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoParams.setMargins(0, 0, 0, 24);
        imgLogo.setLayoutParams(logoParams);
        mainLayout.addView(imgLogo);

        // زر إضافة مشترك جديد
        Button btnAdd = new Button(this);
        btnAdd.setText("+ إضافة مشترك جديد");
        btnAdd.setTextColor(Color.WHITE);
        btnAdd.setTextSize(16);
        btnAdd.setTypeface(null, Typeface.BOLD);
        
        GradientDrawable btnBackground = new GradientDrawable();
        btnBackground.setColor(Color.parseColor("#00838F"));
        btnBackground.setCornerRadius(16);
        btnAdd.setBackground(btnBackground);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 120);
        btnParams.setMargins(0, 0, 0, 24);
        btnAdd.setLayoutParams(btnParams);
        mainLayout.addView(btnAdd);

        // قائمة المشتركين (ListView)
        listView = new ListView(this);
        listView.setDividerHeight(16);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        listView.setLayoutParams(listParams);
        mainLayout.addView(listView);

        setContentView(mainLayout);

        // load البيانات
        loadSubscribers();

        // حدث الضغط على زر الإضافة
        btnAdd.setOnClickListener(v -> showAddSubscriberDialog());
    }

    private void loadSubscribers() {
        subscriberList = dbHelper.getAllSubscribers();
        adapter = new SubscriberAdapter(this, subscriberList);
        listView.setAdapter(adapter);
    }

    // --- 2. نافذة إضافة مشترك جديد (Dialog) ---
    private void showAddSubscriberDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("إضافة مشترك جديد");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText inputName = new EditText(this);
        inputName.setHint("اسم المشترك");
        layout.addView(inputName);

        final EditText inputStart = new EditText(this);
        inputStart.setHint("تاريخ التفعيل (مثال: 2026/11/01)");
        layout.addView(inputStart);

        final EditText inputEnd = new EditText(this);
        inputEnd.setHint("تاريخ الانتهاء (مثال: 2026/12/01)");
        layout.addView(inputEnd);

        final EditText inputPrice = new EditText(this);
        inputPrice.setHint("سعر الاشتراك (مثال: 35,000)");
        layout.addView(inputPrice);

        final Spinner spinnerStatus = new Spinner(this);
        String[] statuses = {"واصل", "غير واصل"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses);
        spinnerStatus.setAdapter(spinnerAdapter);
        layout.addView(spinnerStatus);

        builder.setView(layout);

        builder.setPositiveButton("حفظ", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            String start = inputStart.getText().toString().trim();
            String end = inputEnd.getText().toString().trim();
            String price = inputPrice.getText().toString().trim();
            String status = spinnerStatus.getSelectedItem().toString();

            if (!name.isEmpty()) {
                dbHelper.addSubscriber(name, start, end, price, status);
                loadSubscribers();
                Toast.makeText(MainActivity.this, "تمت الإضافة بنجاح", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "يرجى كتابة اسم المشترك", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    // --- 3. نافذة تجديد الاشتراك وتوثيق الأرشيف ---
    private void showRenewDialog(Subscriber s) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تجديد اشتراك: " + s.name);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText inputMonthYear = new EditText(this);
        inputMonthYear.setHint("الشهر المؤرشف (مثال: 2026-11)");
        layout.addView(inputMonthYear);

        builder.setView(layout);

        builder.setPositiveButton("تأكيد التجديد والأرشفة", (dialog, which) -> {
            String monthYear = inputMonthYear.getText().toString().trim();
            if (monthYear.isEmpty()) monthYear = "سجل قديم";

            // نقل البيانات الحالية للأرشيف وتصفير معلومات الشهر الجديد
            dbHelper.archiveAndResetSubscriber(s.id, s.name, s.startDate, s.endDate, s.price, s.status, monthYear);
            loadSubscribers();
            Toast.makeText(MainActivity.this, "تم أرشفة الشهر القديم وتجديد الاشتراك", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    // --- 4. نموذج بيانات المشترك (Model) ---
    public static class Subscriber {
        public int id;
        public String name;
        public String startDate;
        public String endDate;
        public String price;
        public String status;

        public Subscriber(int id, String name, String startDate, String endDate, String price, String status) {
            this.id = id;
            this.name = name;
            this.startDate = startDate;
            this.endDate = endDate;
            this.price = price;
            this.status = status;
        }
    }

    // --- 5. محول القائمة وحواف العناصر (Adapter & Border UI) ---
    private class SubscriberAdapter extends ArrayAdapter<Subscriber> {
        public SubscriberAdapter(Context context, List<Subscriber> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Subscriber s = getItem(position);

            LinearLayout container = new LinearLayout(getContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(24, 24, 24, 24);

            // تصميم الحواف السوداء الرفيعة لتمييز المشتركين
            GradientDrawable border = new GradientDrawable();
            border.setColor(Color.WHITE);
            border.setStroke(3, Color.parseColor("#000000")); // حواف سوداء رفيعة
            border.setCornerRadius(12);
            container.setBackground(border);

            // اسم المشترك
            TextView txtName = new TextView(getContext());
            txtName.setText(s.name);
            txtName.setTextSize(18);
            txtName.setTypeface(null, Typeface.BOLD);
            txtName.setTextColor(Color.BLACK);
            container.addView(txtName);

            // تواريخ الاشتراك والسعر
            TextView txtDetails = new TextView(getContext());
            txtDetails.setText("تاريخ التفعيل: " + s.startDate + " | الانتهاء: " + s.endDate + "\nالسعر: " + s.price);
            txtDetails.setTextSize(13);
            txtDetails.setTextColor(Color.parseColor("#444444"));
            txtDetails.setPadding(0, 8, 0, 8);
            container.addView(txtDetails);

            // حالة الدفع
            TextView txtStatus = new TextView(getContext());
            txtStatus.setText("حالة الدفع: " + s.status);
            txtStatus.setTextSize(14);
            txtStatus.setTypeface(null, Typeface.BOLD);
            if (s.status.equals("واصل")) {
                txtStatus.setTextColor(Color.parseColor("#2E7D32")); // أخضر
            } else {
                txtStatus.setTextColor(Color.parseColor("#C62828")); // أحمر
            }
            container.addView(txtStatus);

            // زر التجديد للأرشيف
            Button btnRenew = new Button(getContext());
            btnRenew.setText("تجديد الاشتراك للأرشيف");
            btnRenew.setTextSize(12);
            btnRenew.setTextColor(Color.WHITE);
            
            GradientDrawable renewBg = new GradientDrawable();
            renewBg.setColor(Color.parseColor("#37474F"));
            renewBg.setCornerRadius(8);
            btnRenew.setBackground(renewBg);

            btnRenew.setOnClickListener(v -> showRenewDialog(s));
            container.addView(btnRenew);

            return container;
        }
    }

    // --- 6. إدارة قاعدة البيانات والأرشيف الشهري (SQLite Helper) ---
    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "BakrNet.db";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // جدول المشتركين الحاليين
            db.execSQL("CREATE TABLE subscribers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, start_date TEXT, end_date TEXT, price TEXT, status TEXT)");
            // جدول الأرشيف المستقل للأشهر السابقة
            db.execSQL("CREATE TABLE archive (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, start_date TEXT, end_date TEXT, price TEXT, status TEXT, month_year TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS subscribers");
            db.execSQL("DROP TABLE IF EXISTS archive");
            onCreate(db);
        }

        public void addSubscriber(String name, String startDate, String endDate, String price, String status) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("start_date", startDate);
            values.put("end_date", endDate);
            values.put("price", price);
            values.put("status", status);
            db.insert("subscribers", null, values);
        }

        public List<Subscriber> getAllSubscribers() {
            List<Subscriber> list = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM subscribers", null);
            if (cursor.moveToFirst()) {
                do {
                    list.add(new Subscriber(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getString(5)
                    ));
                } while (cursor.moveToNext());
            }
            cursor.close();
            return list;
        }

        public void archiveAndResetSubscriber(int id, String name, String startDate, String endDate, String price, String status, String monthYear) {
            SQLiteDatabase db = this.getWritableDatabase();

            // 1. نقل السجل الشهري الحالي للأرشيف
            ContentValues archiveValues = new ContentValues();
            archiveValues.put("name", name);
            archiveValues.put("start_date", startDate);
            archiveValues.put("end_date", endDate);
            archiveValues.put("price", price);
            archiveValues.put("status", status);
            archiveValues.put("month_year", monthYear);
            db.insert("archive", null, archiveValues);

            // 2. إبقاء اسم المشترك فارغاً/جاهزاً للتجديد في القائمة الرئيسية
            ContentValues updateValues = new ContentValues();
            updateValues.put("start_date", "");
            updateValues.put("end_date", "");
            updateValues.put("price", "");
            updateValues.put("status", "غير واصل");
            db.update("subscribers", updateValues, "id = ?", new String[]{String.valueOf(id)});
        }
    }
}
