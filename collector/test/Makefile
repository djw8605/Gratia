def:
	@echo You need to run:
	@echo    ./runPurgeTest.sh -p your_port -c -d -l 
	@echo See
	@echo    ./runPurgeTest.sh -h 
	@echo for details

build:
	(cd ../../build-scripts; gmake all testtimeout )

test: build
	./runPurgeTest.sh -c -d -l -f -k -m -t

# Target to use when 'just' wanting to re-run the test without rebuilding nor re-installing the collector
onlytest:  
	./runPurgeTest.sh -d -l -f -k -m -t


clean:
	rm -rf MAGIC_VDT_LOCATION ProbeConfig collector-for-test *~ *.validate *.count
