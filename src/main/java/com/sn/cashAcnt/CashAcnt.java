package com.sn.cashAcnt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.mail.reporter.StockObserverable;
import com.sn.sim.SimStockDriver;
import com.sn.stock.Stock;
import com.sn.stock.StockMarket;
import com.sn.trade.strategy.imp.STConstants;

public class CashAcnt implements ICashAccount {

	static Logger log = Logger.getLogger(CashAcnt.class);
	private String actId;
	private double initMny;
	private double usedMny;
	private double pftMny;
	private int splitNum;
	private double maxUsePct;
	private boolean dftAcnt;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public CashAcnt(String id) {

		loadAcnt(id);
	}

	public double getMaxAvaMny() {

		double useableMny = initMny * maxUsePct;
		if ((useableMny - usedMny) > initMny / splitNum) {
			return initMny / splitNum;
		} else {
			return useableMny - usedMny;
		}
	}

	public boolean loadAcnt(String id) {
		Connection con = DBManager.getConnection();
		String sql = "select * from CashAcnt where acntId = '" + id + "'";

		log.info("load cashAcnt info from db:" + id);
		log.info(sql);
		try {
			Statement stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);

			if (rs.next()) {
				actId = id;
				initMny = rs.getDouble("init_mny");
				usedMny = rs.getDouble("used_mny");
				pftMny = rs.getDouble("pft_mny");
				splitNum = rs.getInt("split_num");
				dftAcnt = rs.getInt("dft_acnt_flg") > 0;
				maxUsePct = rs.getDouble("max_useable_pct");
				log.info("actId:" + actId + " initMny:" + initMny + " usedMny:" + usedMny + " calMny:" + pftMny
						+ " splitNum:" + splitNum + " max_useable_pct:" + maxUsePct + " dftAcnt:" + dftAcnt);
			}
			rs.close();
			con.close();
			con = null;
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public String getActId() {
		return actId;
	}

	public void setActId(String actId) {
		this.actId = actId;
	}

	public double getInitMny() {
		return initMny;
	}

	public void setInitMny(double initMny) {
		this.initMny = initMny;
	}

	public double getUsedMny() {
		return usedMny;
	}

	public void setUsedMny(double usedMny) {
		this.usedMny = usedMny;
	}

	public double getPftMny() {
		return pftMny;
	}

	public void setPftMny(double calMny) {
		this.pftMny = calMny;
	}

	public int getSplitNum() {
		return splitNum;
	}

	public void setSplitNum(int splitNum) {
		this.splitNum = splitNum;
	}

	public boolean isDftAcnt() {
		return dftAcnt;
	}

	public void setDftAcnt(boolean dftAcnt) {
		this.dftAcnt = dftAcnt;
	}

	public int getSellableAmt(String stkId, String sellDt) {
		Connection con = DBManager.getConnection();
		String sql = "select case when sum(b.amount) is null then 0 else sum(b.amount) end SellableAmt from TradeDtl b "
				+ "      where b.stkId = '" + stkId + "'" + "        and acntId = '" + actId + "'"
				+ "        and b.buy_flg = 1 " + "        and to_char(dl_dt, 'yyyy-mm-dd') < '" + sellDt + "' "
				+ "      order by b.seqnum";
		ResultSet rs = null;

		int sellableAmt = 0;
		int soldAmt = 0;

		try {
			Statement stm = con.createStatement();
			log.info(sql);
			rs = stm.executeQuery(sql);
			if (rs.next()) {
				sellableAmt = rs.getInt("SellableAmt");
			}

			rs.close();
			stm.close();

			if (sellableAmt > 0) {
				stm = con.createStatement();
				sql = "select case when sum(b.amount) is null then 0 else sum(b.amount) end SoldAmt from TradeDtl b "
						+ "      where b.stkId = '" + stkId + "'" + "        and acntId = '" + actId + "'"
						+ "        and b.buy_flg = 0 " + "      order by b.seqnum";

				log.info(sql);
				rs = stm.executeQuery(sql);
				if (rs.next()) {
					soldAmt = rs.getInt("SoldAmt");
				}
				rs.close();
				stm.close();
			}
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		log.info("sellable/Sold Amt for :" + stkId + " is (" + sellableAmt + "/" + soldAmt + ")");
		return sellableAmt - soldAmt;
	}

	public int getUnSellableAmt(String stkId, String sellDt) {
		Connection con = DBManager.getConnection();
		String sql = "select case when sum(b.amount) is null then 0 else sum(b.amount) end unSellableAmt from TradeDtl b "
				+ "      where b.stkId = '" + stkId + "'" + "        and b.acntId = '" + actId + "'"
				+ "        and b.buy_flg = 1 " + "        and to_char(b.dl_dt, 'yyyy-mm-dd') = '" + sellDt + "' "
				+ "      order by b.seqnum";
		ResultSet rs = null;

		int unSellableAmt = 0;

		try {
			Statement stm = con.createStatement();
			log.info(sql);
			rs = stm.executeQuery(sql);
			if (rs.next()) {
				unSellableAmt = rs.getInt("unSellableAmt");
			}
			rs.close();
			stm.close();
			con.close();
			con = null;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		log.info("unsellable Amt for :" + stkId + " is (" + unSellableAmt + ")");
		return unSellableAmt;
	}

	public boolean calProfit(String ForDt, Map<String, Stock> stockSet) {
		Connection con = DBManager.getConnection();

		String sql = "select stkId from TradeHdr h where h.acntId = '" + actId + "'";

		ResultSet rs = null;
		pftMny = 0;
		double subpftMny = 0;
		boolean has_trade_flg = false;
		try {
			Statement stm = con.createStatement();
			log.info(sql);
			rs = stm.executeQuery(sql);
			Map<String, Stock> stks = stockSet;
			while (rs.next()) {
			    has_trade_flg = true;
				String stkId = rs.getString("stkId");
				Stock s = stks.get(stkId);

				int inHandMnt = getSellableAmt(stkId, ForDt) + getUnSellableAmt(stkId, ForDt);

				log.info("in hand amt:" + inHandMnt + " price:" + s.getCur_pri() + " with cost:" + usedMny);
				subpftMny = inHandMnt * s.getCur_pri();

				sql = "update TradeHdr set pft_mny = " + subpftMny + ", pft_price =" + s.getCur_pri()
						+ ", in_hand_qty = " + inHandMnt + " where acntId ='" + actId + "' and stkId ='" + stkId + "'";
				Statement stm2 = con.createStatement();
				log.info(sql);
				stm2.execute(sql);
				stm2.close();
			}

			rs.close();
			stm.close();
			
			if (!has_trade_flg) {
			    log.info("no trade record for:" + actId + " skip calculate profit.");
	             con.close();
			    return false;
			}

			sql = "select sum(pft_mny) tot_pft_mny from TradeHdr h where acntId = '" + actId + "'";

			stm = con.createStatement();
			rs = stm.executeQuery(sql);

			double tot_pft_mny = 0.0;
			if (rs.next()) {
				tot_pft_mny = rs.getDouble("tot_pft_mny");
				sql = "update CashAcnt set pft_mny = " + tot_pft_mny + " where acntId = '" + actId + "'";
				Statement stm2 = con.createStatement();

				pftMny = tot_pft_mny;

				log.info(sql);

				stm2.execute(sql);
				stm2.close();
			}
			rs.close();
			stm.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
			log.info("calProfit returned with exception:" + e.getMessage());
			return false;
		}
		return true;
	}

	public String reportAcntProfitWeb() {
		String msg = "Account: " + this.actId + " profit report<br/>";
		msg += "<table border = 1>" + "<tr>" + "<th> Cash Account</th> " + "<th> Init Money </th> "
				+ "<th> Used Money </th> " + "<th> Split Number </th> " + "<th> MaxUse Pct</th> "
				+ "<th> Default Account</th> " + "<th> Account Profit</th> " + "<th> Report Date</th> </tr> ";

		String dt = "";
		SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();
		dt = f.format(date);

		msg += "<tr> <td>" + actId + "</td>" + "<td> " + initMny + "</td>" + "<td> " + usedMny + "</td>" + "<td> "
				+ splitNum + "</td>" + "<td> " + maxUsePct + "</td>" + "<td> " + (dftAcnt ? "yes" : "No") + "</td>"
				+ "<td> " + pftMny + "</td>" + "<td> " + dt + "</td> </tr> </table>";

		String detailTran = "Detail Transactions <br/>";

		detailTran += "<table border = 1>" + "<tr>" + "<th> Cash Account</th> " + "<th> Stock Id </th> "
				+ "<th> Sequence Number </th> " + "<th> Price </th> " + "<th> Amount </th> " + "<th> Buy/Sell </th> "
				+ "<th> Transaction Date</th> </tr> ";

		Connection con = DBManager.getConnection();
		String sql = "select stkId," + "           seqnum," + "           round(price, 2) price," + "           amount,"
				+ "           buy_flg," + "           to_char(dl_dt, 'hh:mi:ss yyyy-mm-dd') dl_dt" + " from TradeDtl d "
				+ " where d.acntId ='" + actId + "' order by d.stkId, d.seqnum ";

		try {
			Statement stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);
			while (rs.next()) {
				detailTran += "<tr> <td>" + actId + "</td>" + "<td> " + rs.getString("stkId") + "</td>" + "<td> "
						+ rs.getInt("seqnum") + "</td>" + "<td> " + rs.getDouble("price") + "</td>" + "<td> "
						+ rs.getInt("amount") + "</td>" + "<td> " + (rs.getInt("buy_flg") > 0 ? "B" : "S") + "</td>"
						+ "<td> " + rs.getString("dl_dt") + "</td></tr>";
			}

			detailTran += "</table>";
			rs.close();
			stm.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		String totMsg = msg + detailTran;
		log.info("got profit information:" + totMsg);

		return totMsg;

	}

	public void printAcntInfo() {
		DecimalFormat df = new DecimalFormat("##.##");
		String pftPct = df.format((pftMny - usedMny) / usedMny * 100);
		String profit = df.format(pftMny - usedMny);
		log.info("##################################################################################################");
		log.info("|AccountId\t|InitMny\t|UsedMny\t|PftMny\t|SplitNum\t|MaxUsePct\t|DftAcnt\t|PP\t|Profit|");
		log.info("|" + actId + "\t|" + df.format(initMny) + "\t\t|" + df.format(usedMny) + "\t\t|" + df.format(pftMny)
				+ "\t|" + splitNum + "\t\t|" + df.format(maxUsePct) + "\t\t|" + dftAcnt + "\t\t|" + pftPct + "%\t|"
				+ profit + "\t|");
		log.info("##################################################################################################");
	}

	@Override
	public void printTradeInfo() {
		// TODO Auto-generated method stub
		Connection con = DBManager.getConnection();
		Statement stm = null;
		String sql = null;
		DecimalFormat df = new DecimalFormat("##.##");
		try {
			stm = con.createStatement();
			sql = "select acntId," + "     stkId," + "    round(pft_mny, 2) pft_mny, " + "    in_hand_qty, "
					+ "    round(pft_price, 2) pft_price, " + "    to_char(add_dt, 'yyyy-mm-dd hh24:mi:ss') add_dt "
					+ "from TradeHdr where acntId = '" + actId + "' order by stkid ";
			// log.info(sql);
			ResultSet rs = stm.executeQuery(sql);
			log.info("=======================================================================================");
			log.info("|AccountID\t|StockID\t|Pft_mny\t|InHandQty\t|PftPrice\t|TranDt|");
			while (rs.next()) {
				log.info("|" + rs.getString("acntId") + "\t|" + rs.getString("stkId") + "\t\t|"
						+ rs.getString("pft_mny") + "\t\t|" + rs.getInt("in_hand_qty") + "\t\t|"
						+ df.format(rs.getDouble("pft_price")) + "\t\t|" + rs.getString("add_dt") + "|");
				Statement stmdtl = con.createStatement();
				String sqldtl = "select stkid, seqnum, price, amount, to_char(dl_dt, 'yyyy-mm-dd hh24:mi:ss') dl_dt, buy_flg "
						+ "  from tradedtl where stkid ='" + rs.getString("stkId") + "' order by seqnum";
				// log.info(sql);

				ResultSet rsdtl = stmdtl.executeQuery(sqldtl);
				log.info("\tStockID\tSeqnum\tPrice\tAmount\tB/S\tsubTotal\tTranDt");
				while (rsdtl.next()) {
					log.info("\t" + rsdtl.getString("stkid") + "\t" + rsdtl.getInt("seqnum") + "\t"
							+ df.format(rsdtl.getDouble("price")) + "\t" + rsdtl.getInt("amount") + "\t"
							+ (rsdtl.getInt("buy_flg") > 0 ? "B" : "S") + "\t"
							+ df.format((rsdtl.getInt("buy_flg") > 0 ? -1 : 1) * rsdtl.getDouble("price")
									* rsdtl.getInt("amount"))
							+ "\t\t" + rsdtl.getString("dl_dt") + "\t");
				}
				rsdtl.close();
				stmdtl.close();
			}
			log.info("=======================================================================================");
			rs.close();
			stm.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean initAccount() {
		// TODO Auto-generated method stub
		Connection con = DBManager.getConnection();
		Statement stm = null;
		String sql = "delete from cashacnt where dft_acnt_flg = 1";
		try {
			stm = con.createStatement();
			stm.execute(sql);
			stm.close();
			stm = con.createStatement();
			sql = "insert into cashacnt values('testCashAct001',50000,0,0,8,0.5,1,sysdate)";
			stm.execute(sql);
			stm.close();
			con.close();
			loadAcnt(actId);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean hasStockInHandBeforeDays(Stock s, int days) {
		// TODO Auto-generated method stub
		log.info("now check if stock " + s.getName() + " in hand with price:" + s.getCur_pri() + " against CashAcount: "
				+ actId);

		boolean hasStockInHand = false;

		boolean sim_mode = actId.startsWith(STConstants.ACNT_SIM_PREFIX);
		
	    Timestamp tm = s.getDl_dt();
	    String startDate = null;
	    if (tm == null) {
	        startDate = "sysdate";
	    }
	    else {
	        startDate = "to_date('" + tm.toLocaleString() + "', 'yyyy-mm-dd HH24:MI:SS')";
	    }
	         
		hasStockInHand = hasStockInHandBeforeDays(s.getID(), sim_mode,startDate, days);
		
		return hasStockInHand;
	}
	
	public static boolean hasStockInHandBeforeDays(String stkId, boolean sim_mode, String startDate, int days) {
	   Connection con = DBManager.getConnection();
	    boolean hasStockInHand = false;
	    String like_clause = "";
	    double buyQty = 0;
	    double sellQty = 0;
	    
	    if (!sim_mode) {
	        like_clause = " like '" + STConstants.ACNT_TRADE_PREFIX + "%'";
	    }
	    else {
	        like_clause = " like '" + STConstants.ACNT_SIM_PREFIX + "%'";
	    }
	    
	    log.info("Now checking stock:" + stkId + "in hand, sim_mode:" + sim_mode + ", startDate:" + startDate + ", Days:" + days);
	    try {
	        String sql = "select d.buy_flg, d.amount "
	                   + "  from Tradedtl d "
	                   + " where d.stkId = '" + stkId + "'"
	                   + "   and d.dl_dt <=  " + startDate + " - " + days
	                   + "   and d.acntId " + like_clause
	                   + " order by seqnum desc";

	        log.info(sql);
	        Statement stm = con.createStatement();
	        ResultSet rs = stm.executeQuery(sql);
	        while (rs.next()) {
	            if(rs.getInt("buy_flg") > 0) {
	                buyQty = rs.getInt("amount");
	                sellQty -= buyQty;
	            }
	            else {
	                sellQty += rs.getInt("amount");
	            }
	            if (sellQty < 0) {
	                break;
	            }
	        }
	        
	        log.info("Stock:" + stkId + " sellQty:" + sellQty + ", hasStockInHand:" + hasStockInHand);
	        if (sellQty < 0) {
	            hasStockInHand = true;
	        }
	        rs.close();
	        stm.close();
	        con.close();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return hasStockInHand;
	}
	
	   public static boolean hasStockInHand(String stkId, boolean sim_mode) {
	       Connection con = DBManager.getConnection();
	        boolean hasStockInHand = false;
	        String like_clause = "";
	        double buyQty = 0;
	        double sellQty = 0;
	        
	        if (!sim_mode) {
	            like_clause = " like '" + STConstants.ACNT_TRADE_PREFIX + "%'";
	        }
	        else {
	            like_clause = " like '" + STConstants.ACNT_SIM_PREFIX + "%'";
	        }
	        
	        try {
	            String sql = "select d.buy_flg, d.amount "
	                       + "  from Tradedtl d "
	                       + " where d.stkId = '" + stkId + "'"
	                       + "   and d.acntId " + like_clause
	                       + " order by seqnum desc";

	            log.info(sql);
	            Statement stm = con.createStatement();
	            ResultSet rs = stm.executeQuery(sql);
	            while (rs.next()) {
	                if(rs.getInt("buy_flg") > 0) {
	                    buyQty = rs.getInt("amount");
	                    sellQty -= buyQty;
	                }
	                else {
	                    sellQty += rs.getInt("amount");
	                }
	                if (sellQty < 0) {
	                    break;
	                }
	            }
	            
	            log.info("Stock:" + stkId + " sellQty:" + sellQty + ", hasStockInHand:" + hasStockInHand);
	            if (sellQty < 0) {
	                hasStockInHand = true;
	            }
	            rs.close();
	            stm.close();
	            con.close();
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	        return hasStockInHand;
	    }

	@Override
	public double getInHandStockCostPrice(Stock s) {
		// TODO Auto-generated method stub
		log.info("get stock " + s.getName() + " in hand with current price:" + s.getCur_pri()
				+ " cost price against CashAcount: " + actId);

		Connection con = DBManager.getConnection();
		try {
			String sql = "select * from Tradedtl d  where d.stkId = '" + s.getID() + "'" + "  and d.acntId = '" + actId
					+ "' order by seqnum";

			log.info(sql);
			Statement stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);
			int inhandQty = 0;
			double costmny = 0.0;
			double costpri = 0.0;
			while (rs.next()) {
				if (rs.getInt("buy_flg") > 0) {
					inhandQty += rs.getInt("amount");
					costmny += rs.getInt("amount") * rs.getDouble("price");
				} else {
					inhandQty -= rs.getInt("amount");
					costmny -= rs.getInt("amount") * rs.getDouble("price");
				}

				if (inhandQty == 0) {
					if (costmny > 0) {
						log.info("Sole all stock, but lost money:" + costmny);
						costmny = 0;
					} else {
						log.info("Sole all stock, but gain money:" + -costmny);
						costmny = 0;
					}
				}
			}
			log.info("InhandQty:" + inhandQty + ", costmny:" + costmny);
			if (inhandQty > 0) {
				costpri = costmny / inhandQty;
				log.info("costpri:" + costpri);
			}
			rs.close();
			stm.close();
			con.close();
			return costpri;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public double getStockCostRatio(Stock s) {
		// TODO Auto-generated method stub
		log.info("check stock " + s.getName() + " in hand used money ratio against CashAcount: " + actId);

		Connection con = DBManager.getConnection();
		try {
			String sql = "select sum(case when buy_flg = 1 then 1 else -1 end * amount * price) costMny from Tradedtl d  where d.stkId = '"
					+ s.getID() + "'" + "  and d.acntId = '" + actId + "'";

			log.info(sql);
			Statement stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);

			double ratio = 0;
			if (rs.next()) {
				log.info("costMny:" + rs.getDouble("costMny") + ", initMny:" + initMny);

				if (rs.getDouble("costMny") > 0) {
					ratio = rs.getDouble("costMny") / initMny;
				}
				log.info("ratio:" + ratio);
			}

			rs.close();
			stm.close();
			con.close();
			return ratio;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public Double getLstBuyPri(Stock s) {
		// TODO Auto-generated method stub
		log.info("check stock " + s.getName() + " last buy price against CashAcount: " + actId);

		Connection con = DBManager.getConnection();
		try {
			String sql = "select * from Tradedtl d  where d.stkId = '" + s.getID() + "'" + "  and d.acntId = '" + actId
					+ "' order by seqnum desc";

			log.info(sql);
			Statement stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);

			Double lstbuypri = 0.0;
			if (rs.next()) {
				lstbuypri = rs.getDouble("price");
				log.info("get last buy price:" + lstbuypri);
			}
			rs.close();
			stm.close();
			con.close();
			return lstbuypri;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
}
