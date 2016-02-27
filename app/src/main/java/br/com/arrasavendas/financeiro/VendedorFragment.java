package br.com.arrasavendas.financeiro;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.arrasavendas.R;

/**
 * Created by lsimaocosta on 11/02/16.
 */
public class VendedorFragment extends Fragment {
    public static final String USERNAME = "VENDEDOR_USERNAME";

    private TextView textViewTotalEmDinheiro;
    private TextView textViewTotalNoCartao;
    private TextView textViewBonus;
    private TextView textViewSalario;
    private final int dourado = Color.rgb(222, 215, 11);
    private final int verde = Color.rgb(104, 241, 175);
    private final int azul = Color.rgb(164, 228, 251);

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_financeiro_vendedor, container, false);

        Bundle args = getArguments();
        String username = args.getString(USERNAME);
        FinanceiroDAO dao = (FinanceiroDAO) args.getSerializable(FinanceiroPagerAdapter.FINANCEIRO_DAO);

        textViewTotalEmDinheiro = (TextView) rootView.findViewById(R.id.textViewTotalEmDinheiro);
        textViewTotalNoCartao = (TextView) rootView.findViewById(R.id.textViewTotalNoCartao);
        textViewBonus = (TextView) rootView.findViewById(R.id.textViewBonus);
        textViewSalario = (TextView) rootView.findViewById(R.id.textViewSalario);

        NumberFormat formatter = NumberFormat.getCurrencyInstance();

        textViewTotalEmDinheiro.setText(formatter.format(dao.getTotalEmDinheiro(username) / 100.0));
        textViewTotalNoCartao.setText(formatter.format(dao.getTotalNoCartao(username) / 100.0));
        textViewBonus.setText(dao.getBonus(username).toString());
        textViewSalario.setText(formatter.format(dao.getSalario(username) / 100.0));

        setupBarChart(rootView,dao,username);

        return rootView;
    }

    private void setupBarChart(View rootView, FinanceiroDAO dao, String username) {

        BarChart mChart = (BarChart) rootView.findViewById(R.id.bar_chart);
        mChart.setDescription(null);
        mChart.setPinchZoom(false);
        mChart.setDrawBarShadow(false);
        mChart.setDrawGridBackground(false);

        Legend l = mChart.getLegend();
        l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);
        l.setYOffset(0f);
        l.setYEntrySpace(0f);
        l.setTextSize(10f);
        l.setCustom(new int[]{verde, dourado, azul}, new String[]{"À Vista", "Meta batida", "Cartão"});

        XAxis xAxis = mChart.getXAxis();
        xAxis.setLabelRotationAngle(-20);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelsToSkip(0);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setValueFormatter(new DinheiroFormater());
        leftAxis.setDrawGridLines(false);
        leftAxis.setSpaceTop(30f);
        leftAxis.setDrawLabels(false);

        mChart.getAxisRight().setEnabled(false);
        mChart.animateXY(2000, 2000);
        mChart.setData(getData(dao,username));
        mChart.invalidate();

    }

    private BarData getData(FinanceiroDAO dao, String username) {

        Map<String, FinanceiroDAO.HistoricoDetail> historico = dao.getHistorico(username);

        ArrayList<String> xVals = new ArrayList<String>();
        for (String week : historico.keySet())
            xVals.add(week);

        ArrayList<BarEntry> yValoresEmDinheiro = new ArrayList<BarEntry>();
        ArrayList<BarEntry> yvaloresNoCartao = new ArrayList<BarEntry>();

        for (int i = 0; i < xVals.size(); i++) {
            String week = xVals.get(i);
            FinanceiroDAO.HistoricoDetail historicoDetail = historico.get(week);

            float dinheiroEmReais = historicoDetail.dinheiro / 100f;
            float cartaoEmReais = historicoDetail.cartao / 100f;

            yValoresEmDinheiro.add(new BarEntry(dinheiroEmReais, i));
            yvaloresNoCartao.add(new BarEntry(cartaoEmReais, i));
        }

        List<String> strikes = dao.getStrikes(username);

        // create 3 datasets with different types
        BarDataSet set1 = new BarDataSet(yValoresEmDinheiro, "Vendas à Vista");
        if (strikes == null) {
            set1.setColor(verde);
        } else {
            int[] colors = new int[xVals.size()];

            for (int i = 0; i < xVals.size(); i++) {
                if (strikes.contains(xVals.get(i)))
                    colors[i] = dourado;
                else
                    colors[i] = verde;
            }
            set1.setColors(colors);
        }
        //set1.setStackLabels();
        set1.setValueTextSize(10);

        BarDataSet set2 = new BarDataSet(yvaloresNoCartao, "Vendas à cartão");
        set2.setColor(azul);
        set2.setValueTextSize(10);


        ArrayList<IBarDataSet> dataSets = new ArrayList<IBarDataSet>();
        dataSets.add(set1);
        dataSets.add(set2);

        BarData data = new BarData(xVals, dataSets);
        // add space between the dataset groups in percent of bar-width
        data.setGroupSpace(80f);
        data.setValueFormatter(new DinheiroFormater());

        return data;
    }

}