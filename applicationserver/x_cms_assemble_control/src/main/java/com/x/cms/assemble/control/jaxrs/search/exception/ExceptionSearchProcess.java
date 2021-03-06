package com.x.cms.assemble.control.jaxrs.search.exception;

import com.x.base.core.project.exception.PromptException;

public class ExceptionSearchProcess extends PromptException {

	private static final long serialVersionUID = 1859164370743532895L;

	public ExceptionSearchProcess( String message ) {
		super( message );
	}
	
	public ExceptionSearchProcess( Throwable e, String message ) {
		super( message, e );
	}
}
