package br.com.arrasavendas.entregas;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import br.com.arrasavendas.R;
import br.com.arrasavendas.model.Venda;

public class EditVendaDialog extends DialogFragment {

    public static final String VENDA = EditVendaDialog.class.getName() + "activity_venda";
    private Listener listener;
    private EditVendaPagerAdapter pagerAdapter;


    public static EditVendaDialog newInstance(Venda venda) {
        EditVendaDialog dlg = new EditVendaDialog();
        Bundle bundle = new Bundle();
        bundle.putSerializable(EditVendaDialog.VENDA, venda);
        dlg.setArguments(bundle);
        return dlg;
    }

    /**
     * CLICK do botao OK da janela
     */
    public void onClickOK() {
        if (listener != null) {
            pagerAdapter.commitChanges();
            Venda v = (Venda) getArguments().get(EditVendaDialog.VENDA);
            listener.onOK(v);
            dismiss();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_venda, null);
        Venda venda = (Venda) getArguments().get(EditVendaDialog.VENDA);
        getDialog().setTitle("Editar Venda");

        this.pagerAdapter = new EditVendaPagerAdapter(venda, getChildFragmentManager());
        ViewPager mViewPager = (ViewPager) view.findViewById(R.id.pager);
        mViewPager.setAdapter(pagerAdapter);

        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));


        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnCancelar = (Button) view.findViewById(R.id.btn_cancelar);
        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });
        Button btnOK = (Button) view.findViewById(R.id.btn_ok);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickOK();
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onOK(Venda venda);
    }

}
