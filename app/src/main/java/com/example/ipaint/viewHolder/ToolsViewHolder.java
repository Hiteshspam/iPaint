package com.example.ipaint.viewHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipaint.Interface.ViewOnClick;
import com.example.ipaint.R;

import org.w3c.dom.Text;

public class ToolsViewHolder extends RecyclerView.ViewHolder {

    public ImageView icone;
    public TextView name;

    private ViewOnClick viewOnCLick;

    public void setViewOnCLick(ViewOnClick viewOnCLick) {
        this.viewOnCLick = viewOnCLick;
    }

    public ToolsViewHolder(@NonNull View itemView) {
        super(itemView);

        icone = itemView.findViewById(R.id.tools_icone);
        name = itemView.findViewById(R.id.tools_name);


        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                viewOnCLick.onClick(getAdapterPosition());

            }
        });
    }
}
