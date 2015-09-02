package br.com.arrasavendas.imagesManager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;
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
    private ImagesManagerActivity imagesManagerActivity;
    private LayoutInflater mInflater;

    public ImageAdapter(ImagesManagerActivity imagesManagerActivity) {
        this.imagesManagerActivity = imagesManagerActivity;
        mInflater = (LayoutInflater) imagesManagerActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return imagesManagerActivity.count;
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
                    R.layout.galleryitem, null);
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
                if (imagesManagerActivity.thumbnailsSelection[id]) {
                    cb.setChecked(false);
                    imagesManagerActivity.thumbnailsSelection[id] = false;
                } else {
                    cb.setChecked(true);
                    imagesManagerActivity.thumbnailsSelection[id] = true;
                }
            }
        });
        holder.imageview.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                int id = v.getId();

                File file = new File(imagesManagerActivity.localPath[id]);

                String authority = "br.com.arrasavendas.fileprovider";
                Uri uriForFile = FileProvider.getUriForFile(imagesManagerActivity,
                        authority, file);

                Log.d("localpath", "" + file.exists());
                Log.d("localpath", imagesManagerActivity.localPath[id]);
                Log.d("localpath", uriForFile.toString());


                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(uriForFile, "image/*");
                imagesManagerActivity.startActivity(intent);
            }
        });
        holder.imageview.setImageBitmap(imagesManagerActivity.thumbnails[position]);
        holder.checkbox.setChecked(imagesManagerActivity.thumbnailsSelection[position]);
        holder.id = position;
        return convertView;
    }
}
