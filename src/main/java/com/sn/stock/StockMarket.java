package com.sn.stock;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.Stock.StockData;
import com.sn.trade.strategy.imp.STConstants;

public class StockMarket{

    static Logger log = Logger.getLogger(StockMarket.class);
    
    static private ConcurrentHashMap<String, Stock> stocks = new ConcurrentHashMap<String, Stock>();
    static private ConcurrentHashMap<String, Stock> gzstocks = new ConcurrentHashMap<String, Stock>();
    static private ConcurrentHashMap<String, Stock> recomstocks = null;
    
    static private Map<String, Boolean> gzStockSellMode = new HashMap<String, Boolean>();
    
    private static int StkNum = 0;
    private static int TotInc = 0;
    private static int TotDec = 0;
    private static int TotEql = 0;
    private static double AvgIncPct = 0.0;
    private static double AvgDecPct = 0.0;
    private static double totIncDlMny = 0.0;
    private static double totDecDlMny = 0.0;
    private static double totEqlDlMny = 0.0;
    private static double Degree = 0.0;
    private static int cachedDaysCnt = 0;
    
    public static String GZ_STOCK_SELECT = "select distinct s.id, s.name, s.area "
    		                             + "from stk s, usrStk u "
    		                             + "where s.id = u.id "
    		                             + "  and u.gz_flg = 1 "
    		                             + "  and u.suggested_by in ('"
    		                             + STConstants.SUGGESTED_BY_FOR_USER + "','"
    		                             + STConstants.SUGGESTED_BY_FOR_SYSTEM_GRANTED + "','"
    		                             + STConstants.SUGGESTED_BY_FOR_SYSTEM_READ_FOR_TRADE + "') "
    		                             + "order by s.id";
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
            for (String s : stocks.keySet()) {
                Stock stk = stocks.get(s);
                stk.printStockInfo();
            }
    }
    
    static public Boolean getStockSellMode(String stkid) {
        Boolean sell_mode = null;
        sell_mode = gzStockSellMode.get(stkid);
        
        if (sell_mode == null) {
            log.info("sell mode for stock:" + stkid + " is not set yet.");
            return null;
        }
        else {
            log.info("sell mode for stock:" + stkid + " is " + sell_mode);
            return sell_mode;
        }
    }
    
    static public void putStockSellMode(String stkid, boolean sell_mode) {
        log.info("put stock sell mode " + sell_mode + " for stock:" + stkid);
        gzStockSellMode.put(stkid, sell_mode);
    }
    
    static public boolean loadStocks() {

        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        
        stocks.clear();
        Stock s = null;
        int cnt = 0;
        try {
            stm = con.createStatement();
            String sql = "select id, name from stk order by id";
            rs = stm.executeQuery(sql);
            
            String id, name;
            
            while (rs.next()) {
                id = rs.getString("id");
                name = rs.getString("name");
                s = new Stock(id, name, StockData.SMALL_SZ);
                stocks.put(id, s);
                cnt++;
                log.info("LoadStocks completed:" + cnt * 1.0 / 2811);
            }
            rs.close();
            stm.close();
            con.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        log.info("StockMarket loadStock " + cnt + " successed!");
        return true;
    }
    
    static public boolean loadGzStocks() {

        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        
        gzstocks.clear();
        Stock s = null;
        int cnt = 0;
        try {
            stm = con.createStatement();
            rs = stm.executeQuery(GZ_STOCK_SELECT);
            
            String id, name;
            
            while (rs.next()) {
                id = rs.getString("id");
                name = rs.getString("name");
                s = new Stock(id, name, StockData.SMALL_SZ);
                gzstocks.put(id, s);
                cnt++;
                log.info("LoadStocks completed:" + cnt * 1.0 / 2811);
            }
            rs.close();
            stm.close();
            con.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        log.info("StockMarket loadStock " + cnt + " successed!");
        return true;
    }
    
    static public boolean addGzStocks(Stock s) {
        gzstocks.put(s.getID(), s);
        log.info("StockMarket addGzStocks " + s.getID() + " successed!");
        return true;
    }
    
    static public boolean addGzStocks(String stkId) {

        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        
        Stock s = null;
        try {
            stm = con.createStatement();
            String sql = "select s.id, s.name from stk s, usrStk u where s.id = u.id and u.gz_flg = 1 and s.id = '" + stkId + "'";
            rs = stm.executeQuery(sql);
            
            String id, name;
            
            if (rs.next()) {
                id = rs.getString("id");
                name = rs.getString("name");
                s = new Stock(id, name, StockData.SMALL_SZ);
                gzstocks.put(id, s);
                log.info("addGzStocks completed for: " + stkId);
            }
            rs.close();
            stm.close();
            con.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        log.info("StockMarket addGzStocks " + stkId + " successed!");
        return true;
    }
    
    static public boolean removeGzStocks(String stkId) {

        boolean removed = false;
        if (!gzstocks.isEmpty()) {
             Stock rm = gzstocks.remove(stkId);
             if (rm != null) {
                 removed = true;
             }
        }
        if (removed) {
            log.info("removeGzStocks success for: " + stkId);
            return true;
        }
        else {
            log.info("removeGzStocks failed for: " + stkId);
            return false;
        }
    }
    
    public static ConcurrentHashMap<String, Stock> getStocks() {
        synchronized (StockMarket.class) {
            if (stocks.isEmpty()) {
                loadStocks();
            }
            return stocks;
        }
    }

    public static void setStocks(ConcurrentHashMap<String, Stock> stocks) {
        StockMarket.stocks = stocks;
    }

    public static ConcurrentHashMap<String, Stock> getGzstocks() {
        if (gzstocks.isEmpty()) {
            loadGzStocks();
        }
        return gzstocks;
    }

    public static void setGzstocks(ConcurrentHashMap<String, Stock> gzstocks) {
        StockMarket.gzstocks = gzstocks;
    }

    public static ConcurrentHashMap<String, Stock> getRecomstocks() {
        return recomstocks;
    }

    public static void setRecomstocks(ConcurrentHashMap<String, Stock> recomstocks) {
        StockMarket.recomstocks = recomstocks;
    }

    public static int getStkNum() {
        return StkNum;
    }

    public static void setStkNum(int stkNum) {
        StkNum = stkNum;
    }

    public static int getTotInc() {
        return TotInc;
    }

    public static void setTotInc(int totInc) {
        TotInc = totInc;
    }

    public static int getTotDec() {
        return TotDec;
    }

    public static void setTotDec(int totDec) {
        TotDec = totDec;
    }

    public static int getTotEql() {
        return TotEql;
    }

    public static void setTotEql(int totEql) {
        TotEql = totEql;
    }

    public static double getAvgIncPct() {
        return AvgIncPct;
    }

    public static void setAvgIncPct(double avgIncPct) {
        AvgIncPct = avgIncPct;
    }

    public static double getAvgDecPct() {
        return AvgDecPct;
    }

    public static void setAvgDecPct(double avgDecPct) {
        AvgDecPct = avgDecPct;
    }

    public static double getTotIncDlMny() {
        return totIncDlMny;
    }

    public static void setTotIncDlMny(double totIncDlMny) {
        StockMarket.totIncDlMny = totIncDlMny;
    }

    public static double getTotDecDlMny() {
        return totDecDlMny;
    }

    public static void setTotDecDlMny(double totDecDlMny) {
        StockMarket.totDecDlMny = totDecDlMny;
    }

    public static double getTotEqlDlMny() {
        return totEqlDlMny;
    }

    public static void setTotEqlDlMny(double totEqlDlMny) {
        StockMarket.totEqlDlMny = totEqlDlMny;
    }

    public static double getDegree() {
        return Degree;
    }

    public static void setDegree(double degree) {
        Degree = degree;
    }
    
    public static boolean isJumpWater(int tailSz, double jumpPct, double stockJumpCntPct) {
    	int total = stocks.size();
    	if (total == 0) {
    		log.info("stocks is emtpy, can not check jump water for market.");
    		return false;
    	}
    	int jumpCnt = 0;
    	for(String s : stocks.keySet()) {
    		Stock stk = stocks.get(s);
    		if (stk.isJumpWater(tailSz, jumpPct)) {
    			jumpCnt++;
    		}
    	}
    	log.info("total stocks:" + total + " and jump water stocks:" + jumpCnt);
    	double actPct = jumpCnt * 1.0 / total;
    	log.info("Passed param [tailSz, stockJumpCntPct, jumpPct]=[" + tailSz + "," + stockJumpCntPct + "," + jumpPct + "] actPct:" + actPct + " return " + (actPct >= stockJumpCntPct ? "true" : "false"));
    	if (actPct >= stockJumpCntPct) {
    		return true;
    	}
    	return false;
    }
    
    public static int getNumDaysAhead(String stkId, int daysAhead) {
    	
    	if (cachedDaysCnt > 0) {
    		log.info("use cachedDaysCnt:" + cachedDaysCnt);
    		return cachedDaysCnt;
    	}
        Connection con = DBManager.getConnection();
        Statement stm = null;
        int daysCnt = 0;

        String dayStr = "";
        try {
            stm = con.createStatement();
            String sql = "select distinct to_char(dl_dt, 'yyyy-mm-dd') DayStr " +
                         " from stkdat2 " +
                         " where td_opn_pri > 0 " +
                         "   and id = '" + stkId + "'" +
                         "  order by DayStr desc";
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);

            while (rs.next() && daysAhead > 0) {
                daysAhead--;
            }
            
            if (!rs.isAfterLast()) {
                dayStr = rs.getString("DayStr");
                rs.close();
                stm.close();
                stm = con.createStatement();
                sql = "select to_date(to_char(sysdate, 'yyyy-mm-dd'), 'yyyy-mm-dd hh24:mi:ss') - to_date('" + dayStr + "', 'yyyy-mm-dd hh24:mi:ss') daysCnt from dual";
                log.info(sql);
                rs = stm.executeQuery(sql);
                if(rs.next()) {
                    daysCnt = rs.getInt("daysCnt");
                }
            }
            log.info("get daysCnt:" + daysCnt);
            
            cachedDaysCnt= daysCnt;
            rs.close();
            stm.close();
            con.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return daysCnt;
    }
    
    public static boolean isGzStocksJumpWater(int tailSz, double jumpPct, double stockJumpCntPct) {
    	if (gzstocks.isEmpty()) {
    		getGzstocks();
    	}
    	int total = gzstocks.size();
    	if (total == 0) {
    		log.info("stocks is emtpy, can not check jump water for gz stocks.");
    		return false;
    	}
    	int jumpCnt = 0;
    	for(String s : gzstocks.keySet()) {
    		Stock stk = gzstocks.get(s);
    		if (stk.isJumpWater(tailSz, jumpPct)) {
    			jumpCnt++;
    		}
    	}
    	log.info("total Gz stocks:" + total + " and jump water stocks:" + jumpCnt);
    	double actPct = jumpCnt * 1.0 / total;
    	log.info("Passed param [tailSz, stockJumpCntPct, jumpPct]=[" + tailSz + "," + stockJumpCntPct + "," + jumpPct + "] actPct:" + actPct + " return " + (actPct >= stockJumpCntPct ? "true" : "false"));
    	if (actPct >= stockJumpCntPct) {
    		return true;
    	}
    	return false;
    }
    
    static public boolean calIndex(Timestamp tm) {

        Connection con = DBManager.getConnection();
        Statement stm = null;
        String deadline = null;
        if (tm == null) {
        	deadline = "sysdate";
        }
        else {
        	deadline = "to_date('" + tm.toLocaleString() + "', 'yyyy-mm-dd HH24:MI:SS')";
        }

        int catagory = -2;
        try {
            stm = con.createStatement();
            String sql = "select sum(case when cur_pri > td_opn_pri then 1 else 0 end) IncNum, " +
                         "       sum(case when cur_pri < td_opn_pri then 1 else 0 end) DecNum, " +
                         "       sum(case when cur_pri = td_opn_pri then 1 else 0 end) EqlNum, " +
                         " avg((cur_pri - td_opn_pri)/td_opn_pri) avgPct," +
                         " sum(dl_mny_num) totDlMny," +
                         " case when cur_pri > td_opn_pri then 1 " +
                         "               when cur_pri < td_opn_pri then -1 " +
                         "               when cur_pri = td_opn_pri then 0 end catagory " +
                         " from stkdat2 " +
                         " where td_opn_pri > 0 " +
                         "   and dl_dt <= " + deadline +
                         "   and not exists (select 'x' from stkdat2 skd where skd.id = stkdat2.id and skd.ft_id > stkdat2.ft_id and skd.dl_dt <= " + deadline + ")" +
                         " group by case when cur_pri > td_opn_pri then 1 " +
                         "               when cur_pri < td_opn_pri then -1 " +
                         "               when cur_pri = td_opn_pri then 0 end" +
                         " order by catagory asc";
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);

            while (rs.next()) {
                catagory = rs.getInt("catagory");
                if (catagory == -1)
                {
                    TotDec = rs.getInt("DecNum");
                    AvgDecPct = rs.getDouble("avgPct");
                    totDecDlMny = rs.getDouble("totDlMny");
                }
                else if (catagory == 0)
                {
                    TotEql = rs.getInt("EqlNum");
                    totEqlDlMny = rs.getDouble("totDlMny");
                }
                else if (catagory == 1)
                {
                    TotInc = rs.getInt("IncNum");
                    AvgIncPct = rs.getDouble("avgPct");
                    totIncDlMny = rs.getDouble("totDlMny");
                }
            }
            
            StkNum = TotDec + TotInc + TotEql;
            Degree = (TotInc * AvgIncPct + TotDec * AvgDecPct) * 100.0 / (TotInc * 0.1 + TotDec * 0.1);
            
            rs.close();
            stm.close();
            con.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return true;
    }
    
    static public String getShortDesc() {
    	if (Degree == 0.0) {
    		calIndex(null);
    	}
    	DecimalFormat df = new DecimalFormat("##.##");
        return "温度:" + df.format(Degree) + "[" + StkNum + "/" + df.format((totDecDlMny +totEqlDlMny + totIncDlMny)/100000000) + "亿 "
    			+ TotInc + "/" + df.format(AvgIncPct) + "/" + df.format(totIncDlMny/100000000) + "亿涨 "
                + TotDec + "/" + df.format(AvgDecPct) + "/" + df.format(totDecDlMny/100000000) + "亿跌 "
    			+ TotEql + "/" + df.format(totEqlDlMny/100000000) + "亿平]";
    }
    
    static public String getDegreeMny() {
    	if (Degree == 0.0) {
    		calIndex(null);
    	}
    	
    	DecimalFormat df = new DecimalFormat("##.##");
        return "温度:" + df.format(Degree) + "[" + StkNum + "/" + df.format((totDecDlMny +totEqlDlMny + totIncDlMny)/100000000) + "亿 ]";
    }
    
    static public String getLongDsc() {
        
    	if (Degree == 0.0) {
    		calIndex(null);
    	}
    	
    	DecimalFormat df = new DecimalFormat("##.##");
        String index = "<table border = 1>" +
        "<tr>" +
        "<th> Stock Count</th> " +
        "<th> Total+ </th> " +
        "<th> AvgPct+ </th> " +
        "<th> TotDlMny+ </th> " +
        "<th> Total-</th> " +
        "<th> AvgPct-</th> " + 
        "<th> TotDlMny- </th> " +
        "<th> Total= </th> " +
        "<th> TotDlMny= </th> " +
        "<th> Degree </th> </tr> ";
        index += "<tr> <td>" + StkNum + "</td>" +
        "<td> " + TotInc + "</td>" +
         "<td> " + df.format(AvgIncPct*100) + "%</td>" +
         "<td> " + df.format(totIncDlMny/100000000) + "亿</td>" +
         "<td> " + TotDec + "</td>" +
         "<td> " + df.format(AvgDecPct*100) + "%</td>" +
         "<td> " + df.format(totDecDlMny/100000000) + "亿</td>" +
         "<td> " + TotEql + "</td>" +
         "<td> " + df.format(totEqlDlMny/100000000) + "亿</td>" +
         "<td> " + df.format((TotInc * AvgIncPct + TotDec * AvgDecPct) * 100.0 / (TotInc * 0.1 + TotDec * 0.1)) + " C</tr></table>";
        return index;
    }
    static public boolean isMarketTooCold(Timestamp tm) {
        if (Degree == 0.0) {
            calIndex(tm);
        }
        log.info("Degree is:" + Degree + ", is it too cool for -30?");
        return Degree < -30;
    }
    
    static public boolean hasMostDecStock() {
    	log.info("TotInc:" + TotInc + ", TotDec:" + TotDec + ", Ratio:" + Math.abs(TotInc - TotDec) / Math.min(TotInc, TotDec));
        return TotInc * 1.0/ (TotInc + TotDec) < 1.0/3.0;
    }
    
    static double getMnyRatioIncDec() {
        if (totDecDlMny == 0) {
            //not possible ratio.
            return 10000;
        }
        return totIncDlMny / totDecDlMny;
    }
}
