def:
	@echo You need to run:
	@echo    ./runPurgeTest.sh -p your_port -c -d -l 
	@echo See
	@echo    ./runPurgeTest.sh -h 
	@echo for details

build:
	(cd ../../build-scripts; gmake )

test: build
	./runPurgeTest.sh -c -d -l -f -k -t

clean:
	rm -rf MAGIC_VDT_LOCATION ProbeConfig collector-for-test *~ *.validate *.count
