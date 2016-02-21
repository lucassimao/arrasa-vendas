package br.com.arrasavendas.imagesManager;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import br.com.arrasavendas.R;

import java.io.File;

/**
 * Created by lsimaocosta on 27/07/15.
 */
public class ImageAdapter extends BaseAdapter {
    private final boolean[] thumbnailsSelection;
    private final int count;
    private final Bitmap[] thumbnails;
    private final String[] localPath;
    private final Context ctx;
    private LayoutInflater mInflater;

    public ImageAdapter(int count, boolean[] thumbnailsSelection, Bitmap[] thumbnails, String[] localPath,Context ctx) {
        this.count = count;
        this.ctx=ctx;
        this.localPath =localPath;
        this.thumbnailsSelection = thumbnailsSelection;
        this.thumbnails = thumbnails;
        mInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return this.count;
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(
                    R.layout.item_gallery, null);
            holder.imageview = (ImageView) convertView.findViewById(R.id.thumbImage);
            holder.checkbox = (CheckBox) convertView.findViewById(R.id.itemCheckBox);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.checkbox.setId(position);
        holder.imageview.setId(position);
        holder.checkbox.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                CheckBox cb = (CheckBox) v;
                int id = cb.getId();
                if (thumbnailsSelection[id]) {
                    cb.setChecked(false);
                    thumbnailsSelection[id] = false;
                } else {
                    cb.setChecked(true);
                    thumbnailsSelection[id] = true;
                }
            }
        });
        holder.imageview.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                int id = v.getId();

                File file = new File(localPath[id]);

                String authority = "br.com.arrasavendas.fileprovider";
                Uri uriForFile = FileProvider.getUriForFile(ctx,authority, file);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(uriForFile, "image/*");
                ctx.startActivity(intent);
            }
        });
        holder.imageview.setImageBitmap(thumbnails[position]);
        holder.checkbox.setChecked(thumbnailsSelection[position]);
        holder.id = position;
        return convertView;
    }
}
