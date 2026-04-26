package com.yourapp.carbot.service;

public class ParserRunStats {

    private int bazosParsed;
    private int sautoParsed;
    private int tipcarsParsed;

    private int totalSaved;

    public int getBazosParsed() {
        return bazosParsed;
    }

    public void setBazosParsed(int bazosParsed) {
        this.bazosParsed = bazosParsed;
    }

    public int getSautoParsed() {
        return sautoParsed;
    }

    public void setSautoParsed(int sautoParsed) {
        this.sautoParsed = sautoParsed;
    }

    public int getTipcarsParsed() {
        return tipcarsParsed;
    }

    public void setTipcarsParsed(int tipcarsParsed) {
        this.tipcarsParsed = tipcarsParsed;
    }

    public int getTotalSaved() {
        return totalSaved;
    }

    public void setTotalSaved(int totalSaved) {
        this.totalSaved = totalSaved;
    }
}