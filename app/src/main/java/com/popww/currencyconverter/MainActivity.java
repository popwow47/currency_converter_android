// MainActivity.java
package com.popww.currencyconverter;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText amountInput;
    private TextView fromCurrencyText, fromCurrencyName, toCurrencyText, toCurrencyName;
    private TextView fromCurrencyFlag, toCurrencyFlag;
    private TextView resultText, statusText, lastUpdateText, apiCounterText, exchangeRateText;
    private ImageView statusIcon, swapButton, themeButton;
    private Button updateButton;
    private CardView resultCard, apiCounterCard, fromCurrencyCard, toCurrencyCard;
    private ProgressBar progressBar;

    private HashMap<String, Double> fiatRates;
    private HashMap<String, HashMap<String, Double>> cryptoRates;
    private ExecutorService executorService;
    private Handler mainHandler;

    private static final String PREFS_NAME = "CurrencyConverterPrefs";
    private static final String KEY_REQUEST_COUNT = "request_count";
    private static final String KEY_REQUEST_DATE = "request_date";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final int MAX_REQUESTS = 1500; //2880; // CoinGecko лимит
    private int requestCount = 0;
    private SharedPreferences prefs;

    private List<Currency> currencies;
    private Currency selectedFromCurrency;
    private Currency selectedToCurrency;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        boolean isDarkMode = prefs.getBoolean(KEY_THEME_MODE, false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeCurrencies();
        loadRequestCount();
        initializeViews();
        setupListeners();
        updateApiCounter();

        fiatRates = new HashMap<>();
        cryptoRates = new HashMap<>();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        selectedFromCurrency = currencies.get(0); // USD
        selectedToCurrency = currencies.get(1);   // EUR
        updateCurrencyDisplays();

        fetchExchangeRates();
    }

    private void initializeCurrencies() {
        currencies = new ArrayList<>();

        // Фиатные валюты (32 основных)
        currencies.add(new Currency("USD", "Доллар США", "$", "🇺🇸", "fiat", null));
        currencies.add(new Currency("EUR", "Евро", "€", "🇪🇺", "fiat", null));
        currencies.add(new Currency("GBP", "Фунт стерлингов", "£", "🇬🇧", "fiat", null));
        currencies.add(new Currency("JPY", "Японская йена", "¥", "🇯🇵", "fiat", null));
        currencies.add(new Currency("CNY", "Китайский юань", "¥", "🇨🇳", "fiat", null));
        currencies.add(new Currency("RUB", "Российский рубль", "₽", "🇷🇺", "fiat", null));
        currencies.add(new Currency("UAH", "Украинская гривна", "₴", "🇺🇦", "fiat", null));
        currencies.add(new Currency("PLN", "Польский злотый", "zł", "🇵🇱", "fiat", null));
        currencies.add(new Currency("CHF", "Швейцарский франк", "Fr", "🇨🇭", "fiat", null));
        currencies.add(new Currency("CAD", "Канадский доллар", "C$", "🇨🇦", "fiat", null));
        currencies.add(new Currency("AUD", "Австралийский доллар", "A$", "🇦🇺", "fiat", null));
        currencies.add(new Currency("NZD", "Новозеландский доллар", "NZ$", "🇳🇿", "fiat", null));
        currencies.add(new Currency("SEK", "Шведская крона", "kr", "🇸🇪", "fiat", null));
        currencies.add(new Currency("NOK", "Норвежская крона", "kr", "🇳🇴", "fiat", null));
        currencies.add(new Currency("DKK", "Датская крона", "kr", "🇩🇰", "fiat", null));
        currencies.add(new Currency("TRY", "Турецкая лира", "₺", "🇹🇷", "fiat", null));
        currencies.add(new Currency("INR", "Индийская рупия", "₹", "🇮🇳", "fiat", null));
        currencies.add(new Currency("BRL", "Бразильский реал", "R$", "🇧🇷", "fiat", null));
        currencies.add(new Currency("MXN", "Мексиканское песо", "$", "🇲🇽", "fiat", null));
        currencies.add(new Currency("ZAR", "Южноафриканский рэнд", "R", "🇿🇦", "fiat", null));
        currencies.add(new Currency("AED", "Дирхам ОАЭ", "د.إ", "🇦🇪", "fiat", null));
        currencies.add(new Currency("SAR", "Саудовский риял", "﷼", "🇸🇦", "fiat", null));
        currencies.add(new Currency("ILS", "Израильский шекель", "₪", "🇮🇱", "fiat", null));
        currencies.add(new Currency("KRW", "Южнокорейская вона", "₩", "🇰🇷", "fiat", null));
        currencies.add(new Currency("SGD", "Сингапурский доллар", "S$", "🇸🇬", "fiat", null));
        currencies.add(new Currency("HKD", "Гонконгский доллар", "HK$", "🇭🇰", "fiat", null));
        currencies.add(new Currency("THB", "Тайский бат", "฿", "🇹🇭", "fiat", null));
        currencies.add(new Currency("MYR", "Малайзийский ринггит", "RM", "🇲🇾", "fiat", null));
        currencies.add(new Currency("IDR", "Индонезийская рупия", "Rp", "🇮🇩", "fiat", null));
        currencies.add(new Currency("PHP", "Филиппинское песо", "₱", "🇵🇭", "fiat", null));
        currencies.add(new Currency("VND", "Вьетнамский донг", "₫", "🇻🇳", "fiat", null));
        currencies.add(new Currency("PKR", "Пакистанская рупия", "Rs", "🇵🇰", "fiat", null));

        // Криптовалюты (30 штук)
        currencies.add(new Currency("BTC", "Bitcoin", "₿", "🟠", "crypto", "bitcoin"));
        currencies.add(new Currency("ETH", "Ethereum", "Ξ", "🔷", "crypto", "ethereum"));
        currencies.add(new Currency("USDT", "Tether", "₮", "🟢", "crypto", "tether"));
        currencies.add(new Currency("BNB", "Binance Coin", "BNB", "🟡", "crypto", "binancecoin"));
        currencies.add(new Currency("XRP", "Ripple", "XRP", "⚪", "crypto", "ripple"));
        currencies.add(new Currency("ADA", "Cardano", "₳", "🔵", "crypto", "cardano"));
        currencies.add(new Currency("DOGE", "Dogecoin", "Ð", "🟡", "crypto", "dogecoin"));
        currencies.add(new Currency("SOL", "Solana", "◎", "🟣", "crypto", "solana"));
        currencies.add(new Currency("DOT", "Polkadot", "•", "🔴", "crypto", "polkadot"));
        currencies.add(new Currency("MATIC", "Polygon", "MATIC", "🟣", "crypto", "matic-network"));
        currencies.add(new Currency("LTC", "Litecoin", "Ł", "⚪", "crypto", "litecoin"));
        currencies.add(new Currency("SHIB", "Shiba Inu", "SHIB", "🔴", "crypto", "shiba-inu"));
        currencies.add(new Currency("TRX", "Tron", "TRX", "🔴", "crypto", "tron"));
        currencies.add(new Currency("AVAX", "Avalanche", "AVAX", "🔴", "crypto", "avalanche-2"));
        currencies.add(new Currency("UNI", "Uniswap", "UNI", "🦄", "crypto", "uniswap"));
        currencies.add(new Currency("LINK", "Chainlink", "LINK", "🔵", "crypto", "chainlink"));
        currencies.add(new Currency("XLM", "Stellar", "*", "⚫", "crypto", "stellar"));
        currencies.add(new Currency("ATOM", "Cosmos", "ATOM", "🔵", "crypto", "cosmos"));
        currencies.add(new Currency("XMR", "Monero", "ɱ", "🟠", "crypto", "monero"));
        currencies.add(new Currency("ETC", "Ethereum Classic", "ΞC", "🟢", "crypto", "ethereum-classic"));
        currencies.add(new Currency("BCH", "Bitcoin Cash", "BCH", "🟢", "crypto", "bitcoin-cash"));
        currencies.add(new Currency("ALGO", "Algorand", "ALGO", "⚫", "crypto", "algorand"));
        currencies.add(new Currency("VET", "VeChain", "VET", "🔵", "crypto", "vechain"));
        currencies.add(new Currency("FIL", "Filecoin", "FIL", "🔵", "crypto", "filecoin"));
        currencies.add(new Currency("ICP", "Internet Computer", "ICP", "🟣", "crypto", "internet-computer"));
        currencies.add(new Currency("NEAR", "NEAR Protocol", "NEAR", "⚫", "crypto", "near"));
        currencies.add(new Currency("APT", "Aptos", "APT", "🔵", "crypto", "aptos"));
        currencies.add(new Currency("HBAR", "Hedera", "ℏ", "⚫", "crypto", "hedera-hashgraph"));
        currencies.add(new Currency("QNT", "Quant", "QNT", "⚪", "crypto", "quant-network"));
        currencies.add(new Currency("ARB", "Arbitrum", "ARB", "🔵", "crypto", "arbitrum"));
    }

    private void initializeViews() {
        amountInput = findViewById(R.id.amountInput);
        fromCurrencyCard = findViewById(R.id.fromCurrencyCard);
        toCurrencyCard = findViewById(R.id.toCurrencyCard);
        fromCurrencyFlag = findViewById(R.id.fromCurrencyFlag);
        fromCurrencyText = findViewById(R.id.fromCurrencyText);
        fromCurrencyName = findViewById(R.id.fromCurrencyName);
        toCurrencyFlag = findViewById(R.id.toCurrencyFlag);
        toCurrencyText = findViewById(R.id.toCurrencyText);
        toCurrencyName = findViewById(R.id.toCurrencyName);
        resultText = findViewById(R.id.resultText);
        statusText = findViewById(R.id.statusText);
        statusIcon = findViewById(R.id.statusIcon);
        swapButton = findViewById(R.id.swapButton);
        updateButton = findViewById(R.id.updateButton);
        resultCard = findViewById(R.id.resultCard);
        lastUpdateText = findViewById(R.id.lastUpdateText);
        progressBar = findViewById(R.id.progressBar);
        apiCounterText = findViewById(R.id.apiCounterText);
        apiCounterCard = findViewById(R.id.apiCounterCard);
        themeButton = findViewById(R.id.themeButton);
        exchangeRateText = findViewById(R.id.exchangeRateText);

        amountInput.setText("1");
    }

    private void loadRequestCount() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String savedDate = prefs.getString(KEY_REQUEST_DATE, "");

        if (today.equals(savedDate)) {
            requestCount = prefs.getInt(KEY_REQUEST_COUNT, 0);
        } else {
            requestCount = 0;
            saveRequestCount();
        }
    }

    private void saveRequestCount() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_REQUEST_COUNT, requestCount);
        editor.putString(KEY_REQUEST_DATE, today);
        editor.apply();
    }

    private void incrementRequestCount() {
        requestCount++;
        saveRequestCount();
        updateApiCounter();
    }

    private boolean canMakeRequest() {
        return requestCount < MAX_REQUESTS;
    }

    private int getRemainingRequests() {
        return MAX_REQUESTS - requestCount;
    }

    private void updateApiCounter() {
        String counterText = requestCount + " из " + MAX_REQUESTS + " запросов";
        apiCounterText.setText(counterText);

        int remaining = getRemainingRequests();
        if (remaining <= 0) {
            apiCounterCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        } else if (remaining < 100) {
            apiCounterCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
        } else {
            apiCounterCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        }
    }

    private void updateCurrencyDisplays() {
        fromCurrencyFlag.setText(selectedFromCurrency.flag);
        fromCurrencyText.setText(selectedFromCurrency.code);
        fromCurrencyName.setText(selectedFromCurrency.symbol + " " + selectedFromCurrency.name);

        toCurrencyFlag.setText(selectedToCurrency.flag);
        toCurrencyText.setText(selectedToCurrency.code);
        toCurrencyName.setText(selectedToCurrency.symbol + " " + selectedToCurrency.name);

        updateExchangeRate();
    }

    private void updateExchangeRate() {
        double rate = convertCurrency(1.0, selectedFromCurrency, selectedToCurrency);
        if (rate > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date());
            DecimalFormat df = new DecimalFormat("#.####");
            exchangeRateText.setText(dateStr + "\n1 " + selectedFromCurrency.code + " = " +
                    df.format(rate) + " " + selectedToCurrency.code);
            exchangeRateText.setVisibility(View.VISIBLE);
        } else {
            exchangeRateText.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString();

                if (!input.matches("^\\d*\\.?\\d*$")) {
                    String cleaned = input.replaceAll("[^\\d.]", "");
                    int dotIndex = cleaned.indexOf('.');
                    if (dotIndex != -1) {
                        String beforeDot = cleaned.substring(0, dotIndex + 1);
                        String afterDot = cleaned.substring(dotIndex + 1).replace(".", "");
                        cleaned = beforeDot + afterDot;
                    }
                    amountInput.removeTextChangedListener(this);
                    amountInput.setText(cleaned);
                    amountInput.setSelection(cleaned.length());
                    amountInput.addTextChangedListener(this);
                }
                convertAndDisplay();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        fromCurrencyCard.setOnClickListener(v -> showCurrencyPickerDialog(true));
        toCurrencyCard.setOnClickListener(v -> showCurrencyPickerDialog(false));

        swapButton.setOnClickListener(v -> {
            Currency temp = selectedFromCurrency;
            selectedFromCurrency = selectedToCurrency;
            selectedToCurrency = temp;
            updateCurrencyDisplays();
            convertAndDisplay();
        });

        updateButton.setOnClickListener(v -> fetchExchangeRates());
        themeButton.setOnClickListener(v -> toggleTheme());
    }

    private void toggleTheme() {
        boolean isDarkMode = prefs.getBoolean(KEY_THEME_MODE, false);
        isDarkMode = !isDarkMode;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_THEME_MODE, isDarkMode);
        editor.apply();

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void showCurrencyPickerDialog(boolean isFrom) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_currency_picker, null);

        EditText searchField = dialogView.findViewById(R.id.searchField);
        RecyclerView recyclerView = dialogView.findViewById(R.id.currencyRecyclerView);
        Button btnAll = dialogView.findViewById(R.id.btnFilterAll);
        Button btnFiat = dialogView.findViewById(R.id.btnFilterFiat);
        Button btnCrypto = dialogView.findViewById(R.id.btnFilterCrypto);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Currency> filteredList = new ArrayList<>(currencies);
        CurrencyAdapter adapter = new CurrencyAdapter(filteredList, currency -> {
            if (isFrom) {
                selectedFromCurrency = currency;
            } else {
                selectedToCurrency = currency;
            }
            updateCurrencyDisplays();
            convertAndDisplay();
        });

        recyclerView.setAdapter(adapter);

        Runnable updateFilter = () -> {
            filteredList.clear();
            String query = searchField.getText().toString().toLowerCase();

            for (Currency currency : currencies) {
                boolean matchesFilter = currentFilter.equals("all") ||
                        (currentFilter.equals("fiat") && currency.type.equals("fiat")) ||
                        (currentFilter.equals("crypto") && currency.type.equals("crypto"));

                boolean matchesSearch = query.isEmpty() ||
                        currency.code.toLowerCase().contains(query) ||
                        currency.name.toLowerCase().contains(query);

                if (matchesFilter && matchesSearch) {
                    filteredList.add(currency);
                }
            }
            adapter.notifyDataSetChanged();
        };

        View.OnClickListener filterListener = v -> {
            btnAll.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            btnFiat.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            btnCrypto.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

            if (v == btnAll) {
                currentFilter = "all";
                btnAll.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
            } else if (v == btnFiat) {
                currentFilter = "fiat";
                btnFiat.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
            } else if (v == btnCrypto) {
                currentFilter = "crypto";
                btnCrypto.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
            }
            updateFilter.run();
        };

        btnAll.setOnClickListener(filterListener);
        btnFiat.setOnClickListener(filterListener);
        btnCrypto.setOnClickListener(filterListener);

        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFilter.run();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnAll.performClick();

        builder.setView(dialogView);
        builder.setTitle("Выберите валюту");
        builder.setNegativeButton("Закрыть", null);

        AlertDialog dialog = builder.create();
        adapter.setDialog(dialog);
        dialog.show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void updateConnectionStatus(boolean isConnected) {
        if (isConnected) {
            statusIcon.setImageResource(android.R.drawable.presence_online);
            statusText.setText("Подключено");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            statusIcon.setImageResource(android.R.drawable.presence_offline);
            statusText.setText("Нет подключения");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void fetchExchangeRates() {
        if (!canMakeRequest()) {
            Toast.makeText(this, "Лимит запросов исчерпан! (" + MAX_REQUESTS + "/день)",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!isNetworkAvailable()) {
            updateConnectionStatus(false);
            Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
            return;
        }

        updateConnectionStatus(true);
        progressBar.setVisibility(View.VISIBLE);
        updateButton.setEnabled(false);
        updateButton.setText("Обновление...");

        executorService.execute(() -> {
            try {
                // Получаем криптовалюты через CoinGecko
                StringBuilder cryptoIds = new StringBuilder();
                for (Currency c : currencies) {
                    if (c.type.equals("crypto") && c.geckoId != null) {
                        if (cryptoIds.length() > 0) cryptoIds.append(",");
                        cryptoIds.append(c.geckoId);
                    }
                }

                String geckoUrl = "https://api.coingecko.com/api/v3/simple/price?ids=" +
                        cryptoIds + "&vs_currencies=usd,eur,rub,uah,gbp,jpy,cny,pln,chf,cad,aud,try,inr,brl,mxn,zar,aed,krw,sgd,hkd,thb";

                URL url = new URL(geckoUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject cryptoData = new JSONObject(response.toString());
                HashMap<String, HashMap<String, Double>> newCryptoRates = new HashMap<>();

                for (Currency c : currencies) {
                    if (c.type.equals("crypto") && c.geckoId != null && cryptoData.has(c.geckoId)) {
                        JSONObject rates = cryptoData.getJSONObject(c.geckoId);
                        HashMap<String, Double> currencyRates = new HashMap<>();

                        Iterator<String> keys = rates.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            currencyRates.put(key.toUpperCase(), rates.getDouble(key));
                        }
                        newCryptoRates.put(c.code, currencyRates);
                    }
                }

                // Получаем фиатные валюты
                URL fiatUrl = new URL("https://api.exchangerate-api.com/v4/latest/USD");
                HttpURLConnection fiatConnection = (HttpURLConnection) fiatUrl.openConnection();
                fiatConnection.setRequestMethod("GET");
                fiatConnection.setConnectTimeout(5000);
                fiatConnection.setReadTimeout(5000);

                BufferedReader fiatReader = new BufferedReader(
                        new InputStreamReader(fiatConnection.getInputStream())
                );
                StringBuilder fiatResponse = new StringBuilder();
                while ((line = fiatReader.readLine()) != null) {
                    fiatResponse.append(line);
                }
                fiatReader.close();

                JSONObject fiatData = new JSONObject(fiatResponse.toString());
                JSONObject rates = fiatData.getJSONObject("rates");

                HashMap<String, Double> newFiatRates = new HashMap<>();
                Iterator<String> keys = rates.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    newFiatRates.put(key, rates.getDouble(key));
                }

                mainHandler.post(() -> {
                    fiatRates = newFiatRates;
                    cryptoRates = newCryptoRates;
                    progressBar.setVisibility(View.GONE);
                    updateButton.setEnabled(true);
                    updateButton.setText("Обновить курсы");

                    incrementRequestCount();

                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    lastUpdateText.setText("Обновлено: " + sdf.format(new Date()));

                    convertAndDisplay();
                    updateExchangeRate();
                    Toast.makeText(MainActivity.this,
                            "Курсы обновлены (" + requestCount + "/" + MAX_REQUESTS + ")",
                            Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateButton.setEnabled(true);
                    updateButton.setText("Обновить курсы");
                    Toast.makeText(MainActivity.this,
                            "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private double convertCurrency(double amount, Currency from, Currency to) {
        if (amount <= 0) return 0;

        try {
            // Крипта → Крипта
            if (from.type.equals("crypto") && to.type.equals("crypto")) {
                if (cryptoRates.containsKey(from.code) && cryptoRates.containsKey(to.code)) {
                    Double fromUsd = cryptoRates.get(from.code).get("USD");
                    Double toUsd = cryptoRates.get(to.code).get("USD");
                    if (fromUsd != null && toUsd != null && toUsd != 0) {
                        return amount * (fromUsd / toUsd);
                    }
                }
            }
            // Крипта → Фиат
//            else if (from.type.equals("crypto") && to.type.equals("fiat")) {
//                if (cryptoRates.containsKey(from
//
//                        <function_calls>
//                        <invoke name="artifacts">
//                        <parameter name="command">update</parameter>
//                        <parameter name="id">currency_converter_android</parameter>
//                        <parameter name="old_str">
//                        // Крипта → Фиат
//            else if (from.type.equals("crypto") && to.type.equals("fiat")) {
//                    if (cryptoRates.containsKey(from</parameter>
//                            <parameter name="new_str">

                            // Крипта → Фиат
            else if (from.type.equals("crypto") && to.type.equals("fiat")) {
                        if (cryptoRates.containsKey(from.code)) {
                            Double rate = cryptoRates.get(from.code).get(to.code);
                            if (rate != null) {
                                return amount * rate;
                            }
                        }
                    }
// Фиат → Крипта
                    else if (from.type.equals("fiat") && to.type.equals("crypto")) {
                        if (cryptoRates.containsKey(to.code)) {
                            Double rate = cryptoRates.get(to.code).get(from.code);
                            if (rate != null && rate != 0) {
                                return amount / rate;
                            }
                        }
                    }
// Фиат → Фиат
                    else if (from.type.equals("fiat") && to.type.equals("fiat")) {
                        if (fiatRates.containsKey(from.code) && fiatRates.containsKey(to.code)) {
                            double amountInUsd = amount / fiatRates.get(from.code);
                            return amountInUsd * fiatRates.get(to.code);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 0;
        }



    private void convertAndDisplay() {
        String amountStr = amountInput.getText().toString();

        if (amountStr.isEmpty() || (fiatRates.isEmpty() && cryptoRates.isEmpty())) {
            resultCard.setVisibility(View.GONE);
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                resultCard.setVisibility(View.GONE);
                return;
            }

            double result = convertCurrency(amount, selectedFromCurrency, selectedToCurrency);

            if (result > 0) {
                DecimalFormat df;
                if (selectedToCurrency.type.equals("crypto")) {
                    df = new DecimalFormat("#.########");
                } else {
                    df = new DecimalFormat("#.##");
                }
                resultText.setText(df.format(result) + " " + selectedToCurrency.code);
                resultCard.setVisibility(View.VISIBLE);
            } else {
                resultCard.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            resultCard.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    }

