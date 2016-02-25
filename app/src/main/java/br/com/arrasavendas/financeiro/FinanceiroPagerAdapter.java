package br.com.arrasavendas.financeiro;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.model.FinanceiroDAO;

class FinanceiroPagerAdapter extends FragmentPagerAdapter {

    private String[] tabTitles;
    private FinanceiroDAO dao;
    public static final String FINANCEIRO_DAO = "FINANCEIRO_DAO";


    public FinanceiroPagerAdapter(FragmentManager fm,FinanceiroDAO dao) {
        super(fm);

        this.dao = dao;
        Application app = Application.getInstance();

        if (!app.isAdmin()) {
            String username = app.getCurrentUser();
            tabTitles = new String[]{username};
        }else{
            String[] vendedores = dao.getVendedores();
            tabTitles = new String[vendedores.length+1];
            int i = 0;
            for(String v : vendedores)
                tabTitles[i++] = v;

            tabTitles[i] = "Resumo";
        }

    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        Bundle args = new Bundle();
        args.putSerializable(FINANCEIRO_DAO, dao);

        if (!tabTitles[position].equals("Resumo")){
            fragment = new VendedorFragment();
            args.putString(VendedorFragment.USERNAME, tabTitles[position]);
            fragment.setArguments(args);
        }else{
            fragment = new ResumoCaixaFragment();
            fragment.setArguments(args);
        }


        return fragment;
    }

    @Override
    public int getCount() {
        if (tabTitles != null)
            return tabTitles.length;
         else return 0;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String username = tabTitles[position];
        int idx = username.indexOf('@');

        if (idx != -1)
            return username.substring(0, idx);
        else
            return username;
    }

}