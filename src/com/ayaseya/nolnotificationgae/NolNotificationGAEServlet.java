package com.ayaseya.nolnotificationgae;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class NolNotificationGAEServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/plain");
		resp.getWriter().println("Hello, world");
		
//		int count=0;
//		for(int i=0;i<1001;i++){
//			count++;
//			String regId="TestDevice_"+count;
//			Datastore.register(regId);
//		}
		
	}
}
