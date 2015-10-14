package br.com.arrasavendas.entregas;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import br.com.arrasavendas.R;
import br.com.arrasavendas.Utilities;
import br.com.arrasavendas.model.FormaPagamento;
import br.com.arrasavendas.model.ItemVenda;
import br.com.arrasavendas.model.StatusVenda;
import br.com.arrasavendas.model.TurnoEntrega;
import br.com.arrasavendas.model.Venda;

public class VendasExpandableListAdapter extends BaseExpandableListAdapter {

    private final int BLUE_LUCAS;
    private final int PINK_ADNA;
    private final int AMARELO_MCLARA;
    private Map<Long, List<Venda>> vendasPorDataDeEntrega;
    private List<Long> datasDeEntregas;
    private LayoutInflater inflater;
    private Context ctx;
    private List<Venda> vendas;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - EEEE", Locale.getDefault());

    public VendasExpandableListAdapter(Context ctx, List<Venda> vendas) {
        this.ctx = ctx;
        sdf.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
        this.inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Resources resources = ctx.getResources();
        this.BLUE_LUCAS = resources.getColor(R.color.blueLucas);
        this.PINK_ADNA = resources.getColor(R.color.pinkAdna);
        this.AMARELO_MCLARA = resources.getColor(R.color.amareloMClara);

        vendasPorDataDeEntrega = new HashMap<Long, List<Venda>>();
        this.vendas = vendas;
        atualizarDatasDeEntregas();

    }


    public void atualizarDatasDeEntregas() {

        datasDeEntregas = new LinkedList<Long>();

        for (Venda v : vendas) {
            Date dataEntrega = v.getDataEntrega();

            Calendar c = GregorianCalendar.getInstance(TimeZone.getTimeZone("Etc/UTC"));
            c.setTime(dataEntrega);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            long time = c.getTimeInMillis();

            if (!datasDeEntregas.contains(time)) {
                datasDeEntregas.add(time);
                vendasPorDataDeEntrega.put(time, new LinkedList<Venda>());
            }

            vendasPorDataDeEntrega.get(time).add(v);
        }

        Collections.sort(datasDeEntregas);
        Collections.reverse(datasDeEntregas);

        ordenarVendas();
        notifyDataSetChanged();
    }

    public void removerVenda(Venda venda) {
        vendas.remove(venda);
        atualizarDatasDeEntregas();
        notifyDataSetChanged();
    }

    public void refreshView() {
        ordenarVendas();
        notifyDataSetChanged();
    }

    private void ordenarVendas() {
        for (List<Venda> vendas : vendasPorDataDeEntrega.values()) {
            Collections.sort(vendas, new Comparator<Venda>() {
                @Override
                public int compare(Venda venda, Venda venda2) {

                    if (venda.getTurnoEntrega() == venda2.getTurnoEntrega())
                        return 0;
                    else
                        return venda.getTurnoEntrega().equals(TurnoEntrega.Tarde) ? 1 : -1;

                }
            });
        }
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.vendasPorDataDeEntrega.get(
                datasDeEntregas.get(groupPosition)).get(childPosition);

    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final Venda venda = (Venda) getChild(groupPosition, childPosition);

        convertView = inflater.inflate(R.layout.vendas_list_row_details, null);

        if (venda.getVendedor() == null)
            convertView.setBackgroundColor(Color.WHITE);
        else
            switch (venda.getVendedor()) {
                case Lucas:
                    convertView.setBackgroundColor(BLUE_LUCAS);
                    break;
                case Adna:
                    convertView.setBackgroundColor(PINK_ADNA);
                    break;
                case MariaClara:
                    convertView.setBackgroundColor(AMARELO_MCLARA);
                    break;
                default:

            }

        convertView.setTag(venda.getId());

        TextView txtCliente = (TextView) convertView.findViewById(R.id.txtCliente);
        txtCliente.setText(venda.getCliente().getNome());

        TextView txtValor = (TextView) convertView.findViewById(R.id.txtValor);
        txtValor.setText(String.format("R$ %.2f",venda.getValorTotal()));

        TextView txtTurno = (TextView) convertView.findViewById(R.id.txtTurno);
        txtTurno.setText(venda.getTurnoEntrega().name());

        if (venda.getStatus().equals(StatusVenda.PagamentoRecebido)) {
            ImageView img = (ImageView) convertView.findViewById(R.id.imgFormaPagamento);

            switch (venda.getFormaDePagamento()) {
                case AVista:
                    img.setBackgroundResource(R.drawable.dollar_currency_sign);
                    img.setVisibility(View.VISIBLE);
                    break;
                case PagSeguro:
                    img.setBackgroundResource(R.drawable.credit_card_icon);
                    img.setVisibility(View.VISIBLE);
                    break;

            }
        }

        if (venda.getStatus().equals(StatusVenda.AguardandoPagamento) && venda.getFormaDePagamento().equals(FormaPagamento.PagSeguro)) {
            ImageView img = (ImageView) convertView.findViewById(R.id.imgFormaPagamento);

            Drawable originalIcon = ctx.getResources().getDrawable(R.drawable.credit_card_icon);
            Drawable dimmedIcon = Utilities.convertDrawableToGrayScale(originalIcon);
            img.setBackground(dimmedIcon);
            img.setVisibility(View.VISIBLE);
        }

        if (venda.getItens() != null) {
            LinearLayout lLayout = (LinearLayout) convertView.findViewById(R.id.rowLayout);
            TextView txtView = (TextView) convertView.findViewById(R.id.txtItensVenda);
            txtView.setText(Html.fromHtml(TextUtils.join("<br>", venda.getItens())));
        }

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.vendasPorDataDeEntrega.get(
                datasDeEntregas.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        Long time = datasDeEntregas.get(groupPosition);
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("Etc/UTC"));
        c.setTimeInMillis(time);
        return sdf.format(c.getTime());
    }

    @Override
    public int getGroupCount() {
        return this.datasDeEntregas.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater
                    .inflate(R.layout.vendas_list_row_group, null);
        }
        String dataEntrega = (String) getGroup(groupPosition);
        ((CheckedTextView) convertView).setText(dataEntrega);
        ((CheckedTextView) convertView).setChecked(isExpanded);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        // TODO Auto-generated method stub
        return false;
    }


}
