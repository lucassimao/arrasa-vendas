package br.com.arrasavendas.financeiro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import br.com.arrasavendas.DatabaseHelper;
import br.com.arrasavendas.model.MovimentoCaixa;

/**
 * Created by lsimaocosta on 09/02/16.
 */
public class FinanceiroDAO extends Observable implements Serializable {

    private final String COLUMN_JSON = "json";

    private transient SQLiteDatabase db;
    private transient JSONObject obj;

    public FinanceiroDAO(Context ctx) {
        this.db = new DatabaseHelper(ctx).getWritableDatabase();
    }

    public void close(){
        if(this.db != null)
            this.db.close();
    }

    public void deleteAll() {
        int rows = this.db.delete(DatabaseHelper.TABLE_FINANCEIRO, null, null);
    }

    public void save(String jsonString) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_JSON, jsonString);
        long _rowId = this.db.insert(DatabaseHelper.TABLE_FINANCEIRO, null, cv);
    }

    public long getTotalEmDinheiro(String usernameVendedor) {
        try {
            JSONObject vendedor = getVendedor(usernameVendedor);
            return vendedor.getLong("dinheiro");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public long getTotalNoCartao(String usernameVendedor) {
        try {
            JSONObject vendedor = getVendedor(usernameVendedor);
            return vendedor.getLong("cartao");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Integer getBonus(String usernameVendedor) {
        try {
            JSONObject vendedor = getVendedor(usernameVendedor);
            if (vendedor.has("strikes")) {
                JSONArray strikes = vendedor.getJSONArray("strikes");
                return strikes.length();
            } else
                return 0;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public long getSalario(String usernameVendedor) {
        try {
            JSONObject vendedor = getVendedor(usernameVendedor);
            return vendedor.getLong("salario");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private final JSONObject getJSONObject() {
        if (this.obj == null) {
            String[] projections = {COLUMN_JSON};
            Cursor cursor = db.query(DatabaseHelper.TABLE_FINANCEIRO, projections, null, null, null, null, null);

            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                String json = cursor.getString(cursor.getColumnIndex(COLUMN_JSON));

                try {
                    this.obj = new JSONObject(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            cursor.close();
        }

        return this.obj;
    }


    public Map<String, HistoricoDetail> getHistorico(String usernameVendedor) {
        Map<String, HistoricoDetail> map = new LinkedHashMap<>();

        try {
            JSONObject vendedor = getVendedor(usernameVendedor);
            JSONObject historico = vendedor.getJSONObject("historico");
            Iterator<String> keys = historico.keys();

            while (keys.hasNext()) {
                String week = keys.next();
                JSONObject detail = historico.getJSONObject(week);
                map.put(week, new HistoricoDetail(detail.getLong("dinheiro"), detail.getLong("cartao")));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }

    private final JSONObject getVendedor(String usernameVendedor) throws JSONException {
        JSONObject vendedores = getJSONObject().getJSONObject("vendedores");
        return vendedores.getJSONObject(usernameVendedor);
    }

    public List<String> getStrikes(String usernameVendedor) {
        try {

            JSONObject vendedor = getVendedor(usernameVendedor);

            if (!vendedor.has("strikes"))
                return null;
            else {
                List<String> list = new LinkedList<>();
                DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                DateFormat df2 = new SimpleDateFormat("dd/MM");

                JSONArray strikes = vendedor.getJSONArray("strikes");

                for (int i = 0; i < strikes.length(); ++i) {
                    JSONObject stk = strikes.getJSONObject(i);

                    Date weekStart = df.parse(stk.getString("week_start"));
                    Date weekEnd = df.parse(stk.getString("week_end"));

                    list.add(df2.format(weekStart) + " - " + df2.format(weekEnd));
                }
                return list;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String[] getVendedores() {
        String[] loginVendedores = null;

        try {

            JSONObject obj = getJSONObject();
            JSONObject vendedores = obj.getJSONObject("vendedores");
            loginVendedores = new String[vendedores.length()];
            Iterator<String> i = vendedores.keys();
            int idx = 0;
            while (i.hasNext()) {
                loginVendedores[idx++] = i.next();
            }

            return loginVendedores;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return loginVendedores;
    }

    public long getTotalEmDinheiro() {
        String[] vendedores = getVendedores();
        long total = 0;
        for (String username : vendedores)
            total += getTotalEmDinheiro(username);

        return total;
    }

    public long getTotalNoCartao() {
        String[] vendedores = getVendedores();
        long total = 0;

        for (String username : vendedores)
            total += getTotalNoCartao(username);

        return total;
    }

    public List<MovimentoCaixa> getMovimentos() {
        List<MovimentoCaixa> movimentos = new LinkedList<>();
        try {

            JSONObject obj = getJSONObject();
            JSONArray movs = obj.getJSONArray("movimentos");

            for (int i = 0; i < movs.length(); ++i)
                movimentos.add(new MovimentoCaixa(movs.getJSONObject(i)));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return movimentos;
    }

    public boolean addMovimento(MovimentoCaixa mc,long lastUpdatedTimestamp){
        try {
            JSONObject obj = getJSONObject();
            JSONArray movs = obj.getJSONArray("movimentos");
            movs.put(mc.toJSONObject());

            // atualizando o last_updated das informações financeiras
            obj.put("last_updated",lastUpdatedTimestamp);

            deleteAll();
            save(obj.toString());
            setChanged();
            notifyObservers();

            return true;

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

    }

    public Long lastUpdated() {
        try {
            JSONObject obj = getJSONObject();
            if (obj != null)
                return obj.getLong("last_updated");
            else
                return 0L;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    public class HistoricoDetail {
        public final long dinheiro, cartao;

        public HistoricoDetail(long dinheiro, long cartao) {
            this.cartao = cartao;
            this.dinheiro = dinheiro;
        }

        @Override
        public String toString() {
            return "HistoricoDetail{" +
                    "dinheiro=" + dinheiro +
                    ", cartao=" + cartao +
                    '}';
        }
    }
}
