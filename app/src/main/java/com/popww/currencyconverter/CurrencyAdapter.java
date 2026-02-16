// CurrencyAdapter.java
package com.popww.currencyconverter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CurrencyAdapter extends RecyclerView.Adapter<CurrencyAdapter.ViewHolder> {

    private List<Currency> currencies;
    private OnCurrencySelectedListener listener;
    private AlertDialog dialog;

    public interface OnCurrencySelectedListener {
        void onCurrencySelected(Currency currency);
    }

    public CurrencyAdapter(List<Currency> currencies, OnCurrencySelectedListener listener) {
        this.currencies = currencies;
        this.listener = listener;
    }

    public void setDialog(AlertDialog dialog) {
        this.dialog = dialog;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_currency, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Currency currency = currencies.get(position);
        holder.flagText.setText(currency.flag);
        holder.codeText.setText(currency.code);
        holder.nameText.setText(currency.symbol + " " + currency.name);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCurrencySelected(currency);
            }
            if (dialog != null) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public int getItemCount() {
        return currencies.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView flagText;
        TextView codeText;
        TextView nameText;

        ViewHolder(View itemView) {
            super(itemView);
            flagText = itemView.findViewById(R.id.currencyFlag);
            codeText = itemView.findViewById(R.id.currencyCode);
            nameText = itemView.findViewById(R.id.currencyName);
        }
    }
}