package br.com.arrasavendas.entregas;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import br.com.arrasavendas.R;
import br.com.arrasavendas.Utilities;

/**
 * Created by lsimaocosta on 25/02/16.
 */
class AnexosListAdapter extends BaseAdapter {

    private final Activity ctx;
    private final OnClickBtnExcluirAnexo listener;
    private Bitmap[] thumbnails;
    private String[] anexos;

    public interface OnClickBtnExcluirAnexo {
        void excluirAnexo(int position);
    }

    public AnexosListAdapter(String[] anexos, Activity ctx, OnClickBtnExcluirAnexo listener) {
        super();
        this.ctx = ctx;
        this.anexos = anexos;
        this.listener = listener;
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

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imagePath, options);

                // se for algum arquivo de image, cria um thumbnail
                if (options.outWidth != -1 && options.outHeight != -1) {

                    options.inSampleSize = Utilities.calculateInSampleSize(options, 50, 50);
                    options.inJustDecodeBounds = false;
                    thumbnails[i] = BitmapFactory.decodeFile(imagePath, options);

                } else {
                    // o arquivo eh do tipo pdf, n precisa de thumbnail
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
        // se for uma image
        if (thumbnails[position] != null)
            imageView.setImageBitmap(thumbnails[position]);
        else
            imageView.setImageResource(R.drawable.pdf_icon);

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

}
