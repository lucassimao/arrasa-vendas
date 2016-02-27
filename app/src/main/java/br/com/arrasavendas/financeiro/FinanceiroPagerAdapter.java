package br.com.arrasavendas.financeiro;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import br.com.arrasavendas.Application;

class FinanceiroPagerAdapter extends FragmentPagerAdapter {

    private final static String RESUMO_TAB = "Resumo";
    private String[] tabTitles;
    private Fragment[] fragments;
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

            tabTitles[i] = RESUMO_TAB;
        }

        fragments = new Fragment[tabTitles.length];

    }

    @Override
    public Fragment getItem(int position) {

        if (fragments[position] == null){

            Fragment fragment = null;
            Bundle args = new Bundle();
            args.putSerializable(FINANCEIRO_DAO, dao);

            if (!tabTitles[position].equals(RESUMO_TAB)){
                fragment = new VendedorFragment();
                args.putString(VendedorFragment.USERNAME, tabTitles[position]);
            }else{
                fragment = new ResumoCaixaFragment();
            }

            fragment.setArguments(args);

            fragments[position] = fragment;
        }

        return fragments[position];
    }

    @Override
    public int getCount() {
        if (tabTitles != null)
            return tabTitles.length;
         else return 0;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String pageTitle = tabTitles[position];
        int idx = pageTitle.indexOf('@');

        if (idx != -1)
            return pageTitle.substring(0, idx);
        else
            return pageTitle;
    }

}