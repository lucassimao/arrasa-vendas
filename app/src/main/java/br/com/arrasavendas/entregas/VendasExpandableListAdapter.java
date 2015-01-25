package br.com.arrasavendas.entregas;

import java.text.SimpleDateFormat;
import java.util.*;

import android.os.Bundle;
import android.widget.*;
import br.com.arrasavendas.venda.*;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import br.com.arrasavendas.R;
import br.com.arrasavendas.entregas.ExcluirVendaAsyncTask.OnComplete;

public class VendasExpandableListAdapter extends BaseExpandableListAdapter {

    private Map<Long, List<Venda>> vendasPorDataDeEntrega;
    private List<Long> datasDeEntregas;
    private LayoutInflater inflater;
    private Context ctx;
    private FragmentManager fragmentManager;
    private List<Venda> vendas;
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - EEEE", Locale.getDefault());

    public VendasExpandableListAdapter(FragmentManager fm, Context ctx, List<Venda> vendas) {
        this.ctx = ctx;
        this.fragmentManager = fm;
        sdf.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
        this.inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vendasPorDataDeEntrega = new HashMap<Long, List<Venda>>();
        this.vendas = vendas;
        atualizarDatasDeEntregas();

    }


    private void atualizarDatasDeEntregas() {

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

        if (venda.getVendedor() != null) {
            int blueLucas = ctx.getResources().getColor(R.color.blueLucas);
            int pinkAdna = ctx.getResources().getColor(R.color.pinkAdna);
            int amareloMClara = ctx.getResources().getColor(R.color.amareloMClara);

            switch (venda.getVendedor()) {
                case Lucas:
                    convertView.setBackgroundColor(blueLucas);
                    break;
                case Adna:
                    convertView.setBackgroundColor(pinkAdna);
                    break;
                case MariaClara:
                    convertView.setBackgroundColor(amareloMClara);
                    break;

            }
        }
        convertView.setTag(venda.getId());

        TextView txtCliente = (TextView) convertView.findViewById(R.id.txtCliente);
        txtCliente.setText(venda.getCliente().getNome());

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


        Button btnExcluir = (Button) convertView.findViewById(R.id.btnDelete);
        btnExcluir.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                new AlertDialog.Builder(ctx)
                        .setTitle("Rapaz ...")
                        .setMessage("Tem certeza?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                final ProgressDialog dlg = ProgressDialog.show(ctx, "Excluindo", "Aguarde ...");

                                new ExcluirVendaAsyncTask(venda.getId(), new OnComplete() {

                                    @Override
                                    public void run(HttpResponse response) {
                                        dlg.dismiss();

                                        int statusCode = response.getStatusLine().getStatusCode();

                                        switch (statusCode) {
                                            case HttpStatus.SC_NO_CONTENT:
                                                vendas.remove(venda);
                                                atualizarDatasDeEntregas();
                                                notifyDataSetChanged();
                                                Toast.makeText(ctx, "Venda excluida com sucesso!", Toast.LENGTH_LONG).show();
                                                break;
                                            case HttpStatus.SC_UNPROCESSABLE_ENTITY:
                                                Toast.makeText(ctx, "Erro ao excluir venda!", Toast.LENGTH_LONG).show();
                                                break;
                                            default:
                                                Toast.makeText(ctx, "Erro " + statusCode, Toast.LENGTH_SHORT).show();
                                        }

                                    }
                                }, ctx).execute();


                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
            }

        });

        Button btnEditAddress = (Button) convertView.findViewById(R.id.btnEditAddress);
        btnEditAddress.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                EditClienteDialog dlg = new EditClienteDialog();
                Bundle bundle = new Bundle();
                bundle.putSerializable("venda", venda);
                dlg.setArguments(bundle);

                dlg.setClienteDialogListener(new EditClienteDialog.ClienteDialogListener() {

                    @Override
                    public void onClienteDialogPositiveClick(View dialog) {

                        Cliente updatedCliente = new Cliente();

                        EditText editTextNome = (EditText) dialog.findViewById(R.id.editTextNome);
                        updatedCliente.setNome(editTextNome.getText().toString());

                        EditText editTextDDDTelefone = (EditText) dialog.findViewById(R.id.editTextDDDTelefone);
                        updatedCliente.setDddTelefone(editTextDDDTelefone.getText().toString());

                        EditText editTextTelefone = (EditText) dialog.findViewById(R.id.editTextTelefone);
                        updatedCliente.setTelefone(editTextTelefone.getText().toString());

                        EditText editTextDDDCelular = (EditText) dialog.findViewById(R.id.editTextDDDCelular);
                        updatedCliente.setDddCelular(editTextDDDCelular.getText().toString());

                        EditText editTextCelular = (EditText) dialog.findViewById(R.id.editTextCelular);
                        updatedCliente.setCelular(editTextCelular.getText().toString());

                        EditText editTextEndereco = (EditText) dialog.findViewById(R.id.editTextEndereco);
                        updatedCliente.setEndereco(editTextEndereco.getText().toString());

                        EditText editTextBairro = (EditText) dialog.findViewById(R.id.editTextBairro);
                        updatedCliente.setBairro(editTextBairro.getText().toString());

                        Spinner spinnerTurnoEntrega = (Spinner) dialog.findViewById(R.id.spinnerTurnoEntrega);
                        TurnoEntrega turnoEntrega = TurnoEntrega.valueOf(spinnerTurnoEntrega.getSelectedItem().toString());

                        CheckBox checkBoxJaPagou = (CheckBox) dialog.findViewById(R.id.cbJaPagou);
                        StatusVenda statusVenda = (checkBoxJaPagou.isChecked()) ? StatusVenda.PagamentoRecebido : StatusVenda.AguardandoPagamento;

                        atualizar(venda, updatedCliente, turnoEntrega, statusVenda);

                    }

                });

                dlg.show(fragmentManager, "tag");
            }

        });


        Button btnEditDataEntrega = (Button) convertView.findViewById(R.id.btnUpdateDataEntrega);
        btnEditDataEntrega.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                final Calendar dataEntrega = Calendar.getInstance();
                dataEntrega.setTime(venda.getDataEntrega());


                DatePickerDialog dlg = new DatePickerDialog(ctx, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker arg0, int ano, int mes, int diaDoMes) {

                        Calendar novaDataDeEntrega = GregorianCalendar.getInstance(TimeZone.getTimeZone("Etc/UTC"));
                        novaDataDeEntrega.set(Calendar.YEAR, ano);
                        novaDataDeEntrega.set(Calendar.MONTH, mes);
                        novaDataDeEntrega.set(Calendar.DAY_OF_MONTH, diaDoMes);

                        atualizarDataEntrega(venda, novaDataDeEntrega.getTime());

                    }


                }, dataEntrega.get(Calendar.YEAR), dataEntrega.get(Calendar.MONTH), dataEntrega.get(Calendar.DAY_OF_MONTH));

                dlg.setTitle("Atualize a data da entrega");

                dlg.show();
            }

        });


        if (venda.getItens() != null) {
            LinearLayout lLayout = (LinearLayout) convertView.findViewById(R.id.rowLayout);

            for (ItemVenda i : venda.getItens()) {
                TextView txtView = new TextView(this.ctx);
                txtView.setTextSize(13);
                LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(50, 0, 0, 0);
                txtView.setText(i.toString());

                lLayout.addView(txtView, layoutParams);
            }
        }

        return convertView;
    }

    private void atualizarDataEntrega(final Venda venda, final Date novaDataDeEntrega) {
        final ProgressDialog dlg = ProgressDialog.show(ctx, "Atualizando data de entrega", "Aguarde ...");

        new UpdateDataEntregaVendaAsyncTask(venda.getId(), novaDataDeEntrega, new br.com.arrasavendas.entregas.UpdateDataEntregaVendaAsyncTask.OnComplete() {

            @Override
            public void run(HttpResponse response) {
                dlg.dismiss();


                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
                    String string = "Erro ao atualizar venda, verifique se todos os campos foram preenchidos!";
                    Toast.makeText(ctx, string, Toast.LENGTH_LONG).show();

                } else if (statusCode == HttpStatus.SC_OK) {
                    venda.setDataEntrega(novaDataDeEntrega);
                    atualizarDatasDeEntregas();
                    notifyDataSetChanged();
                    Toast.makeText(ctx, "Data de entrega atualizada com sucesso!", Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(ctx, "Erro " + statusCode, Toast.LENGTH_SHORT).show();
                }

            }
        }, ctx).execute();
    }

    private void atualizar(final Venda venda, final Cliente updatedCliente, final TurnoEntrega turnoEntrega, final StatusVenda statusVenda) {

        final ProgressDialog dlg = ProgressDialog.show(ctx, "Atualizando venda", "Aguarde ...");

        new UpdateClienteVendaAsyncTask(venda.getId(), updatedCliente, turnoEntrega, statusVenda, new br.com.arrasavendas.entregas.UpdateClienteVendaAsyncTask.OnComplete() {

            @Override
            public void run(HttpResponse response) {
                dlg.dismiss();


                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
                    String string = "Erro ao atualizar venda, verifique se todos os campos foram preenchidos!";
                    Toast.makeText(ctx, string, Toast.LENGTH_LONG).show();

                } else if (statusCode == HttpStatus.SC_OK) {
                    notifyDataSetChanged();
                    venda.setCliente(updatedCliente);
                    venda.setTurnoEntrega(turnoEntrega);
                    venda.setStatus(statusVenda);
                    ordenarVendas();
                    Toast.makeText(ctx, "Venda atualizada com sucesso!", Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(ctx, "Erro " + statusCode, Toast.LENGTH_SHORT).show();
                }

            }
        }, ctx).execute();


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
