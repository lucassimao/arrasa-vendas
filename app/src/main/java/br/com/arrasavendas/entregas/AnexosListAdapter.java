package br.com.arrasavendas.entregas;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import br.com.arrasavendas.R;
import br.com.arrasavendas.Utilities;

/**
 * Created by lsimaocosta on 25/02/16.
 */
class AnexosListAdapter extends BaseAdapter {

    private final Activity ctx;
    private final OnClickBtnExcluirAnexo listener;
    private final Bitmap PDF_BITMAP;
    private Bitmap[] thumbnails;
    private String[] anexos;

    public AnexosListAdapter(String[] anexos, Activity ctx, OnClickBtnExcluirAnexo listener) {
        super();
        this.ctx = ctx;
        this.anexos = anexos;
        this.listener = listener;
        PDF_BITMAP = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.pdf_icon);
        notifyDataSetChanged();
    }

    public String[] getAnexos() {
        return anexos;
    }

    public void setAnexos(String[] anexos) {
        this.anexos = anexos;
        notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        criarThumbnails();
    }

    private void criarThumbnails() {
        if (anexos != null) {
            int count = anexos.length;

            this.thumbnails = new Bitmap[count];

            for (int i = 0; i < count; i++) {
                String filename = anexos[i];
                String imagePath = Utilities.ImageFolder.ANEXOS.getPath(ctx) + filename;

                if (filename.toLowerCase().endsWith("pdf")) {
                    thumbnails[i] = PDF_BITMAP;
                } else {

                    if (new File(imagePath).exists()) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(imagePath,options);

                        // se for algum arquivo de image, cria um thumbnail
                        if (options.outWidth != -1 && options.outHeight != -1) {
                            options.inSampleSize = Utilities.calculateInSampleSize(options, 50, 50);
                            options.inJustDecodeBounds = false;
                            thumbnails[i] = BitmapFactory.decodeFile(imagePath, options);
                        }
                    } else
                        thumbnails[i] = null;

                }

            }
        }

    }

    @Override
    public int getCount() {
        if (anexos != null)
            return this.anexos.length;
        else
            return 0;
    }

    public Object getItem(int position) {
        return anexos[position];
    }

    @Override
    public long getItemId(int position) {
        return anexos[position].hashCode();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = ctx.getLayoutInflater().inflate(R.layout.list_item_anexos_manager, parent, false);

        ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);

        if (thumbnails[position] != null)
            imageView.setImageBitmap(thumbnails[position]);

        TextView textView = (TextView) convertView.findViewById(R.id.textView);
        textView.setText(anexos[position]);

        ImageButton imageButton = (ImageButton) convertView.findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null)
                    listener.excluirAnexo(position);
            }
        });
        return convertView;
    }

    public interface OnClickBtnExcluirAnexo {
        void excluirAnexo(int position);
    }

}
