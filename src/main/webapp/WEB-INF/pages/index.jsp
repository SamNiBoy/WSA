<%@ page language="java" import="java.io.*,java.util.*, com.sn.reporter.WeChatReporter" pageEncoding="UTF-8"%>
    <%
               String path = request.getContextPath();
    %>
<%
out.print("hello, welcome, sam");
BufferedReader br = new BufferedReader(new InputStreamReader((ServletInputStream)request.getInputStream()));
String line = null;
String sb = new String();
while((line = br.readLine())!=null){
   sb+= line;
}
String xmlS = sb;
System.out.println(xmlS);
WeChatReporter wcr = new WeChatReporter();
System.out.println("after wechat create");
wcr.setWcMsg(xmlS);
String msg = wcr.getResponse();
out.print(msg);
%>
