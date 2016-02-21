package br.com.arrasavendas.financeiro;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.NumberFormat;

/**
 * Created by lsimaocosta on 11/02/16.
 */
class DinheiroFormater implements ValueFormatter, YAxisValueFormatter {

    private NumberFormat formatter = NumberFormat.getCurrencyInstance();

    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        return getFormattedValue(value, null);
    }

    @Override
    public String getFormattedValue(float value, YAxis yAxis) {
        if (value > 0) {
            return formatter.format(value);
        } else
            return "";
    }
}