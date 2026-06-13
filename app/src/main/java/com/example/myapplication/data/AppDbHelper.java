package com.example.myapplication.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Lokalna baza aplikacije. Sadrži korisnike, podatke za igre
 * (pitanja, spojnice, asocijacije) i rezultate odigranih igara.
 */
public class AppDbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "slagalica.db";
    public static final int DB_VERSION = 3;

    public static final String T_USERS = "users";
    public static final String T_QUESTIONS = "questions";
    public static final String T_SPOJNICE = "spojnice_pairs";
    public static final String T_ASSOC_SETS = "association_sets";
    public static final String T_ASSOC_COLUMNS = "association_columns";
    public static final String T_RESULTS = "game_results";

    public AppDbHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_USERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT NOT NULL,"
                + "email TEXT NOT NULL,"
                + "region TEXT NOT NULL,"
                + "avatar INTEGER NOT NULL DEFAULT 0,"
                + "tokens INTEGER NOT NULL DEFAULT 5,"
                + "stars INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE " + T_QUESTIONS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "text TEXT NOT NULL,"
                + "answer_a TEXT NOT NULL,"
                + "answer_b TEXT NOT NULL,"
                + "answer_c TEXT NOT NULL,"
                + "answer_d TEXT NOT NULL,"
                + "correct_index INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE " + T_SPOJNICE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "set_id INTEGER NOT NULL,"
                + "left_item TEXT NOT NULL,"
                + "right_item TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + T_ASSOC_SETS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "final_solution TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + T_ASSOC_COLUMNS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "set_id INTEGER NOT NULL,"
                + "col_index INTEGER NOT NULL,"
                + "cell_1 TEXT NOT NULL,"
                + "cell_2 TEXT NOT NULL,"
                + "cell_3 TEXT NOT NULL,"
                + "cell_4 TEXT NOT NULL,"
                + "solution TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + T_RESULTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER NOT NULL,"
                + "game TEXT NOT NULL,"
                + "player_score INTEGER NOT NULL,"
                + "opponent_score INTEGER NOT NULL,"
                + "won INTEGER NOT NULL,"
                + "detail_a INTEGER NOT NULL DEFAULT 0,"
                + "detail_b INTEGER NOT NULL DEFAULT 0,"
                + "played_at INTEGER NOT NULL)");

        seedQuestions(db);
        seedSpojnice(db);
        seedAsocijacije(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_RESULTS);
        db.execSQL("DROP TABLE IF EXISTS " + T_ASSOC_COLUMNS);
        db.execSQL("DROP TABLE IF EXISTS " + T_ASSOC_SETS);
        db.execSQL("DROP TABLE IF EXISTS " + T_SPOJNICE);
        db.execSQL("DROP TABLE IF EXISTS " + T_QUESTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + T_USERS);
        onCreate(db);
    }

    private void seedQuestions(SQLiteDatabase db) {
        insertQuestion(db, "Koji je glavni grad Srbije?",
                "Beograd", "Novi Sad", "Niš", "Subotica", 0);
        insertQuestion(db, "Koliko planeta ima Sunčev sistem?",
                "7", "8", "9", "10", 1);
        insertQuestion(db, "Ko je napisao roman \"Na Drini ćuprija\"?",
                "Miloš Crnjanski", "Ivo Andrić", "Branko Ćopić", "Meša Selimović", 1);
        insertQuestion(db, "Koja je najveća planeta u Sunčevom sistemu?",
                "Saturn", "Zemlja", "Jupiter", "Mars", 2);
        insertQuestion(db, "Koje godine je počeo Prvi svetski rat?",
                "1912", "1914", "1918", "1939", 1);
        insertQuestion(db, "Na kojoj reci leži Novi Sad?",
                "Dunav", "Sava", "Tisa", "Morava", 0);
        insertQuestion(db, "Koja je hemijska formula vode?",
                "H2O", "CO2", "O2", "NaCl", 0);
        insertQuestion(db, "Koliko sekundi ima jedan minut?",
                "30", "60", "90", "100", 1);
        insertQuestion(db, "Ko je izumeo naizmeničnu struju?",
                "Tomas Edison", "Nikola Tesla", "Mihajlo Pupin", "Isak Njutn", 1);
        insertQuestion(db, "Koliko nogu ima pauk?",
                "6", "8", "10", "12", 1);
        insertQuestion(db, "Koji je glavni grad Francuske?",
                "Lion", "Marsej", "Pariz", "Nica", 2);
        insertQuestion(db, "Koliko kontinenata postoji?",
                "5", "6", "7", "8", 2);
        insertQuestion(db, "Koji je najviši planinski vrh na svetu?",
                "K2", "Mont Everest", "Anapurna", "Kilimandžaro", 1);
        insertQuestion(db, "Koje boje je hlorofil?",
                "Crvene", "Plave", "Zelene", "Žute", 2);
        insertQuestion(db, "Koliko igrača ima fudbalski tim na terenu?",
                "9", "10", "11", "12", 2);
    }

    private void insertQuestion(SQLiteDatabase db, String text,
                                String a, String b, String c, String d, int correct) {
        ContentValues v = new ContentValues();
        v.put("text", text);
        v.put("answer_a", a);
        v.put("answer_b", b);
        v.put("answer_c", c);
        v.put("answer_d", d);
        v.put("correct_index", correct);
        db.insert(T_QUESTIONS, null, v);
    }

    private void seedSpojnice(SQLiteDatabase db) {
        // Set 1: izvođači i pesme
        insertPair(db, 1, "Bajaga", "Pozitivna geografija");
        insertPair(db, 1, "Riblja Čorba", "Lutka sa naslovne strane");
        insertPair(db, 1, "Zdravko Čolić", "Ti si mi u krvi");
        insertPair(db, 1, "EKV", "Krug");
        insertPair(db, 1, "Bijelo Dugme", "Lipe cvatu");
        // Set 2: države i glavni gradovi
        insertPair(db, 2, "Francuska", "Pariz");
        insertPair(db, 2, "Italija", "Rim");
        insertPair(db, 2, "Španija", "Madrid");
        insertPair(db, 2, "Grčka", "Atina");
        insertPair(db, 2, "Nemačka", "Berlin");
    }

    private void insertPair(SQLiteDatabase db, long setId, String left, String right) {
        ContentValues v = new ContentValues();
        v.put("set_id", setId);
        v.put("left_item", left);
        v.put("right_item", right);
        db.insert(T_SPOJNICE, null, v);
    }

    private void seedAsocijacije(SQLiteDatabase db) {
        long set1 = insertAssocSet(db, "LETO");
        insertAssocColumn(db, set1, 0, "ZRAK", "PLAŽA", "NAOČARE", "TOPLO", "SUNCE");
        insertAssocColumn(db, set1, 1, "MORE", "PUTOVANJE", "HOTEL", "KUFER", "ODMOR");
        insertAssocColumn(db, set1, 2, "HLADNO", "KORNET", "ČOKOLADA", "LETNJE", "SLADOLED");
        insertAssocColumn(db, set1, 3, "ŠKOLA", "DECA", "JUL", "SLOBODA", "RASPUST");

        long set2 = insertAssocSet(db, "ZIMA");
        insertAssocColumn(db, set2, 0, "PAHULJA", "BELINA", "GRUDVA", "SANKE", "SNEG");
        insertAssocColumn(db, set2, 1, "PRAZNIK", "JELKA", "DEDA MRAZ", "PONOĆ", "NOVA GODINA");
        insertAssocColumn(db, set2, 2, "STAZA", "KOPAONIK", "ŽIČARA", "SLALOM", "SKIJANJE");
        insertAssocColumn(db, set2, 3, "MESEC", "PRVI", "HLADNOĆA", "RASPUST", "JANUAR");
    }

    private long insertAssocSet(SQLiteDatabase db, String finalSolution) {
        ContentValues v = new ContentValues();
        v.put("final_solution", finalSolution);
        return db.insert(T_ASSOC_SETS, null, v);
    }

    private void insertAssocColumn(SQLiteDatabase db, long setId, int colIndex,
                                   String c1, String c2, String c3, String c4, String solution) {
        ContentValues v = new ContentValues();
        v.put("set_id", setId);
        v.put("col_index", colIndex);
        v.put("cell_1", c1);
        v.put("cell_2", c2);
        v.put("cell_3", c3);
        v.put("cell_4", c4);
        v.put("solution", solution);
        db.insert(T_ASSOC_COLUMNS, null, v);
    }
}
