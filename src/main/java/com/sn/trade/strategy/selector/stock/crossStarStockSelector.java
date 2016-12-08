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
import com.sn.trade.strategy.imp.TradeStrategyImp;

public class crossStarStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(crossStarStockSelector.class);
    int lostdays = 7;
    int gaindays = 2;
    double dayPct = 0.005;
    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac) {
        if (s.getSd().keepDaysClsPriLost(lostdays, gaindays, dayPct) && s.getSd().keepDaysClsPriGain(gaindays, 0, dayPct)) {
                    log.info("returned true because keep "+ lostdays + " days lost, and keep " + gaindays + " gain, for pct:"+ dayPct);
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
		if (harder) {
			if (lostdays >= 15) {
				log.info("lostdays can not more than 15");
			}
			else {
				lostdays++;
			}
			if (dayPct >= 0.03) {
				log.info("dayPct can not more than 0.03");
			}
			else {
			    dayPct += dayPct/10;
			}
			gaindays++;
			if (gaindays > 3) {
				gaindays=3;
			}
		}
		else {
			if (lostdays <= 3) {
				log.info("lostdays can not less than 3");
			}
			else {
				lostdays--;
			}
			if (dayPct <= 0.001) {
				log.info("dayPct can not less than 0.001");
			}
			else {
				dayPct -= dayPct/10;
			}
			gaindays--;
			if (gaindays<1) {
				gaindays = 1;
			}
		}
		log.info("try " + (harder ? " harder" : " loose") + " lostdays:" + lostdays + " dayPct:" + dayPct + " gaindays:" + gaindays);
		return false;
	}
    @Override
    public Integer getTradeModeId() {
        // TODO Auto-generated method stub
        return null;
    }
	@Override
	public boolean shouldStockExitTrade(String s) {
		// TODO Auto-generated method stub
		return false;
	}
}
