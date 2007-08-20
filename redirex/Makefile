
WEBDIR = /ftp/webtools/redirex

duh:
	@echo Please specify a valid make target.
	
tar:
	rm -f redirex.tar.gz
	tar cfvz redirex.tar.gz redirex redirex.conf redirex_test.conf \
		 redirex.html redirex.png log.doc Makefile
		 
lint:
	perl -c redirex
	weblint redirex.html

test:
	perl redirex -c redirex_test.conf

publish:	tar
	cp -p redirex.html $(WEBDIR)/index.html
	cp -p redirex.tar.gz redirex.png $(WEBDIR)
