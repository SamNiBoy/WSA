package com.sn.trade.strategy.selector.buypoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trade.strategy.imp.STConstants;
import com.sn.trade.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.work.task.SellModeWatchDog;
import com.sn.work.task.SuggestStock;

public class QtyBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(QtyBuyPointSelector.class);
	
	@Override
	public boolean isGoodBuyPoint(Stock stk, ICashAccount ac) {

		double tradeThresh = STConstants.BASE_TRADE_THRESH;
        Double maxPri = stk.getMaxCurPri();
        Double minPri = stk.getMinCurPri();
        Double yt_cls_pri = stk.getYtClsPri();
        Double cur_pri = stk.getCur_pri();
        
        boolean isStockFreshAdded = false;
        boolean okBuy = false;
        
        if (maxPri != null && minPri != null && yt_cls_pri != null && cur_pri != null) {
        
        	//double marketDegree = StockMarket.getDegree();
        	
        	//tradeThresh = getBuyThreshValueByDegree(marketDegree, stk, ac);
        	isStockFreshAdded = SuggestStock.isStockFreshAddedForTrade(stk);
        	
        	if (isStockFreshAdded) {
        		tradeThresh = 0;
        		log.info("Stock is freshly added for trade, use 0 value:" + tradeThresh);
        	}
        	
        	double maxPct = (maxPri - minPri) / yt_cls_pri;
        	double curPct =(cur_pri - minPri) / yt_cls_pri;
        
        	boolean qtyPlused = stk.isLstQtyPlused();
        	
        	log.info("maxPct:" + maxPct + ", tradeThresh:" + tradeThresh + ", curPct:" + curPct + ", isQtyPlused:" + qtyPlused);
        	
        	if (maxPct >= tradeThresh && curPct < maxPct * 1.0 / 10.0 && stk.isLstQtyPlused() && stk.isLstPriTurnaround(true)) {
        		log.info("isGoodBuyPoint true says Check Buy:" + stk.getDl_dt() + " stock:" + stk.getID()
        				+ " maxPri:" + maxPri + " minPri:" + minPri + " maxPct:" + maxPct + " curPri:" + cur_pri);
        		okBuy = true;
        	} else if (stk.isStoppingJumpWater() && !StockMarket.isGzStocksJumpWater(5, 0.01, 0.5)) {
        		log.info("Stock cur price is stopping dumping, isGoodBuyPoint return true.");
        		//for testing purpose, still return false;
        		okBuy = true;
        	}
        	else {
        		log.info("isGoodBuyPoint false Check Buy:" + stk.getDl_dt() + " stock:" + stk.getID()
        				+ " maxPri:" + maxPri + " minPri:" + minPri + " maxPct:" + maxPct + " curPri:" + cur_pri + " tradeThresh:" + tradeThresh);
        		okBuy = false;
        	}
        } else {
        	log.info("isGoodBuyPoint says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
        	okBuy = false;
        }
        
        if (isStockFreshAdded && okBuy) {
    		SuggestStock.resetStockFreshAddedForTrade(stk);
        }
		return okBuy;
	}
	
    public double getBuyThreshValueByDegree(double Degree, Stock stk, ICashAccount ac) {
    	
    	double baseThresh = STConstants.BASE_TRADE_THRESH;
    	
    	Timestamp tm = stk.getDl_dt();
        String deadline = null;
        if (tm == null) {
        	deadline = "sysdate";
        }
        else {
        	deadline = "to_date('" + tm.toLocaleString() + "', 'yyyy-mm-dd HH24:MI:SS')";
        }
        
    	try {
    		Connection con = DBManager.getConnection();
    		Statement stm = con.createStatement();
    		String sql = "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev "
    				   + "  from stkdat2 "
    				   + " where id ='" + stk.getID() + "'"
    				   + "   and to_char(dl_dt, 'yyyy-mm-dd') = to_char(" + deadline + ", 'yyyy-mm-dd')";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		if (rs.next()) {
    			double dev = rs.getDouble("dev");
    			log.info("dev calculated for stock:" + stk.getID() + " is:" + dev);
    			if (dev >= 0.01 && dev <= 0.04) {
    				baseThresh = STConstants.BASE_TRADE_THRESH * (dev - 0.01) / (0.04 - 0.01) + STConstants.BASE_TRADE_THRESH;
    			}
    		}
    		else {
    			baseThresh = STConstants.BASE_TRADE_THRESH;
    		}
    		rs.close();
    		stm.close();
    		
    		stm = con.createStatement();
    		// If the stock just get gzed in 5 minutes, but no buy, then half the threshold value to easy buy.
    		sql = "select 'x'" +
    			  " from usrStk " +
    			  "where gz_flg = 1 " +
    			  "  and id = '" + stk.getID() + "'" +
    			  "  and add_dt >= " + deadline + " - 5.0/(24 * 60)" +
    		      "  and not exists (select 'y' " +
    		      "                    from SellBuyRecord r " +
    		      "                   where r.stkid = usrStk.id " +
    		      "                     and r.buy_flg = 1 " +
    		      "                     and to_char(" + deadline + ", 'yyyy-mm-dd') = to_char(r.dl_dt,'yyyy-mm-dd'))";
    		log.info(sql);
    		rs = stm.executeQuery(sql);
    		if (rs.next()) {
    		    log.info("Stock:" + stk.getID() + " just get added to trade in 5 minutes, but not brought, use half trade threshhold value: " + baseThresh + " to easy buy.");
    		    baseThresh = baseThresh / 2.0;
    		}
    		
    		rs.close();
    		stm.close();
    		con.close();
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	double ratio = 1;
    	if (Degree < 0) {
    		if (Degree >= -10) {
    			ratio = 1;
    		}
    		else if (Degree < -10 && Degree >= -20) {
    			ratio = 1.5;
    	    }
    	    else if (Degree < -20) {
    	    	ratio = 2;
    	    }
    	}
    	else {
    		if (Degree < 10) {
    			ratio = 1;
    		}
    		else if (Degree < 20){
    			ratio = 1.1;
    		}
    		else if (Degree < 30) {
    			ratio = 1.5;
    		}
    		else {
    			ratio = 2;
    		}
    	}
    	log.info("Calculate buy thresh value with Degree:" + Degree + ", baseThresh:" + baseThresh + " ratio:" + ratio + " final thresh value:" + ratio * baseThresh);

    	return ratio * baseThresh;
    }

	@Override
	public int getBuyQty(Stock s, ICashAccount ac) {
		// TODO Auto-generated method stub
        double useableMny = 0;
        int buyMnt = 0;
        int maxMnt = 0;
        
        if (ac != null) {
            useableMny = ac.getMaxAvaMny();
            maxMnt = (int)(useableMny/s.getCur_pri()) / 100 * 100;
            
            buyMnt = maxMnt;
            log.info("getBuyQty, useableMny:" + useableMny + " buyMnt:" + buyMnt + " maxMnt:" + maxMnt);
        }
        else {
        	if (s.getCur_pri() <= 20) {
        		buyMnt = 200;
        	}
        	else {
        		buyMnt = 100;
        	}
        	log.info("getBuyQty, cur_pri:" + s.getCur_pri() + " buyMnt:" + buyMnt);
        }
		return buyMnt;
	}

    @Override
    public boolean matchTradeModeId(Stock s) {
        // TODO Auto-generated method stub
        Integer trade_mode_id = s.getTrade_mode_id();

        log.info("trade_mode_id for stock:" + s.getID() + " is:" + trade_mode_id
                + " expected:" + STConstants.TRADE_MODE_ID_QTYTRADE
                + " or:" + STConstants.TRADE_MODE_ID_MANUAL
                + " or:" + STConstants.TRADE_MODE_ID_AVGPRI);
        if (trade_mode_id != null && (trade_mode_id == STConstants.TRADE_MODE_ID_QTYTRADE || trade_mode_id == STConstants.TRADE_MODE_ID_MANUAL
                || trade_mode_id == STConstants.TRADE_MODE_ID_AVGPRI)) {
            log.info("trade mode matched, continue");
            return true;
        }
        log.info("trade mode does not matched, continue");
        return false;
    }
    

}
