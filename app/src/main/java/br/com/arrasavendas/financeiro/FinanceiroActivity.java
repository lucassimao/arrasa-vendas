package br.com.arrasavendas.financeiro;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.Observable;
import java.util.Observer;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.R;

/**
 * Created by lsimaocosta on 08/02/16.
 * <p/>
 * http://alexzh.com/tutorials/tablayout-android-design-support-library/
 */
public class FinanceiroActivity extends AppCompatActivity implements Observer {


    static final String FINANCEIRO_DAO = "FINANCEIRO_DAO";
    private ViewPager mViewPager;
    private FinanceiroPagerAdapter pagerAdapter;
    private FinanceiroDAO dao;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_financeiro);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.dao = new FinanceiroDAO(this);
        this.dao.addObserver(this);
        pagerAdapter = new FinanceiroPagerAdapter(getSupportFragmentManager(), dao);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(pagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(mViewPager);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.dao.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Application app = Application.getInstance();
        if (app.isAdmin()) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.financeiro_activity_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.contas:
                MovimentoCaixaDialog dlg = new MovimentoCaixaDialog();
                Bundle bundle = new Bundle();
                bundle.putSerializable(FINANCEIRO_DAO, this.dao);
                dlg.setArguments(bundle);
                dlg.show(getSupportFragmentManager(), "MovimentoCaixaDialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }


    @Override
    public void update(Observable observable, Object data) {
        if (observable == this.dao) {
            int currentItem = mViewPager.getCurrentItem();
            Fragment fragment = pagerAdapter.getItem(currentItem);

            if (fragment instanceof ResumoCaixaFragment) {
                ((ResumoCaixaFragment) fragment).atualizarMovimentos(dao);
            }
        }
    }
}
