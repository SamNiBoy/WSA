package com.sn.mail.reporter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;

public class SellModeStockObserverable extends Observable {

    static Logger log = Logger.getLogger(SellModeStockObserverable.class);

    private List<Stock> stocksToSellMode = new ArrayList<Stock>();
    private List<Stock> stocksToUnSellMode = new ArrayList<Stock>();
    private List<SellModeStockSubscriber> ms = new ArrayList<SellModeStockSubscriber>();

    public void addStockToSellMode(List<Stock> lst) {
    	stocksToSellMode.clear();
    	stocksToSellMode.addAll(lst);
    }
    public void addStockToUnsellMode(List<Stock> lst) {
    	stocksToUnSellMode.clear();
    	stocksToUnSellMode.addAll(lst);
    }
	public class SellModeStockSubscriber{
		String openID;
		String mail;
		List<Stock> stockMailed = new ArrayList<Stock>();
		SellModeStockSubscriber(String oid, String ml) {
			openID = oid;
			mail = ml;
		}
		public String subject;
		public String content;
		boolean alreadyMailed(Stock s) {
			if (stockMailed.contains(s)) {
				log.info("Stock:" + s.getID() + " already Mailed to user:" + openID);
				return true;
			}
			return false;
		}
		public boolean setMailed(Stock s) {
			stockMailed.add(s);
			return true;
		}
	}
	
    public List<SellModeStockSubscriber> getSellModeStockSubscribers() {
    	return ms;
    }
    private boolean loadMailScb() {
    	boolean load_success = false;
    	try {
    		Connection con = DBManager.getConnection();
    		Statement stm = con.createStatement();
    		String sql = "select  u.* from usr u where u.mail is not null and u.suggest_stock_enabled = 1";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		String openId;
    		String mail;
    		while (rs.next()) {
    			openId = rs.getString("openID");
    			mail = rs.getString("mail");
    			log.info("loading mailsubscriber:" + openId + ", mail:" + mail);
    			ms.add(new SellModeStockSubscriber(openId, mail));
    			load_success = true;
    		}
    		rs.close();
    		stm.close();
    		con.close();
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	return load_success;
    }
    
    private boolean buildMailforSubscribers() {
        if (ms.size() == 0 || (stocksToSellMode.size() == 0 && stocksToUnSellMode.size() == 0)) {
        	log.info("No user subscribed or no stocks to for sell/unsell mode, no need to send mail.");
        	return false;
        }
        String returnStr = "";
        SimpleDateFormat f = new SimpleDateFormat(" HH:mm:ss");  
        Date date = new Date();  
        returnStr = f.format(date);
        String subject = "买卖模式变化" + returnStr;
        StringBuffer body;
        boolean usr_need_mail = false;
        boolean generated_mail = false;
        
        for (SellModeStockSubscriber u : ms) {
        	u.subject = subject;
        	u.content = "";
        	body = new StringBuffer();
        	usr_need_mail = false;
            body.append("<table bordre = 1>" +
                    "<tr>" +
                    "<th> ID</th> " +
                    "<th> Name</th> " +
                    "<th> Is Sell Mode</th> " +
                    "<th> Price</th></tr>");
            DecimalFormat df = new DecimalFormat("##.##");
            for (Stock s : stocksToSellMode) {
            	if (!u.alreadyMailed(s)) {
                    body.append("<tr> <td>" + s.getID() + "</td>" +
                    "<td> " + s.getName() + "</td>" +
                    "<td>  1 </td>" +
                    "<td> " + df.format(s.getCur_pri() == null ? 0 : s.getCur_pri()) + "</td></tr>");
                    usr_need_mail = true;
                    generated_mail = true;
                    u.setMailed(s);
            	}
            }
            
            for (Stock s : stocksToUnSellMode) {
            	if (!u.alreadyMailed(s)) {
                    body.append("<tr> <td>" + s.getID() + "</td>" +
                    "<td> " + s.getName() + "</td>" +
                    "<td>  0 </td>" +
                    "<td> " + df.format(s.getCur_pri() == null ? 0 : s.getCur_pri()) + "</td></tr>");
                    usr_need_mail = true;
                    generated_mail = true;
                    u.setMailed(s);
            	}
            }
            
            if (usr_need_mail) {
                u.content = body.toString();
            }
            else {
            	u.subject = "";
            	u.content = "";
            }
        }
        return generated_mail;
    }

    public SellModeStockObserverable() {
        this.addObserver(StockObserver.globalObs);
        loadMailScb();
    }
    
    static public void main(String[] args) {
    }

    public void update() {
        if (buildMailforSubscribers()) {
            this.setChanged();
            this.notifyObservers(this);
        }
    }
}