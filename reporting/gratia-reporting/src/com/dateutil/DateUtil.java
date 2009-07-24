package com.dateutil;

import java.util.*;
import java.text.*;

//DateUtil Class to return missing dates
public class DateUtil {

	public String[] getDatesWithNoData(String start_Date,String end_Date, String interval) throws ParseException {

		DateFormat formatterStart ;
		Date dateStart ;
		formatterStart = new SimpleDateFormat("MM/dd/yyyy");
		dateStart = (Date)formatterStart.parse(start_Date);
		Calendar calStart=Calendar.getInstance();
		calStart.setTime(dateStart);


		DateFormat formatterEnd ;
		Date dateEnd ;
		formatterEnd = new SimpleDateFormat("MM/dd/yyyy");
		dateEnd = (Date)formatterEnd.parse(end_Date);
		Calendar calEnd=Calendar.getInstance();
		calEnd.setTime(dateEnd);

		System.out.println("Today is " +dateEnd );

		Calendar start = calStart;
		Calendar end = calEnd;


		Calendar c2 = Calendar.getInstance();
		ArrayList array = new ArrayList();
		if(interval.equals("M")){
			for(Calendar c=start ; c.compareTo(end)<=0 ; c.add(Calendar.MONTH, 1) ) {
			c2.setTimeInMillis(c.getTimeInMillis());
			String strDate = (c2.get(Calendar.MONTH)+1) + "/" +  c2.get(Calendar.DAY_OF_MONTH) + "/" + c2.get(Calendar.YEAR);
			array.add(strDate);
			}
		}
		if(interval.equals("D")) {
			for(Calendar c=start ; c.compareTo(end)<=0 ; c.add(Calendar.DATE, 1) ) {
			c2.setTimeInMillis(c.getTimeInMillis());
			String strDate = (c2.get(Calendar.MONTH)+1) + "/" +  c2.get(Calendar.DAY_OF_MONTH) + "/" + c2.get(Calendar.YEAR);
			array.add(strDate);
			}
		}
    return (String[])array.toArray(new String[0]);
}

	public String[] getDateRange(String start_Date,String end_Date, String interval) {

		try {
			return getDatesWithNoData(start_Date,end_Date,interval);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return new String[0];
	}

public static void main(String s[]) throws Exception {
	DateUtil util = new DateUtil();
	String result[] = util.getDatesWithNoData("05/10/2007", "05/20/2007", "D");
	for(int i=0;i<result.length;i++) {
		System.out.println(result[i]);
	}

}

}
