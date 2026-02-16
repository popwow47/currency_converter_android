// Currency.java
package com.popww.currencyconverter;

public class Currency {
    public String code;
    public String name;
    public String symbol;
    public String flag;
    public String type; // "fiat" или "crypto"
    public String geckoId; // ID для CoinGecko API (только для криптовалют)

    public Currency(String code, String name, String symbol, String flag, String type, String geckoId) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.flag = flag;
        this.type = type;
        this.geckoId = geckoId;
    }
}