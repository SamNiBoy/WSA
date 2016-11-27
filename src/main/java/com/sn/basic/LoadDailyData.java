package com.sn.basic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;

public class LoadDailyData {

    static Logger log = Logger.getLogger(LoadDailyData.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        DBManager dbm;
        String stkId = "", fn = "";
        String path = "F:\\MFC\\WSA\\src\\main\\resources\\StockData";

        try {
            Connection con = DBManager.getConnection();
            Statement stm = null;
            con.setAutoCommit(false);
            stm = con.createStatement();

            File f = new File(path);
            File[] t = f.listFiles(); 

            for (int i=0 ; i<t.length; i++) {
                stkId = t[i].getName().substring(2, 8);
                   fn = t[i].getAbsolutePath();
                   LoadRest(stm, con, stkId, fn);
                   con.commit();
            }
            stm.close();
            con.close();
        } catch (Exception e) {
        }
        finally {
        }

    }

    static int LoadRest(Statement stm, Connection con, String stkID, String fn) throws IOException
    {
        int loadedCnt = 0;
        
        int i;
        FileReader fr = new FileReader(fn);
        BufferedReader br = new BufferedReader(fr);
        String sql = "";

        log.info("for Stk:" + stkID);
        String s = br.readLine();
        String rq = "";
        String yt_cls_pri = "";
        while (s != null) {
            String value[] = s.split(",");
            if (value.length > 1) {
//                for (int j=0; j<value.length; j++) {
//                	log.info("value["+j+"]:" + value[j]);
//                }
                rq = value[0].substring(6, 10) + "-" + value[0].substring(0, 2) + "-" + value[0].substring(3, 5);
                try {
                    sql = "delete from stkdlyinfo where id = '" + stkID + "' and dt = '" + rq + "'";
                    log.info(sql);
                    stm.execute(sql);
                    
                    sql = "insert into stkdlyinfo (id, dt, yt_cls_pri, td_cls_pri, td_opn_pri, td_hst_pri, td_lst_pri, dl_stk_num, dl_mny_num)"
                		+ " values('" + stkID + "','" + rq + "'," + (yt_cls_pri.length() > 0?yt_cls_pri:value[4]) +"," + value[4] + "," + value[1] + ","
                		+ value[2] + "," + value[3] + "," + value[5] + "," + value[6] + ")";
                    log.info(sql);
                	stm.executeUpdate(sql);
                }
                catch(Exception e) {
                	log.error(e.getMessage() + " continue...");
                }
                yt_cls_pri = value[4];
            }
            s = br.readLine();
            loadedCnt++;
        }
        
        br.close();
        log.info("Total loaded new records:" + loadedCnt + " for stock:" + stkID);
        return loadedCnt;
    }
}
