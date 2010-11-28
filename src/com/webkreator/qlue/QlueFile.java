package com.webkreator.qlue;

import java.io.File;

public class QlueFile extends File {
	
	public static final long serialVersionUID = 1L;
		
	private String contentType;

	public QlueFile(String pathname) {
		super(pathname);		
	}

	public String getContentType() {
		return contentType;
	}

	void setContentType(String contentType) {
		this.contentType = contentType;
	}	
}
