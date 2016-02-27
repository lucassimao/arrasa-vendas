package br.com.arrasavendas.financeiro;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import br.com.arrasavendas.R;
import br.com.arrasavendas.model.MovimentoCaixa;

/**
 * Created by lsimaocosta on 11/02/16.
 */
public class ResumoCaixaFragment extends Fragment {

    private TextView textViewTotalEmDinheiro;
    private TextView textViewTotalNoCartao;
    private TableLayout table;
    private TextView textViewTotal;
    private TextView textViewMovimentos;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(
                R.layout.fragment_financeiro_resumo, container, false);

        Bundle args = getArguments();
        FinanceiroDAO dao = (FinanceiroDAO) args.getSerializable(FinanceiroPagerAdapter.FINANCEIRO_DAO);

        textViewTotalEmDinheiro = (TextView) rootView.findViewById(R.id.textViewTotalEmDinheiro);
        textViewTotalNoCartao = (TextView) rootView.findViewById(R.id.textViewTotalNoCartao);
        textViewTotal = (TextView) rootView.findViewById(R.id.textViewTotal);
        textViewMovimentos = (TextView) rootView.findViewById(R.id.textViewMovimentos);
        table = (TableLayout) rootView.findViewById(R.id.table);

        atualizarMovimentos(dao);

        NumberFormat formatter = NumberFormat.getCurrencyInstance();

        long totalEmDinheiro = dao.getTotalEmDinheiro();
        textViewTotalEmDinheiro.setText(formatter.format(totalEmDinheiro / 100.0));

        long totalNoCartao = dao.getTotalNoCartao();
        textViewTotalNoCartao.setText(formatter.format(totalNoCartao / 100.0));

        long total = totalEmDinheiro + totalNoCartao;
        textViewTotal.setText(formatter.format(total / 100.0));

        return rootView;
    }

    void atualizarMovimentos(FinanceiroDAO dao) {

        table.removeAllViews();

        LayoutInflater inflater = getActivity().getLayoutInflater();
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        NumberFormat formatter = NumberFormat.getCurrencyInstance();

        TableRow header = (TableRow) inflater.inflate(R.layout.table_header_movimentos, null);
        table.addView(header);


        BigDecimal totalVencimentos = BigDecimal.ZERO;

        for (MovimentoCaixa b : dao.getMovimentos()) {
            totalVencimentos = totalVencimentos.add(b.getValor());
            TableRow row = (TableRow) inflater.inflate(R.layout.table_row_movimentos, null);
            ((TextView) row.findViewById(R.id.descricao)).setText(b.getDescricao());
            ((TextView) row.findViewById(R.id.tipo)).setText(b.getTipoMovimento().toString());
            ((TextView) row.findViewById(R.id.data)).setText(df.format(b.getData()));
            ((TextView) row.findViewById(R.id.valor)).setText(nf.format(b.getValor()));

            table.addView(row);
        }

        textViewMovimentos.setText(formatter.format(totalVencimentos));

    }
}

