package com.sn.trade.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.StockMarket;
import com.sn.trade.strategy.imp.STConstants;
import com.sn.trade.strategy.imp.TradeStrategyImp;

public class ClosePriceTrendStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(ClosePriceTrendStockSelector.class);
    int days = 60;
	double curPctLowLvl = 0.1;
    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac) {
    	Double maxYtClsPri = s.getMaxDlyTdClsPri(days);
    	Double minYtClsPri = s.getMinDlyTdClsPri(days);
    	Double curPri = s.getTdCls_pri(0);
    	Double fiveDayAvgClsPri = s.getAvgTDClsPri(5, 0);
    	Double fiveDayAvgClsPri1 = s.getAvgTDClsPri(5, 1);
    	Double tenDayAvgClsPri = s.getAvgTDClsPri(10, 0);
    	Double tenDayAvgClsPri1 = s.getAvgTDClsPri(10, 1);
    	
    	if (maxYtClsPri == null || minYtClsPri == null || curPri == null || tenDayAvgClsPri1 == null) {
    		log.info("ClosePriceTrendStockSelector return false because null max/min price.");
    		return false;
    	}
    	
    	double maxPct = (maxYtClsPri - minYtClsPri) / minYtClsPri;
    	double curPct = (curPri - minYtClsPri) / minYtClsPri;
    	
    	log.info("param days:" + days + " maxPct:" + maxPct + " curPct:" + curPct + " curPctLowLvl:" + curPctLowLvl);
    	log.info("fiveDayAvgClsPri:" + fiveDayAvgClsPri + " tenDayAvgClsPri:" + tenDayAvgClsPri);
    	log.info("fiveDayAvgClsPri1:" + fiveDayAvgClsPri1 + " tenDayAvgClsPri1:" + tenDayAvgClsPri1);
    	
    	boolean fiveup =(fiveDayAvgClsPri > fiveDayAvgClsPri1);
    	boolean crossover = (fiveDayAvgClsPri - tenDayAvgClsPri) * (fiveDayAvgClsPri1 - tenDayAvgClsPri1) < 0;

        if (curPct <= maxPct * curPctLowLvl && fiveup && crossover) {
            log.info("Now, today close price is in low, and 5 days golden cross 10 days.");
            return true;
        }
        log.info("returned false for isGoodStock()");
        return false;
    }
	@Override
	public boolean isORCriteria() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isMandatoryCriteria() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean adjustCriteria(boolean harder) {
		// TODO Auto-generated method stub
		log.info("curPctLowLvl:" + curPctLowLvl);
		if (harder) {
			days++;
			if (days >= 90) {
				days = 90;
			}
			curPctLowLvl = curPctLowLvl - 0.01;
			if (curPctLowLvl <= 0.01) {
				curPctLowLvl = 0.01;
			}
		}
		else {
			curPctLowLvl = curPctLowLvl + 0.01;
			days--;
			if (days < 20) {
				days = 20;
			}
			curPctLowLvl = curPctLowLvl + 0.01;
			if (curPctLowLvl >= 0.2) {
				curPctLowLvl = 0.2;
			}
		}
		log.info("after try " + (harder ? " harder" : " loose"));
		log.info("curPctLowLvl:" + curPctLowLvl);
		return true;
	}
    @Override
    public Integer getTradeModeId() {
        // TODO Auto-generated method stub
        return STConstants.TRADE_MODE_ID_MANUAL;
    }
	@Override
	public boolean shouldStockExitTrade(String s) {
		// TODO Auto-generated method stub
		return false;
	}
}
