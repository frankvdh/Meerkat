package com.meerkat.ui.wificonnection;

import android.net.wifi.ScanResult;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.meerkat.R;

import java.util.List;

/**
 * Displays the ssid from a list of {@link ScanResult}s including a header at the top of
 * the {@link RecyclerView} to label the data.
 */
public class ApListAdapter extends RecyclerView.Adapter<ViewHolder> {
    private static final int HEADER_POSITION = 0;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private ScanResultClickListener scanResultClickListener;

    private final List<String> accessPoints;

    public ApListAdapter(List<String> list, ScanResultClickListener scanResultClickListener) {
        accessPoints = list;
        this.scanResultClickListener = scanResultClickListener;
    }

    public static class ViewHolderHeader extends RecyclerView.ViewHolder {
        public ViewHolderHeader(View view) {
            super(view);
        }
    }

    public class ViewHolderItem extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView ssidTextView;

        public ViewHolderItem(View view) {
            super(view);
            view.setOnClickListener(this);
            ssidTextView = view.findViewById(R.id.ssid_text_view);
        }

        @Override
        public void onClick(View view) {
            scanResultClickListener.onScanResultItemClick(getItem(getAdapterPosition()));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER)
        return new ViewHolderHeader(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.aplist_row_header, parent, false));
        return new ViewHolderItem(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.aplist_row_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        if (!(viewHolder instanceof ViewHolderHeader)) {
            ViewHolderItem viewHolderItem = (ViewHolderItem) viewHolder;
            String currentScanResult = getItem(position);
            viewHolderItem.ssidTextView.setText(currentScanResult);
        }
    }

    /*
     * Because we added a header item to the list, we need to decrement the position by one to get
     * the proper place in the list.
     */
    private String getItem(int position) {
        return accessPoints.get(position - 1);
    }

    // Returns size of list plus the header item (adds extra item).
    @Override
    public int getItemCount() {
        return accessPoints.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position == HEADER_POSITION ? TYPE_HEADER : TYPE_ITEM;
    }

    // Used to inform the class containing the RecyclerView that one of the ScanResult items in the
    // list was clicked.
    public interface ScanResultClickListener {
        void onScanResultItemClick(String scanResult);
    }
}

