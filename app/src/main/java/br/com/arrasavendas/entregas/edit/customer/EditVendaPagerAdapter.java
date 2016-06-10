package br.com.arrasavendas.entregas.edit.customer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import br.com.arrasavendas.model.Venda;

class EditVendaPagerAdapter extends FragmentPagerAdapter {

    private final String[] tabTitles = {"Cliente", "Entrega", "Pagamento","Correios"};
    private final Venda venda;
    private Fragment[] fragments;
    private String TAG = EditVendaPagerAdapter.class.getSimpleName();


    public EditVendaPagerAdapter(Venda venda, FragmentManager fm) {
        super(fm);
        fragments = new Fragment[tabTitles.length];
        this.venda=venda;
    }

    @Override
    public Fragment getItem(int position) {

        if (fragments[position] == null) {

            Fragment fragment = null;
            Bundle args = new Bundle();
            args.putSerializable(EditVendaDialog.VENDA, venda);

            switch (position) {
                case 0:
                    fragment = new EditClientFragment();
                    break;
                case 1:
                    fragment = new EditDeliveryFragment();
                    break;
                case 2:
                    fragment = new EditPaymentFragment();
                    break;
                case 3:
                    fragment = new EditCorreiosFragment();
                    break;
            }
            fragment.setArguments(args);
            fragments[position] = fragment;
        }
        return fragments[position];
    }

    @Override
    public int getCount() {
        return tabTitles.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }

}