package com.rax.autostabilizer.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.rax.autostabilizer.Models.Stabilizer;
import com.rax.autostabilizer.R;

import java.util.ArrayList;
import java.util.List;

public class StabilizerListAdapter extends RecyclerView.Adapter<StabilizerListAdapter.StabilizerViewHolder> {

    ClickListener mListener;
    public List<Stabilizer> mStabilizerList;

    public StabilizerListAdapter(ClickListener mListener) {
        this.mListener = mListener;
        mStabilizerList = new ArrayList<>();
    }

    @NonNull
    @Override
    public StabilizerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stabilizer, parent, false);
        return new StabilizerViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StabilizerViewHolder holder, int position) {
        Stabilizer stabilizer = mStabilizerList.get(position);
        holder.txtStabilizerName.setText(stabilizer.getName());
        holder.txtStabilizerIP.setText(stabilizer.getIPAddress());
        holder.txtStabilizerMac.setText(stabilizer.getMacAddress());
    }

    @Override
    public int getItemCount() {
        return mStabilizerList.size();
    }

    public void setData(List<Stabilizer> mStabilizerList) {
        DiffUtil.DiffResult updatedList = DiffUtil.calculateDiff(new MyDiffCallback(mStabilizerList, this.mStabilizerList));
        this.mStabilizerList.clear();
        this.mStabilizerList.addAll(mStabilizerList);
        updatedList.dispatchUpdatesTo(this);
    }

    public interface ClickListener {
        public void OnStabilizerLongClick(int pos,Stabilizer stabilizer,View v);
        public void OnStabilizerClicked(Stabilizer stabilizer);
    }

    public static class MyDiffCallback extends DiffUtil.Callback {

        List<Stabilizer> oldList;
        List<Stabilizer> newList;

        MyDiffCallback(List<Stabilizer> newList, List<Stabilizer> oldList) {
            this.newList = newList;
            this.oldList = oldList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return
                    oldList.get(oldItemPosition).getMacAddress().equals(newList.get(newItemPosition).getMacAddress()) &&
                            oldList.get(oldItemPosition).getIPAddress().equals(newList.get(newItemPosition).getIPAddress());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return
                    oldList.get(oldItemPosition).getMacAddress().equals(newList.get(newItemPosition).getMacAddress()) &&
                            oldList.get(oldItemPosition).getIPAddress().equals(newList.get(newItemPosition).getIPAddress());
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            //you can return particular field for changed item.
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }

    public class StabilizerViewHolder extends RecyclerView.ViewHolder {
        TextView txtStabilizerName, txtStabilizerIP, txtStabilizerMac;
        CardView parent;

        public StabilizerViewHolder(@NonNull View itemView) {
            super(itemView);
            txtStabilizerName = itemView.findViewById(R.id.txtStabilizerName);
            txtStabilizerIP = itemView.findViewById(R.id.txtStabilizerIP);
            txtStabilizerMac= itemView.findViewById(R.id.txtStabilizerMac);
            parent = itemView.findViewById(R.id.item_parent);
            parent.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    mListener.OnStabilizerLongClick(getAdapterPosition(), mStabilizerList.get(getAdapterPosition()), view);
                    return true;
                }
            });
            parent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.OnStabilizerClicked(mStabilizerList.get(getAdapterPosition()));
                }
            });
        }
    }
}
